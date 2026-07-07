package com.ghanaride.config;

import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles successful Google OAuth2 login.
 *
 * Flow:
 * 1. Extract email + name from Google profile
 * 2. Find existing user OR register new one
 * 3. Update lastLoginAt
 * 4. Redirect to saved request OR role dashboard
 *
 * FIX: Added saved-request handling, logging,
 * error handling, and lastLoginAt update.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuthSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final UserRepository userRepository;

    private final RequestCache requestCache =
            new HttpSessionRequestCache();

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        // Extract Google profile info
        OAuth2User oAuth2User =
                (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        String googleId =
                oAuth2User.getAttribute("sub"); // Google ID

        // Email is required
        if (email == null || email.isBlank()) {
            log.error(
                    "OAuth2 login failed: no email in " +
                            "Google profile"
            );
            response.sendRedirect(
                    "/login?error=oauth_no_email"
            );
            return;
        }

        // Sanitize name
        String displayName =
                (name != null && !name.isBlank())
                        ? name.trim()
                        : email.split("@")[0]; // Use email prefix

        User user;
        try {
            // Find existing user or register new one
            user = userService.findByEmail(
                            email.toLowerCase()
                    )
                    .orElseGet(() -> {
                        log.info(
                                "New OAuth user registering: {}",
                                email
                        );
                        return userService.registerOAuthUser(
                                email, displayName, googleId
                        );
                    });

        } catch (Exception e) {
            log.error(
                    "OAuth login failed for email: {} — {}",
                    email, e.getMessage(), e
            );
            response.sendRedirect(
                    "/login?error=oauth_failed"
            );
            return;
        }

        // Check account is enabled
        if (!user.isEnabled()) {
            log.warn(
                    "OAuth login blocked — disabled: {}",
                    email
            );
            response.sendRedirect(
                    "/login?error=disabled"
            );
            return;
        }

        // Update lastLoginAt
        try {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            log.warn(
                    "Could not update lastLoginAt for " +
                            "OAuth user: {}",
                    email, e
            );
        }

        log.info(
                "OAuth login success: {} ({}) role={}",
                email,
                user.getUsername(),
                user.getRole()
        );

        // Check for saved request
        SavedRequest savedRequest =
                requestCache.getRequest(request, response);

        if (savedRequest != null) {
            String savedUrl =
                    savedRequest.getRedirectUrl();
            if (isSafeRedirectUrl(savedUrl)) {
                requestCache.removeRequest(
                        request, response
                );
                response.sendRedirect(savedUrl);
                return;
            }
        }

        // Redirect based on role
        String redirectUrl = switch (user.getRole()) {
            case ADMIN   -> "/admin/dashboard";
            case DRIVER  -> "/driver/dashboard";
            case COMPANY -> "/company/dashboard";
            default      -> "/dashboard";
        };

        log.debug(
                "OAuth redirecting {} to: {}",
                email, redirectUrl
        );

        response.sendRedirect(redirectUrl);
    }

    // =========================================================
    // SAFE REDIRECT CHECK
    // =========================================================
    private boolean isSafeRedirectUrl(String url) {
        if (url == null || url.isBlank()) return false;

        if (url.startsWith("http://") ||
                url.startsWith("https://")) {
            return url.contains("ghanaride.me");
        }

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
}