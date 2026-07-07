package com.ghanaride.config;

import com.ghanaride.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles failed login attempts.
 *
 * Responsibilities:
 * 1. Record failed attempt in DB
 * 2. Lock account after too many failures
 * 3. Redirect with appropriate error message
 * 4. Never reveal whether email/username exists
 *
 * FIX: Was reading "username" parameter but form
 * uses "email" parameter — now reads both.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        // FIX: Read both "email" and "username" params
        // (form sends "email", but fallback to "username")
        String identifier = request.getParameter("email");
        if (identifier == null || identifier.isBlank()) {
            identifier = request.getParameter("username");
        }

        // Get client IP for logging
        String clientIp = getClientIp(request);

        // Handle specific exception types FIRST
        // before recording attempts
        // (some failures shouldn't count as attempts)

        // Account disabled (email not verified)
        if (exception instanceof DisabledException) {
            log.info(
                    "Login failed — account disabled: {} " +
                            "ip={}",
                    identifier, clientIp
            );
            response.sendRedirect(
                    "/login?error=disabled"
            );
            return;
        }

        // Account locked (too many attempts)
        if (exception instanceof LockedException) {
            log.warn(
                    "Login failed — account locked: {} " +
                            "ip={}",
                    identifier, clientIp
            );
            response.sendRedirect(
                    "/login?error=locked"
            );
            return;
        }

        // Bad credentials — record the attempt
        if (identifier != null &&
                !identifier.isBlank() &&
                exception instanceof BadCredentialsException) {

            loginAttemptService
                    .recordFailedAttempt(identifier);

            log.info(
                    "Failed login: identifier={} ip={}",
                    identifier, clientIp
            );

            // Check if now locked after this attempt
            if (loginAttemptService.isLocked(identifier)) {
                long minutes = loginAttemptService
                        .getMinutesUntilUnlock(identifier);

                log.warn(
                        "Account locked after failed attempts: " +
                                "{} ip={} unlocks in {}min",
                        identifier, clientIp, minutes
                );

                response.sendRedirect(
                        "/login?error=locked&minutes=" + minutes
                );
                return;
            }

            // Warn user about remaining attempts
            // (only when they're getting close to lockout)
            int remaining = loginAttemptService
                    .getRemainingAttempts(identifier);

            if (remaining > 0 && remaining <= 2) {
                response.sendRedirect(
                        "/login?error=true&remaining=" +
                                remaining
                );
                return;
            }
        }

        // Generic failure (don't reveal reason)
        response.sendRedirect("/login?error=true");
    }

    /**
     * Gets real client IP behind Railway's proxy.
     */
    private String getClientIp(
            HttpServletRequest request
    ) {
        String xForwardedFor =
                request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null &&
                !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}