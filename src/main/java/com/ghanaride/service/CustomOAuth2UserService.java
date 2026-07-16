package com.ghanaride.security;

import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

/**
 * Custom OAuth2 User Service - Handles Google login flow.
 * Creates or updates users from OAuth2 profile data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oauth2User.getAttributes();

        String emailRaw = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleIdRaw = (String) attributes.get("sub");

        if (emailRaw == null || emailRaw.isBlank()) {
            log.error("OAuth2 login failed ({}): no email in profile attributes={}", registrationId, attributes.keySet());
            throw new OAuth2AuthenticationException(new OAuth2Error("no_email"), "Email not found from OAuth2 provider");
        }

        // Make everything final / effectively final for lambda capture
        final String email = emailRaw.toLowerCase().trim();
        final String displayName = (name != null && !name.isBlank()) ? name.trim() : email.split("@")[0];
        final String googleId = googleIdRaw;

        // find or create – variables inside lambda are now final
        User user;
        try {
            user = userService.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Auto-registering new OAuth user: {} via {}", email, registrationId);
                    return userService.registerOAuthUser(email, displayName, googleId);
                });
        } catch (Exception e) {
            // Defensive: if registerOAuthUser race hits duplicate, load existing
            log.warn("OAuth auto-register failed for {}, trying load existing: {}", email, e.getMessage());
            user = userService.findByEmail(email)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                    new OAuth2Error("user_create_failed"), e.getMessage(), e));
        }

        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("disabled"), "Account disabled");
        }

        // Build a proper UserDetails with ROLE_* for Spring Security
        CustomUserDetails userDetails = new CustomUserDetails(
            user.getUsername(),
            user.getPassword(),
            user.isEmailVerified(),
            user.isEnabled(),
            !user.isAccountLocked(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );

        // Return bridge object that is BOTH OAuth2User and UserDetails
        return new CustomOAuth2User(userDetails, attributes, userNameAttributeName);
    }
}