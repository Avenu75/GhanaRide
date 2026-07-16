package com.ghanaride.controller;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles ALL booking operations:
 *
 * PUBLIC (no login):
 * - /rides Browse available trips
 * - /rides/search Search with filters
 *
 * AUTHENTICATED:
 * - /dashboard Passenger dashboard (role-aware redirect)
 * - /booking/{tripId} Show booking confirmation page
 * - POST /booking/{tripId} Create a booking
 * - /booking/receipt/{id} View receipt
 * - /my-bookings View all my bookings
 * - POST /booking/{id}/cancel Cancel a booking
 * - /boarding-pass/{bookingId} QR boarding pass
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TripService tripService;
    private final UserService userService;
    private final SeatService seatService;
    private final WalletService walletService;
    private final NotificationService notificationService;

    // Ghana cities — single source of truth
    private static final List<String> LOCATIONS = List.of(
        "Accra", "Cape Coast", "Kumasi", "Takoradi",
        "Tamale", "Sunyani", "Ho", "Koforidua",
        "Tema", "Winneba", "Bolgatanga", "Wa",
        "Techiman", "Obuasi", "Kasoa"
    );

    // =========================================================
    // PASSENGER DASHBOARD
    // Role-aware: redirects drivers/admins to their dashboards
    // =========================================================

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String date,
            Principal principal,
            Model model
    ) {
        User currentUser = userService.getCurrentUser(principal);

        // Role-based redirect
        switch (currentUser.getRole()) {
            case DRIVER -> { return "redirect:/driver/dashboard"; }
            case ADMIN -> { return "redirect:/admin/dashboard"; }
            case COMPANY -> { return "redirect:/company/dashboard"; }
            default -> { /* continue to passenger dashboard */ }
        }

        boolean isSearch = (from != null && !from.isBlank()) || (to != null && !to.isBlank());

        List<Trip> trips;
        if (isSearch) {
            trips = tripService.searchAvailableTrips(from, to, date);
            model.addAttribute("searchFrom", from);
            model.addAttribute("searchTo", to);
            model.addAttribute("searchDate", date);
        } else {
            // Load trips for passenger dashboard
            // Show APPROVED + FULL so users see full ones as unavailable (not vanished)
            trips = tripService.findApprovedAndFullTrips();
        }

        // Always set isSearchResult — even on plain dashboard visit —
        // so th:if/ternary checks in template never see null
        model.addAttribute("isSearchResult", isSearch);

        // Passenger's booking stats
        List<Booking> myBookings = bookingService.findByUserWithDetails(currentUser);

        long upcomingCount = myBookings.stream()
            .filter(b -> b != null
                && b.getStatus() != BookingStatus.CANCELLED
                && b.getTrip() != null
                && b.getTrip().getDepartureTime() != null
                && b.getTrip().getDepartureTime().isAfter(LocalDateTime.now()))
            .count();

        long completedCount = myBookings.stream()
            .filter(b -> b != null
                && b.getTrip() != null
                && b.getTrip().getStatus() == TripStatus.COMPLETED)
            .count();

        Wallet wallet = walletService.getOrCreateWallet(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trips", trips);
        model.addAttribute("locations", LOCATIONS);
        model.addAttribute("myBookings", myBookings);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("wallet", wallet);
        model.addAttribute("pageTitle", "Dashboard — GhanaRide");

        return "dashboard";
    }

    // =========================================================
    // PUBLIC: BROWSE AVAILABLE RIDES
    // No login required — improves conversion
    // =========================================================

    @GetMapping("/rides")
    public String browseRides(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String date,
            Model model
    ) {
        List<Trip> trips;

        boolean isSearch = (from != null && !from.isBlank()) || (to != null && !to.isBlank());

        if (isSearch) {
            trips = tripService.searchAvailableTrips(from, to, date);
            model.addAttribute("searchFrom", from);
            model.addAttribute("searchTo", to);
            model.addAttribute("searchDate", date);
        } else {
            trips = tripService.findAllApprovedUpcoming();
        }

        model.addAttribute("isSearchResult", isSearch);
        model.addAttribute("trips", trips);
        model.addAttribute("locations", LOCATIONS);
        model.addAttribute("pageTitle",
            (isSearch && from != null && to != null)
                ? "Rides: " + from + " → " + to + " — GhanaRide"
                : "Find Available Rides — GhanaRide");
        model.addAttribute("pageDescription",
            "Browse and book affordable intercity rides across Ghana. " +
            "Accra, Kumasi, Cape Coast, Tamale, Takoradi and more.");
        model.addAttribute("pageUrl", "https://ghanaride.me/rides");

        return "rides";
    }

    // =========================================================
    // BOOKING CONFIRMATION PAGE — GET
    // Shows trip details before user confirms booking
    // =========================================================

    @GetMapping("/booking/{tripId}")
    @PreAuthorize("isAuthenticated()")
    public String showBookingPage(
            @PathVariable Long tripId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Trip> tripOpt = tripService.findById(tripId);

        if (tripOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Trip not found.");
            return "redirect:/dashboard";
        }

        Trip trip = tripOpt.get();

        if (trip.getStatus() == null || trip.getStatus() != TripStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("error", "This trip is no longer available.");
            return "redirect:/dashboard";
        }

        if (trip.getAvailableSeats() == null || trip.getAvailableSeats() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Sorry, this trip is fully booked.");
            return "redirect:/dashboard";
        }

        if (trip.getDepartureTime() == null || trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "This trip has already departed.");
            return "redirect:/dashboard";
        }

        User currentUser = userService.getCurrentUser(principal);

        // v3.2 PROFILE COMPLETION ENFORCEMENT – block booking if phone missing
        if (!userService.isProfileComplete(currentUser)) {
            redirectAttributes.addFlashAttribute("error",
                "Please complete your profile – a valid Ghana phone number is required before booking. " +
                "Google accounts need to add a phone number once.");
            redirectAttributes.addFlashAttribute("highlight", "phoneNumber");
            return "redirect:/profile?complete=booking&tripId=" + tripId;
        }

        // Driver can't book their own trip
        if (trip.getDriver() != null && trip.getDriver().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "You cannot book your own trip.");
            return "redirect:/dashboard";
        }

        // Check if already booked
        if (bookingService.hasActiveBookingForTrip(currentUser, trip)) {
            redirectAttributes.addFlashAttribute("error", "You already have an active booking for this trip.");
            return "redirect:/dashboard";
        }

        // Generate seat map for this trip
        List<SeatMap> seatMap = seatService.ensureSeatMap(trip);

        model.addAttribute("trip", trip);
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("wallet", walletService.getOrCreateWallet(currentUser));
        model.addAttribute("pageTitle", "Book " + trip.getFromLocation() + " → " + trip.getToLocation() + " — GhanaRide");

        return "booking-confirm";
    }

    // =========================================================
    // CREATE BOOKING — POST
    // Supports wallet, card (Paystack), and cash payments
    // =========================================================

    @PostMapping("/booking/{tripId}")
    @PreAuthorize("isAuthenticated()")
    public String createBooking(
            @PathVariable Long tripId,
            @RequestParam(required = false) String seatNumber,
            @RequestParam(required = false) String paymentMethod, // WALLET, PAYSTACK, CASH
            @RequestParam(required = false) String passengerName,   // For RELATIVE booking
            @RequestParam(required = false) String passengerPhone,  // For RELATIVE booking
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);

        try {
            Booking booking = bookingService.createBooking(
                currentUser, tripId, seatNumber, paymentMethod, passengerName, passengerPhone
            );

            log.info("Booking created: {} for user {} on trip {}",
                booking.getBookingReference(), currentUser.getEmail(), tripId);

            redirectAttributes.addFlashAttribute("success",
                "Booking confirmed! Your reference: " + booking.getBookingReference());

            return "redirect:/booking/receipt/" + booking.getId();

        } catch (IllegalStateException e) {
            // Business rule violation
            log.warn("Booking rejected for trip {}: {}", tripId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/" + tripId;

        } catch (Exception e) {
            log.error("Booking failed for trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error",
                "Booking failed. Please try again or contact support@ghanaride.me");
            return "redirect:/booking/" + tripId;
        }
    }

    // =========================================================
    // RECEIPT / BOOKING DETAILS
    // =========================================================

    @GetMapping("/booking/receipt/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewReceipt(
            @PathVariable Long id,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt = bookingService.findById(id);

        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Booking not found.");
            return "redirect:/my-bookings";
        }

        Booking booking = bookingOpt.get();

        // Ownership check
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to view booking {} owned by {}",
                currentUser.getEmail(), id, booking.getUser().getEmail());
            redirectAttributes.addFlashAttribute("error", "You can only view your own bookings.");
            return "redirect:/my-bookings";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "Receipt " + booking.getBookingReference() + " — GhanaRide");

        return "receipt";
    }

    // =========================================================
    // MY BOOKINGS LIST
    // =========================================================

    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public String myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal,
            Model model
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookingDate"));

        Page<Booking> bookings = bookingService.findByUserPaginated(currentUser, pageable);

        model.addAttribute("bookings", bookings);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "My Bookings — GhanaRide");

        return "my-bookings";
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    @PostMapping("/booking/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public String cancelBooking(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt = bookingService.findById(id);

        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Booking not found.");
            return "redirect:/my-bookings";
        }

        Booking booking = bookingOpt.get();

        // Ownership check
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to cancel booking {} owned by {}",
                currentUser.getEmail(), id, booking.getUser().getEmail());
            redirectAttributes.addFlashAttribute("error", "You can only cancel your own bookings.");
            return "redirect:/my-bookings";
        }

        // Check cancellation eligibility
        if (!bookingService.canCancelBooking(booking)) {
            redirectAttributes.addFlashAttribute("error",
                "This booking can no longer be cancelled. " +
                "Cancellations must be made at least 2 hours before departure. " +
                "Please contact support for help.");
            return "redirect:/my-bookings";
        }

        try {
            bookingService.cancelBooking(id);
            log.info("Booking {} cancelled by user {}", id, currentUser.getEmail());

            redirectAttributes.addFlashAttribute("success",
                "Booking cancelled successfully. " +
                "Your seat has been released. " +
                "If you paid online, a refund will be processed within 3-5 business days.");

        } catch (IllegalStateException e) {
            log.warn("Cancel rejected for booking {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Cancel failed for booking {}", id, e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to cancel booking. Please contact support at support@ghanaride.me");
        }

        return "redirect:/my-bookings";
    }

    // =========================================================
    // QR CODE BOARDING PASS
    // =========================================================

    @GetMapping("/boarding-pass/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public String boardingPass(
            @PathVariable Long bookingId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Booking not found.");
            return "redirect:/my-bookings";
        }

        Booking booking = bookingOpt.get();

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Access denied.");
            return "redirect:/my-bookings";
        }

        // Only show boarding pass for ACTIVE/CONFIRMED bookings
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("error", "This booking has been cancelled.");
            return "redirect:/my-bookings";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "Boarding Pass " + booking.getBookingReference() + " — GhanaRide");

        return "boarding-pass";
    }
}