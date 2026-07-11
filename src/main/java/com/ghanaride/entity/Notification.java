package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user_read", columnList = "user_id, readFlag")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    public enum Type { BOOKING_CONFIRMED, TRIP_REMINDER, DRIVER_ASSIGNED, PAYMENT_SUCCESS, REFUND_PROCESSED, PROMO, SYSTEM, REVIEW_REQUEST }
    public enum Channel { IN_APP, EMAIL, SMS, WHATSAPP, PUSH }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(length = 500)
    private String message;

    @Column(length = 255)
    private String actionUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Channel channel = Channel.IN_APP;

    @Builder.Default
    @Column(nullable = false)
    private boolean readFlag = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
