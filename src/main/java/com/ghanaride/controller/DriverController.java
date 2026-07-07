package com.ghanaride.controller;

import com.ghanaride.entity.*;
import com.ghanaride.service.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Driver portal controller.
 * Handles trip management, passenger viewing,
 * and driver dashboard.
 *
 * All endpoints require DRIVER or ADMIN role.
 */
@Slf4j
@Controller
@RequestMapping("/driver")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")  // Class-level security
public class DriverController {

    private final UserService userService;
    private final CarService carService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final FileStorageService fileStorageService;

    // Ghana cities list — could be moved to DB/config
    private static final List<String> LOCATIONS = List.of(
            "Accra", "Cape Coast", "Kumasi", "Takoradi", "Tamale",
            "Sunyani", "Ho", "Koforidua", "Tema", "Winneba",
            "Bolgatanga", "Wa", "Techiman", "Obuasi", "Kasoa"
    );

    // Allowed image MIME types
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    // =========================================================
    // DASHBOARD
    // =========================================================
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        List<Trip> trips = tripService.findByDriver(currentUser);
        List<Car> cars = carService.findByDriver(currentUser);

        // Build bookings map for each trip
        Map<Long, List<Booking>> bookingsMap = new HashMap<>();
        for (Trip trip : trips) {
            bookingsMap.put(
                    trip.getId(),
                    bookingService.findByTripId(trip.getId())
            );
        }

        // Stats for dashboard cards
        long totalTrips = trips.size();
        long activeTrips = trips.stream()
                .filter(t -> t.getStatus() == TripStatus.APPROVED)
                .count();
        long completedTrips = trips.stream()
                .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                .count();
        long totalPassengers = bookingsMap.values().stream()
                .mapToLong(List::size)
                .sum();

        boolean hasActiveTrip =
                tripService.driverHasActiveTrip(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trips", trips);
        model.addAttribute("cars", cars);
        model.addAttribute("bookingsMap", bookingsMap);
        model.addAttribute("hasActiveTrip", hasActiveTrip);
        model.addAttribute("totalTrips", totalTrips);
        model.addAttribute("activeTrips", activeTrips);
        model.addAttribute("completedTrips", completedTrips);
        model.addAttribute("totalPassengers", totalPassengers);
        model.addAttribute("locations", LOCATIONS);
        model.addAttribute("pageTitle",
                "Driver Dashboard — GhanaRide");

        return "driver/dashboard";
    }

    // =========================================================
    // ADD TRIP — GET
    // =========================================================
    @GetMapping("/add-trip")
    public String showAddTripForm(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        List<Car> driverCars = carService.findByDriver(currentUser);

        if (tripService.driverHasActiveTrip(currentUser)) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("error",
                    "You already have an active trip. Please complete " +
                            "or cancel your current trip before adding a new one.");
            model.addAttribute("locations", LOCATIONS);
            model.addAttribute("driverCars", driverCars);
            model.addAttribute("pageTitle",
                    "Add Trip — GhanaRide Driver Portal");
            return "driver/add-trip";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("locations", LOCATIONS);
        model.addAttribute("driverCars", driverCars);
        model.addAttribute("pageTitle",
                "Add Trip — GhanaRide Driver Portal");
        return "driver/add-trip";
    }

    // =========================================================
    // ADD TRIP — POST
    // =========================================================
    @PostMapping("/add-trip")
    public String addTrip(
            @RequestParam @NotBlank String carBrand,
            @RequestParam @NotBlank String numberPlate,
            @RequestParam(required = false) MultipartFile carImage,
            @RequestParam @NotBlank String fromLocation,
            @RequestParam @NotBlank String toLocation,
            @RequestParam(required = false) String pickupStation,
            @RequestParam @NotBlank String departureTime,
            @RequestParam @DecimalMin("1.00") BigDecimal tripAmount,
            @RequestParam @Min(1) Integer totalSeats,
            @RequestParam(required = false) String description,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);

        // Prevent duplicate active trips
        if (tripService.driverHasActiveTrip(currentUser)) {
            redirectAttributes.addFlashAttribute("error",
                    "You already have an active trip. Please complete " +
                            "or cancel it before adding a new one.");
            return "redirect:/driver/dashboard";
        }

        // Validate departure time
        LocalDateTime departureDateTime;
        try {
            departureDateTime = LocalDateTime.parse(departureTime);
            // Must be in the future
            if (departureDateTime.isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error",
                        "Departure time must be in the future.");
                return "redirect:/driver/add-trip";
            }
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid departure time format. " +
                            "Please use the date picker.");
            return "redirect:/driver/add-trip";
        }

        // Validate from/to are different
        if (fromLocation.equalsIgnoreCase(toLocation)) {
            redirectAttributes.addFlashAttribute("error",
                    "Departure and destination cannot be the same.");
            return "redirect:/driver/add-trip";
        }

        // Handle car — reuse existing or create new
        Car car;
        try {
            if (carService.existsByNumberPlate(numberPlate)) {
                car = carService.findByDriver(currentUser).stream()
                        .filter(c -> c.getNumberPlate()
                                .equalsIgnoreCase(numberPlate))
                        .findFirst()
                        .orElse(null);

                if (car == null) {
                    redirectAttributes.addFlashAttribute("error",
                            "This number plate is registered to " +
                                    "another driver.");
                    return "redirect:/driver/add-trip";
                }
            } else {
                // Create new car
                car = new Car();
                car.setDriver(currentUser);
                car.setCarBrand(carBrand.trim());
                car.setNumberPlate(
                        numberPlate.trim().toUpperCase()
                );
                car.setStatus(CarStatus.APPROVED);

                // Handle car image upload
                if (carImage != null && !carImage.isEmpty()) {
                    // Validate file type
                    String contentType = carImage.getContentType();
                    if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                        redirectAttributes.addFlashAttribute("error",
                                "Car image must be JPEG, PNG, or WebP.");
                        return "redirect:/driver/add-trip";
                    }
                    // Validate file size
                    if (carImage.getSize() > MAX_IMAGE_SIZE) {
                        redirectAttributes.addFlashAttribute("error",
                                "Car image must be smaller than 5MB.");
                        return "redirect:/driver/add-trip";
                    }
                    String imagePath =
                            fileStorageService.storeFile(carImage);
                    car.setCarImagePath("/" + imagePath);
                }
                car = carService.saveCar(car);
            }
        } catch (Exception e) {
            log.error("Error processing car for driver: {}",
                    currentUser.getEmail(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error saving vehicle details. Please try again.");
            return "redirect:/driver/add-trip";
        }

        // Create and save trip
        try {
            Trip trip = new Trip();
            trip.setCar(car);
            trip.setDriver(currentUser);
            trip.setFromLocation(fromLocation);
            trip.setToLocation(toLocation);
            trip.setPickupStation(pickupStation);
            trip.setDepartureTime(departureDateTime);
            trip.setTripAmount(tripAmount);
            trip.setTotalSeats(totalSeats);
            trip.setAvailableSeats(totalSeats);
            trip.setDescription(
                    description != null ? description.trim() : null
            );
            trip.setStatus(TripStatus.PENDING);

            tripService.saveTrip(trip);

            log.info("Driver {} added new trip: {} → {} on {}",
                    currentUser.getEmail(),
                    fromLocation, toLocation,
                    departureDateTime
            );

            redirectAttributes.addFlashAttribute("success",
                    "Trip added successfully! It is pending admin " +
                            "approval and will be visible to passengers soon.");
            return "redirect:/driver/dashboard";

        } catch (Exception e) {
            log.error("Error saving trip for driver: {}",
                    currentUser.getEmail(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to create trip. Please try again.");
            return "redirect:/driver/add-trip";
        }
    }

    // =========================================================
    // CANCEL TRIP
    // =========================================================
    @PostMapping("/trips/{tripId}/cancel")
    public String cancelTrip(
            @PathVariable Long tripId,
            @RequestParam String cancelReason,
            @RequestParam(required = false) String cancelReasonDetails,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);

        Optional<Trip> tripOpt = tripService.findById(tripId);

        // Ownership check
        if (tripOpt.isEmpty() ||
                !tripOpt.get().getDriver().getId()
                        .equals(currentUser.getId())) {
            log.warn("Driver {} attempted to cancel trip {} " +
                            "they don't own",
                    currentUser.getEmail(), tripId);
            redirectAttributes.addFlashAttribute("error",
                    "Trip not found or access denied.");
            return "redirect:/driver/dashboard";
        }

        Trip trip = tripOpt.get();

        // Can't cancel already completed/cancelled trips
        if (trip.getStatus() == TripStatus.COMPLETED ||
                trip.getStatus() == TripStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot cancel a trip that is already " +
                            trip.getStatus().toString().toLowerCase() + ".");
            return "redirect:/driver/dashboard";
        }

        // 3-hour rule: can't cancel with bookings < 3h before departure
        long bookingsCount =
                bookingService.findByTripId(trip.getId()).size();
        if (bookingsCount > 0 &&
                LocalDateTime.now().isAfter(
                        trip.getDepartureTime().minusHours(3))) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot cancel a trip with bookings less than " +
                            "3 hours before departure. " +
                            "Please contact admin for assistance.");
            return "redirect:/driver/dashboard";
        }

        try {
            tripService.cancelTrip(
                    tripId, cancelReason, cancelReasonDetails, currentUser
            );

            log.info("Driver {} cancelled trip {} reason: {}",
                    currentUser.getEmail(), tripId, cancelReason);

            redirectAttributes.addFlashAttribute("success",
                    "Trip cancelled successfully. " +
                            "Passengers have been notified.");

        } catch (Exception e) {
            log.error("Error cancelling trip {} for driver {}",
                    tripId, currentUser.getEmail(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to cancel trip. Please try again or " +
                            "contact support.");
        }
        return "redirect:/driver/dashboard";
    }

    // =========================================================
    // MARK TRIP AS FULL
    // =========================================================
    @PostMapping("/trips/{tripId}/full")
    public String markTripFull(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Trip> tripOpt = tripService.findById(tripId);

        if (tripOpt.isEmpty() ||
                !tripOpt.get().getDriver().getId()
                        .equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "Trip not found or access denied.");
            return "redirect:/driver/dashboard";
        }

        try {
            tripService.markAsFull(tripId);
            log.info("Driver {} marked trip {} as full",
                    currentUser.getEmail(), tripId);
            redirectAttributes.addFlashAttribute("success",
                    "Trip marked as full. " +
                            "No new bookings will be accepted.");
        } catch (Exception e) {
            log.error("Error marking trip {} as full", tripId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to update trip status. Please try again.");
        }
        return "redirect:/driver/dashboard";
    }

    // =========================================================
    // VIEW PASSENGERS
    // =========================================================
    @GetMapping("/trips/{tripId}/passengers")
    public String viewPassengers(
            @PathVariable Long tripId,
            Principal principal,
            Model model
    ) {
        User currentUser = userService.getCurrentUser(principal);
        Optional<Trip> tripOpt = tripService.findById(tripId);

        if (tripOpt.isEmpty() ||
                !tripOpt.get().getDriver().getId()
                        .equals(currentUser.getId())) {
            log.warn("Driver {} attempted to view passengers " +
                            "of trip {} they don't own",
                    currentUser.getEmail(), tripId);
            return "redirect:/driver/dashboard";
        }

        Trip trip = tripOpt.get();
        List<Booking> bookings =
                bookingService.findByTripId(tripId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trip", trip);
        model.addAttribute("bookings", bookings);
        model.addAttribute("pageTitle",
                "Passengers — Trip to " + trip.getToLocation() +
                        " | GhanaRide");

        return "driver/trip-passengers";
    }
}