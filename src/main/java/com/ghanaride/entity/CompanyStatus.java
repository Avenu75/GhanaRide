package com.ghanaride.entity;

/**
 * Company verification status.
 */
public enum CompanyStatus {
    PENDING("Pending Verification"),
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    REJECTED("Rejected"),
    CLOSED("Closed");

    private final String displayName;

    CompanyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}