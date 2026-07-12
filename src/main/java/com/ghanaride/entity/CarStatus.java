package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Car/vehicle status.
 */
@Getter
@RequiredArgsConstructor
public enum CarStatus {
    ACTIVE("Active"),
    MAINTENANCE("Under Maintenance"),
    INACTIVE("Inactive"),
    RETIRED("Retired"),
    PENDING_DOCS("Pending Documents"),
    SUSPENDED("Suspended");

    private final String displayName;
}