package com.ghanaride.service;

import com.ghanaride.entity.PasswordResetToken;
import com.ghanaride.entity.User;
import com.ghanaride.repository.PasswordResetTokenRepository;
import com.ghanaride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles password reset token lifecycle:
 * 1. Generate token → store in DB → send email
 * 2. Validate token (not expired, not used)
 * 3. Reset password → invalidate token
 *
 * Rate limiting: stored in DB (not in-memory)
 * so it works across restarts and multiple instances.
 *
 * Token expiry: 24 hours (industry standard)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository
            tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    // Rate limit: 1 request per minute
    private static final int RATE_LIMIT_MINUTES = 1;

    // Token valid for 24 hours (industry standard)
    // was 15 minutes — too short for Ghanaian users
    // who may have unreliable email delivery
    private static final int TOKEN_EXPIRY_HOURS = 24;

    // =========================================================
    // CREATE PASSWORD RESET TOKEN
    // Returns true if request was processed
    // Returns false if rate limited
    // Never reveals whether user exists (prevents enumeration)
    // =========================================================
    @Transactional
    public boolean createPasswordResetToken(
            String emailOrUsername
    ) {
        // DB-based rate limiting
        // (works across restarts + multiple instances)
        Optional<PasswordResetToken> existingToken =
                tokenRepository
                        .findLatestByEmailOrUsername(
                                emailOrUsername
                        );

        if (existingToken.isPresent()) {
            LocalDateTime lastRequest =
                    existingToken.get().getCreatedAt();
            if (lastRequest != null &&
                    lastRequest.plusMinutes(RATE_LIMIT_MINUTES)
                            .isAfter(LocalDateTime.now())) {
                log.warn(
                        "Rate limit: password reset for {}",
                        emailOrUsername
                );
                return false;
            }
        }

        // Find user (try email first, then username)
        Optional<User> userOpt =
                userRepository.findByEmail(
                        emailOrUsername.toLowerCase()
                );

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(
                    emailOrUsername
            );
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Delete any existing tokens for this user
            tokenRepository.deleteByUser(user);

            // Generate secure token
            String token = UUID.randomUUID().toString();

            // Create and save token
            PasswordResetToken resetToken =
                    new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setCreatedAt(LocalDateTime.now());
            resetToken.setExpiryDate(
                    LocalDateTime.now()
                            .plusHours(TOKEN_EXPIRY_HOURS)
            );
            tokenRepository.save(resetToken);

            // Send real email (async — non-blocking)
            String fullName =
                    user.getFullName() != null
                            ? user.getFullName()
                            : user.getUsername();

            emailService.sendPasswordResetEmail(
                    user.getEmail(), fullName, token
            );

            log.info(
                    "Password reset token created for user: {}",
                    user.getEmail()
            );

        } else {
            // Log warning but return true
            // Never reveal if account exists!
            log.warn(
                    "Password reset requested for " +
                            "non-existent account: {}",
                    emailOrUsername
            );
        }

        return true;
    }

    // =========================================================
    // VALIDATE TOKEN
    // Returns token if valid, empty if expired/invalid
    // =========================================================
    public Optional<PasswordResetToken>
    validatePasswordResetToken(String token) {

        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired());
    }

    // =========================================================
    // CHECK IF TOKEN IS VALID (boolean version)
    // Used by AuthController
    // =========================================================
    public boolean isPasswordResetTokenValid(String token) {
        return validatePasswordResetToken(token).isPresent();
    }

    // =========================================================
    // RESET PASSWORD
    // Updates password and immediately invalidates token
    // =========================================================
    @Transactional
    public void resetPassword(
            PasswordResetToken resetToken,
            String newPassword
    ) {
        User user = resetToken.getUser();

        // Encode and save new password
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );
        userRepository.save(user);

        // Immediately invalidate the token
        // (prevents token reuse)
        tokenRepository.delete(resetToken);

        log.info(
                "Password successfully reset for user: {}",
                user.getEmail()
        );
    }

    // =========================================================
    // RESET BY TOKEN STRING (used by AuthController)
    // =========================================================
    @Transactional
    public void resetPassword(
            String token,
            String newPassword
    ) {
        PasswordResetToken resetToken =
                validatePasswordResetToken(token)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired reset token"
                                )
                        );
        resetPassword(resetToken, newPassword);
    }

    // =========================================================
    // SCHEDULED CLEANUP
    // Runs every hour — removes expired tokens from DB
    // =========================================================
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = tokenRepository
                    .deleteByExpiryDateBefore(
                            LocalDateTime.now()
                    );
            if (deleted > 0) {
                log.info(
                        "Cleaned up {} expired password " +
                                "reset tokens",
                        deleted
                );
            }
        } catch (Exception e) {
            log.error(
                    "Failed to cleanup password reset tokens",
                    e
            );
        }
    }
}