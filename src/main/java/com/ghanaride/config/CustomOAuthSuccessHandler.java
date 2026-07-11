package com.ghanaride.config;

import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import com.ghanaride.security.CustomOAuth2User;
import com.ghanaride.security.CustomUserDetails;
import com.ghanaride.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Handles successful Google OAuth2 login.
 *
 * FIXES APPLIED (2026-07-10):
 *  - Works with BOTH CustomOAuth2User (preferred) and legacy OAuth2User
 *  - Ensures proper ROLE_* authorities are present in SecurityContext
 *  - Null-safe user lookup, displayName sanitizing, email normalization
 *  - Updates lastLoginAt safely
 *  - Prevents 500 after "Continue with Google" by re-wrapping the Authentication
 *    when authorities are missing (SCOPE_* only)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final UserRepository userRepository;

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        // Extract principal – supports CustomOAuth2User and plain OAuth2User
        String email;
        String name;
        String googleId;
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if (authentication.getPrincipal() instanceof CustomOAuth2User customOAuthUser) {
            // Preferred path – already has DB user + ROLE_*
            email = customOAuthUser.getEmail();
            name = customOAuthUser.getFullName();
            // attribute "sub" may still be accessible
            Object sub = customOAuthUser.getAttributes().get("sub");
            googleId = sub != null ? sub.toString() : null;

            log.debug("OAuth success via CustomOAuth2User: {} authorities={}", email, authorities);
        } else if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            googleId = oAuth2User.getAttribute("sub");
        } else if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
            // Already converted elsewhere
            email = cud.getEmail();
            name = cud.getFullName();
            googleId = null;
        } else {
            log.error("OAuth success with unexpected principal type: {}", authentication.getPrincipal().getClass());
            response.sendRedirect("/login?error=oauth_failed");
            return;
        }

        // Email is required
        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed: no email in Google profile. principal={}", authentication.getPrincipal());
            response.sendRedirect("/login?error=oauth_no_email");
            return;
        }

        email = email.toLowerCase().trim();

        // Sanitize name – never null, max 100 chars (matches User.fullName column)
        String displayName = (name != null && !name.isBlank()) ? name.trim() : email.split("@")[0];
        if (displayName.length() > 100) {
            displayName = displayName.substring(0, 100);
        }

        final String finalEmail = email;
        final String finalDisplayName = displayName;
        final String finalGoogleId = googleId;

        User user;
        try {
            // Find existing user or register new one
            user = userService.findByEmail(finalEmail)
                    .orElseGet(() -> {
                        log.info("New OAuth user registering: {}", finalEmail);
                        return userService.registerOAuthUser(finalEmail, finalDisplayName, finalGoogleId);
                    });

        } catch (Exception e) {
            log.error("OAuth login failed for email: {} — {}", email, e.getMessage(), e);
            response.sendRedirect("/login?error=oauth_failed");
            return;
        }

        // Check account is enabled
        if (!user.isEnabled()) {
            log.warn("OAuth login blocked — disabled: {}", email);
            response.sendRedirect("/login?error=disabled");
            return;
        }

        // --- CRITICAL FIX: ensure SecurityContext has ROLE_* authorities ---
        // Legacy OAuth2AuthenticationToken only has SCOPE_* authorities,
        // causing hasRole() checks in SecurityConfig to fail -> 403/500.
        // If current authorities don't contain ROLE_, rebuild authentication.
        boolean hasRoleAuthority = authorities.stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_"));

        if (!hasRoleAuthority) {
            log.info("Re-wrapping OAuth authentication to inject ROLE_{} for {}", user.getRole(), email);
            List<GrantedAuthority> roleAuthorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            CustomUserDetails userDetails = new CustomUserDetails(
                    user.getUsername(),
                    user.getPassword(),
                    user.isEmailVerified(),
                    user.isEnabled(),
                    !user.isAccountLocked(),
                    roleAuthorities,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            );

            // Preserve OAuth2 attributes if we can
            Authentication newAuth;
            if (authentication instanceof OAuth2AuthenticationToken oauthToken &&
                    authentication.getPrincipal() instanceof OAuth2User oAuth2User) {

                // Build a CustomOAuth2User so we keep attributes
                com.ghanaride.security.CustomOAuth2User bridge =
                        new com.ghanaride.security.CustomOAuth2User(
                                userDetails,
                                oAuth2User.getAttributes(),
                                oauthToken.getAuthorizedClientRegistrationId()
                        );

                newAuth = new OAuth2AuthenticationToken(
                        bridge,
                        roleAuthorities,
                        oauthToken.getAuthorizedClientRegistrationId()
                );
            } else {
                newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, roleAuthorities
                );
            }

            SecurityContextHolder.getContext().setAuthentication(newAuth);
            // Ensure session is updated (Spring Security will persist at end of request,
            // but explicit save helps on Railway with session fixation migrate)
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            authentication = newAuth;
        }

        // Update lastLoginAt
        try {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            log.warn("Could not update lastLoginAt for OAuth user: {}", email, e);
        }
        log.info("OAuth login success: {} ({}) role={}", email, user.getUsername(), user.getRole());

        // v3.2 – PROFILE COMPLETION ENFORCEMENT
        // Google users often lack phoneNumber – force /profile first
        try {
            if (!userService.isProfileComplete(user)) {
                log.info("OAuth first-login – profile incomplete, forcing /profile for {}", email);
                request.getSession().setAttribute("OAUTH_JUST_REGISTERED", true);
                response.sendRedirect("/profile?complete=oauth&welcome=true");
                return;
            }
        } catch (Exception e) {
            log.warn("Profile completeness check failed, continuing to dashboard: {}", e.getMessage());
        }

        // Check for saved request

        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (savedRequest != null) {
            String savedUrl = savedRequest.getRedirectUrl();
            if (isSafeRedirectUrl(savedUrl)) {
                requestCache.removeRequest(request, response);
                response.sendRedirect(savedUrl);
                return;
            }
        }

        // Redirect based on role
        String redirectUrl = switch (user.getRole()) {
            case ADMIN -> "/admin/dashboard";
            case DRIVER -> "/driver/dashboard";
            case COMPANY -> "/company/dashboard";
            default -> "/dashboard";
        };

        log.debug("OAuth redirecting {} to: {}", email, redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    // =========================================================
    // SAFE REDIRECT CHECK
    // =========================================================
    private boolean isSafeRedirectUrl(String url) {
        if (url == null || url.isBlank()) return false;

        // Allow same-site relative URLs, block open redirects
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url.contains("ghanaride.me") || url.contains("localhost");
        }

        String[] excludedPaths = {
                "/login", "/logout", "/register",
                "/forgot-password", "/reset-password",
                "/error", "/oauth2"
        };

        for (String excluded : excludedPaths) {
            if (url.startsWith(excluded)) return false;
        }

        return url.startsWith("/");
    }
}
