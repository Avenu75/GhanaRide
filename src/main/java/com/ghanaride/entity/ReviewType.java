package com.ghanaride.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Review entity for driver/trip ratings.
 */
@Getter
@RequiredArgsConstructor
public enum ReviewType {
    DRIVER_RATING("Driver Rating"),
    TRIP_REVIEW("Trip Review"),
    SERVICE_FEEDBACK("Service Feedback");

    private final String displayName;
}