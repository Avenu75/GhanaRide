package com.ghanaride.controller;

import com.ghanaride.dto.ContactFormDTO;
import com.ghanaride.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles all public informational pages:
 * About, Contact, Terms, Privacy, FAQ, Routes
 *
 * All routes here are permitAll() in SecurityConfig
 *
 * FIXED: Removed duplicate "/" mapping that conflicted with HomeController
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final ContactService contactService;

    // HOME removed - HomeController handles "/" now. Keeping /home and /index for backward compat
    @GetMapping({"/index", "/home"})
    public String homeRedirect() {
        return "redirect:/";
    }

    @GetMapping("/about")
    public String showAboutPage(Model model) {
        model.addAttribute("pageTitle", "About GhanaRide — Ghana's Trusted Transport Platform");
        model.addAttribute("pageDescription",
                "Learn about GhanaRide, Ghana's trusted campus and intercity transport booking platform. ");
        model.addAttribute("pageUrl", "https://ghanaride.me/about");
        return "about";
    }

    @GetMapping("/contact")
    public String showContactPage(Model model) {
        model.addAttribute("pageTitle", "Contact GhanaRide — Get in Touch");
        model.addAttribute("pageDescription", "Contact GhanaRide support team.");
        model.addAttribute("pageUrl", "https://ghanaride.me/contact");
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactFormDTO());
        }
        return "contact";
    }

    @PostMapping("/contact")
    public String handleContactSubmit(
            @Valid @ModelAttribute("contactForm") ContactFormDTO contactForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Contact GhanaRide");
            return "contact";
        }
        try {
            contactService.sendContactEmail(contactForm);
            log.info("Contact form submitted by: {} <{}>", contactForm.getName(), contactForm.getEmail());
            redirectAttributes.addFlashAttribute("success",
                    "Thank you for contacting us, " + contactForm.getName() + "! We'll get back to you within 24 hours.");
        } catch (Exception e) {
            log.error("Failed to send contact email from: {}", contactForm.getEmail(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Sorry, we couldn't send your message right now. Please email support@ghanaride.me");
        }
        return "redirect:/contact";
    }

    @GetMapping("/terms")
    public String showTermsPage(Model model) {
        model.addAttribute("pageTitle", "Terms of Service — GhanaRide");
        model.addAttribute("pageUrl", "https://ghanaride.me/terms");
        model.addAttribute("lastUpdated", "January 2025");
        return "terms";
    }

    @GetMapping("/privacy")
    public String showPrivacyPage(Model model) {
        model.addAttribute("pageTitle", "Privacy Policy — GhanaRide");
        model.addAttribute("pageUrl", "https://ghanaride.me/privacy");
        model.addAttribute("lastUpdated", "January 2025");
        return "privacy";
    }

    @GetMapping("/refunds")
    public String showRefundsPage(Model model) {
        model.addAttribute("pageTitle", "Refund Policy — GhanaRide");
        model.addAttribute("pageUrl", "https://ghanaride.me/refunds");
        return "refunds";
    }

    @GetMapping("/faq")
    public String showFaqPage(Model model) {
        model.addAttribute("pageTitle", "Help Center — GhanaRide");
        model.addAttribute("pageUrl", "https://ghanaride.me/faq");
        return "faq";
    }

    @GetMapping("/routes")
    public String showRoutesPage(Model model) {
        model.addAttribute("pageTitle", "Popular Routes — GhanaRide");
        model.addAttribute("pageUrl", "https://ghanaride.me/routes");
        return "routes";
    }
}
