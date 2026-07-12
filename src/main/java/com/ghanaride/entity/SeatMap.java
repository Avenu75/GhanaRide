package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seat map for a specific trip.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"trip", "car", "heldBy"})
@Entity
@Table(
    name = "seat_maps",
    indexes = {
        @Index(name = "idx_seatmap_trip", columnList = "trip_id"),
        @Index(name = "idx_seatmap_status", columnList = "status"),
        @Index(name = "idx_seatmap_number", columnList = "seat_number")
    }
)
public class SeatMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "column_label", length = 5)
    private String columnLabel;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 20, nullable = false)
    private SeatType seatType = SeatType.STANDARD;

    @Builder.Default
    @Column(name = "extra_legroom")
    private Boolean extraLegroom = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_booking_id")
    private Booking heldBy;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SeatType {
        WINDOW, AISLE, STANDARD, EXTRA_LEGROOM, EMERGENCY_EXIT
    }

    public enum SeatStatus {
        AVAILABLE, HELD, BOOKED, BLOCKED, DAMAGED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeatMap other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}