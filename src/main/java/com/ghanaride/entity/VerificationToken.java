package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Email verification token.
 * Generated on registration, sent via email.
 * User clicks link to verify their email.
 *
 * Currently not used (emailVerified=true on register).
 * Enable when you turn on email verification.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user"})
@Entity
@Table(
        name = "verification_tokens",
        indexes = {
                @Index(name = "idx_vt_token",
                        columnList = "token"),
                @Index(name = "idx_vt_user",
                        columnList = "user_id"),
                @Index(name = "idx_vt_expires",
                        columnList = "expires_at")
        }
)
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 36
    )
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "verified",
            nullable = false
    )
    private boolean verified = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public VerificationToken(User user) {
        this.user      = user;
        this.token     = UUID.randomUUID().toString();
        this.expiresAt = LocalDateTime.now().plusHours(24);
        this.createdAt = LocalDateTime.now();
        this.verified  = false;
    }

    // =========================================================
    // BUSINESS LOGIC
    // =========================================================
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !verified && !isExpired();
    }

    // =========================================================
    // EQUALS & HASHCODE
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VerificationToken other))
            return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}