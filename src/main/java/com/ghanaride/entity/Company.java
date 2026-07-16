package com.ghanaride.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transport company entity.
 */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CompanyStatus status = CompanyStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes", length = 500)
    private String verificationNotes;

    // Financial
    @Column(name = "wallet_balance", precision = 15, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

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

    public Company() {
    }

    public Company(Long id, String companyName, String companyEmail, String companyPhone, String registrationNumber,
                   String taxId, String address, String city, String region, String postalCode, String description,
                   String logoPath, String website, CompanyStatus status, LocalDateTime verifiedAt,
                   String verificationNotes, BigDecimal walletBalance, BigDecimal commissionRate, List<Trip> trips,
                   List<User> users, List<Car> cars, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyName = companyName;
        this.companyEmail = companyEmail;
        this.companyPhone = companyPhone;
        this.registrationNumber = registrationNumber;
        this.taxId = taxId;
        this.address = address;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.description = description;
        this.logoPath = logoPath;
        this.website = website;
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.verificationNotes = verificationNotes;
        this.walletBalance = walletBalance;
        this.commissionRate = commissionRate;
        this.trips = trips;
        this.users = users;
        this.cars = cars;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Company company = new Company();

        public Builder id(Long id) { company.setId(id); return this; }
        public Builder companyName(String companyName) { company.setCompanyName(companyName); return this; }
        public Builder companyEmail(String companyEmail) { company.setCompanyEmail(companyEmail); return this; }
        public Builder companyPhone(String companyPhone) { company.setCompanyPhone(companyPhone); return this; }
        public Builder registrationNumber(String registrationNumber) { company.setRegistrationNumber(registrationNumber); return this; }
        public Builder taxId(String taxId) { company.setTaxId(taxId); return this; }
        public Builder address(String address) { company.setAddress(address); return this; }
        public Builder city(String city) { company.setCity(city); return this; }
        public Builder region(String region) { company.setRegion(region); return this; }
        public Builder postalCode(String postalCode) { company.setPostalCode(postalCode); return this; }
        public Builder description(String description) { company.setDescription(description); return this; }
        public Builder logoPath(String logoPath) { company.setLogoPath(logoPath); return this; }
        public Builder website(String website) { company.setWebsite(website); return this; }
        public Builder status(CompanyStatus status) { company.setStatus(status); return this; }
        public Builder verifiedAt(LocalDateTime verifiedAt) { company.setVerifiedAt(verifiedAt); return this; }
        public Builder verificationNotes(String verificationNotes) { company.setVerificationNotes(verificationNotes); return this; }
        public Builder walletBalance(BigDecimal walletBalance) { company.setWalletBalance(walletBalance); return this; }
        public Builder commissionRate(BigDecimal commissionRate) { company.setCommissionRate(commissionRate); return this; }
        public Builder trips(List<Trip> trips) { company.setTrips(trips); return this; }
        public Builder users(List<User> users) { company.setUsers(users); return this; }
        public Builder cars(List<Car> cars) { company.setCars(cars); return this; }
        public Builder createdAt(LocalDateTime createdAt) { company.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { company.setUpdatedAt(updatedAt); return this; }
        public Company build() { return company; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public CompanyStatus getStatus() { return status; }
    public void setStatus(CompanyStatus status) { this.status = status; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public List<Trip> getTrips() { return trips; }
    public void setTrips(List<Trip> trips) { this.trips = trips; }
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
    public List<Car> getCars() { return cars; }
    public void setCars(List<Car> cars) { this.cars = cars; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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