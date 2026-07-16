package com.ghanaride.entity;

/**
 * Payment status.
 */
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

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}