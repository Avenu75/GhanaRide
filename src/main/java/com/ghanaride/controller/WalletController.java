package com.ghanaride.controller;

import com.ghanaride.entity.User;
import com.ghanaride.entity.Wallet;
import com.ghanaride.service.NotificationService;
import com.ghanaride.service.UserService;
import com.ghanaride.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/wallet")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping
    public String wallet(Principal principal, Model model,
                         @RequestParam(defaultValue = "0") int page) {
        User user = userService.getCurrentUser(principal);
        Wallet wallet = walletService.getOrCreateWallet(user);
        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", walletService.history(user.getId(), page, 20));
        model.addAttribute("recent", walletService.recent(user.getId()));
        model.addAttribute("currentUser", user);
        return "wallet";
    }

    @PostMapping("/topup")
    public String topup(@RequestParam BigDecimal amount,
                        @RequestParam(defaultValue = "PAYSTACK") String provider,
                        Principal principal,
                        RedirectAttributes ra) {
        try {
            User user = userService.getCurrentUser(principal);
            if (amount.compareTo(new BigDecimal("5")) < 0) {
                ra.addFlashAttribute("error", "Minimum top-up is GH₵5");
                return "redirect:/wallet";
            }
            walletService.topup(user, amount, provider, null);
            notificationService.push(user, com.ghanaride.entity.Notification.Type.PAYMENT_SUCCESS,
                    "Wallet topped up", "GH₵" + amount + " added successfully.", "/wallet");
            ra.addFlashAttribute("success", "GH₵" + amount + " added to your GhanaRide Wallet!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Top-up failed: " + e.getMessage());
        }
        return "redirect:/wallet";
    }
}
