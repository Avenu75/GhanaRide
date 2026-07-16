package com.ghanaride.controller;

import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

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
            model.addAttribute("user", user);
            return "register";
        }
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username is already taken");
            model.addAttribute("user", user);
            return "register";
        }
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email is already registered");
            model.addAttribute("user", user);
            return "register";
        }

        User savedUser = userService.registerUser(user, companyName, companyEmail,
                companyPhone, companyLocation, companyDescription, registrationNumber);
        log.info("New user registered: {} ({})", savedUser.getUsername(), savedUser.getEmail());

        return "redirect:/login?registered=true";
    }
}