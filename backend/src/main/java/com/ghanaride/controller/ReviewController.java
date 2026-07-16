package com.ghanaride.controller;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.Review;
import com.ghanaride.entity.User;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.ReviewRepository;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reviews")
    public String submit(@RequestParam Long bookingId,
                         @RequestParam int rating,
                         @RequestParam(required = false) String comment,
                         Principal principal,
                         RedirectAttributes ra) {
        User user = userService.getCurrentUser(principal);
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null || !b.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Invalid booking");
            return "redirect:/my-bookings";
        }
        if (rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "Rating 1-5");
            return "redirect:/my-bookings";
        }
        Review r = new Review();
        r.setRating(rating);
        r.setComment(comment != null && comment.length() > 500 ? comment.substring(0,500) : comment);
        r.setPassenger(user);
        r.setDriver(b.getTrip().getDriver());
        r.setTrip(b.getTrip());
        reviewRepository.save(r);
        ra.addFlashAttribute("success", "Thank you! Your 5-star review helps Ghana drivers.");
        return "redirect:/my-bookings";
    }
}
