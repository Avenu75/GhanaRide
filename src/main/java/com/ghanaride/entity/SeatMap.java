package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_maps", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "seatNumber"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatMap {
    public enum SeatStatus { AVAILABLE, HELD, BOOKED, BLOCKED }
    public enum SeatType { WINDOW, AISLE, FRONT, VIP }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 6)
    private String seatNumber; // 1A, 1B, 2A...

    @Column(nullable = false)
    private Integer rowNumber;

    @Column(nullable = false, length = 2)
    private String columnLabel; // A B C D

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SeatType seatType = SeatType.AISLE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_booking_id")
    private Booking heldBy;

    private java.time.LocalDateTime holdExpiresAt;

    @Builder.Default
    private Boolean isEmergencyExit = false;
    @Builder.Default
    private Boolean extraLegroom = false;
}
