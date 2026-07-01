package com.ghanaride.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    private final boolean emailVerified;

    public CustomUserDetails(String username, String password, boolean emailVerified, Collection<? extends GrantedAuthority> authorities) {
        // We set 'enabled' to true always, so they can log in even if unverified
        super(username, password, true, true, true, true, authorities);
        this.emailVerified = emailVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
