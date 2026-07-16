package com.ghanaride.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Profile Update DTO.
 */
@Data
@NoArgsConstructor
public class ProfileUpdateDTO {

    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @Pattern(regexp = "^[0-9+\\s-]{10,20}$", message = "Invalid phone number format")
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Gender must be 20 characters or less")
    private String gender;

    @Size(max = 255, message = "Address must be 255 characters or less")
    private String address;

    // Notification preferences
    private Boolean emailNotifications = true;
    private Boolean pushNotifications = true;
    private Boolean smsAlerts = true;
    private Boolean promoEmails = false;
    private Boolean priceDropAlerts = true;
}