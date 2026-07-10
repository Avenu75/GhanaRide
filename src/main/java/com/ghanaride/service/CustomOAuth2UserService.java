package com.ghanaride.service;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.User;
import com.ghanaride.security.CustomOAuth2User;
import com.ghanaride.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
 * Loads (or auto-registers) a Google user and returns
 * a CustomOAuth2User that carries ROLE_USER etc.,
 * fixing the 500 / 403 after "Continue with Google".
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

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed ({}): no email in profile attributes={}", registrationId, attributes.keySet());
            throw new OAuth2AuthenticationException(new OAuth2Error("no_email"), "Email not found from OAuth2 provider");
        }

        email = email.toLowerCase().trim();
        String displayName = (name != null && !name.isBlank()) ? name.trim() : email.split("@")[0];

        // find or create
        String finalEmail = email;
        User user = userService.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Auto-registering new OAuth user: {} via {}", finalEmail, registrationId);
                    return userService.registerOAuthUser(finalEmail, displayName, googleId);
                });

        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("disabled"), "Account disabled");
        }

        // Build a proper UserDetails with ROLE_*
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