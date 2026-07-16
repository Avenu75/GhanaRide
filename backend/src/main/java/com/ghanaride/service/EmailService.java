package com.ghanaride.service;

import com.ghanaride.dto.ContactFormDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Email Service - Handles all email sending operations.
 * Supports both plain text and HTML templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name:GhanaRide}")
    private String fromName;

    @Value("${app.mail.reply-to:support@ghanaride.me}")
    private String replyTo;

    // =========================================================
    // CONTACT FORM
    // =========================================================

    public void sendContactEmail(ContactFormDTO form) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo("support@ghanaride.me");
            message.setReplyTo(form.getEmail());
            message.setSubject("Contact Form: " + form.getSubject());
            message.setText(
                "Name: " + form.getName() + "\n" +
                "Email: " + form.getEmail() + "\n" +
                "Phone: " + (form.getPhone() != null ? form.getPhone() : "Not provided") + "\n\n" +
                "Message:\n" + form.getMessage()
            );
            mailSender.send(message);
            log.info("Contact email sent from {}", form.getEmail());
        } catch (Exception e) {
            log.error("Failed to send contact email", e);
            throw new RuntimeException("Failed to send email");
        }
    }

    // =========================================================
    // WELCOME EMAIL
    // =========================================================

    @Async
    public void sendWelcomeEmail(com.ghanaride.entity.User user) {
        try {
            sendTemplateEmail(
                user.getEmail(),
                "Welcome to GhanaRide! 🇬🇭",
                "welcome",
                Map.of("userName", user.getFullName())
            );
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", user.getEmail(), e);
        }
    }

    // =========================================================
    // BOOKING CONFIRMATION
    // =========================================================

    @Async
    public void sendBookingConfirmationEmail(com.ghanaride.entity.Booking booking) {
        try {
            sendTemplateEmail(
                booking.getUser().getEmail(),
                "Booking Confirmed: " + booking.getBookingReference() + " — GhanaRide",
                "booking-confirmation",
                Map.of(
                    "booking", booking,
                    "trip", booking.getTrip(),
                    "userName", booking.getUser().getFullName()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation for {}", booking.getBookingReference(), e);
        }
    }

    // =========================================================
    // PAYMENT RECEIPT
    // =========================================================

    @Async
    public void sendPaymentReceiptEmail(com.ghanaride.entity.Booking booking) {
        try {
            sendTemplateEmail(
                booking.getUser().getEmail(),
                "Payment Receipt: " + booking.getBookingReference() + " — GhanaRide",
                "payment-receipt",
                Map.of(
                    "booking", booking,
                    "trip", booking.getTrip(),
                    "userName", booking.getUser().getFullName()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send payment receipt for {}", booking.getBookingReference(), e);
        }
    }

    // =========================================================
    // BOOKING CANCELLATION
    // =========================================================

    @Async
    public void sendCancellationEmail(com.ghanaride.entity.Booking booking, String reason) {
        try {
            sendTemplateEmail(
                booking.getUser().getEmail(),
                "Booking Cancelled: " + booking.getBookingReference() + " — GhanaRide",
                "booking-cancellation",
                Map.of(
                    "booking", booking,
                    "trip", booking.getTrip(),
                    "reason", reason,
                    "userName", booking.getUser().getFullName()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send cancellation email for {}", booking.getBookingReference(), e);
        }
    }

    // =========================================================
    // DRIVER VERIFICATION
    // =========================================================

    @Async
    public void sendDriverVerificationEmail(com.ghanaride.entity.User driver, boolean approved, String reason) {
        try {
            sendTemplateEmail(
                driver.getEmail(),
                approved ? "Driver Account Approved! 🎉 — GhanaRide" : "Driver Application Update — GhanaRide",
                "driver-verification",
                Map.of(
                    "driverName", driver.getFullName(),
                    "approved", approved,
                    "reason", reason
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send driver verification email to {}", driver.getEmail(), e);
        }
    }

    // =========================================================
    // COMPANY VERIFICATION
    // =========================================================

    @Async
    public void sendCompanyVerificationEmail(com.ghanaride.entity.User company, boolean approved, String reason) {
        try {
            sendTemplateEmail(
                company.getEmail(),
                approved ? "Company Account Approved! 🎉 — GhanaRide" : "Company Application Update — GhanaRide",
                "company-verification",
                Map.of(
                    "companyName", company.getFullName(),
                    "approved", approved,
                    "reason", reason
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send company verification email to {}", company.getEmail(), e);
        }
    }

    // =========================================================
    // PASSWORD RESET
    // =========================================================

    @Async
    public void sendPasswordResetEmail(com.ghanaride.entity.User user, String resetLink) {
        try {
            sendTemplateEmail(
                user.getEmail(),
                "Reset Your GhanaRide Password",
                "password-reset",
                Map.of(
                    "userName", user.getFullName(),
                    "resetLink", resetLink,
                    "expiryHours", 24
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}", user.getEmail(), e);
        }
    }

    // =========================================================
    // TRIP REMINDERS
    // =========================================================

    @Async
    public void sendTripReminderEmail(com.ghanaride.entity.Booking booking, int hoursBefore) {
        try {
            sendTemplateEmail(
                booking.getUser().getEmail(),
                "Reminder: Your trip departs in " + hoursBefore + " hour(s) — GhanaRide",
                "trip-reminder",
                Map.of(
                    "booking", booking,
                    "trip", booking.getTrip(),
                    "hoursBefore", hoursBefore,
                    "userName", booking.getUser().getFullName()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send trip reminder for booking {}", booking.getBookingReference(), e);
        }
    }

    // =========================================================
    // TEMPLATE ENGINE HELPER
    // =========================================================

    private void sendTemplateEmail(String to, String subject, String template, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setReplyTo(replyTo);

            Context context = new Context();
            context.setVariables(variables);
            
            String html = templateEngine.process(template, context);
            helper.setText(html, true);

            mailSender.send(message);
            log.debug("Template email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send template email to {}", to, e);
            throw new RuntimeException("Email send failed", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // ADMIN NOTIFICATIONS
    // =========================================================

    public void sendAdminAlert(String subject, String message) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo("admin@ghanaride.me");
            msg.setSubject("[ADMIN ALERT] " + subject);
            msg.setText(message);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send admin alert", e);
        }
    }

    public void sendContactFormEmail(ContactFormDTO form) {
    }
}