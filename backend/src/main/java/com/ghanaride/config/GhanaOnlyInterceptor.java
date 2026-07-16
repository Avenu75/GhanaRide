package com.ghanaride.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.TimeZone;

/**
 * v5.2 GHANA ONLY – enforces:
 * - Timezone Africa/Accra
 * - Currency GHS locked
 * - Blocks obviously non-GH requests (optional – soft)
 */
@Slf4j
@Component
public class GhanaOnlyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Force Ghana timezone context per request
        TimeZone.setDefault(TimeZone.getTimeZone(GhanaOnlyConfig.TIMEZONE));

        // Add Ghana headers – for Bolt-style localization
        response.setHeader("X-GhanaRide-Region", "GH");
        response.setHeader("X-GhanaRide-Currency", "GHS");
        response.setHeader("X-GhanaRide-Timezone", "Africa/Accra");

        // Optional IP geofence – soft warning only (many Ghana users via VPN / Starlink)
        String cfCountry = request.getHeader("CF-IPCountry");
        if (cfCountry != null && !cfCountry.isBlank() && !"GH".equalsIgnoreCase(cfCountry) && !"XX".equals(cfCountry)) {
            // log only – do NOT block – Ghanaians abroad still book for family
            log.debug("Non-GH request detected via Cloudflare: {} -> {}", cfCountry, request.getRequestURI());
            request.setAttribute("ghana_warning", true);
        }

        // Always allow – GhanaRide is Ghana-focused but not IP-locked
        // (diaspora books for relatives)
        return true;
    }
}
