package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Represents a transport company / fleet operator.
 * Each company has one admin User account.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user"})
@Entity
@Table(
        name = "companies",
        indexes = {
                @Index(name = "idx_company_user",
                        columnList = "user_id"),
                @Index(name = "idx_company_email",
                        columnList = "email")
        }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user account that manages this company
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(
            name = "company_name",
            length = 100,
            nullable = false
    )
    private String companyName;

    @Column(length = 100, nullable = false)
    private String email;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 255, nullable = false)
    private String location;

    @Column(length = 1000)
    private String description;

    @Column(
            name = "registration_number",
            length = 50
    )
    private String registrationNumber;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "website", length = 255)
    private String website;

    @Column(
            name = "is_verified",
            nullable = false
    )
    private boolean verified = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
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

    // =========================================================
    // EQUALS & HASHCODE (ID-based)
    // =========================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}