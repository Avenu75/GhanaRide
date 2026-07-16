package com.ghanaride.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Supported payment types.
 */
enum PaymentType {
    CASH,
    WALLET,
    CARD,
    MOBILE_MONEY,
    BANK_TRANSFER,
    PAYSTACK
}

/**
 * Payment entity for tracking payments.
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_booking", columnList = "booking_id"),
        @Index(name = "idx_payment_user", columnList = "user_id"),
        @Index(name = "idx_payment_reference", columnList = "reference", unique = true),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_created", columnList = "created_at")
    }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private PaymentType type;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "reference", length = 100, unique = true, nullable = false)
    private String reference;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(name = "description", length = 255)
    private String description;

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

    public Payment() {
    }

    public Payment(Long id, Booking booking, User user, Wallet wallet, PaymentType type, BigDecimal amount,
                   String currency, PaymentStatus status, String reference, String provider, String providerRef,
                   String description, String metadata, LocalDateTime completedAt, LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.booking = booking;
        this.user = user;
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.reference = reference;
        this.provider = provider;
        this.providerRef = providerRef;
        this.description = description;
        this.metadata = metadata;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Payment payment = new Payment();

        public Builder id(Long id) { payment.setId(id); return this; }
        public Builder booking(Booking booking) { payment.setBooking(booking); return this; }
        public Builder user(User user) { payment.setUser(user); return this; }
        public Builder wallet(Wallet wallet) { payment.setWallet(wallet); return this; }
        public Builder type(PaymentType type) { payment.setType(type); return this; }
        public Builder amount(BigDecimal amount) { payment.setAmount(amount); return this; }
        public Builder currency(String currency) { payment.setCurrency(currency); return this; }
        public Builder status(PaymentStatus status) { payment.setStatus(status); return this; }
        public Builder reference(String reference) { payment.setReference(reference); return this; }
        public Builder provider(String provider) { payment.setProvider(provider); return this; }
        public Builder providerRef(String providerRef) { payment.setProviderRef(providerRef); return this; }
        public Builder description(String description) { payment.setDescription(description); return this; }
        public Builder metadata(String metadata) { payment.setMetadata(metadata); return this; }
        public Builder completedAt(LocalDateTime completedAt) { payment.setCompletedAt(completedAt); return this; }
        public Builder createdAt(LocalDateTime createdAt) { payment.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { payment.setUpdatedAt(updatedAt); return this; }
        public Payment build() { return payment; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public PaymentType getType() { return type; }
    public void setType(PaymentType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderRef() { return providerRef; }
    public void setProviderRef(String providerRef) { this.providerRef = providerRef; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
        if (!(o instanceof Payment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}