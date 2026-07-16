package com.ghanaride.entity;

/**
 * User roles in the system.
 */
public enum Role {
    USER("Passenger"),
    DRIVER("Driver"),
    COMPANY("Company"),
    ADMIN("Admin");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}