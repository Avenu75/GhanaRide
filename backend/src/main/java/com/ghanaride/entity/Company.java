package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transport company entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"trips", "users", "cars"})
@Entity
@Table(
    name = "companies",
    indexes = {
        @Index(name = "idx_company_name", columnList = "company_name"),
        @Index(name = "idx_company_status", columnList = "status"),
        @Index(name = "idx_company_email", columnList = "company_email")
    }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", length = 150, nullable = false, unique = true)
    private String companyName;

    @Column(name = "company_email", length = 100, unique = true)
    private String companyEmail;

    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    @Column(name = "registration_number", length = 50, unique = true)
    private String registrationNumber;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "website")
    private String website;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CompanyStatus status = CompanyStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes", length = 500)
    private String verificationNotes;

    // Financial
    @Builder.Default
    @Column(name = "wallet_balance", precision = 15, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("10.00");

    // Relationships
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<User> users;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Car> cars;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = CompanyStatus.PENDING;
        if (walletBalance == null) walletBalance = BigDecimal.ZERO;
        if (commissionRate == null) commissionRate = new BigDecimal("10.00");
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CompanyStatus {
        PENDING("Pending Verification"),
        ACTIVE("Active"),
        SUSPENDED("Suspended"),
        REJECTED("Rejected"),
        CLOSED("Closed");

        private final String displayName;

        CompanyStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

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