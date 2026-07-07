package com.ghanaride.config;

import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles successful login.
 *
 * Responsibilities:
 * 1. Reset failed attempt counter
 * 2. Update lastLoginAt timestamp
 * 3. Redirect to saved request OR role dashboard
 *    (saved request = where user was trying to go
 *     before being redirected to login)
 *
 * FIX: Added saved-request handling so users are
 * redirected back to where they were trying to go.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;

    // Spring Security's request cache stores where the
    // user was trying to go before login
    private final RequestCache requestCache =
            new HttpSessionRequestCache();

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        String username = authentication.getName();

        // 1. Reset failed login attempts
        loginAttemptService.resetAttempts(username);

        // 2. Update lastLoginAt in DB (async-safe)
        try {
            userRepository
                    .findByUsernameOrEmail(username, username)
                    .ifPresent(user -> {
                        user.setLastLoginAt(
                                LocalDateTime.now()
                        );
                        userRepository.save(user);
                    });
        } catch (Exception e) {
            // Don't fail login if this update fails
            log.warn(
                    "Could not update lastLoginAt for: {}",
                    username, e
            );
        }

        log.info(
                "Successful login: user={} ip={}",
                username,
                getClientIp(request)
        );

        // 3. Check for saved request
        // (e.g., user tried to access /my-bookings
        //  before login — redirect them there)
        SavedRequest savedRequest =
                requestCache.getRequest(request, response);

        if (savedRequest != null) {
            String savedUrl =
                    savedRequest.getRedirectUrl();

            // Only use saved URL if it's safe
            // (not a login/logout/error page)
            if (isSafeRedirectUrl(savedUrl)) {
                log.debug(
                        "Redirecting to saved request: {}",
                        savedUrl
                );
                requestCache.removeRequest(
                        request, response
                );
                response.sendRedirect(savedUrl);
                return;
            }
        }

        // 4. Redirect based on role
        String redirectUrl = determineRedirectUrl(
                authentication
        );

        log.debug(
                "Redirecting {} to: {}",
                username, redirectUrl
        );

        response.sendRedirect(redirectUrl);
    }

    // =========================================================
    // DETERMINE REDIRECT URL BASED ON ROLE
    // =========================================================
    private String determineRedirectUrl(
            Authentication authentication
    ) {
        for (GrantedAuthority authority :
                authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "/admin/dashboard";
            } else if (role.equals("ROLE_DRIVER")) {
                return "/driver/dashboard";
            } else if (role.equals("ROLE_COMPANY")) {
                return "/company/dashboard";
            }
        }
        // Default: passenger dashboard
        return "/dashboard";
    }

    // =========================================================
    // SAFE REDIRECT CHECK
    // Prevent open redirect vulnerabilities
    // =========================================================
    private boolean isSafeRedirectUrl(String url) {
        if (url == null || url.isBlank()) return false;

        // Must be a relative path (not external URL)
        // or our own domain
        if (url.startsWith("http://") ||
                url.startsWith("https://")) {
            // Only allow our own domain
            return url.contains("ghanaride.me");
        }

        // Exclude auth-related pages
        // (no point redirecting back to login)
        String[] excludedPaths = {
                "/login", "/logout", "/register",
                "/forgot-password", "/reset-password",
                "/error"
        };

        for (String excluded : excludedPaths) {
            if (url.startsWith(excluded)) return false;
        }

        return true;
    }

    // =========================================================
    // GET CLIENT IP (handles Railway proxy)
    // =========================================================
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