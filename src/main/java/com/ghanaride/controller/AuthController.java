package com.ghanaride.controller;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Authentication Controller - Handles login, registration, password reset.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    // =========================================================
    // LOGIN
    // =========================================================

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String session,
            Model model
    ) {
        // If already authenticated, redirect to dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pageTitle", "Login — GhanaRide");
        model.addAttribute("pageDescription", "Sign in to your GhanaRide account to book rides, manage bookings, and access your wallet.");

        if (redirect != null) {
            model.addAttribute("redirect", redirect);
        }
        if (error != null) {
            model.addAttribute("error", "Invalid username/email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully.");
        }
        if (session != null) {
            model.addAttribute("error", "Your session has expired. Please log in again.");
        }

        return "login";
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @GetMapping("/register")
    public String showRegisterPage(
            @RequestParam(required = false) String tab,
            Model model
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pageTitle", "Create Account — GhanaRide");
        model.addAttribute("pageDescription", "Join thousands of travelers across Ghana. Book rides, become a driver, or manage your fleet.");

        // Preserve tab selection from query param
        String activeTab = (tab != null ? tab.toUpperCase() : "PASSENGER");
        model.addAttribute("activeTab", activeTab);

        // Add empty DTO for form binding
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterRequestDTO());
        }

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterRequestDTO dto,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile licenseFile,
            @RequestParam(required = false) MultipartFile idFile,
            @RequestParam(required = false) MultipartFile registrationCertificate,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        // Validate password match
        if (!dto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        // Role-specific validation
        String role = dto.getRole() != null ? dto.getRole().toUpperCase() : "PASSENGER";

        if ("DRIVER".equals(role)) {
            if (licenseFile == null || licenseFile.isEmpty()) {
                bindingResult.rejectValue("licenseFile", "error.licenseFile", "Driver license is required");
            }
            if (idFile == null || idFile.isEmpty()) {
                bindingResult.rejectValue("idFile", "error.idFile", "ID document is required");
            }
        }

        if ("COMPANY".equals(role)) {
            if (dto.getCompanyName() == null || dto.getCompanyName().isBlank()) {
                bindingResult.rejectValue("companyName", "error.companyName", "Company name is required");
            }
            if (registrationCertificate == null || registrationCertificate.isEmpty()) {
                bindingResult.rejectValue("registrationCertificate", "error.registrationCertificate", "Registration certificate is required");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeTab", role);
            model.addAttribute("pageTitle", "Create Account — GhanaRide");
            return "register";
        }

        try {
            User user;
            switch (role) {
                case "DRIVER" -> user = userService.registerDriver(dto, licenseFile, idFile);
                case "COMPANY" -> user = userService.registerCompany(dto, registrationCertificate);
                default -> user = userService.registerPassenger(dto);
            }

            log.info("User registered: {} ({})", user.getEmail(), user.getRole());

            redirectAttributes.addFlashAttribute("success",
                "Account created successfully! Welcome to GhanaRide, " + user.getFullName() + ".");

            return "redirect:/login?registered=true";

        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            bindingResult.rejectValue("email", "error.email", e.getMessage());
            model.addAttribute("activeTab", role);
            model.addAttribute("pageTitle", "Create Account — GhanaRide");
            return "register";
        } catch (Exception e) {
            log.error("Registration error", e);
            bindingResult.rejectValue("email", "error.email", "Registration failed. Please try again.");
            model.addAttribute("activeTab", role);
            model.addAttribute("pageTitle", "Create Account — GhanaRide");
            return "register";
        }
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("pageTitle", "Forgot Password — GhanaRide");
        model.addAttribute("pageDescription", "Enter your email or username and we'll send you a password reset link.");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String emailOrUsername,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.sendPasswordResetEmail(emailOrUsername);
            redirectAttributes.addFlashAttribute("success",
                "If the account exists, a password reset link has been sent to your email.");
        } catch (Exception e) {
            log.error("Password reset request failed", e);
            // Don't reveal if email exists
            redirectAttributes.addFlashAttribute("success",
                "If the account exists, a password reset link has been sent to your email.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
            @RequestParam String token,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!userService.isValidPasswordResetToken(token)) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired reset token.");
            return "redirect:/forgot-password";
        }

        model.addAttribute("pageTitle", "Reset Password — GhanaRide");
        model.addAttribute("token", token);
        model.addAttribute("resetForm", new ResetPasswordDTO());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @Valid @ModelAttribute("resetForm") ResetPasswordDTO dto,
            BindingResult bindingResult,
            @RequestParam String token,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Reset Password — GhanaRide");
            model.addAttribute("token", token);
            return "reset-password";
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
            model.addAttribute("pageTitle", "Reset Password — GhanaRide");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            userService.resetPassword(token, dto.getNewPassword());
            redirectAttributes.addFlashAttribute("success", "Password has been reset. You can now log in with your new password.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Password reset failed", e);
            bindingResult.rejectValue("newPassword", "error.newPassword", "Failed to reset password. Token may be expired.");
            model.addAttribute("pageTitle", "Reset Password — GhanaRide");
            model.addAttribute("token", token);
            return "reset-password";
        }
    }

    // =========================================================
    // LOGOUT SUCCESS
    // =========================================================

    @GetMapping("/logout-success")
    public String logoutSuccess(Model model) {
        return "redirect:/?logged_out=true";
    }
}