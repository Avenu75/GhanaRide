package com.ghanaride.config;

import com.ghanaride.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GhanaRide Security Configuration
 *
 * Handles:
 * - Public vs authenticated route rules
 * - Form login + Google OAuth2
 * - Security headers (HSTS, CSP, etc.)
 * - Session management
 * - Remember-me
 * - CSRF protection
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)    // Enables @PreAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    // =========================================================
    // Dependencies (injected via constructor by Lombok)
    // =========================================================
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomOAuthSuccessHandler oAuthSuccessHandler;

    // =========================================================
    // Config values from application.properties
    // =========================================================
    @Value("${app.security.remember-me-key}")
    private String rememberMeKey;

    @Value("${app.security.remember-me-validity:2592000}")
    private int rememberMeValidity;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.base-url}")
    private String appBaseUrl;

    // =========================================================
    // AUTHENTICATION PROVIDER
    // Wires together UserDetailsService + PasswordEncoder
    // =========================================================
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        // Show "bad credentials" instead of "user not found"
        // (prevents username enumeration attacks)
        authProvider.setHideUserNotFoundExceptions(true);
        return authProvider;
    }

    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }

    // =========================================================
    // SESSION EVENT PUBLISHER
    // Required for concurrent session control to work
    // =========================================================
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // =========================================================
    // MAIN SECURITY FILTER CHAIN
    // =========================================================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // -------------------------------------------------
                // CSRF PROTECTION
                // Thymeleaf th:action auto-injects CSRF tokens
                // so all forms are protected automatically.
                // We only exclude Paystack webhook callbacks
                // (they come from Paystack's servers, not browsers)
                // -------------------------------------------------
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/payment/webhook",       // Paystack webhook
                                "/api/**"                 // REST API (uses JWT instead)
                        )
                )

                // -------------------------------------------------
                // SECURITY HEADERS
                // Protects against XSS, clickjacking, MITM, etc.
                // -------------------------------------------------
                .headers(headers -> headers

                        // HSTS: Force HTTPS for 1 year
                        // (After this is deployed, browsers will NEVER
                        //  try HTTP for your domain)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )

                        // Prevent clickjacking (same as X-Frame-Options: DENY)
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)

                        // Prevent MIME type sniffing
                        .contentTypeOptions(contentType -> {})

                        // Content Security Policy
                        // Allows Bootstrap CDN, Google OAuth, Paystack
                        // Adjust if you add other CDN sources
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(buildCspPolicy())
                        )

                        // Referrer Policy: Don't leak URL details
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )

                        // Permissions Policy: Disable unused browser features
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(self), " +
                                        "payment=(self), usb=(), bluetooth=()")
                        )
                )

                // -------------------------------------------------
                // AUTHORIZATION RULES
                // Order matters: more specific rules first
                // -------------------------------------------------
                .authorizeHttpRequests(auth -> auth

                        // --- Fully PUBLIC pages ---
                        // Core pages
                        .requestMatchers(
                                "/",
                                "/about",
                                "/contact",
                                "/register",
                                "/login",
                                "/forgot-password",
                                "/reset-password"
                        ).permitAll()

                        // FIX #1: Legal pages (were incorrectly
                        // requiring authentication — now public)
                        .requestMatchers(
                                "/terms",
                                "/privacy",
                                "/refunds",
                                "/faq"
                        ).permitAll()

                        // FIX #2: SEO & crawling files
                        // (were redirecting to /login!)
                        .requestMatchers(
                                "/sitemap.xml",
                                "/robots.txt",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/apple-touch-icon.png"
                        ).permitAll()

                        // Static resources
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/webjars/**"
                        ).permitAll()

                        // Public uploads (profile photos, car images)
                        // Driver documents are protected below
                        .requestMatchers("/uploads/cars/**").permitAll()
                        .requestMatchers("/uploads/profiles/**").permitAll()

                        // FIX #3: Actuator health for Railway
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // PWA files
                        .requestMatchers(
                                "/manifest.json",
                                "/sw.js",
                                "/offline.html"
                        ).permitAll()

                        // OAuth2 flow
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // Error pages
                        .requestMatchers("/error", "/error/**").permitAll()

                        // Paystack payment callbacks
                        .requestMatchers("/payment/**").permitAll()

                        // Public route browsing (no login needed
                        // to SEE available rides — improves conversion)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/rides",
                                "/rides/search",
                                "/routes/**"
                        ).permitAll()

                        // --- PROTECTED: Driver document uploads
                        // (only accessible by owner or admin)
                        .requestMatchers("/uploads/documents/**")
                        .hasAnyRole("DRIVER", "ADMIN")

                        // --- PROTECTED: Actuator (admin only)
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")

                        // --- ROLE-BASED ACCESS ---
                        .requestMatchers(
                                "/dashboard",
                                "/booking/**",
                                "/my-bookings/**",
                                "/profile/**",
                                "/reviews/**"
                        ).hasAnyRole("USER", "DRIVER", "COMPANY", "ADMIN")

                        .requestMatchers("/driver/**")
                        .hasAnyRole("DRIVER", "ADMIN")

                        .requestMatchers("/company/**")
                        .hasAnyRole("COMPANY", "ADMIN")

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // API endpoints (JWT authenticated)
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/**").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // -------------------------------------------------
                // FORM LOGIN
                // -------------------------------------------------
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .usernameParameter("username")     // Matches login.html's "username"
                        // input field. CustomUserDetailsService
                        // looks the value up as either a
                        // username OR an email, so this
                        // single field supports both.
                        .passwordParameter("password")
                        .permitAll()
                )

                // -------------------------------------------------
                // GOOGLE OAUTH2 LOGIN
                // -------------------------------------------------
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oAuthSuccessHandler)
                        .failureHandler(failureHandler)
                )

                // -------------------------------------------------
                // REMEMBER ME
                // "Remember me for 30 days" checkbox on login form
                // -------------------------------------------------
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(rememberMeValidity)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("GHANARIDE_REMEMBER_ME")
                )

                // -------------------------------------------------
                // LOGOUT
                // -------------------------------------------------
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logged_out=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "GHANARIDE_REMEMBER_ME")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // -------------------------------------------------
                // SESSION MANAGEMENT
                // Prevents session fixation attacks
                // Limits concurrent sessions per user
                // -------------------------------------------------
                .sessionManagement(session -> session
                        // Migrate session on login (prevents fixation)
                        .sessionFixation(
                                sf -> sf.migrateSession()
                        )
                        // Max 3 concurrent sessions per user
                        // (prevents account sharing abuse)
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?session=expired")
                )

                // -------------------------------------------------
                // EXCEPTION HANDLING
                // Custom pages for access denied
                // -------------------------------------------------
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                        .authenticationEntryPoint((request, response, authException) -> {
                            // If it's an API request, return 401 JSON
                            // If it's a web request, redirect to login
                            String requestUri = request.getRequestURI();
                            if (requestUri.startsWith("/api/")) {
                                response.setContentType("application/json");
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write(
                                        "{\"error\":\"Unauthorized\"," +
                                                "\"message\":\"Please login to continue\"}"
                                );
                            } else {
                                response.sendRedirect("/login?redirect=" +
                                        java.net.URLEncoder.encode(requestUri,
                                                java.nio.charset.StandardCharsets.UTF_8));
                            }
                        })
                );

        return http.build();
    }

    // =========================================================
    // CONTENT SECURITY POLICY BUILDER
    // Allows exactly what GhanaRide needs, blocks everything else
    // =========================================================
    private String buildCspPolicy() {
        return String.join("; ",
                // Scripts: self + Bootstrap CDN + Paystack + Google
                "default-src 'self'",

                // Scripts from trusted CDNs only
                "script-src 'self' " +
                        "'unsafe-inline' " +      // Required for Thymeleaf inline scripts
                        "https://js.paystack.co " +
                        "https://accounts.google.com " +
                        "https://cdn.jsdelivr.net " +
                        "https://cdnjs.cloudflare.com",

                // Styles from trusted CDNs
                "style-src 'self' " +
                        "'unsafe-inline' " +      // Required for Bootstrap
                        "https://fonts.googleapis.com " +
                        "https://cdn.jsdelivr.net " +
                        "https://cdnjs.cloudflare.com",

                // Fonts from Google
                "font-src 'self' " +
                        "https://fonts.gstatic.com " +
                        "data:",

                // Images from self + data URIs (for inline images)
                "img-src 'self' " +
                        "data: " +
                        "https: " +              // Allow HTTPS images
                        "https://lh3.googleusercontent.com", // Google profile pics

                // Frames: Paystack payment modal + Google OAuth
                "frame-src 'self' " +
                        "https://js.paystack.co " +
                        "https://accounts.google.com " +
                        "https://checkout.paystack.com",

                // API connections allowed
                "connect-src 'self' " +
                        "https://api.paystack.co " +
                        "https://accounts.google.com " +
                        "wss://ghanaride.me",     // WebSocket for real-time features

                // Form submissions only to self
                "form-action 'self' " +
                        "https://accounts.google.com",

                // Disallow embedding in iframes (prevents clickjacking)
                "frame-ancestors 'none'",

                // Only load resources over HTTPS
                "upgrade-insecure-requests"
        );
    }
}