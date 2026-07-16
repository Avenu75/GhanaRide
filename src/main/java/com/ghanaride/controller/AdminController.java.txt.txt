package com.ghanaride.controller;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.TripStatus;
import com.ghanaride.entity.User;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.CarService;
import com.ghanaride.service.TripService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final CarService carService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("totalUsers", userService.countByRole(Role.USER));
        model.addAttribute("totalDrivers", userService.countByRole(Role.DRIVER));
        model.addAttribute("totalTrips", tripService.countAll());
        model.addAttribute("pendingTrips", tripService.countByStatus(TripStatus.PENDING));
        model.addAttribute("approvedTrips", tripService.countByStatus(TripStatus.APPROVED));
        model.addAttribute("totalBookings", bookingService.countAll());
        return "admin/dashboard";
    }

    @GetMapping("/trips")
    public String manageTrips(Principal principal, Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("trips", tripService.findAllTrips());
        return "admin/trips";
    }

    @PostMapping("/trips/{tripId}/approve")
    public String approveTrip(@PathVariable Long tripId,
                              RedirectAttributes redirectAttributes) {
        tripService.approveTrip(tripId);
        redirectAttributes.addFlashAttribute("success", "Trip approved successfully!");
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/reject")
    public String rejectTrip(@PathVariable Long tripId,
                             RedirectAttributes redirectAttributes) {
        tripService.rejectTrip(tripId);
        redirectAttributes.addFlashAttribute("success", "Trip rejected successfully!");
        return "redirect:/admin/trips";
    }

    // ===== MARK AS FAILED TO SHOW =====
    @PostMapping("/trips/{tripId}/fail")
    public String markTripFailed(@PathVariable Long tripId,
                                 @RequestParam String failReason,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            tripService.markAsFailedToShow(tripId, failReason, currentUser);
            redirectAttributes.addFlashAttribute("success", "Trip marked as failed to show.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/trips";
    }

    // ===== DELETE TRIP =====
    @PostMapping("/trips/{tripId}/delete")
    public String deleteTrip(@PathVariable Long tripId,
                             RedirectAttributes redirectAttributes) {
        try {
            tripService.deleteTrip(tripId);
            redirectAttributes.addFlashAttribute("success", "Trip deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete trip: " + e.getMessage());
        }
        return "redirect:/admin/trips";
    }

    @GetMapping("/users")
    public String manageUsers(Principal principal, Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    // ===== DELETE USER =====
    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            // Prevent admin from deleting themselves
            User currentUser = userService.getCurrentUser(principal);
            if (currentUser.getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error",
                        "You cannot delete your own account!");
                return "redirect:/admin/users";
            }
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ===== DELETE BOOKING =====
    @PostMapping("/bookings/{bookingId}/delete")
    public String deleteBooking(@PathVariable Long bookingId,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.deleteBooking(bookingId);
            redirectAttributes.addFlashAttribute("success", "Booking deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete booking: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    // ===== DELETE CAR =====
    @PostMapping("/cars/{carId}/delete")
    public String deleteCar(@PathVariable Long carId,
                            RedirectAttributes redirectAttributes) {
        try {
            carService.deleteCar(carId);
            redirectAttributes.addFlashAttribute("success", "Car deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete car: " + e.getMessage());
        }
        return "redirect:/admin/trips";
    }

    @GetMapping("/bookings")
    public String manageBookings(Principal principal, Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("bookings", bookingService.findAllBookings());
        return "admin/bookings";
    }
}