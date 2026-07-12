package com.ghanaride.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Notification type enumeration.
 */
@Getter
@AllArgsConstructor
public enum NotificationType {
    BOOKING_CONFIRMED("Booking Confirmed"),
    BOOKING_CANCELLED("Booking Cancelled"),
    TRIP_CANCELLED("Trip Cancelled"),
    TRIP_REMINDER("Departure Reminder"),
    PAYMENT_RECEIVED("Payment Received"),
    REFUND_PROCESSED("Refund Processed"),
    NEW_BOOKING("New Passenger"),
    TRIP_APPROVED("Trip Approved"),
    TRIP_REJECTED("Trip Rejected"),
    WALLET_TOPUP("Wallet Topped Up"),
    LOYALTY_EARNED("Loyalty Points Earned"),
    REVIEW_RECEIVED("New Review"),
    SYSTEM_MAINTENANCE("System Maintenance"),
    PROMOTION("Promotion"),
    PRICE_DROP("Price Drop Alert"),
    DRIVER_VERIFICATION("Driver Verification"),
    COMPANY_VERIFICATION("Company Verification"),
    PASSWORD_CHANGED("Password Changed"),
    LOGIN_ALERT("Login Alert"),
    TWO_FACTOR_ENABLED("Two Factor Enabled");

    private final String displayName;
}