package com.ghanaride.controller;

import com.ghanaride.dto.ProfileUpdateDTO;
import com.ghanaride.entity.User;
import com.ghanaride.service.FileStorageService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ghanaride.repository.UserRepository;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Handles user profile management:
 * - View profile
 * - Update personal info
 * - Upload profile image
 * - Change password
 */
@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    // Allowed image types for profile pictures
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // Max profile image size: 2MB
    // (smaller than car images since profile pics
    //  are displayed at small sizes)
    private static final long MAX_PROFILE_IMAGE_SIZE =
            2 * 1024 * 1024;

    // Max field lengths
    private static final int MAX_NAME_LENGTH    = 100;
    private static final int MAX_PHONE_LENGTH   = 20;
    private static final int MAX_ADDRESS_LENGTH = 255;

    // =========================================================
    // VIEW PROFILE
    // v3.2 – adds profile-completion banner support
    //   ?complete=oauth|booking|required
    //   ?redirect=/booking/7
    //   ?tripId=7
    //   ?welcome=true
    // =========================================================
    @GetMapping
    public String viewProfile(
            Principal principal,
            Model model,
            @RequestParam(required = false) String complete,
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) String welcome
    ) {
        User currentUser =
                userService.getCurrentUser(principal);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle",
                "My Profile — GhanaRide");
        model.addAttribute("pageDescription",
                "Manage your GhanaRide profile, personal " +
                        "information, and account settings.");

        // v3.2 profile completion helpers
        model.addAttribute("missingFields", userService.getMissingProfileFields(currentUser));
        model.addAttribute("profileComplete", userService.isProfileComplete(currentUser));

        // pass-through UI flags for Thymeleaf banners
        if (complete != null) model.addAttribute("complete", complete);
        if (redirect != null) model.addAttribute("redirectUrl", redirect);
        if (tripId != null) model.addAttribute("tripId", tripId);
        if (welcome != null) model.addAttribute("welcome", welcome);

        // If coming from booking flow, show a stronger CTA
        if ("booking".equals(complete) || "required".equals(complete)) {
            model.addAttribute("profileAlert",
                    "A Ghana phone number is required before you can book. Drivers use this to confirm pickup.");
        } else if ("oauth".equals(complete)) {
            model.addAttribute("profileAlert",
                    "Welcome to GhanaRide! Complete your profile – add your phone number to start booking.");
        }

        return "profile";
    }

    // =========================================================
    // UPDATE PROFILE INFO
    // =========================================================
    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        // Input validation
        if (fullName == null || fullName.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Full name is required.");
            return "redirect:/profile";
        }

        if (fullName.length() > MAX_NAME_LENGTH) {
            redirectAttributes.addFlashAttribute("error",
                    "Full name is too long " +
                            "(max " + MAX_NAME_LENGTH + " characters).");
            return "redirect:/profile";
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Phone number is required.");
            return "redirect:/profile";
        }

        if (phoneNumber.length() > MAX_PHONE_LENGTH) {
            redirectAttributes.addFlashAttribute("error",
                    "Phone number is too long.");
            return "redirect:/profile";
        }

        // v5.2 GHANA ONLY – strict +233 / 0XX validation
        if (!com.ghanaride.config.GhanaOnlyConfig.isValidGhanaPhone(phoneNumber)) {
            redirectAttributes.addFlashAttribute("error",
                    "Please enter a valid Ghana phone number – e.g. 0244123456 or +233244123456. Ghana only.");
            return "redirect:/profile";
        }

        if (address != null &&
                address.length() > MAX_ADDRESS_LENGTH) {
            redirectAttributes.addFlashAttribute("error",
                    "Address is too long " +
                            "(max " + MAX_ADDRESS_LENGTH + " characters).");
            return "redirect:/profile";
        }

        // Validate gender value
        if (gender != null && !gender.isBlank() &&
                !Set.of("MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY")
                        .contains(gender.toUpperCase())) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid gender value.");
            return "redirect:/profile";
        }

        // Parse date of birth safely
        LocalDate dob = null;
        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            try {
                dob = LocalDate.parse(dateOfBirth);

                // Must be at least 16 years old
                if (dob.isAfter(
                        LocalDate.now().minusYears(16))) {
                    redirectAttributes.addFlashAttribute("error",
                            "You must be at least 16 years old " +
                                    "to use GhanaRide.");
                    return "redirect:/profile";
                }

                // Must be realistic (not before 1900)
                if (dob.isBefore(LocalDate.of(1900, 1, 1))) {
                    redirectAttributes.addFlashAttribute("error",
                            "Please enter a valid date of birth.");
                    return "redirect:/profile";
                }

            } catch (DateTimeParseException e) {
                redirectAttributes.addFlashAttribute("error",
                        "Invalid date of birth format. " +
                                "Please use the date picker.");
                return "redirect:/profile";
            }
        }

        // Update via service layer (not directly via repo)
        try {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setFullName(fullName.trim());
            dto.setPhoneNumber(phoneNumber.trim());
            dto.setAddress(address != null ? address.trim() : null);
            dto.setDateOfBirth(dob);
            dto.setGender(gender != null ? gender.toUpperCase() : null);
            User user = userService.getCurrentUser(principal);
            userService.updateProfile(user, dto);

            log.info("Profile updated for user: {}",
                    principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Profile updated successfully!");

        } catch (Exception e) {
            log.error("Profile update failed for user: {}",
                    principal.getName(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to update profile. Please try again.");
        }

        return "redirect:/profile";
    }

    // =========================================================
    // UPLOAD PROFILE IMAGE
    // =========================================================
    @PostMapping("/upload-image")
    public String uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        // Check file was selected
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Please select an image to upload.");
            return "redirect:/profile";
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null ||
                !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            redirectAttributes.addFlashAttribute("error",
                    "Profile image must be JPEG, PNG, or WebP. " +
                            "Other formats are not supported.");
            return "redirect:/profile";
        }

        // Validate file size
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            redirectAttributes.addFlashAttribute("error",
                    "Profile image must be smaller than 2MB. " +
                            "Please compress your image and try again.");
            return "redirect:/profile";
        }

        // Validate filename (prevent path traversal)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null &&
                (originalFilename.contains("..") ||
                        originalFilename.contains("/"))) {
            log.warn(
                    "Suspicious filename in profile upload: {} " +
                            "by user: {}",
                    originalFilename, principal.getName()
            );
            redirectAttributes.addFlashAttribute("error",
                    "Invalid file name.");
            return "redirect:/profile";
        }

        try {
            User user = userService.getCurrentUser(principal);
            userService.updateAvatar(user, file);

            log.info("Profile image updated for user: {}",
                    principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Profile picture updated successfully!");

        } catch (Exception e) {
            log.error(
                    "Profile image upload failed for user: {}",
                    principal.getName(), e
            );
            // Generic error — don't expose file system details
            redirectAttributes.addFlashAttribute("error",
                    "Failed to upload image. " +
                            "Please try again with a different image.");
        }

        return "redirect:/profile";
    }

    // =========================================================
    // CHANGE PASSWORD
    // (Separate from password reset — user must know
    //  current password)
    // =========================================================
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        // Validate new password length
        if (newPassword == null ||
                newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error",
                    "New password must be at least 8 characters.");
            return "redirect:/profile";
        }

        // Validate password strength
        boolean hasUpper = newPassword.chars()
                .anyMatch(Character::isUpperCase);
        boolean hasLower = newPassword.chars()
                .anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars()
                .anyMatch(Character::isDigit);

        if (!hasUpper || !hasLower || !hasDigit) {
            redirectAttributes.addFlashAttribute("error",
                    "New password must contain at least one " +
                            "uppercase letter, one lowercase letter, " +
                            "and one number.");
            return "redirect:/profile";
        }

        // Validate passwords match
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("error",
                    "New passwords do not match.");
            return "redirect:/profile";
        }

        // Don't allow same password
        if (currentPassword.equals(newPassword)) {
            redirectAttributes.addFlashAttribute("error",
                    "New password must be different from " +
                            "your current password.");
            return "redirect:/profile";
        }

        try {
            User user = userService.getCurrentUser(principal);
            userService.changePassword(
                    user, currentPassword, newPassword
            );

            log.info("Password changed for user: {}",
                    principal.getName());

            redirectAttributes.addFlashAttribute("success",
                    "Password changed successfully! " +
                            "Please use your new password next time " +
                            "you log in.");

        } catch (IllegalArgumentException e) {
            // Service throws this for wrong current password
            redirectAttributes.addFlashAttribute("error",
                    "Current password is incorrect.");
        } catch (Exception e) {
            log.error(
                    "Password change failed for user: {}",
                    principal.getName(), e
            );
            redirectAttributes.addFlashAttribute("error",
                    "Failed to change password. Please try again.");
        }

        return "redirect:/profile";
    }
}