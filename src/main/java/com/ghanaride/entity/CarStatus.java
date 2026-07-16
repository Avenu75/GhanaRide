package com.ghanaride.entity;

/**
 * Car/vehicle status.
 */
public enum CarStatus {
    ACTIVE("Active"),
    MAINTENANCE("Under Maintenance"),
    INACTIVE("Inactive"),
    RETIRED("Retired"),
    PENDING_DOCS("Pending Documents"),
    SUSPENDED("Suspended"),
    REJECTED("Rejected");

    private final String displayName;

    CarStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}