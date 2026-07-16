package com.ghanaride.controller;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.BookingStatus;
import com.ghanaride.entity.User;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.PaymentService;
import com.ghanaride.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

/**
 * Handles all payment flows:
 * - Payment method selection
 * - Cash payment processing
 * - Paystack online payment (cards + Mobile Money)
 * - Paystack webhook (server-to-server callback)
 * - Payment verification
 *
 * Security note: /payment/webhook is excluded from CSRF
 * in SecurityConfig because it comes from Paystack servers.
 * All other endpoints require authentication.
 */
@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final UserService userService;

    // Injected from environment variable — NO default fallback
    // with real keys. Empty string forces config error at startup
    // if not set (caught by GhanaRideApplication validator)
    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Value("${paystack.public.key}")
    private String paystackPublicKey;

    @Value("${app.base-url}")
    private String appBaseUrl;

    // =========================================================
    // PAYMENT SELECTION PAGE
    // User chooses: Mobile Money, Card, or Cash
    // =========================================================
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public String showPaymentSelection(
            @PathVariable Long bookingId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt =
                bookingService.findById(bookingId);

        // Validate booking exists and belongs to this user
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
                    "User {} attempted to access payment " +
                            "for booking {} owned by {}",
                    currentUser.getEmail(),
                    bookingId,
                    booking.getUser().getEmail()
            );
            redirectAttributes.addFlashAttribute("error",
                    "Access denied.");
            return "redirect:/my-bookings";
        }

        // Don't allow payment for already-paid bookings
        if (booking.getStatus() == BookingStatus.CONFIRMED ||
                booking.getStatus() == BookingStatus.PAID) {
            redirectAttributes.addFlashAttribute("info",
                    "This booking has already been paid.");
            return "redirect:/booking/receipt/" + bookingId;
        }

        // Don't allow payment for cancelled bookings
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("error",
                    "This booking has been cancelled " +
                            "and cannot be paid for.");
            return "redirect:/my-bookings";
        }

        // Build Paystack callback URL
        String callbackUrl = appBaseUrl +
                "/payment/verify?bookingId=" + bookingId;

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("booking", booking);
        model.addAttribute("trip", booking.getTrip());

        // Pass PUBLIC key to template for Paystack JS
        // NEVER pass secret key to frontend!
        model.addAttribute("paystackPublicKey",
                paystackPublicKey);
        model.addAttribute("callbackUrl", callbackUrl);

        // Amount in kobo/pesewas
        // (Paystack requires amount in smallest currency unit)
        long amountInPesewas = booking.getTrip()
                .getTripAmount()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValue();
        model.addAttribute("amountInPesewas", amountInPesewas);

        model.addAttribute("pageTitle",
                "Complete Payment — GhanaRide");

        return "payment";
    }

    // =========================================================
    // CASH PAYMENT
    // Driver collects cash — booking marked as pending cash
    // =========================================================
    @PostMapping("/{bookingId}/cash")
    @PreAuthorize("isAuthenticated()")
    public String payWithCash(
            @PathVariable Long bookingId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Booking> bookingOpt =
                bookingService.findById(bookingId);

        // Validate ownership
        if (bookingOpt.isEmpty() ||
                !bookingOpt.get().getUser().getId()
                        .equals(currentUser.getId())) {
            log.warn(
                    "User {} attempted unauthorized cash " +
                            "payment for booking {}",
                    currentUser.getEmail(), bookingId
            );
            redirectAttributes.addFlashAttribute("error",
                    "Booking not found or access denied.");
            return "redirect:/my-bookings";
        }

        Booking booking = bookingOpt.get();

        // Check booking is in payable state
        if (booking.getStatus() == BookingStatus.CONFIRMED ||
                booking.getStatus() == BookingStatus.PAID) {
            redirectAttributes.addFlashAttribute("info",
                    "This booking has already been paid.");
            return "redirect:/booking/receipt/" + bookingId;
        }

        try {
            paymentService.processCashPayment(booking);

            log.info(
                    "Cash payment processed for booking {} " +
                            "by user {}",
                    bookingId, currentUser.getEmail()
            );

            redirectAttributes.addFlashAttribute("success",
                    "Booking confirmed! Please pay the driver " +
                            "in cash on the day of travel.");
            return "redirect:/booking/receipt/" + bookingId;

        } catch (Exception e) {
            log.error(
                    "Cash payment failed for booking {}",
                    bookingId, e
            );
            redirectAttributes.addFlashAttribute("error",
                    "Failed to process payment. Please try again.");
            return "redirect:/payment/" + bookingId;
        }
    }

    // =========================================================
    // PAYSTACK PAYMENT VERIFICATION
    // Called after user completes Paystack checkout
    // Paystack redirects here with ?reference=xxx
    //
    // NOTE: This is GET because Paystack redirects here.
    // We verify server-side using the secret key —
    // the reference alone is not enough to confirm payment.
    // =========================================================
    @GetMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public String verifyPaystackPayment(
            @RequestParam String reference,
            @RequestParam(required = false) Long bookingId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (reference == null || reference.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid payment reference.");
            return "redirect:/my-bookings";
        }

        try {
            // Verify with Paystack API server-side
            // (never trust client-side confirmation alone)
            Booking booking =
                    paymentService.verifyPaystackPayment(reference);

            // Extra ownership check
            User currentUser =
                    userService.getCurrentUser(principal);
            if (!booking.getUser().getId()
                    .equals(currentUser.getId())) {
                log.warn(
                        "User {} attempted to claim payment " +
                                "for booking owned by {}",
                        currentUser.getEmail(),
                        booking.getUser().getEmail()
                );
                redirectAttributes.addFlashAttribute("error",
                        "Payment verification failed.");
                return "redirect:/my-bookings";
            }

            log.info(
                    "Paystack payment verified: ref={} " +
                            "booking={} user={}",
                    reference,
                    booking.getId(),
                    currentUser.getEmail()
            );

            redirectAttributes.addFlashAttribute("success",
                    "Payment successful! Your seat is confirmed.");
            return "redirect:/booking/receipt/" +
                    booking.getId();

        } catch (Exception e) {
            log.error(
                    "Paystack payment verification failed: " +
                            "ref={}",
                    reference, e
            );
            // Generic error — don't leak payment details
            redirectAttributes.addFlashAttribute("error",
                    "Payment verification failed. If you were " +
                            "charged, please contact support at " +
                            "support@ghanaride.me with reference: " +
                            reference
            );
            return "redirect:/my-bookings";
        }
    }

    // =========================================================
    // PAYSTACK WEBHOOK
    // Called by Paystack servers for async payment events.
    // CSRF exempt (set in SecurityConfig).
    // Validates using Paystack-Signature header.
    // =========================================================
    @PostMapping("/webhook")
    public String handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Paystack-Signature") String signature,
            HttpServletRequest request
    ) {
        try {
            // Validate webhook signature
            boolean isValid = paymentService
                    .validateWebhookSignature(
                            payload, signature, paystackSecretKey
                    );

            if (!isValid) {
                log.warn(
                        "Invalid Paystack webhook signature " +
                                "from IP: {}",
                        request.getRemoteAddr()
                );
                // Return 200 to prevent Paystack retrying
                // but don't process invalid webhook
                return "redirect:/";
            }

            // Process the webhook event
            paymentService.processWebhookEvent(payload);

            log.info("Paystack webhook processed successfully");

        } catch (Exception e) {
            log.error("Paystack webhook processing failed", e);
            // Still return 200 — Paystack will retry
            // if we return 4xx/5xx
        }

        // Paystack expects HTTP 200 response
        return "redirect:/";
    }
}