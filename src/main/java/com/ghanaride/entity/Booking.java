package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a seat booking on a trip.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"user", "trip", "seatMap", "walletTransaction", "review"})
@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_reference", columnList = "booking_reference"),
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_trip", columnList = "trip_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_payment_status", columnList = "payment_status"),
        @Index(name = "idx_booking_date", columnList = "booking_date")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id")
    private SeatMap seatMap;

    @Column(name = "booking_reference", length = 20, unique = true, nullable = false)
    private String bookingReference;

    @Column(name = "seat_number")
    private Integer seatNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", length = 50, nullable = false)
    private BookingType bookingType = BookingType.SELF;

    // For relative bookings
    @Column(name = "passenger_name", length = 100)
    private String passengerName;

    @Column(name = "passenger_phone", length = 20)
    private String passengerPhone;

    // Payment fields
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Cancellation
    @Column(name = "cancel_reason", length = 100)
    private String cancelReason;

    @Column(name = "cancel_reason_details", length = 500)
    private String cancelReasonDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Relationships
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private WalletTransaction walletTransaction;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Review review;

    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
        if (status == null) status = BookingStatus.PENDING_PAYMENT;
        if (bookingType == null) bookingType = BookingType.SELF;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // CONVENIENCE METHODS
    // =========================================================

    public boolean isCancellable() {
        return status != BookingStatus.CANCELLED
            && status != BookingStatus.COMPLETED
            && trip != null
            && trip.getDepartureTime() != null
            && trip.getDepartureTime().isAfter(LocalDateTime.now().plusHours(2));
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE || status == BookingStatus.CONFIRMED;
    }

    public String getDisplaySeat() {
        if (seatMap != null) return seatMap.getSeatNumber();
        return seatNumber != null ? seatNumber.toString() : "—";
    }

    // =========================================================
    // EQUALS & HASHCODE
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