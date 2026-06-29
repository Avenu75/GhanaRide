package com.ghanaride.controller;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.Trip;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    private final List<String> locations = Arrays.asList(
            "Accra", "Cape Coast", "Kumasi", "Takoradi", "Tamale",
            "Sunyani", "Ho", "Koforidua", "Tema", "Winneba"
    );

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        // Show both APPROVED and FULL trips so users can see full ones as unavailable
        model.addAttribute("trips", tripService.findApprovedAndFullTrips());
        model.addAttribute("locations", locations);
        return "dashboard";
    }

    @GetMapping("/booking/{tripId}")
    public String showBookingPage(@PathVariable Long tripId,
                                  Principal principal, Model model) {
        Optional<Trip> tripOpt = tripService.findById(tripId);
        if (tripOpt.isEmpty() ||
                !tripOpt.get().getStatus().name().equals("APPROVED") ||
                tripOpt.get().getAvailableSeats() <= 0) {
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
    public String viewReceipt(@PathVariable Long bookingId,
                              Principal principal, Model model) {
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return "redirect:/dashboard?error=booking_not_found";
        }

        Booking booking = bookingOpt.get();
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("booking", booking);
        // Pass whether user can still cancel
        model.addAttribute("canCancel", bookingService.canCancelBooking(booking));
        return "receipt";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        List<Booking> bookings = bookingService.findByUser(currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("bookings", bookings);
        // Pass cancel eligibility for each booking
        model.addAttribute("bookingService", bookingService);
        return "my-bookings";
    }

    // ===== CANCEL BOOKING =====
    @PostMapping("/booking/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            Optional<Booking> bookingOpt = bookingService.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found.");
                return "redirect:/my-bookings";
            }

            // Make sure only the booking owner can cancel
            User currentUser = userService.getCurrentUser(principal);
            if (!bookingOpt.get().getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only cancel your own bookings.");
                return "redirect:/my-bookings";
            }

            bookingService.cancelBooking(bookingId);
            redirectAttributes.addFlashAttribute("success",
                    "Booking cancelled successfully. Your seat has been released.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }
}