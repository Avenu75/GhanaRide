package com.ghanaride.security;

import com.ghanaride.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Bridges OAuth2User and UserDetails so Google logins
 * get proper ROLE_* authorities and work with @PreAuthorize / hasRole checks.
 */
@Slf4j
public class CustomOAuth2User implements OAuth2User, UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final CustomUserDetails userDetails;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomOAuth2User(CustomUserDetails userDetails,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        this.userDetails = userDetails;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    // ---- OAuth2User ----
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        Object val = attributes.get(nameAttributeKey);
        return val != null ? val.toString() : userDetails.getUsername();
    }

    // ---- UserDetails (delegates to CustomUserDetails) ----
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userDetails.getAuthorities();
    }

    @Override
    public String getPassword() {
        return userDetails.getPassword();
    }

    @Override
    public String getUsername() {
        return userDetails.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return userDetails.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return userDetails.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return userDetails.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return userDetails.isEnabled();
    }

    // ---- Convenience getters ----
    public Long getUserId() { return userDetails.getUserId(); }
    public String getEmail() { return userDetails.getEmail(); }
    public String getFullName() { return userDetails.getFullName(); }
    public Role getRole() { return userDetails.getRole(); }

    public CustomUserDetails getUserDetails() {
        return userDetails;
    }
}