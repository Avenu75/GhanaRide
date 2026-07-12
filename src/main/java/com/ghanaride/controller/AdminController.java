package com.ghanaride.controller;

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
import com.ghanaride.repository.UserRepository;
import java.security.Principal;

/**
 * Admin portal controller.
 * Full platform management: users, trips, bookings, cars.
 *
 * ALL endpoints require ADMIN role — enforced at both
 * URL level (SecurityConfig) and method level (@PreAuthorize)
 * for defence in depth.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // Class-level — applies to all methods
public class AdminController {

    private final UserService userService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final CarService carService;

    private static final int PAGE_SIZE = 20;

    // =========================================================
    // DASHBOARD
    // =========================================================
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("currentUser",
                userService.getCurrentUser(principal));

        // Stats cards
        model.addAttribute("totalUsers",
                userService.countByRole(Role.USER));
        model.addAttribute("totalDrivers",
                userService.countByRole(Role.DRIVER));
        model.addAttribute("totalCompanies",
                userService.countByRole(Role.COMPANY));
        model.addAttribute("totalTrips",
                tripService.countAll());
        model.addAttribute("pendingTrips",
                tripService.countByStatus(TripStatus.PENDING));
        model.addAttribute("approvedTrips",
                tripService.countByStatus(TripStatus.APPROVED));
        model.addAttribute("completedTrips",
                tripService.countByStatus(TripStatus.COMPLETED));
        model.addAttribute("totalBookings",
                bookingService.countAll());

        // Recent activity for dashboard feed
        model.addAttribute("recentTrips",
                tripService.findRecentTrips(10));
        model.addAttribute("recentBookings",
                bookingService.findRecentBookings(10));

        model.addAttribute("pageTitle",
                "Admin Dashboard \u2014 GhanaRide");

        return "admin/dashboard";
    }

    // =========================================================
    // TRIP MANAGEMENT
    // =========================================================
    @GetMapping("/trips")
    public String manageTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        model.addAttribute("currentUser",
                userService.getCurrentUser(principal));

        // NOTE: Sort is omitted from PageRequest because the
        // repository queries (findAllWithDetails / findByStatusWithDetails)
        // already specify ORDER BY t.createdAt DESC internally.
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        // Filter by status if provided
        Page<Trip> tripsPage;
        if (status != null && !status.isBlank()) {
            try {
                TripStatus tripStatus = TripStatus.valueOf(
                        status.toUpperCase()
                );
                // Use the JOIN FETCH version to avoid LazyInitializationException
                // on trip.driver.fullName when Thymeleaf renders admin/trips.html
                tripsPage = tripService.findByStatusWithDetails(
                        tripStatus, pageable
                );
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                // Invalid status filter — fall back to all trips with driver eager loaded
                tripsPage = tripService.findAllTripsWithDetails(pageable);
            }
        } else {
            // All trips, with driver/company/car eagerly fetched
            tripsPage = tripService.findAllTripsWithDetails(pageable);
        }

        model.addAttribute("tripsPage", tripsPage);
        model.addAttribute("trips", tripsPage.getContent());
        model.addAttribute("totalPages", tripsPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle",
                "Manage Trips \u2014 Admin | GhanaRide");

        return "admin/trips";
    }

    @PostMapping("/trips/{tripId}/approve")
    public String approveTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.approveTrip(tripId);
            log.info("Admin {} approved trip {}",
                    getCurrentUserEmail(principal), tripId);
            redirectAttributes.addFlashAttribute("success",
                    "Trip #" + tripId + " approved successfully. " +
                            "It is now visible to passengers.");
        } catch (Exception e) {
            log.error("Error approving trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to approve trip. Please try again.");
        }
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/reject")
    public String rejectTrip(
            @PathVariable Long tripId,
            @RequestParam(required = false) String rejectReason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Pass reason to service so it can notify driver
            tripService.rejectTrip(tripId, rejectReason);
            log.info("Admin {} rejected trip {} reason: {}",
                    getCurrentUserEmail(principal),
                    tripId, rejectReason);
            redirectAttributes.addFlashAttribute("success",
                    "Trip #" + tripId + " rejected. " +
                            "The driver has been notified.");
        } catch (Exception e) {
            log.error("Error rejecting trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to reject trip. Please try again.");
        }
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/fail")
    public String markTripFailed(
            @PathVariable Long tripId,
            @RequestParam String failReason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User currentUser =
                    userService.getCurrentUser(principal);
            tripService.markAsFailedToShow(
                    tripId, failReason, currentUser
            );
            log.warn("Admin {} marked trip {} as failed: {}",
                    currentUser.getEmail(), tripId, failReason);
            redirectAttributes.addFlashAttribute("success",
                    "Trip #" + tripId + " marked as failed to show.");
        } catch (Exception e) {
            log.error("Error marking trip {} as failed",
                    tripId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to update trip status. Please try again.");
        }
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/delete")
    public String deleteTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.deleteTrip(tripId);
            log.warn("Admin {} DELETED trip {}",
                    getCurrentUserEmail(principal), tripId);
            redirectAttributes.addFlashAttribute("success",
                    "Trip #" + tripId + " deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete trip: it may have active bookings. " +
                            "Cancel all bookings first.");
        }
        return "redirect:/admin/trips";
    }

    // =========================================================
    // USER MANAGEMENT
    // =========================================================
    @GetMapping("/users")
    public String manageUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Principal principal,
            Model model
    ) {
        model.addAttribute("currentUser",
                userService.getCurrentUser(principal));

        Pageable pageable = PageRequest.of(
                page, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<User> usersPage;
        if (search != null && !search.isBlank()) {
            usersPage = userService.searchUsers(
                    search.trim(), pageable
            );
            model.addAttribute("search", search);
        } else {
            usersPage = userService.findAllUsers(pageable);
        }

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("totalPages",
                usersPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle",
                "Manage Users \u2014 Admin | GhanaRide");

        return "admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(
            @PathVariable Long userId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User currentUser =
                    userService.getCurrentUser(principal);

            // Prevent self-deletion
            if (currentUser.getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error",
                        "You cannot delete your own admin account.");
                return "redirect:/admin/users";
            }

            // Get user info before deleting (for log)
            User targetUser = userService.findById(userId)
                    .orElse(null);
            String targetEmail = targetUser != null ?
                    targetUser.getEmail() : "unknown";

            userService.deleteUser(userId);

            log.warn("Admin {} DELETED user {} ({})",
                    currentUser.getEmail(), userId, targetEmail);

            redirectAttributes.addFlashAttribute("success",
                    "User deleted successfully.");

        } catch (Exception e) {
            log.error("Error deleting user {}", userId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete user. They may have active " +
                            "trips or bookings. Please resolve those first.");
        }
        return "redirect:/admin/users";
    }

    // =========================================================
    // BOOKING MANAGEMENT
    // =========================================================
    @GetMapping("/bookings")
    public String manageBookings(
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        model.addAttribute("currentUser",
                userService.getCurrentUser(principal));

        Pageable pageable = PageRequest.of(
                page, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "bookingDate")
        );

        Page<Booking> bookingsPage =
                bookingService.findAllBookings(pageable);

        model.addAttribute("bookingsPage", bookingsPage);
        model.addAttribute("bookings", bookingsPage.getContent());
        model.addAttribute("totalPages",
                bookingsPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle",
                "Manage Bookings \u2014 Admin | GhanaRide");

        return "admin/bookings";
    }

    @PostMapping("/bookings/{bookingId}/delete")
    public String deleteBooking(
            @PathVariable Long bookingId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bookingService.deleteBooking(bookingId);
            log.warn("Admin {} DELETED booking {}",
                    getCurrentUserEmail(principal), bookingId);
            redirectAttributes.addFlashAttribute("success",
                    "Booking #" + bookingId + " deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting booking {}", bookingId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete booking. Please try again.");
        }
        return "redirect:/admin/bookings";
    }

    // =========================================================
    // CAR MANAGEMENT
    // =========================================================
    @PostMapping("/cars/{carId}/delete")
    public String deleteCar(
            @PathVariable Long carId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            carService.deleteCar(carId);
            log.warn("Admin {} DELETED car {}",
                    getCurrentUserEmail(principal), carId);
            redirectAttributes.addFlashAttribute("success",
                    "Vehicle deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting car {}", carId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete vehicle: it may have " +
                            "associated trips. Delete those trips first.");
        }
        return "redirect:/admin/trips";
    }

    // =========================================================
    // HELPER
    // =========================================================
    private String getCurrentUserEmail(Principal principal) {
        if (principal == null) return "unknown";
        try {
            return userService.getCurrentUser(principal).getEmail();
        } catch (Exception e) {
            return principal.getName();
        }
    }
}
