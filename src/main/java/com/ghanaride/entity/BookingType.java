package com.ghanaride.entity;

/**
 * Booking type - self or for someone else (relative).
 */
public enum BookingType {
    SELF("Self Booking"),
    RELATIVE("Booking for Someone Else");

    private final String displayName;

    BookingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}