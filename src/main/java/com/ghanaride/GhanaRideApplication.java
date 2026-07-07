package com.ghanaride;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GhanaRide Application Entry Point
 *
 * Ghana's Trusted Campus and Intercity Transport Platform
 * Built with Spring Boot 3.2.5 + Java 21
 *
 * @author GhanaRide Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableScheduling   // For scheduled tasks (ride reminders,
// payment status checks, cleanup jobs)
@EnableAsync        // For async operations (send email without
// blocking the request thread,
// async SMS notifications)
@EnableCaching      // For Caffeine cache (routes, fares, cities)
public class GhanaRideApplication {

    @Autowired
    private Environment environment;

    @Value("${app.name:GhanaRide}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.base-url:http://localhost:8088}")
    private String appBaseUrl;

    // =========================================================
    // MAIN ENTRY POINT
    // =========================================================
    public static void main(String[] args) {
        // Optimize Spring Boot startup time
        System.setProperty(
                "spring.devtools.restart.enabled",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev")
                        .equals("prod") ? "false" : "true"
        );

        SpringApplication app = new SpringApplication(
                GhanaRideApplication.class
        );

        // Custom banner is loaded from banner.txt (see below)
        app.run(args);
    }

    // =========================================================
    // STARTUP VALIDATION & LOGGING
    // Runs AFTER the application is fully started and ready
    // to serve requests
    // =========================================================
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() throws UnknownHostException {
        String activeProfile = getActiveProfile();
        String hostAddress   = getHostAddress();
        int    serverPort    = getServerPort();

        // -------------------------------------------------
        // Startup Summary Log
        // -------------------------------------------------
        log.info("""
            
            ╔══════════════════════════════════════════════════════╗
            ║                                                      ║
            ║   🚗  {} v{}                          ║
            ║   Ghana's Trusted Transport Platform                 ║
            ║                                                      ║
            ╠══════════════════════════════════════════════════════╣
            ║                                                      ║
            ║   🌍  Profile   : {}                          ║
            ║   🖥️  Host      : {}                   ║
            ║   🔌  Port      : {}                               ║
            ║   🔗  Local URL : http://localhost:{}        ║
            ║   🌐  Public URL: {}                  ║
            ║   ⏰  Started   : {}         ║
            ║                                                      ║
            ╠══════════════════════════════════════════════════════╣
            ║                                                      ║
            ║   📋  Swagger   : {}/swagger-ui.html ║
            ║   ❤️  Health    : {}/actuator/health ║
            ║                                                      ║
            ╚══════════════════════════════════════════════════════╝
            """,
                appName, appVersion,
                activeProfile,
                hostAddress,
                serverPort,
                serverPort,
                appBaseUrl,
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                ),
                appBaseUrl,
                appBaseUrl
        );

        // -------------------------------------------------
        // Production Safety Checks
        // Warn if dangerous settings are active in prod
        // -------------------------------------------------
        if (activeProfile.equals("prod")) {
            validateProductionConfig();
        }

        // -------------------------------------------------
        // Development Hints
        // -------------------------------------------------
        if (activeProfile.equals("dev")) {
            log.info("""
                
                🛠️  DEV MODE ACTIVE
                ─────────────────────────────────────────
                📧  Email:    Using local mail server
                💾  DB DDL:   Auto-update enabled
                🔄  Restart:  DevTools enabled
                📊  SQL:      Logging enabled
                🔓  Security: Relaxed cookie settings
                ─────────────────────────────────────────
                """);
        }
    }

    // =========================================================
    // PRODUCTION CONFIGURATION VALIDATOR
    // Catches dangerous misconfigurations before they
    // cause problems in production
    // =========================================================
    private void validateProductionConfig() {
        log.info("🔍 Running production config validation...");

        boolean hasIssues = false;

        // Check required environment variables
        String[] requiredEnvVars = {
                "SPRING_DATASOURCE_URL",
                "SPRING_DATASOURCE_PASSWORD",
                "PAYSTACK_SECRET_KEY",
                "PAYSTACK_PUBLIC_KEY",
                "GOOGLE_CLIENT_ID",
                "GOOGLE_CLIENT_SECRET",
                "MAIL_USERNAME",
                "MAIL_PASSWORD",
                "REMEMBER_ME_KEY",
                "JWT_SECRET",
                "APP_BASE_URL"
        };

        for (String envVar : requiredEnvVars) {
            String value = System.getenv(envVar);
            if (value == null || value.isBlank()) {
                log.error(
                        "⚠️  MISSING environment variable: {} " +
                                "— this may cause runtime errors!",
                        envVar
                );
                hasIssues = true;
            }
        }

        // Check Paystack key format
        // Live keys start with sk_live_, test keys with sk_test_
        String paystackKey = System.getenv("PAYSTACK_SECRET_KEY");
        if (paystackKey != null) {
            if (paystackKey.startsWith("sk_test_")) {
                log.warn(
                        "⚠️  PAYSTACK_SECRET_KEY is a TEST key in production! " +
                                "Real payments will NOT work. " +
                                "Switch to sk_live_*** key."
                );
                hasIssues = true;
            } else if (paystackKey.startsWith("sk_live_")) {
                log.info("✅ Paystack live key detected");
            }
        }

        // Check App base URL uses HTTPS
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl != null && baseUrl.startsWith("http://")) {
            log.warn(
                    "⚠️  APP_BASE_URL uses HTTP, not HTTPS: {} " +
                            "OAuth2 and secure cookies may not work correctly.",
                    baseUrl
            );
            hasIssues = true;
        }

        // Check remember-me key is not the default
        String rememberMeKey = environment.getProperty(
                "app.security.remember-me-key", ""
        );
        if (rememberMeKey.contains("change-this") ||
                rememberMeKey.equals("dev-remember-me-key-not-secret")) {
            log.error(
                    "🚨 SECURITY: remember-me key is using default/dev value " +
                            "in production! Set REMEMBER_ME_KEY environment variable."
            );
            hasIssues = true;
        }

        if (hasIssues) {
            log.warn("""
                
                ⚠️ ═══════════════════════════════════════════ ⚠️
                   Production config issues detected above.
                   Review and fix before going live.
                ⚠️ ═══════════════════════════════════════════ ⚠️
                """);
        } else {
            log.info("✅ Production config validation passed!");
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================
    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "default";
    }

    private String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private int getServerPort() {
        String port = environment.getProperty("server.port", "8088");
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 8088;
        }
    }
}