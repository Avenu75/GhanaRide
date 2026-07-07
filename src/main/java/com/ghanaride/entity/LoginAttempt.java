package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Tracks failed login attempts per username.
 * Used by LoginAttemptService to lock accounts
 * after too many failures.
 *
 * Stored in DB (not in-memory) so:
 * - Survives app restarts
 * - Works across multiple instances
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(
        name = "login_attempts",
        indexes = {
                @Index(
                        name = "idx_login_attempt_username",
                        columnList = "username"
                )
        }
)
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String username;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount = 0;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public LoginAttempt(String username) {
        this.username     = username;
        this.attemptCount = 0;
        this.lastAttempt  = LocalDateTime.now();
    }

    // =========================================================
    // BUSINESS LOGIC
    // =========================================================

    /**
     * Returns true if the account is currently locked.
     */
    public boolean isLocked() {
        if (lockedUntil == null) return false;
        return LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Returns minutes remaining until unlock.
     * Returns 0 if not locked.
     */
    public long getMinutesUntilUnlock() {
        if (lockedUntil == null || !isLocked()) return 0;
        return java.time.Duration
                .between(LocalDateTime.now(), lockedUntil)
                .toMinutes() + 1;
    }

    // =========================================================
    // EQUALS & HASHCODE
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginAttempt other))
            return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}