package com.ghanaride.security;

import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService - Loads user for Spring Security.
 * Uses full constructor to store extra fields (userId, email, fullName, role)
 * so they're available in security context without extra DB lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Bad credentials");
        }

        String trimmed = username.trim();

        User user = userRepository.findByUsernameOrEmail(trimmed, trimmed)
            .orElseThrow(() -> {
                log.debug("Login attempt for unknown user: {}", trimmed);
                return new UsernameNotFoundException("User not found: " + username);
            });

        // Check email verified
        if (!user.isEmailVerified()) {
            log.info("Login blocked — not verified: {}", user.getEmail());
            throw new DisabledException("Email not verified. Please check your inbox.");
        }

        // Check enabled
        if (!user.isEnabled()) {
            log.warn("Login blocked — disabled: {}", user.getEmail());
            throw new DisabledException("Account disabled. Contact support.");
        }

        // Check locked (either via User entity flag or DB-backed loginAttemptService)
        if (user.isAccountLocked() || loginAttemptService.isLocked(trimmed)) {
            log.warn("Login blocked — locked: {}", user.getEmail());
            long minutes = loginAttemptService.getMinutesUntilUnlock(trimmed);
            if (minutes > 0) {
                throw new LockedException("Account temporarily locked. Try again in " + minutes + " minutes.");
            } else {
                throw new LockedException("Account temporarily locked.");
            }
        }

        log.debug("User loaded: {} role={}", user.getEmail(), user.getRole());

        // Use FULL constructor — stores extra fields so controllers don't need extra DB lookups
        return new CustomUserDetails(
            user.getUsername(),
            user.getPassword(),
            user.isEmailVerified(),
            user.isEnabled(),
            !user.isAccountLocked(),
            Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            ),
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );
    }
}