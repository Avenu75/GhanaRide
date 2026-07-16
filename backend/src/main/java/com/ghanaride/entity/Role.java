package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * User roles in the system.
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("Passenger"),
    DRIVER("Driver"),
    COMPANY("Company"),
    ADMIN("Admin");

    private final String displayName;

    public String toUpperCase() {
        return "";
    }
}