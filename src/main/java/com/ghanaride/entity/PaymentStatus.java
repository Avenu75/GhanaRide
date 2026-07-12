package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Payment status.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    PAID("Paid"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    REFUND_PENDING("Refund Pending"),
    REFUND_FAILED("Refund Failed"),
    PARTIALLY_REFUNDED("Partially Refunded");

    private final String displayName;
}