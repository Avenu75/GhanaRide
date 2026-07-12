package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Payment methods supported by GhanaRide.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    WALLET("GhanaRide Wallet"),
    PAYSTACK("Paystack (Card/MoMo)"),
    MTN_MOMO("MTN Mobile Money"),
    VODAFONE_CASH("Vodafone Cash"),
    AIRTEL_TIGO("AirtelTigo Money"),
    CASH("Cash at Pickup"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay"),
    MOBILE_MONEY("Mobile Money"),
    CARD("Card Payment"),
    BANK_TRANSFER("Bank Transfer");

    private final String displayName;
}