package com.ghanaride.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet transaction history.
 */
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

    public WalletTransaction() {
    }

    public WalletTransaction(Long id, User user, Wallet wallet, Booking booking, TxType type, BigDecimal amount,
                             BigDecimal balanceAfter, BigDecimal loyaltyBalanceAfter, String reference, String provider,
                             String description, TxStatus status, String metadata, LocalDateTime completedAt,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.wallet = wallet;
        this.booking = booking;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.loyaltyBalanceAfter = loyaltyBalanceAfter;
        this.reference = reference;
        this.provider = provider;
        this.description = description;
        this.status = status;
        this.metadata = metadata;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WalletTransaction transaction = new WalletTransaction();

        public Builder id(Long id) { transaction.setId(id); return this; }
        public Builder user(User user) { transaction.setUser(user); return this; }
        public Builder wallet(Wallet wallet) { transaction.setWallet(wallet); return this; }
        public Builder booking(Booking booking) { transaction.setBooking(booking); return this; }
        public Builder type(TxType type) { transaction.setType(type); return this; }
        public Builder amount(BigDecimal amount) { transaction.setAmount(amount); return this; }
        public Builder balanceAfter(BigDecimal balanceAfter) { transaction.setBalanceAfter(balanceAfter); return this; }
        public Builder loyaltyBalanceAfter(BigDecimal loyaltyBalanceAfter) { transaction.setLoyaltyBalanceAfter(loyaltyBalanceAfter); return this; }
        public Builder reference(String reference) { transaction.setReference(reference); return this; }
        public Builder provider(String provider) { transaction.setProvider(provider); return this; }
        public Builder description(String description) { transaction.setDescription(description); return this; }
        public Builder status(TxStatus status) { transaction.setStatus(status); return this; }
        public Builder metadata(String metadata) { transaction.setMetadata(metadata); return this; }
        public Builder completedAt(LocalDateTime completedAt) { transaction.setCompletedAt(completedAt); return this; }
        public Builder createdAt(LocalDateTime createdAt) { transaction.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { transaction.setUpdatedAt(updatedAt); return this; }
        public WalletTransaction build() { return transaction; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public TxType getType() { return type; }
    public void setType(TxType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public BigDecimal getLoyaltyBalanceAfter() { return loyaltyBalanceAfter; }
    public void setLoyaltyBalanceAfter(BigDecimal loyaltyBalanceAfter) { this.loyaltyBalanceAfter = loyaltyBalanceAfter; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TxStatus getStatus() { return status; }
    public void setStatus(TxStatus status) { this.status = status; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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