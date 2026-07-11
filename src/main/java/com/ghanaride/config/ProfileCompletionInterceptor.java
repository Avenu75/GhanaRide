package com.ghanaride.config;

import com.ghanaride.entity.User;
import com.ghanaride.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCompletionInterceptor implements HandlerInterceptor {

    private final UserService userService;

    private static final String[] ALWAYS_ALLOWED = {
            "/profile", "/profile/",
            "/logout", "/login", "/register",
            "/error", "/css/", "/js/", "/images/", "/uploads/",
            "/actuator/health", "/favicon", "/manifest", "/sw.js",
            "/api/"
    };

    private static final String[] PROTECTED_NEEDS_PHONE = {
            "/booking",
            "/payment",
            "/reviews"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        for (String p : ALWAYS_ALLOWED) {
            if (uri.startsWith(p)) return true;
        }
        boolean needsCheck = false;
        for (String p : PROTECTED_NEEDS_PHONE) {
            if (uri.startsWith(p)) { needsCheck = true; break; }
        }
        if (!needsCheck) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }
        try {
            Principal principal = (Principal) auth;
            User user = userService.getCurrentUser(principal);
            if (!userService.isProfileComplete(user)) {
                log.info("Blocking {} – profile incomplete user={}", uri, user.getEmail());
                String target = java.net.URLEncoder.encode(uri + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                        java.nio.charset.StandardCharsets.UTF_8);
                response.sendRedirect("/profile?complete=required&redirect=" + target);
                return false;
            }
        } catch (Exception e) {
            log.warn("ProfileCompletionInterceptor error: {}", e.getMessage());
            return true;
        }
        return true;
    }
}
