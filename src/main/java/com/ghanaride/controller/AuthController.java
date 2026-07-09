package com.ghanaride.controller;

import com.ghanaride.dto.RegisterDTO;
import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles user authentication:
 * - Login page display
 * - Registration (Passenger, Driver, Company)
 * - Password reset flow
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // =========================================================
    // LOGIN PAGE
    // =========================================================
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String session,
            @RequestParam(required = false) String redirect,
            Model model,
            Authentication authentication
    ) {
        // Already logged in? Redirect to dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        // Handle error messages
        if ("too_many_attempts".equals(error)) {
            model.addAttribute("loginError",
                    "Too many failed login attempts. " +
                            "Please wait 15 minutes before trying again.");
        } else if ("oauth_failed".equals(error)) {
            model.addAttribute("loginError",
                    "Google sign-in failed. Please try again " +
                            "or use email/password.");
        } else if ("disabled".equals(error)) {
            model.addAttribute("loginError",
                    "Account is disabled or email is not verified. " +
                            "Please contact support.");
        } else if ("locked".equals(error)) {
            model.addAttribute("loginError",
                    "Your account has been locked due to too many failed attempts. " +
                            "Please try again in 15 minutes.");
        } else if (error != null) {
            // Generic error — don't reveal specific reason
            // (prevents username enumeration)
            model.addAttribute("loginError",
                    "Invalid email or password. Please try again.");
        }

        if ("true".equals(registered)) {
            model.addAttribute("successMessage",
                    "Account created successfully! Please log in.");
        }

        if ("expired".equals(session)) {
            model.addAttribute("loginError",
                    "Your session has expired. Please log in again.");
        }

        // Store redirect URL for post-login navigation
        if (redirect != null && !redirect.isBlank()) {
            model.addAttribute("redirectUrl", redirect);
        }

        // SEO
        model.addAttribute("pageTitle",
                "Login — GhanaRide");
        model.addAttribute("pageDescription",
                "Login to GhanaRide to book intercity and campus " +
                        "rides across Ghana.");

        return "login";
    }

    // =========================================================
    // REGISTRATION PAGE
    // =========================================================
    @GetMapping("/register")
    public String registerPage(
            Model model,
            Authentication authentication
    ) {
        // Already logged in? Redirect
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        if (!model.containsAttribute("registerDTO")) {
            model.addAttribute("registerDTO", new RegisterDTO());
        }

        // SEO
        model.addAttribute("pageTitle",
                "Register — Join GhanaRide");
        model.addAttribute("pageDescription",
                "Join GhanaRide as a passenger, driver, or company. " +
                        "Book safe, affordable intercity and campus rides " +
                        "across Ghana.");

        return "register";
    }

    // =========================================================
    // REGISTRATION HANDLER
    // =========================================================
    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request
    ) {
        // Step 1: Check validation annotations
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Register — GhanaRide");
            model.addAttribute("pageDescription",
                    "Join GhanaRide as a passenger, driver, or company.");
            model.addAttribute("error", "Please fix the highlighted errors below.");
            return "register";
        }

        // Step 2: Password confirmation check
        if (!registerDTO.getPassword().equals(
                registerDTO.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "error.registerDTO",
                    "Passwords do not match"
            );
            model.addAttribute("pageTitle", "Register — GhanaRide");
            model.addAttribute("error", "Passwords do not match. Please verify.");
            return "register";
        }

        // Step 3: Check for existing username
        // Use same error message for username/email taken
        // (prevents user enumeration via registration form)
        boolean hasConflict = false;

        if (userService.existsByUsername(registerDTO.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "error.registerDTO",
                    "This username is already taken"
            );
            hasConflict = true;
        }

        if (userService.existsByEmail(registerDTO.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "error.registerDTO",
                    "An account with this email already exists"
            );
            hasConflict = true;
        }

        if (hasConflict) {
            model.addAttribute("pageTitle", "Register — GhanaRide");
            model.addAttribute("error", "Registration failed. Username or email already exists.");
            return "register";
        }

        // Step 4: Register the user
        try {
            User savedUser = userService.registerUser(
                    registerDTO.toUser(),
                    registerDTO.getCompanyName(),
                    registerDTO.getCompanyEmail(),
                    registerDTO.getCompanyPhone(),
                    registerDTO.getCompanyLocation(),
                    registerDTO.getCompanyDescription(),
                    registerDTO.getRegistrationNumber()
            );

            log.info("New user registered: {} ({}) role={}",
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRole()
            );

            // Success — redirect to login with message
            redirectAttributes.addFlashAttribute("success",
                    "Account created successfully! " +
                            "Please check your email to verify your account, " +
                            "then log in.");
            return "redirect:/login?registered=true";

        } catch (Exception e) {
            // Log the real error
            log.error("Registration failed for email: {}",
                    registerDTO.getEmail(), e);

            // Show generic error — don't expose internal details
            model.addAttribute("pageTitle", "Register — GhanaRide");
            model.addAttribute("error",
                    "Registration failed. Please try again. " +
                            "If the problem persists, contact support.");
            return "register";
        }
    }

}