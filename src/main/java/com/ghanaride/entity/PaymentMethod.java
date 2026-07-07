package com.ghanaride.entity;

/**
 * Payment methods supported by GhanaRide.
 *
 * PAYSTACK     = Generic Paystack (card or mobile money)
 * MOBILE_MONEY = MTN MoMo, Vodafone Cash, AirtelTigo
 * CARD         = Visa, Mastercard via Paystack
 * BANK_TRANSFER= Direct bank transfer via Paystack
 * CASH         = Cash paid directly to driver
 */
public enum PaymentMethod {
    PAYSTACK,       // Generic Paystack payment
    MOBILE_MONEY,   // MTN MoMo, Vodafone Cash, AirtelTigo
    CARD,           // Visa/Mastercard via Paystack
    BANK_TRANSFER,  // Bank transfer via Paystack
    CASH            // Cash to driver on travel day
}