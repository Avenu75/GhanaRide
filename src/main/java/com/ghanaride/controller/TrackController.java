package com.ghanaride.controller;

import com.ghanaride.config.GhanaOnlyConfig;
import com.ghanaride.entity.Booking;
import com.ghanaride.entity.User;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TrackController {

    private final BookingService bookingService;
    private final UserService userService;

    @Value("${google.maps.api-key:}")
    private String googleMapsApiKey;

    @Value("${map.provider:google}")
    private String mapProvider; // google or osm

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/track/{bookingId}")
    public String track(
            @PathVariable Long bookingId,
            Principal principal,
            Model model,
            RedirectAttributes ra) {

        Optional<Booking> opt = bookingService.findByIdWithDetails(bookingId);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Booking not found");
            return "redirect:/my-bookings";
        }
        Booking booking = opt.get();
        User currentUser = userService.getCurrentUser(principal);

        // owner or driver or admin
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());
        boolean isDriver = booking.getTrip().getDriver() != null &&
                booking.getTrip().getDriver().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        if (!isOwner && !isDriver && !isAdmin) {
            ra.addFlashAttribute("error", "Access denied – GhanaRide tracking is private.");
            return "redirect:/my-bookings";
        }

        var trip = booking.getTrip();
        // pickup coords – Ghana stations DB
        double[] pickup = GhanaOnlyConfig.getStationCoords(
                trip.getPickupStation() != null ? trip.getPickupStation() : trip.getFromLocation());
        double[] dropoff = GhanaOnlyConfig.getStationCoords(trip.getToLocation());

        model.addAttribute("booking", booking);
        model.addAttribute("trip", trip);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pickupLat", pickup[0]);
        model.addAttribute("pickupLng", pickup[1]);
        model.addAttribute("dropoffLat", dropoff[0]);
        model.addAttribute("dropoffLng", dropoff[1]);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey != null ? googleMapsApiKey : "");
        model.addAttribute("mapProvider", (googleMapsApiKey == null || googleMapsApiKey.isBlank()) ? "osm" : mapProvider);
        model.addAttribute("ghanaBounds", new double[][]{
                {GhanaOnlyConfig.GHANA_LAT_MIN, GhanaOnlyConfig.GHANA_LNG_MIN},
                {GhanaOnlyConfig.GHANA_LAT_MAX, GhanaOnlyConfig.GHANA_LNG_MAX}
        });
        model.addAttribute("pageTitle", "Live Track • " + trip.getFromLocation() + " → " + trip.getToLocation() + " — GhanaRide");

        return "track";
    }

    // Public share link – optional token later – for now auth required (safer)
}
