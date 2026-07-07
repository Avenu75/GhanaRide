package com.ghanaride.service;

import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Updated to use the full CustomUserDetails constructor
 * so userId, email, fullName, and role are available
 * from the security context without extra DB lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException(
                    "Bad credentials"
            );
        }

        String trimmed = username.trim();

        User user = userRepository
                .findByUsernameOrEmail(
                        trimmed,
                        trimmed.toLowerCase()
                )
                .orElseThrow(() -> {
                    log.debug(
                            "Login attempt for unknown: {}",
                            trimmed
                    );
                    return new UsernameNotFoundException(
                            "Bad credentials"
                    );
                });

        // Check email verified
        if (!user.isEmailVerified()) {
            log.info(
                    "Login blocked — not verified: {}",
                    user.getEmail()
            );
            throw new DisabledException(
                    "Email not verified. " +
                            "Please check your inbox."
            );
        }

        // Check enabled
        if (!user.isEnabled()) {
            log.warn(
                    "Login blocked — disabled: {}",
                    user.getEmail()
            );
            throw new DisabledException(
                    "Account disabled. Contact support."
            );
        }

        // Check locked
        if (user.isAccountLocked()) {
            log.warn(
                    "Login blocked — locked: {}",
                    user.getEmail()
            );
            throw new LockedException(
                    "Account temporarily locked."
            );
        }

        log.debug(
                "User loaded: {} role={}",
                user.getEmail(), user.getRole()
        );

        // Use FULL constructor — stores extra fields
        // so controllers don't need extra DB lookups
        return new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.isEmailVerified(),
                user.isEnabled(),
                !user.isAccountLocked(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                ),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}