package com.ghanaride.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends the email verification link.
     * Catches all exceptions internally and logs them — never throws —
     * so a mail failure never rolls back the user/token transaction.
     *
     * @return true if sent successfully, false on failure
     */
    public boolean sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your GhanaRide account");

            String verifyUrl = baseUrl + "/verify-email?token=" + token;

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                        .header { background: #1a1a2e; padding: 30px; text-align: center; }
                        .header h1 { color: #FCD116; margin: 0; font-size: 28px; }
                        .header p { color: #94a3b8; margin: 5px 0 0; }
                        .body { padding: 40px 30px; }
                        .body h2 { color: #1e293b; margin-top: 0; }
                        .body p { color: #475569; line-height: 1.7; }
                        .btn { display: inline-block; background: #FCD116; color: #000 !important; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 16px; margin: 20px 0; }
                        .note { background: #f8fafc; border-left: 4px solid #FCD116; padding: 12px 16px; border-radius: 4px; margin: 20px 0; }
                        .note p { color: #64748b; margin: 0; font-size: 14px; }
                        .footer { background: #f8fafc; padding: 20px 30px; text-align: center; border-top: 1px solid #e2e8f0; }
                        .footer p { color: #94a3b8; font-size: 13px; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🚌 GhanaRide</h1>
                            <p>Your Ride Across Ghana, Simplified</p>
                        </div>
                        <div class="body">
                            <h2>Welcome, %s! 👋</h2>
                            <p>Thank you for registering with GhanaRide. You're almost ready to start booking rides across Ghana!</p>
                            <p>Please click the button below to verify your email address and activate your account:</p>
                            <div style="text-align: center;">
                                <a href="%s" class="btn">✅ Verify My Email</a>
                            </div>
                            <div class="note">
                                <p>⏰ This link expires in <strong>24 hours</strong>. If it expires, you can request a new one from the login page.</p>
                            </div>
                            <p>If you didn't create a GhanaRide account, you can safely ignore this email.</p>
                            <p>If the button doesn't work, copy and paste this link into your browser:</p>
                            <p style="word-break: break-all; color: #3b82f6; font-size: 13px;">%s</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 GhanaRide. All rights reserved.</p>
                            <p>🇬🇭 Made for Ghana</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName, verifyUrl, verifyUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent successfully to {}", toEmail);
            return true;

        } catch (MessagingException e) {
            // Log clearly but do NOT throw — email failure must not roll back user/token data
            log.error("MAIL FAILURE: Could not send verification email to {}. Cause: {}", toEmail, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("MAIL FAILURE: Unexpected error sending verification email to {}. Cause: {}", toEmail, e.getMessage());
            return false;
        }
    }

    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to GhanaRide! 🎉");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; }
                        .header { background: #006B3F; padding: 30px; text-align: center; }
                        .header h1 { color: #FCD116; margin: 0; }
                        .body { padding: 40px 30px; }
                        .body p { color: #475569; line-height: 1.7; }
                        .btn { display: inline-block; background: #006B3F; color: white !important; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 700; margin: 20px 0; }
                        .footer { background: #f8fafc; padding: 20px; text-align: center; }
                        .footer p { color: #94a3b8; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 Welcome to GhanaRide!</h1>
                        </div>
                        <div class="body">
                            <h2>Hi %s, your account is verified! ✅</h2>
                            <p>You can now book rides across Ghana with trusted drivers.</p>
                            <div style="text-align: center;">
                                <a href="%s/dashboard" class="btn">🚌 Start Booking Rides</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>© 2025 GhanaRide 🇬🇭</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName, baseUrl);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            // Welcome email is not critical — log and continue
            log.warn("Could not send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}