package com.ghanaride.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Profile Update DTO.
 */

public class ProfileUpdateDTO {

    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @Pattern(regexp = "^[+]?[0-9\\s-]{10,20}$", message = "Invalid phone number format")
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

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public Boolean getPushNotifications() { return pushNotifications; }
    public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }
    public Boolean getSmsAlerts() { return smsAlerts; }
    public void setSmsAlerts(Boolean smsAlerts) { this.smsAlerts = smsAlerts; }
    public Boolean getPromoEmails() { return promoEmails; }
    public void setPromoEmails(Boolean promoEmails) { this.promoEmails = promoEmails; }
    public Boolean getPriceDropAlerts() { return priceDropAlerts; }
    public void setPriceDropAlerts(Boolean priceDropAlerts) { this.priceDropAlerts = priceDropAlerts; }
}