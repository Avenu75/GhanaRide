package com.ghanaride.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    @GetMapping("/about")
    public String showAboutPage() {
        return "about";
    }

    @GetMapping("/contact")
    public String showContactPage() {
        return "contact";
    }

    @PostMapping("/contact")
    public String handleContactSubmit(RedirectAttributes redirectAttributes) {
        // In a real application, we would process the contact form (e.g., send an email)
        redirectAttributes.addFlashAttribute("success", "Thank you for contacting us. We will get back to you shortly.");
        return "redirect:/contact";
    }
}
