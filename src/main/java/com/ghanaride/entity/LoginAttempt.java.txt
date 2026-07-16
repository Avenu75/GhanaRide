package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "attempt_count")
    private int attemptCount = 0;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public LoginAttempt(String username) {
        this.username = username;
        this.attemptCount = 0;
        this.lastAttempt = LocalDateTime.now();
    }

    public boolean isLocked() {
        if (lockedUntil == null) return false;
        return LocalDateTime.now().isBefore(lockedUntil);
    }

    public long getMinutesUntilUnlock() {
        if (lockedUntil == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), lockedUntil).toMinutes() + 1;
    }
}