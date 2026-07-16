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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
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
 * Note: PasswordEncoder bean is defined in PasswordEncoderConfig
 * to avoid duplicate bean definitions.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomOAuthSuccessHandler oAuthSuccessHandler;
    private final com.ghanaride.service.CustomOAuth2UserService customOAuth2UserService;

    @Value("${app.security.remember-me-key}")
    private String rememberMeKey;

    @Value("${app.security.remember-me-validity:2592000}")
    private int rememberMeValidity;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    private final ConcurrentHashMap<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockoutUntil = new ConcurrentHashMap<>();

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public OncePerRequestFilter loginRateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String path = request.getRequestURI();
                String method = request.getMethod();
                String ip = getClientIp(request);

                // Only rate limit POST /login and /login/oauth2/*
                if ("POST".equals(method) &&
                        (path.equals("/login") || path.startsWith("/login/oauth2/"))) {

                    Long lockout = lockoutUntil.get(ip);
                    if (lockout != null && lockout > System.currentTimeMillis()) {
                        long remaining = (lockout - System.currentTimeMillis()) / 1000;
                        response.setStatus(429);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\":\"Too many attempts. Try again in " + remaining + " seconds.\"}"
                        );
                        return;
                    }

                    int attempts = loginAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();
                    if (attempts > maxLoginAttempts) {
                        lockoutUntil.put(ip, System.currentTimeMillis() + (lockoutDurationMinutes * 60_000L));
                        response.setStatus(429);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\":\"Account temporarily locked. Try again in " + lockoutDurationMinutes + " minutes.\"}"
                        );
                        return;
                    }
                }

                filterChain.doFilter(request, response);
            }

            private String getClientIp(HttpServletRequest request) {
                String xf = request.getHeader("X-Forwarded-For");
                if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
                String xr = request.getHeader("X-Real-IP");
                if (xr != null && !xr.isBlank()) return xr;
                return request.getRemoteAddr();
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Static resources & public assets
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/uploads/**").permitAll()
                        .requestMatchers("/manifest.json", "/sw.js", "/offline.html").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Public pages
                        .requestMatchers(HttpMethod.GET, "/", "/home", "/about", "/contact", "/terms", "/privacy", "/refunds", "/faq", "/routes", "/rides", "/rides/search").permitAll()
                        .requestMatchers("/login", "/register", "/forgot-password", "/reset-password").permitAll()

                        // OAuth2 flow
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // Paystack callbacks
                        .requestMatchers("/payment/webhook").permitAll()

                        // Driver document uploads (only owner or admin)
                        .requestMatchers("/uploads/documents/**").hasAnyRole("DRIVER", "ADMIN")

                        // Actuator (admin only)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // ROLE-BASED ACCESS
                        .requestMatchers("/dashboard", "/booking/**", "/my-bookings/**",
                                "/profile/**", "/reviews/**", "/wallet/**", "/notifications/**", "/track/**")
                        .hasAnyRole("USER", "DRIVER", "COMPANY", "ADMIN")

                        .requestMatchers("/driver/**").hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers("/company/**").hasAnyRole("COMPANY", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/trips/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )

                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuthSuccessHandler)
                        .failureHandler(failureHandler)
                )

                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(rememberMeValidity)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("GHANARIDE_REMEMBER_ME")
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logged_out=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "GHANARIDE_REMEMBER_ME")
                        .clearAuthentication(true)
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .sessionFixation(sf -> sf.changeSessionId())
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?session=expired")
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.POST, "/payment/webhook"))
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )

                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )
                        .contentSecurityPolicy(csp -> csp.policyDirectives(buildCspPolicy()))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(policy -> policy.policy(
                                "geolocation=(), camera=(), microphone=(), payment=(self)"
                        ))
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestUri = request.getRequestURI();
                            if (requestUri.startsWith("/api/")) {
                                response.setContentType("application/json");
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write(
                                        "{\"error\":\"Unauthorized\",\"message\":\"Please login to continue\"}"
                                );
                            } else {
                                response.sendRedirect("/login?redirect=" +
                                        java.net.URLEncoder.encode(requestUri, java.nio.charset.StandardCharsets.UTF_8));
                            }
                        })
                );

        http.addFilterBefore(loginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private String buildCspPolicy() {
        return String.join("; ",
                "default-src 'self'",
                "script-src 'self' 'unsafe-inline' https://js.paystack.co https://accounts.google.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com",
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com",
                "font-src 'self' https://fonts.gstatic.com data:",
                "img-src 'self' data: https: https://lh3.googleusercontent.com",
                "frame-src 'self' https://js.paystack.co https://accounts.google.com https://checkout.paystack.com",
                "connect-src 'self' https://api.paystack.co https://accounts.google.com wss://ghanaride.me",
                "form-action 'self' https://accounts.google.com",
                "frame-ancestors 'none'",
                "upgrade-insecure-requests"
        );
    }
}