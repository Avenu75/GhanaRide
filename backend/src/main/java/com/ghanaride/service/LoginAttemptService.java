package com.ghanaride.service;

import com.ghanaride.entity.LoginAttempt;
import com.ghanaride.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tracks failed login attempts per username.
 * Locks accounts after MAX_ATTEMPTS failures.
 *
 * Data is stored in DB (not in-memory) so it:
 * - Survives app restarts
 * - Works across multiple instances
 * - Can be audited
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository
            loginAttemptRepository;

    @Value("${app.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockDurationMinutes;

    // =========================================================
    // CHECK IF ACCOUNT IS LOCKED
    // =========================================================
    public boolean isLocked(String username) {
        return loginAttemptRepository
                .findByUsername(username)
                .map(LoginAttempt::isLocked)
                .orElse(false);
    }

    // =========================================================
    // GET MINUTES UNTIL UNLOCK
    // =========================================================
    public long getMinutesUntilUnlock(String username) {
        return loginAttemptRepository
                .findByUsername(username)
                .map(LoginAttempt::getMinutesUntilUnlock)
                .orElse(0L);
    }

    // =========================================================
    // GET REMAINING ATTEMPTS BEFORE LOCKOUT
    // =========================================================
    public int getRemainingAttempts(String username) {
        int attempts = loginAttemptRepository
                .findByUsername(username)
                .map(LoginAttempt::getAttemptCount)
                .orElse(0);
        return Math.max(0, maxAttempts - attempts);
    }

    // =========================================================
    // RECORD FAILED ATTEMPT
    // Called by CustomAuthenticationFailureHandler
    // =========================================================
    @Transactional
    public void recordFailedAttempt(String username) {
        LoginAttempt attempt = loginAttemptRepository
                .findByUsername(username)
                .orElse(new LoginAttempt(username));

        // If previously locked but lock expired → reset
        if (attempt.getLockedUntil() != null &&
                LocalDateTime.now().isAfter(
                        attempt.getLockedUntil())) {
            attempt.setAttemptCount(0);
            attempt.setLockedUntil(null);
            log.info("Lockout expired for: {}", username);
        }

        attempt.setAttemptCount(
                attempt.getAttemptCount() + 1
        );
        attempt.setLastAttempt(LocalDateTime.now());

        // Lock after max attempts
        if (attempt.getAttemptCount() >= maxAttempts) {
            attempt.setLockedUntil(
                    LocalDateTime.now()
                            .plusMinutes(lockDurationMinutes)
            );
            log.warn(
                    "Account locked after {} failed attempts: {}",
                    maxAttempts, username
            );
        } else {
            log.debug(
                    "Failed login attempt {}/{} for: {}",
                    attempt.getAttemptCount(),
                    maxAttempts, username
            );
        }

        loginAttemptRepository.save(attempt);
    }

    // =========================================================
    // RESET ATTEMPTS ON SUCCESSFUL LOGIN
    // Called by CustomAuthenticationSuccessHandler
    // =========================================================
    @Transactional
    public void resetAttempts(String username) {
        loginAttemptRepository
                .findByUsername(username)
                .ifPresent(attempt -> {
                    attempt.setAttemptCount(0);
                    attempt.setLockedUntil(null);
                    loginAttemptRepository.save(attempt);
                    log.debug(
                            "Login attempts reset for: {}",
                            username
                    );
                });
    }

    // =========================================================
    // CLEANUP OLD RECORDS
    // Runs daily at 2am — keeps table clean
    // =========================================================
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoff =
                LocalDateTime.now().minusDays(7);
        try {
            int deleted = loginAttemptRepository
                    .deleteByLastAttemptBefore(cutoff);
            if (deleted > 0) {
                log.info(
                        "Cleaned up {} old login attempt records",
                        deleted
                );
            }
        } catch (Exception e) {
            log.error(
                    "Failed to cleanup login attempt records",
                    e
            );
        }
    }
}