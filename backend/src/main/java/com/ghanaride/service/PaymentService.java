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
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all payment operations.
 *
 * CRITICAL: verifyPaystackPayment() makes a REAL
 * server-to-server API call to Paystack to verify
 * payment status. We NEVER trust client-side
 * confirmation alone — always verify server-side.
 *
 * Paystack API docs:
 * https://paystack.com/docs/api/transaction/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String paystackBaseUrl;

    private static final String PAYSTACK_VERIFY_URL =
            "/transaction/verify/";

    // =========================================================
    // CASH PAYMENT
    // Driver collects cash — booking stays ACTIVE
    // with PENDING payment status until driver confirms
    // =========================================================
    @Transactional
    public void processCashPayment(Booking booking) {
        booking.setPaymentMethod(PaymentMethod.CASH);
        // Cash payment is PENDING until driver confirms
        // receipt on the day of travel
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info(
                "Cash payment registered: booking={} ref={}",
                booking.getId(),
                booking.getBookingReference()
        );

        // Send confirmation email
        emailService.sendBookingConfirmationEmail(booking);
    }

    // =========================================================
    // PAYSTACK PAYMENT VERIFICATION
    //
    // CRITICAL SECURITY: This makes a server-to-server
    // API call to Paystack to verify the payment.
    // We NEVER trust the reference alone — Paystack must
    // confirm the payment was successful.
    //
    // Without this, an attacker could:
    // 1. Initiate a payment
    // 2. Copy the reference
    // 3. Navigate to /payment/verify?reference=xxx
    //    WITHOUT actually paying
    //    → and get a booking for free!
    // =========================================================
    @Transactional
    public Booking verifyPaystackPayment(String reference) {
        log.info(
                "Verifying Paystack payment: ref={}",
                reference
        );

        // Step 1: Call Paystack API to verify
        PaystackVerificationResult result =
                callPaystackVerifyApi(reference);

        // Step 2: Check Paystack says it's successful
        if (!result.isSuccess()) {
            log.warn(
                    "Paystack verification failed: ref={} " +
                            "status={}",
                    reference, result.getStatus()
            );
            throw new IllegalStateException(
                    "Payment verification failed. " +
                            "Status: " + result.getStatus() +
                            ". Please contact support."
            );
        }

        // Step 3: Find booking by reference
        Booking booking = bookingRepository
                .findByBookingReference(reference)
                .orElseThrow(() -> {
                    log.error(
                            "No booking found for ref: {}",
                            reference
                    );
                    return new ResourceNotFoundException(
                            "Booking", "reference", reference
                    );
                });

        // Step 4: Check not already paid
        // (prevents double-processing webhooks)
        if (booking.getPaymentStatus() ==
                PaymentStatus.PAID) {
            log.warn(
                    "Booking already paid: ref={}", reference
            );
            return booking;
        }

        // Step 5: Verify amount matches
        // (prevents partial payment attacks)
        long expectedAmountKobo = booking.getTotalAmount()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValue();

        if (result.getAmountPaid() < expectedAmountKobo) {
            log.error(
                    "Amount mismatch: ref={} " +
                            "expected={} paid={}",
                    reference,
                    expectedAmountKobo,
                    result.getAmountPaid()
            );
            throw new IllegalStateException(
                    "Payment amount mismatch. " +
                            "Expected GH₵" +
                            booking.getTotalAmount() +
                            " but received less. " +
                            "Contact support@ghanaride.me"
            );
        }

        // Step 6: Mark as paid
        booking.setPaymentMethod(
                resolvePaymentMethod(result.getChannel())
        );
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.PAID);
        booking.setTransactionReference(
                result.getPaystackReference()
        );
        booking.setPaymentDate(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        log.info(
                "Payment verified and recorded: ref={} " +
                        "booking={} amount=GH₵{}",
                reference,
                booking.getId(),
                booking.getTotalAmount()
        );

        // Send payment confirmation email (async)
        emailService.sendBookingConfirmationEmail(saved);

        return saved;
    }

    // =========================================================
    // PAYSTACK API CALL
    // Makes HTTP request to Paystack verification endpoint
    // =========================================================
    private PaystackVerificationResult callPaystackVerifyApi(
            String reference
    ) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Build request with Authorization header
            HttpHeaders headers = new HttpHeaders();
            headers.set(
                    "Authorization",
                    "Bearer " + paystackSecretKey
            );
            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            HttpEntity<String> entity =
                    new HttpEntity<>(headers);

            String url = paystackBaseUrl +
                    PAYSTACK_VERIFY_URL +
                    reference;

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            // Parse response
            return parsePaystackResponse(
                    response.getBody()
            );

        } catch (Exception e) {
            log.error(
                    "Paystack API call failed for ref: {}",
                    reference, e
            );
            throw new IllegalStateException(
                    "Unable to verify payment with Paystack. " +
                            "Please try again or contact support."
            );
        }
    }

    // =========================================================
    // PARSE PAYSTACK API RESPONSE
    // =========================================================
    private PaystackVerificationResult parsePaystackResponse(
            String responseBody
    ) {
        try {
            JsonNode root =
                    objectMapper.readTree(responseBody);

            boolean apiSuccess =
                    root.path("status").asBoolean(false);

            if (!apiSuccess) {
                return PaystackVerificationResult
                        .failure("api_error");
            }

            JsonNode data = root.path("data");
            String txStatus =
                    data.path("status").asText("");

            // Paystack success status is "success"
            if (!"success".equals(txStatus)) {
                return PaystackVerificationResult
                        .failure(txStatus);
            }

            long amountPaid =
                    data.path("amount").asLong(0);
            String channel =
                    data.path("channel").asText("unknown");
            String paystackRef =
                    data.path("reference").asText("");

            return PaystackVerificationResult.success(
                    amountPaid, channel, paystackRef
            );

        } catch (Exception e) {
            log.error(
                    "Failed to parse Paystack response", e
            );
            return PaystackVerificationResult
                    .failure("parse_error");
        }
    }

    // =========================================================
    // WEBHOOK SIGNATURE VALIDATION
    // Verifies the webhook came from Paystack, not a fake
    // Uses HMAC-SHA512 signature verification
    // =========================================================
    public boolean validateWebhookSignature(
            String payload,
            String signature,
            String secretKey
    ) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            // Convert to hex string
            String computed =
                    HexFormat.of().formatHex(hmacBytes);

            // Constant-time comparison
            // (prevents timing attacks)
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            log.error(
                    "Webhook signature validation error", e
            );
            return false;
        }
    }

    // =========================================================
    // PROCESS WEBHOOK EVENT
    // Handles async payment events from Paystack
    // =========================================================
    @Transactional
    public void processWebhookEvent(String payload) {
        try {
            JsonNode root =
                    objectMapper.readTree(payload);

            String event =
                    root.path("event").asText("");
            JsonNode data = root.path("data");

            log.info(
                    "Processing Paystack webhook: event={}",
                    event
            );

            switch (event) {
                case "charge.success" -> {
                    String reference =
                            data.path("reference")
                                    .asText("");
                    if (!reference.isBlank()) {
                        // Find and update booking
                        bookingRepository
                                .findByBookingReference(
                                        reference
                                )
                                .ifPresent(booking -> {
                                    if (booking.getPaymentStatus()
                                            != PaymentStatus.PAID) {
                                        booking.setPaymentStatus(
                                                PaymentStatus.PAID
                                        );
                                        booking.setStatus(
                                                BookingStatus.PAID
                                        );
                                        booking.setPaymentDate(
                                                LocalDateTime.now()
                                        );
                                        bookingRepository
                                                .save(booking);
                                        log.info(
                                                "Webhook: booking " +
                                                        "paid ref={}",
                                                reference
                                        );
                                    }
                                });
                    }
                }
                case "charge.failed" -> {
                    log.warn(
                            "Paystack charge failed: {}",
                            data
                    );
                }
                case "refund.processed" -> {
                    log.info(
                            "Paystack refund processed: {}",
                            data
                    );
                    // Handle refund logic here
                }
                default -> log.debug(
                        "Unhandled Paystack event: {}", event
                );
            }

        } catch (Exception e) {
            log.error(
                    "Error processing webhook payload", e
            );
            throw new IllegalStateException(
                    "Webhook processing failed"
            );
        }
    }

    // =========================================================
    // RESOLVE PAYMENT METHOD FROM PAYSTACK CHANNEL
    // =========================================================
    private PaymentMethod resolvePaymentMethod(
            String channel
    ) {
        if (channel == null) return PaymentMethod.PAYSTACK;
        return switch (channel.toLowerCase()) {
            case "mobile_money" -> PaymentMethod.MOBILE_MONEY;
            case "card"         -> PaymentMethod.CARD;
            case "bank"         -> PaymentMethod.BANK_TRANSFER;
            default             -> PaymentMethod.PAYSTACK;
        };
    }

    // =========================================================
    // INNER CLASS: Paystack Verification Result
    // =========================================================
    private static class PaystackVerificationResult {
        private final boolean success;
        private final String status;
        private final long amountPaid;
        private final String channel;
        private final String paystackReference;

        private PaystackVerificationResult(
                boolean success,
                String status,
                long amountPaid,
                String channel,
                String paystackReference
        ) {
            this.success           = success;
            this.status            = status;
            this.amountPaid        = amountPaid;
            this.channel           = channel;
            this.paystackReference = paystackReference;
        }

        static PaystackVerificationResult success(
                long amountPaid,
                String channel,
                String paystackReference
        ) {
            return new PaystackVerificationResult(
                    true, "success",
                    amountPaid, channel, paystackReference
            );
        }

        static PaystackVerificationResult failure(
                String status
        ) {
            return new PaystackVerificationResult(
                    false, status, 0, null, null
            );
        }

        boolean isSuccess()           { return success;           }
        String  getStatus()           { return status;            }
        long    getAmountPaid()       { return amountPaid;        }
        String  getChannel()          { return channel;           }
        String  getPaystackReference(){ return paystackReference; }
    }
}