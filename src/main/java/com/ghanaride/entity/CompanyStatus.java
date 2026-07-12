package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Company verification status.
 */
@Getter
@RequiredArgsConstructor
public enum CompanyStatus {
    PENDING("Pending Verification"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    SUSPENDED("Suspended"),
    INACTIVE("Inactive");

    private final String displayName;
}