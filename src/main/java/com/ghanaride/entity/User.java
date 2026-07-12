package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Core user entity implementing UserDetails for Spring Security.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_account_type", columnList = "account_type"),
        @Index(name = "idx_user_enabled", columnList = "enabled")
    }
)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(length = 100, unique = true, nullable = false)
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

    // Driver specific
    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "license_document_path")
    private String licenseDocumentPath;

    @Column(name = "id_document_path")
    private String idDocumentPath;

    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate;

    @Column(name = "vehicle_model", length = 100)
    private String vehicleModel;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(name = "vehicle_color", length = 30)
    private String vehicleColor;

    @Column(name = "company_registration_path")
    private String companyRegistrationPath;

    @Column(name = "company_registration_no", length = 50)
    private String companyRegistrationNo;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "company_email", length = 100)
    private String companyEmail;

    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    @Column(name = "company_location", length = 255)
    private String companyLocation;

    @Column(name = "company_description", length = 1000)
    private String companyDescription;

    // Account status flags
    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = true;

    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;

    @Builder.Default
    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (emailVerified == null) emailVerified = true;
        if (enabled == null) enabled = true;
        if (accountLocked == null) accountLocked = false;
        if (role == null) role = Role.USER;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // USER DETAILS IMPLEMENTATION
    // =========================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountLocked != null && !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    // =========================================================
    // CONVENIENCE METHODS
    // =========================================================

    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }

    public boolean isAccountLocked() {
        return accountLocked != null && accountLocked;
    }

    public boolean isProfileComplete() {
        return phoneNumber != null && !phoneNumber.isBlank();
    }

    public boolean isDriver() {
        return role == Role.DRIVER;
    }

    public boolean isCompany() {
        return role == Role.COMPANY;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isPassenger() {
        return role == Role.USER;
    }

    // =========================================================
    // EQUALS & HASHCODE
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