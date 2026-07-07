package com.ghanaride.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for the public contact form.
 * Validated before processing to prevent spam/injection.
 */
@Data
public class ContactFormDTO {

    @NotBlank(message = "Your name is required")
    @Size(min = 2, max = 100,
            message = "Name must be between 2 and 100 characters")
    // Prevent HTML/script injection in name field
    @Pattern(
            regexp = "^[a-zA-Z\\s'-]+$",
            message = "Name can only contain letters, spaces, hyphens, " +
                    "and apostrophes"
    )
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email address is too long")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}" +
                    "[-\\s\\.]?[0-9]{4,6}$",
            message = "Please enter a valid phone number"
    )
    private String phone;

    @NotBlank(message = "Subject is required")
    @Size(min = 5, max = 200,
            message = "Subject must be between 5 and 200 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 20, max = 2000,
            message = "Message must be between 20 and 2000 characters")
    private String message;

    // Optional: type of inquiry
    private String inquiryType; // BOOKING, PAYMENT, DRIVER, GENERAL
}