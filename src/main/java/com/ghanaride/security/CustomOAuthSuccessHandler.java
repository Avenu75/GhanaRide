package com.ghanaride.security;

import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OAuth2 Success Handler - Handles post-Google-login redirect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuthSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String email = oauthToken.getPrincipal().getAttribute("email");

        // Ensure user exists in DB (CustomOAuth2UserService already did this, but double-check)
        userService.findByEmail(email).ifPresent(user -> {
            log.info("OAuth2 login success for: {}", user.getEmail());
        });

        // Redirect to intended page or dashboard
        String redirectUrl = request.getParameter("redirect");
        if (redirectUrl == null || redirectUrl.isBlank()) {
            redirectUrl = "/dashboard";
        }

        response.sendRedirect(redirectUrl);
    }
}