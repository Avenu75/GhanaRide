package com.ghanaride.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet entity for user balances and loyalty points.
 */
@Entity
@Table(
    name = "wallets",
    indexes = {
        @Index(name = "idx_wallet_user", columnList = "user_id", unique = true),
        @Index(name = "idx_wallet_currency", columnList = "currency")
    }
)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "loyalty_points", precision = 15, scale = 2, nullable = false)
    private BigDecimal loyaltyPoints = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    private String currency = "GHS";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (loyaltyPoints == null) loyaltyPoints = BigDecimal.ZERO;
        if (currency == null) currency = "GHS";
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Wallet() {
    }

    public Wallet(Long id, User user, BigDecimal balance, BigDecimal loyaltyPoints, String currency, Boolean isActive,
                 LocalDateTime lastTransactionAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.balance = balance;
        this.loyaltyPoints = loyaltyPoints;
        this.currency = currency;
        this.isActive = isActive;
        this.lastTransactionAt = lastTransactionAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Wallet wallet = new Wallet();

        public Builder id(Long id) { wallet.setId(id); return this; }
        public Builder user(User user) { wallet.setUser(user); return this; }
        public Builder balance(BigDecimal balance) { wallet.setBalance(balance); return this; }
        public Builder loyaltyPoints(BigDecimal loyaltyPoints) { wallet.setLoyaltyPoints(loyaltyPoints); return this; }
        public Builder currency(String currency) { wallet.setCurrency(currency); return this; }
        public Builder isActive(Boolean isActive) { wallet.setIsActive(isActive); return this; }
        public Builder lastTransactionAt(LocalDateTime lastTransactionAt) { wallet.setLastTransactionAt(lastTransactionAt); return this; }
        public Builder createdAt(LocalDateTime createdAt) { wallet.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { wallet.setUpdatedAt(updatedAt); return this; }
        public Wallet build() { return wallet; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(BigDecimal loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getLastTransactionAt() { return lastTransactionAt; }
    public void setLastTransactionAt(LocalDateTime lastTransactionAt) { this.lastTransactionAt = lastTransactionAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}