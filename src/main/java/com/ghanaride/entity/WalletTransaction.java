package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_wallet_tx_user", columnList = "user_id"),
    @Index(name = "idx_wallet_tx_created", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {
    public enum TxType { TOPUP, PAYMENT, REFUND, LOYALTY_EARN, LOYALTY_REDEEM, CASHBACK, BONUS }
    public enum TxStatus { PENDING, SUCCESS, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TxType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TxStatus status = TxStatus.SUCCESS;

    @Column(length = 120)
    private String reference;

    @Column(length = 255)
    private String description;

    @Column(length = 40)
    private String provider; // PAYSTACK, MOMO, WALLET, SYSTEM

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
