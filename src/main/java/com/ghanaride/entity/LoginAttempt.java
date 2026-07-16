package com.ghanaride.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Login attempt tracking for security.
 */
@Entity
@Table(
    name = "login_attempts",
    indexes = {
        @Index(name = "idx_login_attempt_username", columnList = "username"),
        @Index(name = "idx_login_attempt_locked", columnList = "locked_until")
    }
)
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (attemptCount == null) attemptCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public long getMinutesUntilUnlock() {
        if (lockedUntil == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), lockedUntil).toMinutes();
    }

    public LoginAttempt() {
    }

    public LoginAttempt(String username) {
        this.username = username;
    }

    public LoginAttempt(Long id, String username, Integer attemptCount, LocalDateTime lastAttempt,
                        LocalDateTime lockedUntil, String ipAddress, String userAgent, LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.attemptCount = attemptCount;
        this.lastAttempt = lastAttempt;
        this.lockedUntil = lockedUntil;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LoginAttempt loginAttempt = new LoginAttempt();

        public Builder id(Long id) { loginAttempt.setId(id); return this; }
        public Builder username(String username) { loginAttempt.setUsername(username); return this; }
        public Builder attemptCount(Integer attemptCount) { loginAttempt.setAttemptCount(attemptCount); return this; }
        public Builder lastAttempt(LocalDateTime lastAttempt) { loginAttempt.setLastAttempt(lastAttempt); return this; }
        public Builder lockedUntil(LocalDateTime lockedUntil) { loginAttempt.setLockedUntil(lockedUntil); return this; }
        public Builder ipAddress(String ipAddress) { loginAttempt.setIpAddress(ipAddress); return this; }
        public Builder userAgent(String userAgent) { loginAttempt.setUserAgent(userAgent); return this; }
        public Builder createdAt(LocalDateTime createdAt) { loginAttempt.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { loginAttempt.setUpdatedAt(updatedAt); return this; }
        public LoginAttempt build() { return loginAttempt; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(LocalDateTime lastAttempt) { this.lastAttempt = lastAttempt; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginAttempt other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}