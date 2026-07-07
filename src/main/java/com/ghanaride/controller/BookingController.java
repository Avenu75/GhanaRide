package com.ghanaride.controller;

import com.ghanaride.entity.*;
import com.ghanaride.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * - /rides          Browse available trips
 * - /rides/{id}     View trip details
 *
 * AUTHENTICATED:
 * - /dashboard              Passenger dashboard
 * - /booking/{tripId}       Show booking confirmation page
 * - POST /booking/{tripId}  Create a booking
 * - /booking/receipt/{id}   View receipt
 * - /my-bookings            View all my bookings
 * - POST /booking/{id}/cancel  Cancel a booking
 *
 * Replaces: UserController (merged) +
 *           BookingController (previous draft)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TripService tripService;
    private final UserService userService;

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
        User currentUser =
                userService.getCurrentUser(principal);

        // Role-based redirect
        switch (currentUser.getRole()) {
            case DRIVER  -> { return "redirect:/driver/dashboard";  }
            case ADMIN   -> { return "redirect:/admin/dashboard";   }
            case COMPANY -> { return "redirect:/company/dashboard"; }
            default -> { /* continue to passenger dashboard */ }
        }

        boolean isSearch = (from != null && !from.isBlank())
                || (to != null && !to.isBlank());

        List<Trip> trips;
        if (isSearch) {
            trips = tripService.searchAvailableTrips(from, to, date);
            model.addAttribute("searchFrom", from);
            model.addAttribute("searchTo",   to);
            model.addAttribute("searchDate", date);
        } else {
            // Load trips for passenger dashboard
            // Show APPROVED + FULL so users see full ones
            // as unavailable (not just vanish)
            trips = tripService.findApprovedAndFullTrips();
        }

        // Always set isSearchResult — even on a plain dashboard
        // visit — so th:if/ternary checks in the template never
        // see a null value (SpEL can't coerce null to boolean).
        model.addAttribute("isSearchResult", isSearch);

        // Passenger's booking stats
        // Uses findByUserWithDetails() (fetch-joins trip/driver/car)
        // since this list is also exposed to the template as
        // "myBookings" and touches trip fields in the stream below.
        List<Booking> myBookings =
                bookingService.findByUserWithDetails(currentUser);

        long upcomingCount = myBookings.stream()
                .filter(b ->
                        b.getStatus() != BookingStatus.CANCELLED &&
                                b.getTrip().getDepartureTime()
                                        .isAfter(LocalDateTime.now()))
                .count();

        long completedCount = myBookings.stream()
                .filter(b ->
                        b.getTrip().getStatus() ==
                                TripStatus.COMPLETED)
                .count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trips", trips);
        model.addAttribute("locations", LOCATIONS);
        model.addAttribute("myBookings", myBookings);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pageTitle",
                "Dashboard — GhanaRide");

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

        boolean isSearch = (from != null && !from.isBlank())
                || (to != null && !to.isBlank());

        if (isSearch) {
            trips = tripService.searchAvailableTrips(
                    from, to, date
            );
            model.addAttribute("searchFrom", from);
            model.addAttribute("searchTo",   to);
            model.addAttribute("searchDate", date);
        } else {
            trips = tripService.findAllApprovedUpcoming();
        }

        // Same fix as /dashboard: always provide a real boolean.
        model.addAttribute("isSearchResult", isSearch);

        model.addAttribute("trips", trips);
        model.addAttribute("locations", LOCATIONS);
        model.addAttribute("pageTitle",
                (isSearch && from != null && to != null)
                        ? "Rides: " + from + " → " + to +
                          " — GhanaRide"
                        : "Find Available Rides — GhanaRide"
        );
        model.addAttribute("pageDescription",
                "Browse and book affordable intercity rides " +
                        "across Ghana. Accra, Kumasi, Cape Coast, " +
                        "Tamale, Takoradi and more.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/rides");

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
        Optional<Trip> tripOpt =
                tripService.findById(tripId);

        // Trip must exist, be APPROVED, and have seats
        if (tripOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Trip not found.");
            return "redirect:/dashboard";
        }

        Trip trip = tripOpt.get();

        if (trip.getStatus() != TripStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("error",
                    "This trip is no longer available.");
            return "redirect:/dashboard";
        }

        if (trip.getAvailableSeats() <= 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Sorry, this trip is fully booked.");
            return "redirect:/dashboard";
        }

        // Trip must not have departed
        if (trip.getDepartureTime()
                .isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error",
                    "This trip has already departed.");
            return "redirect:/dashboard";
        }

        User currentUser =
                userService.getCurrentUser(principal);

        // Driver can't book their own trip
        if (trip.getDriver() != null &&
                trip.getDriver().getId()
                        .equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "You cannot book your own trip.");
            return "redirect:/dashboard";
        }

        // Check if already booked
        if (bookingService.hasUserBookedTrip(
                currentUser, trip)) {
            redirectAttributes.addFlashAttribute("error",
                    "You have already booked this trip.");
            return "redirect:/my-bookings";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trip", trip);
        model.addAttribute("pageTitle",
                "Confirm Booking: " + trip.getFromLocation() +
                        " → " + trip.getToLocation() +
                        " — GhanaRide");

        return "booking-confirm";
    }

    // =========================================================
    // CREATE BOOKING — POST
    // Supports SELF booking and RELATIVE booking
    // =========================================================
    @PostMapping("/booking/{tripId}")
    @PreAuthorize("isAuthenticated()")
    public String createBooking(
            @PathVariable Long tripId,
            @RequestParam String bookingType,
            @RequestParam(required = false) String passengerName,
            @RequestParam(required = false) String passengerPhone,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Trip> tripOpt =
                tripService.findById(tripId);

        if (tripOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Trip not found.");
            return "redirect:/dashboard";
        }

        Trip trip = tripOpt.get();
        User currentUser =
                userService.getCurrentUser(principal);

        // Re-validate trip availability
        // (may have changed between GET and POST)
        if (trip.getStatus() != TripStatus.APPROVED ||
                trip.getAvailableSeats() <= 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Sorry, this trip is no longer available.");
            return "redirect:/dashboard";
        }

        // Re-validate departure time
        if (trip.getDepartureTime()
                .isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error",
                    "This trip has already departed.");
            return "redirect:/dashboard";
        }

        // Check not already booked
        if (bookingService.hasUserBookedTrip(
                currentUser, trip)) {
            redirectAttributes.addFlashAttribute("error",
                    "You have already booked this trip.");
            return "redirect:/my-bookings";
        }

        // Parse booking type safely
        BookingType type;
        try {
            type = "relative".equalsIgnoreCase(bookingType)
                    ? BookingType.RELATIVE
                    : BookingType.SELF;
        } catch (Exception e) {
            type = BookingType.SELF;
        }

        // Validate relative passenger details
        if (type == BookingType.RELATIVE) {
            if (passengerName == null ||
                    passengerName.isBlank()) {
                redirectAttributes.addFlashAttribute("error",
                        "Passenger name is required " +
                                "for relative booking.");
                return "redirect:/booking/" + tripId;
            }
            if (passengerPhone == null ||
                    passengerPhone.isBlank()) {
                redirectAttributes.addFlashAttribute("error",
                        "Passenger phone is required " +
                                "for relative booking.");
                return "redirect:/booking/" + tripId;
            }
            // Validate phone format
            if (!passengerPhone.matches(
                    "^[\\+]?[0-9]{10,13}$")) {
                redirectAttributes.addFlashAttribute("error",
                        "Please enter a valid passenger " +
                                "phone number.");
                return "redirect:/booking/" + tripId;
            }
            // Sanitize
            passengerName  = passengerName.trim();
            passengerPhone = passengerPhone.trim();
        }

        try {
            Booking booking = bookingService.createBooking(
                    currentUser,
                    trip,
                    type,
                    type == BookingType.RELATIVE
                            ? passengerName : null,
                    type == BookingType.RELATIVE
                            ? passengerPhone : null
            );

            log.info(
                    "Booking created: id={} user={} trip={} " +
                            "type={} seats={}",
                    booking.getId(),
                    currentUser.getEmail(),
                    tripId,
                    type,
                    1
            );

            return "redirect:/payment/" + booking.getId();

        } catch (IllegalStateException e) {
            // Business rule violations
            // (no seats, already booked, etc.)
            log.warn(
                    "Booking rejected: user={} trip={} " +
                            "reason={}",
                    currentUser.getEmail(),
                    tripId, e.getMessage()
            );
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage());
            return "redirect:/dashboard";

        } catch (Exception e) {
            log.error(
                    "Booking creation failed: user={} trip={}",
                    currentUser.getEmail(), tripId, e
            );
            redirectAttributes.addFlashAttribute("error",
                    "Failed to create booking. " +
                            "Please try again.");
            return "redirect:/dashboard";
        }
    }

    // =========================================================
    // VIEW RECEIPT
    // =========================================================
    @GetMapping("/booking/receipt/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public String viewReceipt(
            @PathVariable Long bookingId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        // Uses findByIdWithDetails() (fetch-joins trip/driver/car)
        // since receipt.html reads booking.trip.driver.fullName,
        // booking.trip.driver.phoneNumber, booking.trip.car.carBrand.
        // Do NOT switch back to bookingService.findById(...) — that
        // returns lazy proxies and will throw
        // LazyInitializationException during view rendering.
        Optional<Booking> bookingOpt =
                bookingService.findByIdWithDetails(bookingId);

        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Booking not found.");
            return "redirect:/my-bookings";
        }

        Booking booking = bookingOpt.get();
        User currentUser =
                userService.getCurrentUser(principal);

        // Ownership check
        // (admin can view any receipt)
        boolean isOwner = booking.getUser().getId()
                .equals(currentUser.getId());
        boolean isAdmin =
                currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn(
                    "User {} attempted to view receipt " +
                            "for booking {} owned by {}",
                    currentUser.getEmail(),
                    bookingId,
                    booking.getUser().getEmail()
            );
            redirectAttributes.addFlashAttribute("error",
                    "Access denied.");
            return "redirect:/my-bookings";
        }

        // Whether cancellation is still allowed
        boolean canCancel =
                bookingService.canCancelBooking(booking);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("booking", booking);
        model.addAttribute("canCancel", canCancel);
        model.addAttribute("pageTitle",
                "Booking Receipt #" + bookingId +
                        " — GhanaRide");

        return "receipt";
    }

    // =========================================================
    // MY BOOKINGS
    // =========================================================
    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public String myBookings(
            Principal principal,
            Model model
    ) {
        User currentUser =
                userService.getCurrentUser(principal);

        // Uses findByUserWithDetails() (fetch-joins trip/driver/car)
        // since my-bookings.html reads booking.trip.driver.fullName,
        // booking.trip.fromLocation, booking.trip.departureTime, etc.
        // Do NOT switch back to bookingService.findByUser(...) —
        // that returns lazy proxies and will throw
        // LazyInitializationException during view rendering (this is
        // exactly what caused the /my-bookings 500).
        List<Booking> allBookings =
                bookingService.findByUserWithDetails(currentUser);

        // Split into upcoming and past for better UX
        List<Booking> upcomingBookings = allBookings.stream()
                .filter(b ->
                        b.getStatus() != BookingStatus.CANCELLED &&
                                b.getTrip().getDepartureTime()
                                        .isAfter(LocalDateTime.now()))
                .toList();

        List<Booking> pastBookings = allBookings.stream()
                .filter(b ->
                        b.getStatus() == BookingStatus.CANCELLED ||
                                b.getTrip().getDepartureTime()
                                        .isBefore(LocalDateTime.now()))
                .toList();

        // Pre-calculate cancel eligibility for each booking
        // (avoids passing service to template)
        Map<Long, Boolean> cancelEligibility =
                new java.util.HashMap<>();
        for (Booking b : upcomingBookings) {
            cancelEligibility.put(
                    b.getId(),
                    bookingService.canCancelBooking(b)
            );
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("upcomingBookings",
                upcomingBookings);
        model.addAttribute("pastBookings", pastBookings);
        model.addAttribute("cancelEligibility",
                cancelEligibility);
        model.addAttribute("totalBookings",
                allBookings.size());
        model.addAttribute("pageTitle",
                "My Bookings — GhanaRide");
        model.addAttribute("pageDescription",
                "View and manage all your GhanaRide bookings.");

        return "my-bookings";
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================
    @PostMapping("/booking/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public String cancelBooking(
            @PathVariable Long bookingId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser =
                userService.getCurrentUser(principal);

        Optional<Booking> bookingOpt =
                bookingService.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Booking not found.");
            return "redirect:/my-bookings";
        }

        Booking booking = bookingOpt.get();

        // Ownership check
        if (!booking.getUser().getId()
                .equals(currentUser.getId())) {
            log.warn(
                    "User {} attempted to cancel booking {} " +
                            "owned by {}",
                    currentUser.getEmail(),
                    bookingId,
                    booking.getUser().getEmail()
            );
            redirectAttributes.addFlashAttribute("error",
                    "You can only cancel your own bookings.");
            return "redirect:/my-bookings";
        }

        // Check cancellation eligibility
        if (!bookingService.canCancelBooking(booking)) {
            redirectAttributes.addFlashAttribute("error",
                    "This booking can no longer be cancelled. " +
                            "Cancellations must be made at least " +
                            "2 hours before departure. " +
                            "Please contact support for help.");
            return "redirect:/my-bookings";
        }

        try {
            bookingService.cancelBooking(bookingId);

            log.info(
                    "Booking {} cancelled by user {}",
                    bookingId,
                    currentUser.getEmail()
            );

            redirectAttributes.addFlashAttribute("success",
                    "Booking cancelled successfully. " +
                            "Your seat has been released. " +
                            "If you paid online, a refund will be " +
                            "processed within 3-5 business days.");

        } catch (IllegalStateException e) {
            // Business rule violation
            log.warn(
                    "Cancel rejected for booking {}: {}",
                    bookingId, e.getMessage()
            );
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage());
        } catch (Exception e) {
            log.error(
                    "Cancel failed for booking {}",
                    bookingId, e
            );
            redirectAttributes.addFlashAttribute("error",
                    "Failed to cancel booking. " +
                            "Please contact support at " +
                            "support@ghanaride.me");
        }

        return "redirect:/my-bookings";
    }
}