package com.ghanaride.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghanaride.entity.*;
import com.ghanaride.exception.ResourceNotFoundException;
import com.ghanaride.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Handles all payment operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final TripService tripService;
    private final SeatService seatService;
    private final NotificationService notificationService;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String paystackBaseUrl;

    private static final String PAYSTACK_VERIFY_URL = "/transaction/verify/";

    @Transactional
    public void processCashPayment(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PAID) return;
        booking.setPaymentMethod(PaymentMethod.CASH);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        if (booking.getSeatMap() != null) seatService.confirmSeat(booking.getTrip().getId(), booking.getSeatMap().getSeatNumber());
        tripService.decrementAvailableSeats(booking.getTrip().getId());
        emailService.sendBookingConfirmationEmail(booking);
        notificationService.notifyBookingConfirmed(booking);
    }

    @Transactional
    public Booking verifyPaystackPayment(String reference) {
        PaystackVerificationResult result = callPaystackVerifyApi(reference);
        if (!result.isSuccess()) throw new IllegalStateException("Payment verification failed. Status: " + result.getStatus());

        Booking booking = bookingRepository.findByBookingReference(reference)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", reference));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) return booking;

        long expectedAmount = booking.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        if (result.getAmountPaid() < expectedAmount) {
            throw new IllegalStateException("Payment amount mismatch. Contact support@ghanaride.me");
        }

        booking.setPaymentMethod(resolvePaymentMethod(result.getChannel()));
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTransactionReference(result.getPaystackReference());
        booking.setPaymentDate(LocalDateTime.now());

        if (booking.getSeatMap() != null) seatService.confirmSeat(booking.getTrip().getId(), booking.getSeatMap().getSeatNumber());
        tripService.decrementAvailableSeats(booking.getTrip().getId());

        Booking saved = bookingRepository.save(booking);
        emailService.sendPaymentReceiptEmail(saved);
        notificationService.notifyPaymentReceived(saved);
        return saved;
    }

    private PaystackVerificationResult callPaystackVerifyApi(String reference) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + paystackSecretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                paystackBaseUrl + PAYSTACK_VERIFY_URL + reference,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );
            return parsePaystackResponse(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to verify payment with Paystack. Please try again or contact support.", e);
        }
    }

    private PaystackVerificationResult parsePaystackResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.path("status").asBoolean(false)) return PaystackVerificationResult.failure("api_error");
            JsonNode data = root.path("data");
            String txStatus = data.path("status").asText("");
            if (!"success".equals(txStatus)) return PaystackVerificationResult.failure(txStatus);
            return PaystackVerificationResult.success(data.path("amount").asLong(0), data.path("channel").asText("unknown"), data.path("reference").asText(""));
        } catch (Exception e) {
            return PaystackVerificationResult.failure("parse_error");
        }
    }

    public boolean validateWebhookSignature(String payload, String signature, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            String computed = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void processWebhookEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode data = root.path("data");
            switch (event) {
                case "charge.success" -> {
                    String reference = data.path("reference").asText("");
                    if (!reference.isBlank()) {
                        bookingRepository.findByBookingReference(reference).ifPresent(booking -> {
                            if (booking.getPaymentStatus() != PaymentStatus.PAID) {
                                booking.setPaymentStatus(PaymentStatus.PAID);
                                booking.setStatus(BookingStatus.CONFIRMED);
                                booking.setPaymentDate(LocalDateTime.now());
                                if (booking.getSeatMap() != null) seatService.confirmSeat(booking.getTrip().getId(), booking.getSeatMap().getSeatNumber());
                                tripService.decrementAvailableSeats(booking.getTrip().getId());
                                bookingRepository.save(booking);
                                notificationService.notifyPaymentReceived(booking);
                            }
                        });
                    }
                }
                case "charge.failed" -> log.warn("Paystack charge failed: {}", data);
                case "refund.processed" -> log.info("Paystack refund processed: {}", data);
                default -> log.debug("Unhandled Paystack event: {}", event);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Webhook processing failed", e);
        }
    }

    private PaymentMethod resolvePaymentMethod(String channel) {
        if (channel == null) return PaymentMethod.PAYSTACK;
        return switch (channel.toLowerCase()) {
            case "mobile_money" -> PaymentMethod.MOBILE_MONEY;
            case "card" -> PaymentMethod.CARD;
            case "bank" -> PaymentMethod.BANK_TRANSFER;
            default -> PaymentMethod.PAYSTACK;
        };
    }

    private static class PaystackVerificationResult {
        private final boolean success;
        private final String status;
        private final long amountPaid;
        private final String channel;
        private final String paystackReference;

        private PaystackVerificationResult(boolean success, String status, long amountPaid, String channel, String paystackReference) {
            this.success = success;
            this.status = status;
            this.amountPaid = amountPaid;
            this.channel = channel;
            this.paystackReference = paystackReference;
        }

        static PaystackVerificationResult success(long amountPaid, String channel, String paystackReference) {
            return new PaystackVerificationResult(true, "success", amountPaid, channel, paystackReference);
        }

        static PaystackVerificationResult failure(String status) {
            return new PaystackVerificationResult(false, status, 0, null, null);
        }

        boolean isSuccess() { return success; }
        String getStatus() { return status; }
        long getAmountPaid() { return amountPaid; }
        String getChannel() { return channel; }
        String getPaystackReference() { return paystackReference; }
    }
}
