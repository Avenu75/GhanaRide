package com.ghanaride.service;

import com.ghanaride.dto.ContactFormDTO;
import com.ghanaride.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email service for all GhanaRide emails.
 *
 * ALL public methods are @Async — they run in a
 * separate thread pool so email sending never
 * blocks the HTTP request thread.
 *
 * Email failure never propagates to caller —
 * all exceptions are caught and logged internally.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from:noreply@ghanaride.me}")
    private String fromEmail;

    @Value("${app.mail.from-name:GhanaRide}")
    private String fromName;

    @Value("${app.support.email:support@ghanaride.me}")
    private String supportEmail;

    // =========================================================
    // EMAIL VERIFICATION
    // =========================================================
    @Async
    public void sendVerificationEmail(
            String toEmail,
            String fullName,
            String token
    ) {
        String verifyUrl =
                baseUrl + "/verify-email?token=" + token;

        String html = buildVerificationEmailHtml(
                fullName, verifyUrl
        );

        sendEmail(
                toEmail,
                "Verify your GhanaRide account",
                html,
                "Verification email"
        );
    }

    // =========================================================
    // WELCOME EMAIL (after verification)
    // =========================================================
    @Async
    public void sendWelcomeEmail(
            String toEmail,
            String fullName
    ) {
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto;
                          background:white;border-radius:12px;
                          overflow:hidden;">
                <div style="background:#006B3F;padding:30px;
                            text-align:center;">
                  <h1 style="color:#FCD116;margin:0;">
                    🎉 Welcome to GhanaRide!
                  </h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2>Hi %s, you're verified! ✅</h2>
                  <p style="color:#475569;line-height:1.7;">
                    Your account is now active. You can book
                    rides across Ghana with trusted drivers.
                  </p>
                  <div style="text-align:center;margin:30px 0;">
                    <a href="%s/dashboard"
                       style="background:#006B3F;color:white;
                              padding:14px 32px;
                              border-radius:8px;
                              text-decoration:none;
                              font-weight:700;">
                      🚌 Start Booking Rides
                    </a>
                  </div>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(fullName, baseUrl, buildFooter());

        sendEmail(
                toEmail,
                "Welcome to GhanaRide! 🎉",
                html,
                "Welcome email"
        );
    }

    // =========================================================
    // PASSWORD RESET EMAIL
    // =========================================================
    @Async
    public void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String token
    ) {
        String resetUrl =
                baseUrl + "/reset-password?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto;
                          background:white;border-radius:12px;
                          overflow:hidden;">
                <div style="background:#1a1a2e;padding:30px;
                            text-align:center;">
                  <h1 style="color:#FCD116;margin:0;">
                    🔐 GhanaRide
                  </h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2>Password Reset Request</h2>
                  <p style="color:#475569;line-height:1.7;">
                    Hi %s, we received a request to reset
                    your GhanaRide password.
                  </p>
                  <div style="text-align:center;margin:30px 0;">
                    <a href="%s"
                       style="background:#dc2626;color:white;
                              padding:14px 32px;
                              border-radius:8px;
                              text-decoration:none;
                              font-weight:700;">
                      🔑 Reset My Password
                    </a>
                  </div>
                  <div style="background:#fef3c7;
                              border-left:4px solid #f59e0b;
                              padding:12px 16px;
                              border-radius:4px;">
                    <p style="color:#92400e;margin:0;
                              font-size:14px;">
                      ⏰ This link expires in
                      <strong>24 hours</strong>.<br>
                      If you didn't request this,
                      please ignore this email.
                      Your password will not change.
                    </p>
                  </div>
                  <p style="color:#64748b;font-size:13px;
                            margin-top:20px;">
                    If the button doesn't work, copy this link:
                    <br>
                    <span style="word-break:break-all;
                                 color:#3b82f6;">%s</span>
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(fullName, resetUrl, resetUrl,
                buildFooter());

        sendEmail(
                toEmail,
                "Reset your GhanaRide password",
                html,
                "Password reset email"
        );
    }

    // =========================================================
    // BOOKING CONFIRMATION EMAIL
    // =========================================================
    @Async
    public void sendBookingConfirmation(Booking booking) {
        if (booking.getUser() == null ||
                booking.getUser().getEmail() == null) {
            return;
        }

        String toEmail = booking.getUser().getEmail();
        String name = booking.getUser().getFullName() != null
                ? booking.getUser().getFullName()
                : booking.getUser().getUsername();

        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto;
                          background:white;border-radius:12px;
                          overflow:hidden;">
                <div style="background:#006B3F;padding:30px;
                            text-align:center;">
                  <h1 style="color:#FCD116;margin:0;">
                    ✅ Booking Confirmed!
                  </h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2>Hi %s!</h2>
                  <p style="color:#475569;">
                    Your booking is confirmed. Here are the
                    details:
                  </p>
                  <table style="width:100%%;
                                border-collapse:collapse;
                                margin:20px 0;">
                    <tr style="background:#f8fafc;">
                      <td style="padding:12px;
                                 font-weight:bold;
                                 color:#374151;">
                        Reference
                      </td>
                      <td style="padding:12px;color:#374151;">
                        <strong>%s</strong>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:12px;
                                 font-weight:bold;
                                 color:#374151;">
                        Route
                      </td>
                      <td style="padding:12px;color:#374151;">
                        %s → %s
                      </td>
                    </tr>
                    <tr style="background:#f8fafc;">
                      <td style="padding:12px;
                                 font-weight:bold;
                                 color:#374151;">
                        Departure
                      </td>
                      <td style="padding:12px;color:#374151;">
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:12px;
                                 font-weight:bold;
                                 color:#374151;">
                        Seat Number
                      </td>
                      <td style="padding:12px;color:#374151;">
                        %s
                      </td>
                    </tr>
                    <tr style="background:#f8fafc;">
                      <td style="padding:12px;
                                 font-weight:bold;
                                 color:#374151;">
                        Amount
                      </td>
                      <td style="padding:12px;
                                 color:#006B3F;
                                 font-weight:bold;">
                        GH₵ %s
                      </td>
                    </tr>
                  </table>
                  <div style="background:#f0fdf4;
                              border-left:4px solid #006B3F;
                              padding:12px 16px;
                              border-radius:4px;">
                    <p style="color:#166534;margin:0;
                              font-size:14px;">
                      💡 You can cancel within %d minutes
                      of booking. After that, please contact
                      support.
                    </p>
                  </div>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                name,
                booking.getBookingReference(),
                booking.getTrip().getFromLocation(),
                booking.getTrip().getToLocation(),
                booking.getTrip().getDepartureTime(),
                booking.getSeatNumber(),
                booking.getTotalAmount(),
                3, // cancel window minutes
                buildFooter()
        );

        sendEmail(
                toEmail,
                "Booking Confirmed — " +
                        booking.getBookingReference() +
                        " | GhanaRide",
                html,
                "Booking confirmation"
        );
    }

    // =========================================================
    // DRIVER NOTIFICATION EMAIL
    // =========================================================
    @Async
    public void sendBookingNotificationToDriver(Booking booking) {
        if (booking.getTrip().getDriver() == null || booking.getTrip().getDriver().getEmail() == null) {
            return;
        }

        String toEmail = booking.getTrip().getDriver().getEmail();
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif; background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto; background:white;border-radius:12px; overflow:hidden;">
                <div style="background:#006B3F;padding:30px; text-align:center;">
                  <h1 style="color:#FCD116;margin:0;">🚗 New Passenger Booking</h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2>Hi %s,</h2>
                  <p>A new passenger has booked your trip from <strong>%s</strong> to <strong>%s</strong>.</p>
                  <ul>
                    <li>Passenger: %s</li>
                    <li>Seat Number: %s</li>
                    <li>Booking Ref: %s</li>
                  </ul>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                booking.getTrip().getDriver().getFullName(),
                booking.getTrip().getFromLocation(),
                booking.getTrip().getToLocation(),
                booking.getUser().getFullName(),
                booking.getSeatNumber(),
                booking.getBookingReference(),
                buildFooter()
        );

        sendEmail(toEmail, "New Booking on Your Trip — " + booking.getBookingReference(), html, "Driver notification");
    }

    // =========================================================
    // CANCELLATION EMAIL
    // =========================================================
    @Async
    public void sendCancellationEmail(Booking booking) {
        if (booking.getUser() == null || booking.getUser().getEmail() == null) {
            return;
        }

        String toEmail = booking.getUser().getEmail();
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif; background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto; background:white;border-radius:12px; overflow:hidden;">
                <div style="background:#dc2626;padding:30px; text-align:center;">
                  <h1 style="color:white;margin:0;">🚫 Booking Cancelled</h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2>Hi %s,</h2>
                  <p>Your booking <strong>%s</strong> has been successfully cancelled.</p>
                  <p>If you have any questions, please contact our support team.</p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                booking.getUser().getFullName(),
                booking.getBookingReference(),
                buildFooter()
        );

        sendEmail(toEmail, "Booking Cancelled — " + booking.getBookingReference(), html, "Cancellation email");
    }

    // =========================================================
    // CONTACT FORM EMAIL
    // Sends to GhanaRide support inbox
    // =========================================================
    @Async
    public void sendContactFormEmail(
            ContactFormDTO form
    ) {
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto;
                          background:white;border-radius:12px;
                          padding:30px;">
                <h2 style="color:#1a1a2e;">
                  📬 New Contact Form Message
                </h2>
                <table style="width:100%%;
                              border-collapse:collapse;">
                  <tr>
                    <td style="padding:8px;
                               font-weight:bold;width:120px;">
                      Name:
                    </td>
                    <td style="padding:8px;">%s</td>
                  </tr>
                  <tr style="background:#f8fafc;">
                    <td style="padding:8px;font-weight:bold;">
                      Email:
                    </td>
                    <td style="padding:8px;">
                      <a href="mailto:%s">%s</a>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:8px;font-weight:bold;">
                      Phone:
                    </td>
                    <td style="padding:8px;">%s</td>
                  </tr>
                  <tr style="background:#f8fafc;">
                    <td style="padding:8px;font-weight:bold;">
                      Subject:
                    </td>
                    <td style="padding:8px;">%s</td>
                  </tr>
                </table>
                <div style="margin-top:20px;padding:16px;
                            background:#f8fafc;
                            border-radius:8px;">
                  <strong>Message:</strong>
                  <p style="color:#374151;line-height:1.7;">
                    %s
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                form.getName(),
                form.getEmail(), form.getEmail(),
                form.getPhone(),
                form.getSubject(),
                form.getMessage()
                        .replace("\n", "<br>")
        );

        sendEmail(
                supportEmail,
                "Contact: " + form.getSubject() +
                        " — from " + form.getName(),
                html,
                "Contact form"
        );

        // Also send auto-reply to the user
        sendContactAutoReply(form);
    }

    // =========================================================
    // CONTACT FORM AUTO-REPLY
    // =========================================================
    @Async
    public void sendContactAutoReply(ContactFormDTO form) {
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto;
                          background:white;border-radius:12px;
                          overflow:hidden;">
                <div style="background:#1a1a2e;padding:30px;
                            text-align:center;">
                  <h1 style="color:#FCD116;margin:0;">
                    🚌 GhanaRide
                  </h1>
                </div>
                <div style="padding:40px 30px;">
                  <h2>We received your message!</h2>
                  <p style="color:#475569;line-height:1.7;">
                    Hi %s, thank you for contacting GhanaRide.
                    We've received your message and will get
                    back to you within <strong>24 hours</strong>.
                  </p>
                  <div style="background:#f8fafc;
                              border-radius:8px;
                              padding:16px;margin:20px 0;">
                    <strong>Your message:</strong>
                    <p style="color:#374151;font-size:14px;">
                      %s
                    </p>
                  </div>
                  <p style="color:#475569;">
                    If your matter is urgent, you can also
                    reach us at:
                    <a href="mailto:%s">%s</a>
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                form.getName(),
                form.getMessage(),
                supportEmail,
                supportEmail,
                buildFooter()
        );

        sendEmail(
                form.getEmail(),
                "We received your message — GhanaRide Support",
                html,
                "Contact auto-reply"
        );
    }

    // =========================================================
    // VERIFICATION EMAIL HTML BUILDER
    // =========================================================
    private String buildVerificationEmailHtml(
            String fullName, String verifyUrl
    ) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:0 auto;
                          background:white;border-radius:12px;
                          overflow:hidden;
                          box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                <div style="background:#1a1a2e;padding:30px;
                            text-align:center;">
                  <h1 style="color:#FCD116;margin:0;
                             font-size:28px;">
                    🚌 GhanaRide
                  </h1>
                  <p style="color:#94a3b8;margin:5px 0 0;">
                    Your Ride Across Ghana, Simplified
                  </p>
                </div>
                <div style="padding:40px 30px;">
                  <h2 style="color:#1e293b;margin-top:0;">
                    Welcome, %s! 👋
                  </h2>
                  <p style="color:#475569;line-height:1.7;">
                    Thank you for registering with GhanaRide.
                    Click the button below to verify your email
                    and activate your account:
                  </p>
                  <div style="text-align:center;margin:30px 0;">
                    <a href="%s"
                       style="background:#FCD116;color:#000;
                              padding:14px 32px;
                              border-radius:8px;
                              text-decoration:none;
                              font-weight:700;
                              font-size:16px;">
                      ✅ Verify My Email
                    </a>
                  </div>
                  <div style="background:#f8fafc;
                              border-left:4px solid #FCD116;
                              padding:12px 16px;
                              border-radius:4px;">
                    <p style="color:#64748b;margin:0;
                              font-size:14px;">
                      ⏰ This link expires in
                      <strong>24 hours</strong>.
                    </p>
                  </div>
                  <p style="color:#64748b;font-size:13px;
                            margin-top:20px;">
                    If the button doesn't work, copy this link:
                    <br>
                    <span style="word-break:break-all;
                                 color:#3b82f6;">%s</span>
                  </p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                fullName, verifyUrl, verifyUrl,
                buildFooter()
        );
    }

    // =========================================================
    // SHARED FOOTER HTML
    // =========================================================
    private String buildFooter() {
        return """
            <div style="background:#f8fafc;padding:20px 30px;
                        text-align:center;
                        border-top:1px solid #e2e8f0;">
              <p style="color:#94a3b8;font-size:13px;margin:0;">
                © 2025 GhanaRide. All rights reserved.
              </p>
              <p style="color:#94a3b8;font-size:13px;margin:4px 0 0;">
                🇬🇭 Made for Ghana |
                <a href="https://ghanaride.me"
                   style="color:#3b82f6;">
                  ghanaride.me
                </a>
              </p>
            </div>
            """;
    }

    // =========================================================
    // NOTIFICATION EMAIL – v5 WORLD CLASS
    // Generic in-app notification mirror to email
    // =========================================================
    @Async
    public void sendNotificationEmail(String toEmail, String title, String message, String actionUrl) {
        if (toEmail == null || toEmail.isBlank()) return;
        String cta = (actionUrl != null && !actionUrl.isBlank())
                ? "<div style=\"text-align:center;margin:28px 0;\"><a href=\"" + baseUrl + actionUrl + "\" style=\"background:#FCD116;color:#111;padding:13px 28px;border-radius:10px;text-decoration:none;font-weight:700;display:inline-block;\">Open in GhanaRide →</a></div>"
                : "";
        String html = """
            <!DOCTYPE html>
            <html><body style="font-family:Inter,Arial,sans-serif;background:#0c0c18;padding:20px;margin:0;color:#e8e8f0;">
              <div style="max-width:560px;margin:0 auto;background:#16162a;border-radius:16px;overflow:hidden;border:1px solid rgba(255,255,255,0.08);">
                <div style="background:linear-gradient(135deg,#0F9D58,#0a7a44);padding:26px 28px;">
                  <div style="font-weight:800;font-size:20px;color:#FCD116;">🇬🇭 GhanaRide</div>
                  <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">Instant notification</div>
                </div>
                <div style="padding:32px 28px;">
                  <h2 style="margin:0 0 10px;color:#fff;">%s</h2>
                  <p style="color:#a0a0c0;line-height:1.7;font-size:15px;">%s</p>
                  %s
                  <p style="color:#6a6a90;font-size:12px;margin-top:22px;">You’re receiving this because you have GhanaRide notifications enabled.<br>Manage in Profile → Notifications.</p>
                </div>
                %s
              </div>
            </body></html>
            """.formatted(escapeHtml(title), escapeHtml(message), cta, buildFooterDark());
        sendEmail(toEmail, title + " | GhanaRide", html, "Notification email");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private String buildFooterDark() {
        return """
            <div style="background:#111124;padding:18px 28px;text-align:center;border-top:1px solid rgba(255,255,255,0.06);">
              <p style="color:#7878a0;font-size:12px;margin:0;">© 2026 GhanaRide • Accra, Ghana • <a href="https://ghanaride.me" style="color:#FCD116;text-decoration:none;">ghanaride.me</a></p>
              <p style="color:#4a4a6a;font-size:11px;margin:6px 0 0;">The smartest way to travel Ghana</p>
            </div>
            """;
    }

    // =========================================================
    // CORE SEND METHOD
    // Single place for all email sending logic
    // All exceptions caught here — never propagate
    // =========================================================
    private void sendEmail(
            String to,
            String subject,
            String htmlContent,
            String emailType
    ) {
        try {
            MimeMessage message =
                    mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message, true, "UTF-8"
                    );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("{} sent successfully to: {}",
                    emailType, to);

        } catch (MessagingException e) {
            log.error(
                    "MAIL FAILURE: {} to {}: {}",
                    emailType, to, e.getMessage()
            );
        } catch (Exception e) {
            log.error(
                    "MAIL FAILURE: Unexpected error sending " +
                            "{} to {}: {}",
                    emailType, to, e.getMessage()
            );
        }
    }
}