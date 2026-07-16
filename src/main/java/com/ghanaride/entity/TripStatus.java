package com.ghanaride.entity;

/**
 * Trip status lifecycle.
 */
public enum TripStatus {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    FULL("Full"),
    DRAFT("Draft"),
    SCHEDULED("Scheduled"),
    BOARDING("Boarding"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected"),
    EXPIRED("Expired");

    private final String displayName;

    TripStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}