package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an intercity or campus trip
 * offered by a driver or company.
 *
 * A trip has:
 * - A driver (individual) OR a company (fleet)
 * - A car
 * - From/To locations
 * - Departure time
 * - Fixed price per seat
 * - Status lifecycle: PENDING → APPROVED →
 *   FULL/COMPLETED/CANCELLED/EXPIRED
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {
        "driver", "company", "car", "cancelledBy"
})
@Entity
@Table(
        name = "trips",
        indexes = {
                @Index(name = "idx_trip_status",
                        columnList = "status"),
                @Index(name = "idx_trip_departure",
                        columnList = "departure_time"),
                @Index(name = "idx_trip_driver",
                        columnList = "driver_id"),
                @Index(name = "idx_trip_company",
                        columnList = "company_id"),
                @Index(name = "idx_trip_from_to",
                        columnList = "from_location,to_location"),
                @Index(name = "idx_trip_status_departure",
                        columnList = "status,departure_time")
        }
)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    // Individual driver (null for company trips)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    // Company (null for individual driver trips)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(
            name = "from_location",
            length = 100,
            nullable = false
    )
    private String fromLocation;

    @Column(
            name = "to_location",
            length = 100,
            nullable = false
    )
    private String toLocation;

    @Column(
            name = "pickup_station",
            length = 150
    )
    private String pickupStation;

    @Column(
            name = "departure_time",
            nullable = false
    )
    private LocalDateTime departureTime;

    @Column(
            name = "trip_amount",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal tripAmount;

    @Column(
            name = "available_seats",
            nullable = false
    )
    private Integer availableSeats;

    @Column(
            name = "total_seats",
            nullable = false
    )
    private Integer totalSeats;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            length = 50,
            nullable = false
    )
    private TripStatus status = TripStatus.PENDING;

    // =========================================================
    // CANCELLATION FIELDS
    // =========================================================
    @Column(name = "cancel_reason", length = 100)
    private String cancelReason;

    @Column(
            name = "cancel_reason_details",
            length = 500
    )
    private String cancelReasonDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    // =========================================================
    // TIMESTAMPS
    // =========================================================
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // =========================================================
    // LIFECYCLE HOOKS
    // =========================================================
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TripStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // CONVENIENCE METHODS
    // =========================================================

    /**
     * Returns true if the trip is bookable
     * (approved + has seats + not departed)
     */
    public boolean isBookable() {
        return status == TripStatus.APPROVED &&
                availableSeats != null &&
                availableSeats > 0 &&
                departureTime != null &&
                departureTime.isAfter(LocalDateTime.now());
    }

    /**
     * Returns true if the trip is operated
     * by a company (not individual driver)
     */
    public boolean isCompanyTrip() {
        return company != null;
    }

    /**
     * Display name for the operator
     * (driver name or company name)
     */
    public String getOperatorName() {
        if (company != null) {
            return company.getCompanyName();
        }
        if (driver != null) {
            return driver.getFullName() != null
                    ? driver.getFullName()
                    : driver.getUsername();
        }
        return "Unknown";
    }

    // =========================================================
    // EQUALS & HASHCODE (ID-based)
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trip other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}