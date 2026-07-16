package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Booking status lifecycle.
 */
@Getter
@RequiredArgsConstructor
public enum BookingStatus {
    PENDING_PAYMENT("Pending Payment"),
    ACTIVE("Active"),
    CONFIRMED("Confirmed"),
    PAID("Paid"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired"),
    NO_SHOW("No Show");

    private final String displayName;
}