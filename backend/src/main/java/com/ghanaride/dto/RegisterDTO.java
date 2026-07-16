package com.ghanaride.dto;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Registration form DTO with full validation.
 * Replaces direct User entity binding in forms
 * (binding entities directly to forms is a security risk —
 *  attackers can set fields that shouldn't be user-settable)
 */
@Data
public class RegisterDTO {

    // ---------------------------------------------------------
    // Core fields (all account types)
    // ---------------------------------------------------------
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100,
            message = "Name must be between 2 and 100 characters")
    @Pattern(
            regexp = "^[a-zA-Z\\s'-]+$",
            message = "Name can only contain letters, spaces, " +
                    "hyphens, and apostrophes"
    )
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50,
            message = "Username must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_.-]+$",
            message = "Username can only contain letters, numbers, " +
                    "underscores, dots, and hyphens"
    )
    private String username;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?" +
                    "[0-9]{3}[-\\s\\.]?[0-9]{4,6}$",
            message = "Please enter a valid Ghana phone number " +
                    "(e.g. 0244123456 or +233244123456)"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100,
            message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase " +
                    "letter, one lowercase letter, and one number"
    )
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    /**
     * Comes from the hidden "accountType" input on the registration
     * form, driven by the passenger/driver/company selector cards.
     * Expected values: "passenger", "driver", "company".
     * Mapped to the Role enum in toUser() below.
     */
    @NotBlank(message = "Please select an account type")
    private String accountType;

    // ---------------------------------------------------------
    // Company-specific fields (only required if accountType=company)
    // ---------------------------------------------------------
    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String companyLocation;
    private String companyDescription;
    private String registrationNumber;

    // ---------------------------------------------------------
    // Convert DTO to User entity for saving
    // ---------------------------------------------------------
    public User toUser() {
        User user = new User();
        user.setUsername(this.username);
        user.setFullName(this.fullName);
        user.setEmail(this.email.toLowerCase().trim());
        user.setPassword(this.password); // Will be encoded in service
        user.setPhoneNumber(this.phone);
        user.setAccountType(this.accountType);
        user.setRole(mapAccountTypeToRole(this.accountType));
        return user;
    }

    private Role mapAccountTypeToRole(String accountType) {
        if (accountType == null) {
            return Role.USER;
        }
        return switch (accountType.toLowerCase()) {
            case "driver" -> Role.DRIVER;
            case "company" -> Role.COMPANY;
            default -> Role.USER; // "passenger" and any unrecognized value
        };
    }
}