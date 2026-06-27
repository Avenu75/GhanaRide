package com.ghanaride.controller;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.TripStatus;
import com.ghanaride.entity.User;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.TripService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TripService tripService;
    private final BookingService bookingService;

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
    public String approveTrip(@PathVariable Long tripId) {
        tripService.approveTrip(tripId);
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/reject")
    public String rejectTrip(@PathVariable Long tripId) {
        tripService.rejectTrip(tripId);
        return "redirect:/admin/trips";
    }

    @GetMapping("/users")
    public String manageUsers(Principal principal, Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }
}
