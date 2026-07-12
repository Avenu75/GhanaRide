package com.ghanaride.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Notification Preferences DTO.
 */
@Data
@NoArgsConstructor
public class NotificationPreferencesDTO {

    private Boolean emailNotifications = true;
    private Boolean pushNotifications = true;
    private Boolean smsAlerts = true;
    private Boolean promoEmails = false;
    private Boolean priceDropAlerts = true;
    private Boolean departureReminders = true;
    private Boolean paymentReceipts = true;
    private Boolean reviewRequests = true;
}