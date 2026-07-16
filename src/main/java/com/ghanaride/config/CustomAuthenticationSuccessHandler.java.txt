package com.ghanaride.config;

import com.ghanaride.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // Reset failed attempts on successful login
        loginAttemptService.resetAttempts(authentication.getName());

        String redirectUrl = "/dashboard";

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_DRIVER")) {
                redirectUrl = "/driver/dashboard";
                break;
            } else if (role.equals("ROLE_COMPANY")) {
                redirectUrl = "/company/dashboard";
                break;
            } else if (role.equals("ROLE_USER")) {
                redirectUrl = "/dashboard";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}