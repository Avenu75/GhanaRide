package com.ghanaride.service;

import com.ghanaride.entity.LoginAttempt;
import com.ghanaride.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final LoginAttemptRepository loginAttemptRepository;

    // ===== CHECK IF ACCOUNT IS LOCKED =====
    public boolean isLocked(String username) {
        return loginAttemptRepository.findByUsername(username)
                .map(LoginAttempt::isLocked)
                .orElse(false);
    }

    // ===== GET MINUTES UNTIL UNLOCK =====
    public long getMinutesUntilUnlock(String username) {
        return loginAttemptRepository.findByUsername(username)
                .map(LoginAttempt::getMinutesUntilUnlock)
                .orElse(0L);
    }

    // ===== RECORD FAILED ATTEMPT =====
    @Transactional
    public void recordFailedAttempt(String username) {
        LoginAttempt attempt = loginAttemptRepository
                .findByUsername(username)
                .orElse(new LoginAttempt(username));

        // If previously locked but lock expired, reset
        if (attempt.getLockedUntil() != null &&
                LocalDateTime.now().isAfter(attempt.getLockedUntil())) {
            attempt.setAttemptCount(0);
            attempt.setLockedUntil(null);
        }

        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        attempt.setLastAttempt(LocalDateTime.now());

        // Lock account after MAX_ATTEMPTS
        if (attempt.getAttemptCount() >= MAX_ATTEMPTS) {
            attempt.setLockedUntil(
                    LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES)
            );
        }

        loginAttemptRepository.save(attempt);
    }

    // ===== RESET ON SUCCESSFUL LOGIN =====
    @Transactional
    public void resetAttempts(String username) {
        loginAttemptRepository.findByUsername(username)
                .ifPresent(attempt -> {
                    attempt.setAttemptCount(0);
                    attempt.setLockedUntil(null);
                    loginAttemptRepository.save(attempt);
                });
    }

    // ===== GET REMAINING ATTEMPTS =====
    public int getRemainingAttempts(String username) {
        int attempts = loginAttemptRepository.findByUsername(username)
                .map(LoginAttempt::getAttemptCount)
                .orElse(0);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}