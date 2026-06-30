package com.ghanaride.service;

import com.ghanaride.entity.PasswordResetToken;
import com.ghanaride.entity.User;
import com.ghanaride.repository.PasswordResetTokenRepository;
import com.ghanaride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    // In-memory rate limiting map: key = identifier (email/username), value = last request time
    private final Map<String, LocalDateTime> rateLimitMap = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_MINUTES = 1;
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Transactional
    public boolean createPasswordResetToken(String emailOrUsername) {
        // Enforce rate limit
        LocalDateTime now = LocalDateTime.now();
        if (rateLimitMap.containsKey(emailOrUsername)) {
            LocalDateTime lastRequest = rateLimitMap.get(emailOrUsername);
            if (lastRequest.plusMinutes(RATE_LIMIT_MINUTES).isAfter(now)) {
                log.warn("Rate limit triggered for reset request: {}", emailOrUsername);
                return false;
            }
        }
        rateLimitMap.put(emailOrUsername, now);

        // Find the user by username or email
        Optional<User> userOpt = userRepository.findByEmail(emailOrUsername);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(emailOrUsername);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Prevent multiple active tokens by deleting old ones
            tokenRepository.deleteByUser(user);

            // Generate token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(now.plusMinutes(TOKEN_EXPIRY_MINUTES));
            tokenRepository.save(resetToken);

            // Print link to system logs (simulating sending email)
            String resetLink = "http://localhost:8080/reset-password?token=" + token;
            log.info("========================================");
            log.info("PASSWORD RESET LINK GENERATED FOR USER: {}", user.getUsername());
            log.info("Reset Link: {}", resetLink);
            log.info("This link will expire in {} minutes.", TOKEN_EXPIRY_MINUTES);
            log.info("========================================");
        } else {
            // Log warning but do not return error to prevent user enumeration
            log.warn("Password reset requested for non-existent account: {}", emailOrUsername);
        }

        return true;
    }

    public Optional<PasswordResetToken> validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);
        if (resetTokenOpt.isPresent()) {
            PasswordResetToken resetToken = resetTokenOpt.get();
            if (!resetToken.isExpired()) {
                return Optional.of(resetToken);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void resetPassword(PasswordResetToken resetToken, String newPassword) {
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Invalidate the token immediately after successful reset
        tokenRepository.delete(resetToken);
    }
}
