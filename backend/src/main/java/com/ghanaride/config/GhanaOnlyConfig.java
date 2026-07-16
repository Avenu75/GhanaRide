package com.ghanaride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;
import java.util.List;
import java.util.Map;

/**
 * GhanaRide v5.2 – GHANA ONLY EDITION
 * Enforces Ghana-only operation: GHS currency, +233 phones, Ghana geofence,
 * Africa/Accra timezone, en-GH locale.
 */
@Configuration
public class GhanaOnlyConfig {

    // Ghana bounding box – reject out-of-country coordinates
    public static final double GHANA_LAT_MIN = 4.5;
    public static final double GHANA_LAT_MAX = 11.2;
    public static final double GHANA_LNG_MIN = -3.3;
    public static final double GHANA_LNG_MAX = 1.2;

    public static final String COUNTRY_CODE = "GH";
    public static final String CURRENCY = "GHS";
    public static final String PHONE_PREFIX = "+233";
    public static final String TIMEZONE = "Africa/Accra";
    public static final Locale LOCALE = Locale.forLanguageTag("en-GH");

    // Major Ghana transport terminals with GPS – Bolt-style pickup points
    public static final Map<String, double[]> STATIONS = Map.ofEntries(
        Map.entry("Accra", new double[]{5.6037, -0.1870}),
        Map.entry("Circle Station, Accra", new double[]{5.5700, -0.2234}),
        Map.entry("Kaneshie Station, Accra", new double[]{5.5650, -0.2370}),
        Map.entry("Tema Station, Accra", new double[]{5.6698, -0.0166}),
        Map.entry("37 Station, Accra", new double[]{5.5850, -0.1820}),
        Map.entry("Kumasi", new double[]{6.6885, -1.6244}),
        Map.entry("Kejetia Terminal, Kumasi", new double[]{6.6917, -1.6241}),
        Map.entry("Asafo Market, Kumasi", new double[]{6.6780, -1.6100}),
        Map.entry("Cape Coast", new double[]{5.1053, -1.2466}),
        Map.entry("Kotokuraba Station, Cape Coast", new double[]{5.1060, -1.2450}),
        Map.entry("Takoradi", new double[]{4.9010, -1.7600}),
        Map.entry("Market Circle, Takoradi", new double[]{4.9015, -1.7570}),
        Map.entry("Tamale", new double[]{9.4075, -0.8393}),
        Map.entry("Tamale Main Station", new double[]{9.4030, -0.8420}),
        Map.entry("Sunyani", new double[]{7.3328, -2.3250}),
        Map.entry("Ho", new double[]{6.6111, 0.4708}),
        Map.entry("Koforidua", new double[]{6.0944, -0.2591}),
        Map.entry("Tema", new double[]{5.6698, -0.0166}),
        Map.entry("Winneba", new double[]{5.3511, -0.6233}),
        Map.entry("Techiman", new double[]{7.5909, -1.9343}),
        Map.entry("Obuasi", new double[]{6.1970, -1.6700}),
        Map.entry("Kasoa", new double[]{5.5333, -0.4167}),
        Map.entry("Bolgatanga", new double[]{10.7856, -0.8514}),
        Map.entry("Wa", new double[]{10.0601, -2.5099})
    );

    public static boolean isInGhana(double lat, double lng) {
        return lat >= GHANA_LAT_MIN && lat <= GHANA_LAT_MAX
            && lng >= GHANA_LNG_MIN && lng <= GHANA_LNG_MAX;
    }

    public static double[] getStationCoords(String name) {
        if (name == null) return STATIONS.get("Accra");
        // exact match first
        var exact = STATIONS.get(name);
        if (exact != null) return exact;
        // fuzzy: find key containing city
        String lower = name.toLowerCase();
        for (var e : STATIONS.entrySet()) {
            if (lower.contains(e.getKey().split(",")[0].toLowerCase()) ||
                e.getKey().toLowerCase().contains(lower)) {
                return e.getValue();
            }
        }
        // default Accra
        return STATIONS.get("Accra");
    }

    public static boolean isValidGhanaPhone(String phone) {
        if (phone == null) return false;
        String p = phone.replaceAll("[\\s\\-()]", "");
        // +233 XX XXX XXXX  or 0XX XXX XXXX
        return p.matches("^(\\+233|233|0)(20|23|24|25|26|27|28|50|53|54|55|56|57|59)[0-9]{7}$");
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(LOCALE);
        return slr;
    }

    public static List<String> ghanaCities() {
        return List.of(
            "Accra","Kumasi","Cape Coast","Takoradi","Tamale",
            "Sunyani","Ho","Koforidua","Tema","Winneba",
            "Techiman","Obuasi","Kasoa","Bolgatanga","Wa"
        );
    }
}
