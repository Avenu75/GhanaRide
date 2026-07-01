package com.ghanaride.service;

import com.ghanaride.entity.User;
import com.ghanaride.entity.VerificationToken;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ===== SEND VERIFICATION EMAIL =====
    @Transactional
    public void sendVerificationEmail(User user) {
        // Delete existing token if any
        tokenRepository.deleteByUserId(user.getId());

        // Create new token
        VerificationToken token = new VerificationToken(user);
        tokenRepository.save(token);

        // Send email
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                token.getToken()
        );
    }

    // ===== VERIFY TOKEN =====
    @Transactional
    public String verifyToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (verificationToken == null) {
            return "invalid";
        }

        if (verificationToken.isExpired()) {
            return "expired";
        }

        if (verificationToken.isVerified()) {
            return "already_verified";
        }

        // Mark user as verified
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        return "success";
    }
}