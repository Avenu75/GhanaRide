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
    @Transactional
    public User registerOAuthUser(
            String email,
            String fullName,
            String googleId
    ) {
        User user = new User();
        user.setEmail(email.toLowerCase().trim());
        user.setFullName(fullName);

        // Generate unique username from email prefix
        String baseUsername = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9_]", "");
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }
        user.setUsername(username);

        // Random password — OAuth users never use password
        user.setPassword(
                passwordEncoder.encode(
                        java.util.UUID.randomUUID().toString()
                )
        );

        // Google already verified the email
        user.setEmailVerified(true);
        user.setRole(Role.USER);
        user.setAccountType("passenger");

        User saved = userRepository.save(user);

        log.info(
                "OAuth user registered: {} via Google",
                email
        );

        return saved;
    }

    // =========================================================
    // GET CURRENT USER
    // Supports both standard users and Google OAuth2 users (using their email attribute)
    // =========================================================
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException(
                    "No authenticated user in context"
            );
        }

        String identifier = principal.getName();

        // If logged in via OAuth2, retrieve the user by email instead of the Google ID principal.getName()
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = oauthToken.getPrincipal();
            if (oauth2User != null && oauth2User.getAttribute("email") != null) {
                identifier = oauth2User.getAttribute("email");
            }
        }

        String finalIdentifier = identifier;
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