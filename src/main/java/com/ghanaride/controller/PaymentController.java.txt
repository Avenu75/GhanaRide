package com.ghanaride.controller;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.User;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.PaymentService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final UserService userService;

    @Value("${paystack.secret.key:sk_test_placeholder}")
    private String paystackSecretKey;

    @GetMapping("/{bookingId}")
    public String showPaymentSelection(@PathVariable Long bookingId, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);

        if (bookingOpt.isEmpty() || !bookingOpt.get().getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Booking not found.");
            return "redirect:/my-bookings";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("booking", bookingOpt.get());
        
        // Passing public key would happen here if we used real Paystack checkout JS, but we will use a simulated modal.
        return "payment";
    }

    @PostMapping("/{bookingId}/cash")
    public String payWithCash(@PathVariable Long bookingId, Principal principal, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);

        if (bookingOpt.isEmpty() || !bookingOpt.get().getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Booking not found.");
            return "redirect:/my-bookings";
        }

        paymentService.processCashPayment(bookingOpt.get());
        return "redirect:/booking/receipt/" + bookingId;
    }

    @GetMapping("/verify")
    public String verifyPaystackPayment(@RequestParam String reference, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = paymentService.verifyPaystackPayment(reference);
            redirectAttributes.addFlashAttribute("success", "Payment successful!");
            return "redirect:/booking/receipt/" + booking.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
            return "redirect:/my-bookings";
        }
    }
}
