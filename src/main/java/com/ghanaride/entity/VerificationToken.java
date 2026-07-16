package com.ghanaride.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Verification token for email verification.
 */
@Entity
@Table(
    name = "verification_tokens",
    indexes = {
        @Index(name = "idx_vt_user", columnList = "user_id"),
        @Index(name = "idx_vt_token", columnList = "token", unique = true),
        @Index(name = "idx_vt_expiry", columnList = "expiry_date")
    }
)
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", length = 100, unique = true, nullable = false)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "used")
    private Boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (used == null) used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public VerificationToken() {
    }

    public VerificationToken(Long id, User user, String token, LocalDateTime expiryDate, Boolean used,
                             LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
        this.used = used;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final VerificationToken token = new VerificationToken();

        public Builder id(Long id) { token.setId(id); return this; }
        public Builder user(User user) { token.setUser(user); return this; }
        public Builder token(String tokenValue) { token.setToken(tokenValue); return this; }
        public Builder expiryDate(LocalDateTime expiryDate) { token.setExpiryDate(expiryDate); return this; }
        public Builder used(Boolean used) { token.setUsed(used); return this; }
        public Builder createdAt(LocalDateTime createdAt) { token.setCreatedAt(createdAt); return this; }
        public VerificationToken build() { return token; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VerificationToken other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}