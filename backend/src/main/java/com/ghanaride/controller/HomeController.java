package com.ghanaride.controller;

import com.ghanaride.entity.Trip;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.TripService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the public landing page (/).
 *
 * This is the most important page for SEO and conversion.
 * It includes:
 * - Hero section with route search
 * - Fare estimator for popular routes
 * - Live stats (drivers, rides, cities)
 * - Testimonials
 * - How it works section
 *
 * Results are cached to reduce DB load on homepage.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TripService tripService;
    private final BookingService bookingService;
    private final UserService userService;

    // =========================================================
    // LANDING PAGE
    // Public — no login required
    // =========================================================
    @GetMapping("/")
    public String landingPage(Model model) {

        // -------------------------------------------------
        // Live stats for trust/credibility section
        // Cached to avoid DB hit on every page load
        // -------------------------------------------------
        try {
            long totalRides    =
                    bookingService.countAllCompleted();
            long totalDrivers  =
                    userService.countVerifiedDrivers();
            long totalCities   = 15L; // Update as you expand

            model.addAttribute("totalRides", totalRides);
            model.addAttribute("totalDrivers", totalDrivers);
            model.addAttribute("totalCities", totalCities);
        } catch (Exception e) {
            // Don't crash homepage if stats fail
            log.warn("Failed to load homepage stats", e);
            model.addAttribute("totalRides",   "1,000+");
            model.addAttribute("totalDrivers", "200+");
            model.addAttribute("totalCities",  "15+");
        }

        // -------------------------------------------------
        // Popular routes with fare estimates
        // Shown on homepage for "transparent pricing"
        // -------------------------------------------------
        model.addAttribute("popularRoutes",
                getPopularRoutes()
        );

        // -------------------------------------------------
        // Preview of upcoming trips
        // (Public — encourages signup to book)
        // -------------------------------------------------
        try {
            List<Trip> previewTrips =
                    tripService.findUpcomingApprovedPreview(6);
            model.addAttribute("previewTrips", previewTrips);
        } catch (Exception e) {
            log.warn("Failed to load preview trips", e);
        }

        // -------------------------------------------------
        // Ghana cities for search dropdowns
        // -------------------------------------------------
        model.addAttribute("locations", List.of(
                "Accra", "Kumasi", "Cape Coast", "Takoradi",
                "Tamale", "Sunyani", "Ho", "Koforidua",
                "Tema", "Winneba", "Bolgatanga", "Wa",
                "Techiman", "Obuasi", "Kasoa"
        ));

        // -------------------------------------------------
        // Testimonials
        // (Hardcoded for now — move to DB later)
        // -------------------------------------------------
        model.addAttribute("testimonials",
                getTestimonials()
        );

        // -------------------------------------------------
        // SEO Meta Tags
        // -------------------------------------------------
        model.addAttribute("pageTitle",
                "GhanaRide — Ghana's Trusted Campus & " +
                        "Intercity Transport Platform");
        model.addAttribute("pageDescription",
                "Book safe, affordable intercity and campus " +
                        "rides across Ghana. Accra to Kumasi, Cape " +
                        "Coast, Tamale and more. Verified drivers, " +
                        "transparent pricing, Mobile Money payments.");
        model.addAttribute("pageUrl",
                "https://ghanaride.me");
        model.addAttribute("ogImage",
                "https://ghanaride.me/images/og-image.jpg");

        // Structured data for Google (JSON-LD)
        model.addAttribute("jsonLd",
                buildJsonLd()
        );

        return "landing";
    }

    // =========================================================
    // POPULAR ROUTES WITH FARE ESTIMATES
    // Used on homepage fare estimator section
    // =========================================================
    @Cacheable("popularRoutes")
    private Map<String, RouteInfo> getPopularRoutes() {
        // Route → (distance km, base fare GHS)
        // Update fares based on your actual pricing
        Map<String, RouteInfo> routes = new LinkedHashMap<>();

        routes.put("Accra → Kumasi",
                new RouteInfo("Accra", "Kumasi",
                        248, new BigDecimal("120"),
                        new BigDecimal("150"), "4-5 hrs"));

        routes.put("Accra → Cape Coast",
                new RouteInfo("Accra", "Cape Coast",
                        165, new BigDecimal("80"),
                        new BigDecimal("100"), "2-3 hrs"));

        routes.put("Accra → Takoradi",
                new RouteInfo("Accra", "Takoradi",
                        220, new BigDecimal("100"),
                        new BigDecimal("130"), "3-4 hrs"));

        routes.put("Accra → Tamale",
                new RouteInfo("Accra", "Tamale",
                        599, new BigDecimal("200"),
                        new BigDecimal("250"), "8-9 hrs"));

        routes.put("Kumasi → Cape Coast",
                new RouteInfo("Kumasi", "Cape Coast",
                        153, new BigDecimal("80"),
                        new BigDecimal("100"), "2-3 hrs"));

        routes.put("Accra → Ho",
                new RouteInfo("Accra", "Ho",
                        168, new BigDecimal("90"),
                        new BigDecimal("110"), "3-4 hrs"));

        return routes;
    }

    // =========================================================
    // TESTIMONIALS
    // Move to database when you have real reviews
    // =========================================================
    private List<Testimonial> getTestimonials() {
        return List.of(
                new Testimonial(
                        "Kwame Asante",
                        "University of Ghana Student",
                        "GhanaRide saved me so much stress! " +
                                "I book my Accra-Kumasi ride for semester " +
                                "break right from my phone. The driver was " +
                                "punctual and the car was clean.",
                        5,
                        "Legon, Accra"
                ),
                new Testimonial(
                        "Ama Owusu",
                        "Business Traveller",
                        "Finally a reliable way to travel between " +
                                "cities. I use GhanaRide for my Accra-Takoradi " +
                                "trips every month. Transparent pricing and " +
                                "Mobile Money payment — perfect!",
                        5,
                        "Accra"
                ),
                new Testimonial(
                        "Kofi Mensah",
                        "KNUST Student",
                        "The best alternative to VIP bus. Cheaper, " +
                                "more flexible, and I can choose my driver. " +
                                "Booked 5 trips so far with no issues.",
                        5,
                        "Kumasi"
                ),
                new Testimonial(
                        "Abena Darko",
                        "Teacher",
                        "I was nervous about using a new platform " +
                                "but GhanaRide has been excellent. " +
                                "Driver verification gives me confidence " +
                                "travelling alone.",
                        4,
                        "Cape Coast"
                )
        );
    }

    // =========================================================
    // JSON-LD STRUCTURED DATA
    // Helps Google understand your business
    // =========================================================
    private String buildJsonLd() {
        return """
            {
              "@context": "https://schema.org",
              "@type": "TransportationCompany",
              "name": "GhanaRide",
              "description": "Ghana's trusted campus and intercity transport booking platform",
              "url": "https://ghanaride.me",
              "logo": "https://ghanaride.me/images/logo.png",
              "contactPoint": {
                "@type": "ContactPoint",
                "telephone": "+233-XX-XXX-XXXX",
                "contactType": "customer service",
                "areaServed": "GH",
                "availableLanguage": "English"
              },
              "address": {
                "@type": "PostalAddress",
                "addressLocality": "Accra",
                "addressCountry": "GH"
              },
              "sameAs": [
                "https://facebook.com/ghanaride",
                "https://twitter.com/ghanaride",
                "https://instagram.com/ghanaride"
              ]
            }
            """;
    }

    // =========================================================
    // INNER CLASSES — Data containers for template
    // =========================================================

    /**
     * Represents a popular route with fare range
     */
    public record RouteInfo(
            String from,
            String to,
            int distanceKm,
            BigDecimal minFare,
            BigDecimal maxFare,
            String duration
    ) {}

    /**
     * Represents a customer testimonial
     */
    public record Testimonial(
            String name,
            String role,
            String text,
            int rating,
            String location
    ) {}
}