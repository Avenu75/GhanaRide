package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet transaction history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"user", "wallet", "booking"})
@Entity
@Table(
    name = "wallet_transactions",
    indexes = {
        @Index(name = "idx_wtx_user", columnList = "user_id"),
        @Index(name = "idx_wtx_type", columnList = "type"),
        @Index(name = "idx_wtx_status", columnList = "status"),
        @Index(name = "idx_wtx_reference", columnList = "reference"),
        @Index(name = "idx_wtx_created", columnList = "created_at"),
        @Index(name = "idx_wtx_booking", columnList = "booking_id")
    }
)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TxType type;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "loyalty_balance_after", precision = 15, scale = 2)
    private BigDecimal loyaltyBalanceAfter;

    @Column(length = 100, nullable = false)
    private String reference;

    @Column(length = 50)
    private String provider;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TxStatus status = TxStatus.PENDING;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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

    public enum TxType {
        TOPUP, PAYMENT, REFUND, LOYALTY_EARN, LOYALTY_REDEEM, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT, CASHBACK
    }

    public enum TxStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, EXPIRED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WalletTransaction other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}