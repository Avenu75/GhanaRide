package com.ghanaride.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

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
    public Boolean getDepartureReminders() { return departureReminders; }
    public void setDepartureReminders(Boolean departureReminders) { this.departureReminders = departureReminders; }
}