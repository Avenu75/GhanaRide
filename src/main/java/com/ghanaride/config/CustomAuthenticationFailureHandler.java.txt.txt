package com.ghanaride.config;

import com.ghanaride.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String username = request.getParameter("username");

        if (username != null && !username.isEmpty()) {
            loginAttemptService.recordFailedAttempt(username);

            // Check if now locked
            if (loginAttemptService.isLocked(username)) {
                long minutes = loginAttemptService.getMinutesUntilUnlock(username);
                response.sendRedirect("/login?locked=true&minutes=" + minutes);
                return;
            }

            // Check if account is unverified
            if (exception instanceof org.springframework.security.authentication.DisabledException) {
                response.sendRedirect("/login?unverified=true");
                return;
            }

            // Show remaining attempts warning
            int remaining = loginAttemptService.getRemainingAttempts(username);
            if (remaining <= 2) {
                response.sendRedirect("/login?error=true&remaining=" + remaining);
                return;
            }
        }

        response.sendRedirect("/login?error=true");
    }
}