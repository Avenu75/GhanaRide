package com.ghanaride.controller;

import com.ghanaride.dto.RegisterRequestDTO;
import com.ghanaride.dto.ResetPasswordDTO;
import com.ghanaride.entity.User;
import com.ghanaride.service.EmailService;
import com.ghanaride.service.FileStorageService;
import com.ghanaride.service.UserService;
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

/**
 * Authentication Controller - Handles login, registration.
 *
 * FIXED: Removed duplicate /forgot-password and /reset-password mappings.
 * Those are now handled ONLY by PasswordResetController to avoid
 * Ambiguous mapping error.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String session,
            Model model
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("pageTitle", "Login — GhanaRide");
        if (redirect != null) model.addAttribute("redirect", redirect);
        if (error != null) model.addAttribute("error", "Invalid username/email or password. Please try again.");
        if (logout != null) model.addAttribute("success", "You have been logged out successfully.");
        if (session != null) model.addAttribute("error", "Your session has expired. Please log in again.");
        return "login";
    }

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
        String activeTab = (tab != null ? tab.toUpperCase() : "PASSENGER");
        model.addAttribute("activeTab", activeTab);
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
        if (!dto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }
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

    @GetMapping("/logout-success")
    public String logoutSuccess(Model model) {
        return "redirect:/?logged_out=true";
    }
}
