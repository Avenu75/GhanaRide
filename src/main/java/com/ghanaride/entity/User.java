package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core user entity.
 *
 * NOTE: Using @Getter/@Setter instead of @Data because:
 * - @Data generates toString() that triggers lazy loading
 * - @Data generates equals/hashCode using all fields
 *   which breaks JPA entity identity semantics
 * - Explicitly excluding lazy associations from toString
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {
        "password"      // Never log passwords
})
@Entity
@Table(
        name = "users",
        indexes = {
                // These indexes make login, email lookup,
                // and role-based queries fast
                @Index(name = "idx_user_email",
                        columnList = "email"),
                @Index(name = "idx_user_username",
                        columnList = "username"),
                @Index(name = "idx_user_role",
                        columnList = "role"),
                @Index(name = "idx_user_account_type",
                        columnList = "account_type")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            length = 50,
            unique = true,
            nullable = false
    )
    private String username;

    @Column(
            name = "full_name",
            length = 100,
            nullable = false
    )
    private String fullName;

    @Column(
            length = 100,
            unique = true,
            nullable = false
    )
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "account_type", length = 20)
    private String accountType;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 255)
    private String address;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    // =========================================================
    // ACCOUNT STATUS FLAGS
    // Used by CustomUserDetailsService for auth checks
    // Changed fields to Object wrappers 'Boolean' to handle pre-existing NULL values in database
    // =========================================================

    /**
     * Whether the user's email is verified.
     * Currently set to true on registration
     * (email verification is disabled for easier onboarding).
     * Set to false to enable email verification flow.
     */
    @Column(
            name = "email_verified"
    )
    private Boolean emailVerified = true;

    /**
     * Whether the account is active.
     * Admins can disable accounts via admin panel.
     * Disabled users cannot log in.
     */
    @Column(
            name = "enabled"
    )
    private Boolean enabled = true;

    /**
     * Whether the account is locked.
     * Set to true after too many failed login attempts.
     * Automatically cleared after lockout duration.
     */
    @Column(
            name = "account_locked"
    )
    private Boolean accountLocked = false;

    // =========================================================
    // TIMESTAMPS
    // =========================================================
    @Column(name = "created_at", nullable = false,
            updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // =========================================================
    // LIFECYCLE HOOKS
    // =========================================================
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Ensure defaults are set
        if (!emailVerified) emailVerified = true;
        if (!enabled)       enabled       = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // CONVENIENCE CONSTRUCTOR
    // For creating users in tests and OAuth flow
    // =========================================================
    public User(
            String username,
            String fullName,
            String email,
            String password,
            Role role
    ) {
        this.username  = username;
        this.fullName  = fullName;
        this.email     = email;
        this.password  = password;
        this.role      = role;
        this.enabled   = true;
        this.emailVerified = true;
    }

    // =========================================================
    // NULL-SAFE BOOLEAN GETTERS
    // Ensures existing database rows with NULL are treated safely as defaults
    // and supports backward compatibility with .is...() methods
    // =========================================================
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean isAccountLocked() {
        return accountLocked != null && accountLocked;
    }

    // =========================================================
    // EQUALS & HASHCODE
    // Based on ID only — correct for JPA entities
    // (Lombok @Data would use all fields which is wrong)
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}