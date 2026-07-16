package com.ghanaride.controller;

import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.service.FileStorageService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String viewProfile(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", currentUser);
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.getCurrentUser(principal);
        currentUser.setFullName(fullName);
        currentUser.setPhoneNumber(phoneNumber);
        currentUser.setAddress(address);
        currentUser.setGender(gender);
        
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            currentUser.setDateOfBirth(LocalDate.parse(dateOfBirth));
        }

        userRepository.save(currentUser);

        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/profile";
    }

    @PostMapping("/upload-image")
    public String uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select an image to upload.");
            return "redirect:/profile";
        }

        try {
            User currentUser = userService.getCurrentUser(principal);
            String imagePath = fileStorageService.storeProfileImage(file);
            currentUser.setProfileImagePath("/" + imagePath);
            userRepository.save(currentUser);
            redirectAttributes.addFlashAttribute("success", "Profile image updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}
