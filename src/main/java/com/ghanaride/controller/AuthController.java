package com.ghanaride.controller;

import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import com.ghanaride.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final VerificationService verificationService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user,
                               @RequestParam String confirmPassword,
                               @RequestParam(required = false) String companyName,
                               @RequestParam(required = false) String companyEmail,
                               @RequestParam(required = false) String companyPhone,
                               @RequestParam(required = false) String companyLocation,
                               @RequestParam(required = false) String companyDescription,
                               @RequestParam(required = false) String registrationNumber,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username is already taken");
            return "register";
        }
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email is already registered");
            return "register";
        }

        // Register user
        User savedUser = userService.registerUser(user, companyName, companyEmail,
                companyPhone, companyLocation, companyDescription, registrationNumber);

        // Send verification email
        try {
            verificationService.sendVerificationEmail(savedUser);
            redirectAttributes.addFlashAttribute("verificationSent", true);
            redirectAttributes.addFlashAttribute("userEmail", savedUser.getEmail());
        } catch (Exception e) {
            // If email fails, still allow registration but show warning
            redirectAttributes.addFlashAttribute("emailError", true);
        }

        return "redirect:/login?registered=true";
    }

    // ===== VERIFY EMAIL =====
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        String result = verificationService.verifyToken(token);
        model.addAttribute("result", result);
        return "verify-email";
    }

    // ===== RESEND VERIFICATION =====
    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.findByEmail(email).ifPresent(user -> {
                if (!user.isEmailVerified()) {
                    verificationService.sendVerificationEmail(user);
                }
            });
            redirectAttributes.addFlashAttribute("resent", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resentError", true);
        }
        return "redirect:/login";
    }
}