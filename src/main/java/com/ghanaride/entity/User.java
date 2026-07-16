package com.ghanaride.entity;

import jakarta.persistence.*;
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
    @Column(name = "email_verified")
    private Boolean emailVerified = true;

    @Column(name = "enabled")
    private Boolean enabled = true;

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

    public User() {
    }

    public User(Long id, String username, String fullName, String email, String password, String phoneNumber,
                Role role, String accountType, LocalDate dateOfBirth, String gender, String address,
                String profileImagePath, String licenseNumber, LocalDate licenseExpiryDate, String licenseDocumentPath,
                String idDocumentPath, String vehiclePlate, String vehicleModel, Integer vehicleYear, String vehicleColor,
                String companyRegistrationPath, String companyRegistrationNo, String companyName, String companyEmail,
                String companyPhone, String companyLocation, String companyDescription, Boolean emailVerified,
                Boolean enabled, Boolean accountLocked, LocalDateTime createdAt, LocalDateTime updatedAt,
                LocalDateTime lastLoginAt, LocalDateTime passwordChangedAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.accountType = accountType;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.address = address;
        this.profileImagePath = profileImagePath;
        this.licenseNumber = licenseNumber;
        this.licenseExpiryDate = licenseExpiryDate;
        this.licenseDocumentPath = licenseDocumentPath;
        this.idDocumentPath = idDocumentPath;
        this.vehiclePlate = vehiclePlate;
        this.vehicleModel = vehicleModel;
        this.vehicleYear = vehicleYear;
        this.vehicleColor = vehicleColor;
        this.companyRegistrationPath = companyRegistrationPath;
        this.companyRegistrationNo = companyRegistrationNo;
        this.companyName = companyName;
        this.companyEmail = companyEmail;
        this.companyPhone = companyPhone;
        this.companyLocation = companyLocation;
        this.companyDescription = companyDescription;
        this.emailVerified = emailVerified;
        this.enabled = enabled;
        this.accountLocked = accountLocked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
        this.passwordChangedAt = passwordChangedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final User user = new User();

        public Builder id(Long id) { user.setId(id); return this; }
        public Builder username(String username) { user.setUsername(username); return this; }
        public Builder fullName(String fullName) { user.setFullName(fullName); return this; }
        public Builder email(String email) { user.setEmail(email); return this; }
        public Builder password(String password) { user.setPassword(password); return this; }
        public Builder phoneNumber(String phoneNumber) { user.setPhoneNumber(phoneNumber); return this; }
        public Builder role(Role role) { user.setRole(role); return this; }
        public Builder accountType(String accountType) { user.setAccountType(accountType); return this; }
        public Builder dateOfBirth(LocalDate dateOfBirth) { user.setDateOfBirth(dateOfBirth); return this; }
        public Builder gender(String gender) { user.setGender(gender); return this; }
        public Builder address(String address) { user.setAddress(address); return this; }
        public Builder profileImagePath(String profileImagePath) { user.setProfileImagePath(profileImagePath); return this; }
        public Builder licenseNumber(String licenseNumber) { user.setLicenseNumber(licenseNumber); return this; }
        public Builder licenseExpiryDate(LocalDate licenseExpiryDate) { user.setLicenseExpiryDate(licenseExpiryDate); return this; }
        public Builder licenseDocumentPath(String licenseDocumentPath) { user.setLicenseDocumentPath(licenseDocumentPath); return this; }
        public Builder idDocumentPath(String idDocumentPath) { user.setIdDocumentPath(idDocumentPath); return this; }
        public Builder vehiclePlate(String vehiclePlate) { user.setVehiclePlate(vehiclePlate); return this; }
        public Builder vehicleModel(String vehicleModel) { user.setVehicleModel(vehicleModel); return this; }
        public Builder vehicleYear(Integer vehicleYear) { user.setVehicleYear(vehicleYear); return this; }
        public Builder vehicleColor(String vehicleColor) { user.setVehicleColor(vehicleColor); return this; }
        public Builder companyRegistrationPath(String companyRegistrationPath) { user.setCompanyRegistrationPath(companyRegistrationPath); return this; }
        public Builder companyRegistrationNo(String companyRegistrationNo) { user.setCompanyRegistrationNo(companyRegistrationNo); return this; }
        public Builder companyName(String companyName) { user.setCompanyName(companyName); return this; }
        public Builder companyEmail(String companyEmail) { user.setCompanyEmail(companyEmail); return this; }
        public Builder companyPhone(String companyPhone) { user.setCompanyPhone(companyPhone); return this; }
        public Builder companyLocation(String companyLocation) { user.setCompanyLocation(companyLocation); return this; }
        public Builder companyDescription(String companyDescription) { user.setCompanyDescription(companyDescription); return this; }
        public Builder emailVerified(Boolean emailVerified) { user.setEmailVerified(emailVerified); return this; }
        public Builder enabled(Boolean enabled) { user.setEnabled(enabled); return this; }
        public Builder accountLocked(Boolean accountLocked) { user.setAccountLocked(accountLocked); return this; }
        public Builder createdAt(LocalDateTime createdAt) { user.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { user.setUpdatedAt(updatedAt); return this; }
        public Builder lastLoginAt(LocalDateTime lastLoginAt) { user.setLastLoginAt(lastLoginAt); return this; }
        public Builder passwordChangedAt(LocalDateTime passwordChangedAt) { user.setPasswordChangedAt(passwordChangedAt); return this; }
        public User build() { return user; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) { this.licenseExpiryDate = licenseExpiryDate; }
    public String getLicenseDocumentPath() { return licenseDocumentPath; }
    public void setLicenseDocumentPath(String licenseDocumentPath) { this.licenseDocumentPath = licenseDocumentPath; }
    public String getIdDocumentPath() { return idDocumentPath; }
    public void setIdDocumentPath(String idDocumentPath) { this.idDocumentPath = idDocumentPath; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }
    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }
    public String getCompanyRegistrationPath() { return companyRegistrationPath; }
    public void setCompanyRegistrationPath(String companyRegistrationPath) { this.companyRegistrationPath = companyRegistrationPath; }
    public String getCompanyRegistrationNo() { return companyRegistrationNo; }
    public void setCompanyRegistrationNo(String companyRegistrationNo) { this.companyRegistrationNo = companyRegistrationNo; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    public String getCompanyLocation() { return companyLocation; }
    public void setCompanyLocation(String companyLocation) { this.companyLocation = companyLocation; }
    public String getCompanyDescription() { return companyDescription; }
    public void setCompanyDescription(String companyDescription) { this.companyDescription = companyDescription; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAccountLocked() { return accountLocked; }
    public void setAccountLocked(Boolean accountLocked) { this.accountLocked = accountLocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

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