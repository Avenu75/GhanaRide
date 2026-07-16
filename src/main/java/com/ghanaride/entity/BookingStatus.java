package com.ghanaride.entity;

/**
 * Booking status lifecycle.
 */
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

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}