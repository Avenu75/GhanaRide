package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trip status lifecycle.
 */
@Getter
@RequiredArgsConstructor
public enum TripStatus {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    FULL("Full"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected"),
    EXPIRED("Expired");

    private final String displayName;
}