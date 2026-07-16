package com.ghanaride.controller;

import com.ghanaride.entity.PasswordResetToken;
import com.ghanaride.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Handles password reset flow:
 * 1. User submits email → token created → email sent
 * 2. User clicks link → token validated → reset form shown
 * 3. User submits new password → password updated
 *
 * NOTE: AuthController also had /forgot-password and
 * /reset-password methods. Those have been REMOVED from
 * AuthController to avoid duplicate mapping conflicts.
 * This controller is the single source of truth for
 * password reset.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // Minimum password requirements
    private static final int MIN_PASSWORD_LENGTH = 8;

    // =========================================================
    // FORGOT PASSWORD — GET
    // =========================================================
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(
            Authentication authentication,
            Model model
    ) {
        // Already logged in? No need for password reset
        if (authentication != null &&
                authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pageTitle",
                "Forgot Password — GhanaRide");
        model.addAttribute("pageDescription",
                "Reset your GhanaRide account password. " +
                        "Enter your email to receive a reset link.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/forgot-password");

        return "forgot-password";
    }

    // =========================================================
    // FORGOT PASSWORD — POST
    // =========================================================
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam String emailOrUsername,
            HttpServletRequest request,
            Model model
    ) {
        // Basic input validation
        if (emailOrUsername == null ||
                emailOrUsername.isBlank()) {
            model.addAttribute("pageTitle",
                    "Forgot Password — GhanaRide");
            model.addAttribute("error",
                    "Please enter your email address.");
            return "forgot-password";
        }

        // Sanitize input
        String sanitizedInput = emailOrUsername.trim()
                .toLowerCase();

        try {
            boolean sent = passwordResetService
                    .createPasswordResetToken(sanitizedInput);

            if (!sent) {
                // Rate limited — but use vague message
                // (don't reveal whether account exists)
                model.addAttribute("pageTitle",
                        "Forgot Password — GhanaRide");
                model.addAttribute("error",
                        "Please wait at least 1 minute before " +
                                "requesting another reset link.");
                return "forgot-password";
            }

            log.info(
                    "Password reset requested for: {} from IP: {}",
                    sanitizedInput,
                    getClientIp(request)
            );

        } catch (Exception e) {
            // Log the real error but never reveal it
            log.error(
                    "Password reset error for input: {}",
                    sanitizedInput, e
            );
            // Fall through to show generic success
            // (prevents account enumeration)
        }

        // ALWAYS show success — never reveal if account exists
        model.addAttribute("pageTitle",
                "Forgot Password — GhanaRide");
        model.addAttribute("success",
                "If an account exists with that email, " +
                        "a password reset link has been sent. " +
                        "Please check your inbox and spam folder.");

        return "forgot-password";
    }

    // =========================================================
    // RESET PASSWORD — GET
    // (User clicks link from email)
    // =========================================================
    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam String token,
            Model model
    ) {
        // Validate token before showing form
        Optional<PasswordResetToken> tokenOpt =
                passwordResetService.validatePasswordResetToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid/expired password reset token used");
            model.addAttribute("pageTitle",
                    "Forgot Password — GhanaRide");
            model.addAttribute("error",
                    "This reset link is invalid or has expired. " +
                            "Please request a new one.");
            return "forgot-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("pageTitle",
                "Set New Password — GhanaRide");
        model.addAttribute("pageDescription",
                "Set a new secure password for your GhanaRide account.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/reset-password");

        return "reset-password";
    }

    // =========================================================
    // RESET PASSWORD — POST
    // =========================================================
    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Step 1: Re-validate token (may have expired
        // between GET and POST)
        Optional<PasswordResetToken> tokenOpt =
                passwordResetService.validatePasswordResetToken(token);

        if (tokenOpt.isEmpty()) {
            model.addAttribute("pageTitle",
                    "Forgot Password — GhanaRide");
            model.addAttribute("error",
                    "This reset link is invalid or has expired. " +
                            "Please request a new one.");
            return "forgot-password";
        }

        // Step 2: Validate password length
        if (password == null ||
                password.length() < MIN_PASSWORD_LENGTH) {
            model.addAttribute("pageTitle",
                    "Set New Password — GhanaRide");
            model.addAttribute("error",
                    "Password must be at least " +
                            MIN_PASSWORD_LENGTH + " characters long.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        // Step 3: Validate password strength
        if (!isPasswordStrong(password)) {
            model.addAttribute("pageTitle",
                    "Set New Password — GhanaRide");
            model.addAttribute("error",
                    "Password must contain at least one uppercase " +
                            "letter, one lowercase letter, and one number.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        // Step 4: Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("pageTitle",
                    "Set New Password — GhanaRide");
            model.addAttribute("error",
                    "Passwords do not match. Please try again.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        // Step 5: Reset the password
        try {
            passwordResetService.resetPassword(
                    tokenOpt.get(), password
            );

            log.info(
                    "Password successfully reset for user: {} " +
                            "from IP: {}",
                    tokenOpt.get().getUser().getEmail(),
                    getClientIp(request)
            );

            redirectAttributes.addFlashAttribute("success",
                    "Password reset successfully! " +
                            "Please log in with your new password.");
            return "redirect:/login?resetSuccess=true";

        } catch (Exception e) {
            log.error(
                    "Failed to reset password for token", e
            );
            model.addAttribute("pageTitle",
                    "Set New Password — GhanaRide");
            model.addAttribute("error",
                    "Failed to reset password. " +
                            "Please try again or request a new reset link.");
            model.addAttribute("token", token);
            return "reset-password";
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Validates password strength:
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     */
    private boolean isPasswordStrong(String password) {
        boolean hasUpper   = password.chars()
                .anyMatch(Character::isUpperCase);
        boolean hasLower   = password.chars()
                .anyMatch(Character::isLowerCase);
        boolean hasDigit   = password.chars()
                .anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Gets real client IP behind Railway's reverse proxy
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor =
                request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null &&
                !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}