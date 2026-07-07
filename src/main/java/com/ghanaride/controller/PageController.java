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
 * About, Contact, Terms, Privacy, FAQ
 *
 * All routes here are permitAll() in SecurityConfig
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final ContactService contactService;

    // =========================================================
    // ABOUT PAGE
    // =========================================================
    @GetMapping("/about")
    public String showAboutPage(Model model) {
        // SEO attributes — used by Thymeleaf layout
        model.addAttribute("pageTitle",
                "About GhanaRide — Ghana's Trusted Transport Platform");
        model.addAttribute("pageDescription",
                "Learn about GhanaRide, Ghana's trusted campus and " +
                        "intercity transport booking platform. Our mission, " +
                        "values, and the team behind it.");
        model.addAttribute("pageUrl", "https://ghanaride.me/about");
        return "about";
    }

    // =========================================================
    // CONTACT PAGE
    // =========================================================
    @GetMapping("/contact")
    public String showContactPage(Model model) {
        model.addAttribute("pageTitle",
                "Contact GhanaRide — Get in Touch");
        model.addAttribute("pageDescription",
                "Contact GhanaRide support team. We're here to help " +
                        "with bookings, driver queries, and any issues. " +
                        "Reach us via email, phone, or WhatsApp.");
        model.addAttribute("pageUrl", "https://ghanaride.me/contact");

        // Add empty form DTO for Thymeleaf form binding
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
        // Return to form with validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Contact GhanaRide");
            model.addAttribute("pageDescription",
                    "Contact GhanaRide support team.");
            model.addAttribute("pageUrl",
                    "https://ghanaride.me/contact");
            return "contact";
        }

        try {
            // Actually send the contact email (see ContactService)
            contactService.sendContactEmail(contactForm);

            log.info("Contact form submitted by: {} <{}>",
                    contactForm.getName(), contactForm.getEmail());

            redirectAttributes.addFlashAttribute("success",
                    "Thank you for contacting us, " +
                            contactForm.getName() + "! " +
                            "We'll get back to you within 24 hours.");

        } catch (Exception e) {
            // Log the real error, show generic message to user
            log.error("Failed to send contact email from: {}",
                    contactForm.getEmail(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Sorry, we couldn't send your message right now. " +
                            "Please try again or email us directly at " +
                            "support@ghanaride.me");
        }

        return "redirect:/contact";
    }

    // =========================================================
    // FIX #1: TERMS OF SERVICE PAGE
    // Was missing — caused redirect to /login
    // =========================================================
    @GetMapping("/terms")
    public String showTermsPage(Model model) {
        model.addAttribute("pageTitle",
                "Terms of Service — GhanaRide");
        model.addAttribute("pageDescription",
                "GhanaRide Terms of Service. Read our terms covering " +
                        "ride bookings, cancellations, driver conduct, " +
                        "payments via Paystack, and user responsibilities.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/terms");
        model.addAttribute("lastUpdated", "January 2025");
        return "terms";
    }

    // =========================================================
    // FIX #1: PRIVACY POLICY PAGE
    // Was missing — caused redirect to /login
    // =========================================================
    @GetMapping("/privacy")
    public String showPrivacyPage(Model model) {
        model.addAttribute("pageTitle",
                "Privacy Policy — GhanaRide");
        model.addAttribute("pageDescription",
                "GhanaRide Privacy Policy. Learn how we collect, " +
                        "use, and protect your personal data including " +
                        "name, email, phone, and payment information.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/privacy");
        model.addAttribute("lastUpdated", "January 2025");
        return "privacy";
    }

    // =========================================================
    // FIX #1: FAQ PAGE
    // Was missing — caused redirect to /login
    // =========================================================
    @GetMapping("/faq")
    public String showFaqPage(Model model) {
        model.addAttribute("pageTitle",
                "Frequently Asked Questions — GhanaRide");
        model.addAttribute("pageDescription",
                "GhanaRide FAQ. Find answers to common questions " +
                        "about booking rides, payments, cancellations, " +
                        "driver verification, and getting support.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/faq");
        return "faq";
    }

    // =========================================================
    // REFUND POLICY PAGE
    // =========================================================
    @GetMapping("/refunds")
    public String showRefundsPage(Model model) {
        model.addAttribute("pageTitle",
                "Refund Policy — GhanaRide");
        model.addAttribute("pageDescription",
                "GhanaRide Refund Policy. Learn about cancellation " +
                        "timelines, refund processing times, and how to " +
                        "request a refund for your booking.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me/refunds");
        model.addAttribute("lastUpdated", "January 2025");
        return "refunds";
    }

    // =========================================================
    // FIX #2: SITEMAP (served as static file, but
    // adding controller fallback just in case)
    // =========================================================
    @GetMapping("/sitemap.xml")
    public String sitemapFallback() {
        // Static file in /static/sitemap.xml takes precedence
        // This is just a safety fallback
        return "forward:/sitemap.xml";
    }
}