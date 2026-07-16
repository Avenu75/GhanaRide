package com.ghanaride.security;

import com.ghanaride.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Success handler for form login - handles redirect and login attempts tracking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        Authentication authentication) throws java.io.IOException, ServletException {

        String username = authentication.getName();
        loginAttemptService.resetAttempts(username);

        // Handle OAuth2 vs Form login
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            request.getSession().setAttribute("userId", userDetails.getUserId());
        }

        // Redirect to originally requested page or dashboard
        String redirectUrl = request.getParameter("redirect");
        if (redirectUrl == null || redirectUrl.isBlank()) {
            redirectUrl = "/dashboard";
        }

        response.sendRedirect(redirectUrl);
    }
}