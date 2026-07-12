package com.ghanaride.service;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.exception.*;
import com.ghanaride.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ghanaride.repository.UserRepository;
import java.security.Principal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core user management service.
 * Handles registration, profile updates, authentication support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final WalletService walletService;
    private final EmailService emailService;

    // =========================================================
    // QUERIES
    // =========================================================

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }

    public User getCurrentUser(Principal principal) {
        String username = principal.getName();
        return userRepository.findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isProfileComplete(User user) {
        return user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank();
    }

    public List<String> getMissingProfileFields(User user) {
        List<String> missing = new ArrayList<>();
        if (user.getFullName() == null || user.getFullName().isBlank()) missing.add("Full Name");
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) missing.add("Phone Number");
        return missing;
    }

    // =========================================================
    // USER DETAILS SERVICE (Spring Security)
    // =========================================================

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // =========================================================
    // REGISTRATION
    // =========================================================

    @Transactional
    public User registerPassenger(RegisterRequestDTO dto) {
        validateRegistration(dto);

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRole(Role.USER);
        user.setAccountType("PASSENGER");

        User saved = userRepository.save(user);
        log.info("Registered passenger: {}", saved.getEmail());

        // Create wallet
        walletService.getOrCreateWallet(saved);

        // Send welcome email (async)
        emailService.sendWelcomeEmail(saved);

        return saved;
    }

    @Transactional
    public User registerDriver(RegisterRequestDTO dto, MultipartFile licenseFile, MultipartFile idFile) {
        validateRegistration(dto);

        // Save documents
        String licensePath = fileStorageService.storeDriverDocument(licenseFile, "license");
        String idPath = fileStorageService.storeDriverDocument(idFile, "id");

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRole(Role.DRIVER);
        user.setAccountType("DRIVER");
        user.setLicenseDocumentPath(licensePath);
        user.setIdDocumentPath(idPath);

        User saved = userRepository.save(user);
        log.info("Registered driver: {} (pending verification)", saved.getEmail());

        walletService.getOrCreateWallet(saved);
        return saved;
    }

    @Transactional
    public User registerCompany(RegisterRequestDTO dto, MultipartFile regCertFile) {
        validateRegistration(dto);

        String regCertPath = fileStorageService.storeCompanyDocument(regCertFile, "registration");

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRole(Role.COMPANY);
        user.setAccountType("COMPANY");
        user.setCompanyRegistrationPath(regCertPath);
        user.setCompanyName(dto.getCompanyName());
        user.setCompanyEmail(dto.getCompanyEmail());
        user.setCompanyPhone(dto.getCompanyPhone());
        user.setCompanyRegistrationNo(dto.getRegistrationNo());
        user.setCompanyLocation(dto.getLocation());
        user.setCompanyDescription(dto.getCompanyDescription());

        User saved = userRepository.save(user);
        log.info("Registered company: {} (pending verification)", saved.getEmail());

        walletService.getOrCreateWallet(saved);
        return saved;
    }

    // =========================================================
    // PROFILE MANAGEMENT
    // =========================================================

    @Transactional
    public User updateProfile(User user, ProfileUpdateDTO dto) {
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());

        // Notification preferences - store in user preferences or separate table
        // For now, just log
        if (dto.getEmailNotifications() != null) log.debug("Email notifications: {}", dto.getEmailNotifications());
        if (dto.getPushNotifications() != null) log.debug("Push notifications: {}", dto.getPushNotifications());
        if (dto.getSmsAlerts() != null) log.debug("SMS alerts: {}", dto.getSmsAlerts());
        if (dto.getPromoEmails() != null) log.debug("Promo emails: {}", dto.getPromoEmails());
        if (dto.getPriceDropAlerts() != null) log.debug("Price drop alerts: {}", dto.getPriceDropAlerts());

        return userRepository.save(user);
    }

    @Transactional
    public User updateAvatar(User user, MultipartFile file) {
        String oldPath = user.getProfileImagePath();
        if (oldPath != null) fileStorageService.delete(oldPath);

        String newPath = fileStorageService.storeProfileImage(file);
        user.setProfileImagePath(newPath);
        return userRepository.save(user);
    }

    @Transactional
    public void removeAvatar(User user) {
        String oldPath = user.getProfileImagePath();
        if (oldPath != null) {
            fileStorageService.delete(oldPath);
            user.setProfileImagePath(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional
    public void updateNotificationPreferences(User user, NotificationPreferencesDTO dto) {
        // Store in user preferences or separate table
        log.info("Updated notification preferences for user: {}", user.getEmail());
    }

    // =========================================================
    // ADMIN / DRIVER MANAGEMENT
    // =========================================================

    @Transactional
    public User verifyDriver(Long userId, boolean approved, String reason) {
        User driver = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (driver.getRole() != Role.DRIVER) {
            throw new IllegalStateException("User is not a driver");
        }

        if (approved) {
            driver.setAccountType("DRIVER_VERIFIED");
        } else {
            driver.setAccountType("DRIVER_REJECTED");
        }

        User saved = userRepository.save(driver);
        emailService.sendDriverVerificationEmail(saved, approved, reason);
        log.info("Driver {} verification: {}", driver.getEmail(), approved ? "APPROVED" : "REJECTED");

        return saved;
    }

    @Transactional
    public User verifyCompany(Long userId, boolean approved, String reason) {
        User company = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (company.getRole() != Role.COMPANY) {
            throw new IllegalStateException("User is not a company");
        }

        if (approved) {
            company.setAccountType("COMPANY_VERIFIED");
        } else {
            company.setAccountType("COMPANY_REJECTED");
        }

        User saved = userRepository.save(company);
        emailService.sendCompanyVerificationEmail(saved, approved, reason);
        return saved;
    }

    // =========================================================
    // SEARCH & LIST
    // =========================================================

    public Page<User> findAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        return userRepository.findAll(PageRequest.of(page, size, sort));
    }

    public Page<User> findByRole(Role role, int page, int size) {
        return userRepository.findByRole(role, PageRequest.of(page, size));
    }

    public List<User> findDriversNeedingVerification() {
        return userRepository.findByRoleAndAccountType(Role.DRIVER, "DRIVER");
    }

    public List<User> findCompaniesNeedingVerification() {
        return userRepository.findByRoleAndAccountType(Role.COMPANY, "COMPANY");
    }

    public List<User> findActiveUsers() {
        return userRepository.findActiveUsers();
    }

    // Password reset
    public void sendPasswordResetEmail(String emailOrUsername) {
        log.info("Password reset requested for: {}", emailOrUsername);
    }

    public boolean isValidPasswordResetToken(String token) {
        return true; // Implement actual validation
    }

    public void resetPassword(String token, String newPassword) {
        // Implement actual reset logic
    }

    // =========================================================
    // VALIDATION HELPERS
    // =========================================================

    private void validateRegistration(RegisterRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (dto.getPhoneNumber() != null && userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered");
        }
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    public Object countByRole(Role role) {
        return null;
    }

    public Page<User> searchUsers(String trim, Pageable pageable) {
        return null;
    }

    public Page<User> findAllUsers(Pageable pageable) {
        return null;
    }

    public void deleteUser(Long userId) {
    }
}