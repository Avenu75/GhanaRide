package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a seat booking on a trip.
 *
 * Supports two booking types:
 * - SELF: Passenger books for themselves
 * - RELATIVE: Passenger books for someone else
 *   (passengerName + passengerPhone required)
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user", "trip"})
// Excluding lazy associations prevents
// LazyInitializationException in toString()
@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_reference",
                        columnList = "booking_reference"),
                @Index(name = "idx_booking_user",
                        columnList = "user_id"),
                @Index(name = "idx_booking_trip",
                        columnList = "trip_id"),
                @Index(name = "idx_booking_status",
                        columnList = "status"),
                @Index(name = "idx_booking_payment_status",
                        columnList = "payment_status")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(
            name = "booking_reference",
            length = 20,
            unique = true,
            nullable = false
    )
    private String bookingReference;

    @Column(name = "seat_number")
    private Integer seatNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            length = 50,
            nullable = false
    )
    private BookingStatus status;

    @Column(
            name = "total_amount",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "booking_type",
            length = 50,
            nullable = false
    )
    private BookingType bookingType;

    // For RELATIVE bookings
    @Column(name = "passenger_name", length = 100)
    private String passengerName;

    @Column(name = "passenger_phone", length = 20)
    private String passengerPhone;

    // =========================================================
    // PAYMENT FIELDS
    // =========================================================
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            length = 50
    )
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            length = 50,
            nullable = false
    )
    private PaymentStatus paymentStatus =
            PaymentStatus.PENDING;

    @Column(
            name = "transaction_reference",
            length = 100
    )
    private String transactionReference;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // =========================================================
    // LIFECYCLE HOOKS
    // =========================================================
    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        updatedAt   = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (status == null) {
            status = BookingStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // EQUALS & HASHCODE (ID-based)
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}