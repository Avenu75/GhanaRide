package com.ghanaride.security;

import com.ghanaride.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Extended UserDetails that carries additional
 * GhanaRide-specific user information.
 *
 * Storing userId and role here avoids extra DB
 * lookups every time we need this info from the
 * security context.
 *
 * FIX: Was setting enabled=true always which
 * bypassed the disabled account check.
 * Now correctly passes through all account status
 * flags from the User entity.
 */
public class CustomUserDetails
        extends User {

    // Extra fields beyond Spring Security's User
    private final Long   userId;
    private final String email;
    private final String fullName;
    private final Role   role;
    private final boolean emailVerified;

    // =========================================================
    // CONSTRUCTOR — Full (recommended)
    // =========================================================
    public CustomUserDetails(
            String username,
            String password,
            boolean emailVerified,
            boolean enabled,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities,
            Long   userId,
            String email,
            String fullName,
            Role   role
    ) {
        super(
                username,
                password,
                enabled,           // FIX: was always true
                true,              // accountNonExpired
                true,              // credentialsNonExpired
                accountNonLocked,  // FIX: was always true
                authorities
        );
        this.userId        = userId;
        this.email         = email;
        this.fullName      = fullName;
        this.role          = role;
        this.emailVerified = emailVerified;
    }

    // =========================================================
    // CONSTRUCTOR — Minimal (backward compatibility)
    // Used by existing CustomUserDetailsService
    // =========================================================
    public CustomUserDetails(
            String username,
            String password,
            boolean emailVerified,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(
                username,
                password,
                emailVerified,  // enabled = emailVerified
                true,           // accountNonExpired
                true,           // credentialsNonExpired
                true,           // accountNonLocked
                authorities
        );
        this.userId        = null;
        this.email         = null;
        this.fullName      = null;
        this.role          = null;
        this.emailVerified = emailVerified;
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public Long    getUserId()       { return userId;        }
    public String  getEmail()        { return email;         }
    public String  getFullName()     { return fullName;      }
    public Role    getRole()         { return role;          }
    public boolean isEmailVerified() { return emailVerified; }

    /**
     * Convenience: check if user has a specific role.
     * Usage: customUserDetails.hasRole(Role.ADMIN)
     */
    public boolean hasRole(Role roleToCheck) {
        return this.role == roleToCheck;
    }

    /**
     * Convenience: get display name
     * (fullName if available, otherwise username)
     */
    public String getDisplayName() {
        return (fullName != null && !fullName.isBlank())
                ? fullName
                : getUsername();
    }

    @Override
    public String toString() {
        return "CustomUserDetails{" +
                "username='" + getUsername() + "'" +
                ", role=" + role +
                ", emailVerified=" + emailVerified +
                ", enabled=" + isEnabled() +
                "}";
    }

    public Long getId() {
        return userId;
    }
}