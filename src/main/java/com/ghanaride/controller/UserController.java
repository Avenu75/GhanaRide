package com.ghanaride.controller;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.Trip;
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

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TripService tripService;
    private final BookingService bookingService;

    private final List<String> locations = Arrays.asList("Accra", "Cape Coast", "Kumasi", "Takoradi", "Tamale", "Sunyani", "Ho", "Koforidua", "Tema", "Winneba");

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trips", tripService.findApprovedTrips());
        model.addAttribute("locations", locations);
        return "dashboard";
    }

    @GetMapping("/booking/{tripId}")
    public String showBookingPage(@PathVariable Long tripId, Principal principal, Model model) {
        Optional<Trip> tripOpt = tripService.findById(tripId);
        if (tripOpt.isEmpty() || !tripOpt.get().getStatus().name().equals("APPROVED") || tripOpt.get().getAvailableSeats() <= 0) {
            return "redirect:/dashboard?error=trip_unavailable";
        }
        
        User currentUser = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trip", tripOpt.get());
        
        return "booking-confirm";
    }

    @PostMapping("/booking/{tripId}")
    public String createBooking(@PathVariable Long tripId, Principal principal) {
        Optional<Trip> tripOpt = tripService.findById(tripId);
        if (tripOpt.isEmpty()) {
            return "redirect:/dashboard?error=trip_not_found";
        }
        
        User currentUser = userService.getCurrentUser(principal);
        try {
            Booking booking = bookingService.createBooking(currentUser, tripOpt.get());
            return "redirect:/booking/receipt/" + booking.getId();
        } catch (Exception e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/booking/receipt/{bookingId}")
    public String viewReceipt(@PathVariable Long bookingId, Principal principal, Model model) {
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return "redirect:/dashboard?error=booking_not_found";
        }
        
        // Add auth check here if needed (only allow booking owner or admin)
        
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("booking", bookingOpt.get());
        return "receipt";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("bookings", bookingService.findByUser(currentUser));
        return "my-bookings";
    }
}
