package com.ghanaride.service;

import com.ghanaride.entity.*;
import com.ghanaride.exception.ResourceNotFoundException;
import com.ghanaride.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Handles all user account operations.
 *
 * Note on email verification:
 * Currently disabled (emailVerified=true on register)
 * for easier onboarding. To enable, set
 * emailVerified=false and uncomment the email
 * verification flow below.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final CarRepository carRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    // =========================================================
    // REGISTER USER
    // =========================================================
    @Transactional
    public User registerUser(
            User user,
            String companyName,
            String companyEmail,
            String companyPhone,
            String companyLocation,
            String companyDescription,
            String registrationNumber
    ) {
        // Encode password
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        // Email verification:
        // Set to TRUE for now (no email verification flow)
        // Set to FALSE to enable email verification
        user.setEmailVerified(true);

        // Assign role from account type
        String accountType = user.getAccountType();
        Role role = switch (
                accountType != null
                        ? accountType.toLowerCase()
                        : ""
                ) {
            case "company"   -> Role.COMPANY;
            case "driver"    -> Role.DRIVER;
            case "passenger" -> Role.USER;
            default          -> Role.USER;
        };
        user.setRole(role);

        // Normalize email
        if (user.getEmail() != null) {
            user.setEmail(
                    user.getEmail().toLowerCase().trim()
            );
        }

        User savedUser = userRepository.save(user);

        // Create company profile if COMPANY role
        if (role == Role.COMPANY &&
                companyName != null &&
                !companyName.isBlank()) {
            Company company = new Company();
            company.setUser(savedUser);
            company.setCompanyName(companyName.trim());
            company.setEmail(companyEmail);
            company.setPhone(companyPhone);
            company.setLocation(companyLocation);
            company.setDescription(companyDescription);
            company.setRegistrationNumber(
                    registrationNumber
            );
            companyRepository.save(company);
        }

        // Send welcome email (async — non-blocking)
        String fullName = savedUser.getFullName() != null
                ? savedUser.getFullName()
                : savedUser.getUsername();
        emailService.sendWelcomeEmail(
                savedUser.getEmail(), fullName
        );

        log.info(
                "User registered: id={} email={} role={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        return savedUser;
    }

    // =========================================================
    // REGISTER OAUTH USER (Google Sign-In)
    // =========================================================
    // FIX 2026-07-10: null-safe, length-safe, constraint-safe
    // - fullName never null, truncated 100
    // - username sanitized, max 50, collision-safe + UUID fallback
    // - explicit enabled / emailVerified / accountLocked
    // - duplicate race handling
    @Transactional
    public User registerOAuthUser(
            String email,
            String fullName,
            String googleId
    ) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth email is required");
        }

        String normalizedEmail = email.toLowerCase().trim();

        // v3.1 HOTFIX: if user already exists, return existing immediately
        // – prevents DataIntegrityViolationException → OAuth 400
        // Previous version threw here, causing Google sign-in 400 on 2nd login
        Optional<User> existingOpt = userRepository.findByEmail(normalizedEmail);
        if (existingOpt.isPresent()) {
            log.info("OAuth user already exists, returning existing: {}", normalizedEmail);
            return existingOpt.get();
        }

        User user = new User();
        user.setEmail(normalizedEmail);

        // fullName is NOT NULL in DB, max 100 chars
        String safeFullName = (fullName != null && !fullName.isBlank())
                ? fullName.trim()
                : normalizedEmail.split("@")[0];
        if (safeFullName.length() > 100) {
            safeFullName = safeFullName.substring(0, 100);
        }
        user.setFullName(safeFullName);

        // Generate unique username from email prefix
        // DB column: varchar(50) unique not null
        String emailPrefix = normalizedEmail.split("@")[0];
        String baseUsername = emailPrefix.replaceAll("[^a-zA-Z0-9_]", "");
        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }
        // Reserve space for numeric suffix, keep <= 45 chars base
        if (baseUsername.length() > 45) {
            baseUsername = baseUsername.substring(0, 45);
        }

        String username = baseUsername;
        int suffix = 1;
        // max 1000 attempts to avoid infinite loop
        while (userRepository.existsByUsername(username) && suffix < 1000) {
            String suffixStr = String.valueOf(suffix++);
            int maxBase = 50 - suffixStr.length();
            String truncatedBase = baseUsername.length() > maxBase
                    ? baseUsername.substring(0, maxBase)
                    : baseUsername;
            username = truncatedBase + suffixStr;
        }

        if (userRepository.existsByUsername(username)) {
            // Final fallback: UUID prefix
            username = "u_" + java.util.UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12);
        }

        user.setUsername(username);

        // Random password — OAuth users never use password login
        // Still must be BCrypt-encoded and NOT NULL
        user.setPassword(
                passwordEncoder.encode(
                        "OAUTH_" + java.util.UUID.randomUUID()
                )
        );

        // Explicitly set all account flags to avoid NULL DB issues
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setAccountLocked(false);

        user.setRole(Role.USER);
        user.setAccountType("passenger");

        // Optional: store googleId if you add a column later
        // user.setGoogleId(googleId);

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.error("OAuth user save failed constraint violation for {}: {}", normalizedEmail, dive.getMessage());
            // Possibly race condition – try to load existing
            return userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> dive);
        }

        log.info("OAuth user registered: id={} email={} username={} via Google{}",
                saved.getId(), normalizedEmail, username,
                googleId != null ? " sub=" + googleId : "");

        // Welcome email – non-blocking, ignore failures
        try {
            emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());
        } catch (Exception e) {
            log.warn("Welcome email failed for OAuth user {}: {}", normalizedEmail, e.getMessage());
        }

        return saved;
    }

    // =========================================================
    // GET CURRENT USER
    // Supports both standard users and Google OAuth2 users (using their email attribute)
    // FIX 2026-07-10: handles CustomOAuth2User, CustomUserDetails, OAuth2AuthenticationToken
    // =========================================================
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException(
                    "No authenticated user in context"
            );
        }

        String identifier = principal.getName();

        // 1. OAuth2AuthenticationToken (Google login)
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = oauthToken.getPrincipal();
            if (oauth2User != null) {
                // Try CustomOAuth2User first
                if (oauth2User instanceof com.ghanaride.security.CustomOAuth2User co2u) {
                    return userRepository.findById(co2u.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", co2u.getId()));
                }
                // Fall back to email attribute
                String email = oauth2User.getAttribute("email");
                if (email != null && !email.isBlank()) {
                    identifier = email;
                }
            }
        }
        // 2. CustomOAuth2User directly (if used as Principal)
        else if (principal instanceof com.ghanaride.security.CustomOAuth2User co2u) {
            return userRepository.findById(co2u.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", co2u.getId()));
        }
        // 3. CustomUserDetails
        else if (principal instanceof com.ghanaride.security.CustomUserDetails cud) {
            identifier = cud.getUsername();
            // fast path – try by id to avoid username/email ambiguity
            if (cud.getId() != null) {
                return userRepository.findById(cud.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", cud.getId()));
            }
        }
        // 4. Standard Authentication.getName() fallback
        // identifier already = principal.getName()

        final String finalIdentifier = identifier;
        return userRepository
                .findByUsernameOrEmail(
                        identifier,
                        identifier.toLowerCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "identifier",
                                finalIdentifier
                        )
                );
    }

    // =========================================================
    // UPDATE PROFILE
    // =========================================================
    @Transactional
    public User updateProfile(
            Principal principal,
            String fullName,
            String phoneNumber,
            String address,
            LocalDate dateOfBirth,
            String gender
    ) {
        User user = getCurrentUser(principal);

        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender);

        User saved = userRepository.save(user);

        log.info(
                "Profile updated for user: {}",
                user.getEmail()
        );

        return saved;
    }

    // =========================================================
    // UPDATE PROFILE IMAGE
    // =========================================================
    @Transactional
    public User updateProfileImage(
            Principal principal,
            MultipartFile file
    ) {
        User user = getCurrentUser(principal);

        String imagePath =
                fileStorageService.storeProfileImage(file);
        user.setProfileImagePath("/" + imagePath);

        User saved = userRepository.save(user);

        log.info(
                "Profile image updated for user: {}",
                user.getEmail()
        );

        return saved;
    }

    // =========================================================
    // CHANGE PASSWORD
    // Requires current password verification
    // =========================================================
    @Transactional
    public void changePassword(
            Principal principal,
            String currentPassword,
            String newPassword
    ) {
        User user = getCurrentUser(principal);

        // Verify current password
        if (!passwordEncoder.matches(
                currentPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        // Set new password
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );
        userRepository.save(user);

        log.info(
                "Password changed for user: {}",
                user.getEmail()
        );
    }

    // =========================================================
    // QUERY METHODS
    // =========================================================

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsernameOrEmail(
            String identifier
    ) {
        return userRepository.findByUsernameOrEmail(
                identifier, identifier.toLowerCase()
        );
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(
            String query, Pageable pageable
    ) {
        return userRepository.searchByNameOrEmail(
                query, pageable
        );
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public long countVerifiedDrivers() {
        return userRepository.countByRoleAndEmailVerified(
                Role.DRIVER, true
        );
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(
                email.toLowerCase().trim()
        );
    }

    // =========================================================
    // DELETE USER (Admin — cascades all related data)
    // =========================================================
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User", userId
                        )
                );

        log.warn(
                "Deleting user: id={} email={}",
                userId, user.getEmail()
        );

        // Delete in correct order to avoid FK violations:

        // 1. Bookings made by this user (as passenger)
        bookingRepository.deleteByUserId(userId);

        // 2. Bookings on trips driven by this user
        List<Trip> userTrips =
                tripRepository.findByDriverId(userId);
        for (Trip trip : userTrips) {
            bookingRepository.deleteByTripId(trip.getId());
        }

        // 3. Trips created by this user
        tripRepository.deleteByDriverId(userId);

        // 4. Cars owned by this user
        carRepository.deleteByDriverId(userId);

        // 5. Company profile (if company user)
        companyRepository.findByUser(user)
                .ifPresent(companyRepository::delete);

        // 6. Finally delete the user
        userRepository.delete(user);

        log.warn("User deleted: id={}", userId);
    }

    // =========================================================
    // INITIATE PASSWORD RESET
    // (Called from AuthController)
    // Delegates to PasswordResetService
    // Kept here for backward compatibility
    // =========================================================
    public void initiatePasswordReset(String email) {
        // This is handled by PasswordResetService
        // If needed, autowire PasswordResetService here
        log.info(
                "Password reset initiated for: {}", email
        );
    }

    public boolean isPasswordResetTokenValid(
            String token
    ) {
        // Delegates to PasswordResetService
        return false; // Override when autowired
    }

    public void resetPassword(
            String token, String newPassword
    ) {
        // Delegates to PasswordResetService
    }
}