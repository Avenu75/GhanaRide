package com.ghanaride.controller;

import com.ghanaride.entity.PasswordResetToken;
import com.ghanaride.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String emailOrUsername, Model model) {
        boolean sent = passwordResetService.createPasswordResetToken(emailOrUsername);
        if (!sent) {
            model.addAttribute("error", "You must wait at least 1 minute before requesting another reset link.");
            return "forgot-password";
        }
        
        // Never reveal whether user exists: always show success message
        model.addAttribute("success", "If an account exists with this email, a password reset link has been sent.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> tokenOpt = passwordResetService.validatePasswordResetToken(token);
        if (tokenOpt.isEmpty()) {
            model.addAttribute("error", "The reset link is invalid or has expired. Please request a new one.");
            return "forgot-password";
        }
        
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        Optional<PasswordResetToken> tokenOpt = passwordResetService.validatePasswordResetToken(token);
        if (tokenOpt.isEmpty()) {
            model.addAttribute("error", "The reset link is invalid or has expired. Please request a new one.");
            return "forgot-password";
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        passwordResetService.resetPassword(tokenOpt.get(), password);
        return "redirect:/login?resetSuccess=true";
    }
}
