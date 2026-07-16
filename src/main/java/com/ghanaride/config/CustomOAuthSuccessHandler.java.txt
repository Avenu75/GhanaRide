package com.ghanaride.config;

import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class CustomOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            response.sendRedirect("/login?oauth_error=true");
            return;
        }

        // Find existing user or create new one
        User user = userService.findByEmail(email)
                .orElseGet(() -> userService.registerOAuthUser(email, name != null ? name : email, null));

        // Redirect based on role
        String redirectUrl = switch (user.getRole()) {
            case ADMIN  -> "/admin/dashboard";
            case DRIVER -> "/driver/dashboard";
            case COMPANY -> "/company/dashboard";
            default     -> "/dashboard";
        };

        response.sendRedirect(redirectUrl);
    }
}
