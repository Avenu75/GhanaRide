package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Stores password reset tokens.
 * Each token:
 * - Is unique per user
 * - Expires after 24 hours
 * - Is deleted immediately after use
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user"})
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_prt_token",
                        columnList = "token"),
                @Index(name = "idx_prt_user",
                        columnList = "user_id"),
                @Index(name = "idx_prt_expiry",
                        columnList = "expiry_date")
        }
)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 36   // UUID length
    )
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            name = "expiry_date",
            nullable = false
    )
    private LocalDateTime expiryDate;

    // FIX: Added createdAt — referenced in
    // PasswordResetService for rate limiting
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns true if the token has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Returns true if the token is still valid.
     */
    public boolean isValid() {
        return !isExpired();
    }

    // =========================================================
    // EQUALS & HASHCODE
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken other))
            return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}