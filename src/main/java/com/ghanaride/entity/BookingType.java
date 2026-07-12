package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Type of booking - self or for someone else.
 */
@Getter
@RequiredArgsConstructor
public enum BookingType {
    SELF("Self Booking"),
    RELATIVE("Booking for Someone Else");

    private final String displayName;
}