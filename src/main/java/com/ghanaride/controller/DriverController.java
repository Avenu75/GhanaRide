package com.ghanaride.controller;

import com.ghanaride.entity.*;
import com.ghanaride.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/driver")
@RequiredArgsConstructor
public class DriverController {

    private final UserService userService;
    private final CarService carService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final FileStorageService fileStorageService;

    private final List<String> locations = Arrays.asList(
            "Accra", "Cape Coast", "Kumasi", "Takoradi", "Tamale",
            "Sunyani", "Ho", "Koforidua", "Tema", "Winneba"
    );

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        List<Trip> trips = tripService.findByDriver(currentUser);
        List<Car> cars = carService.findByDriver(currentUser);

        Map<Long, List<Booking>> bookingsMap = new HashMap<>();
        for (Trip trip : trips) {
            bookingsMap.put(trip.getId(), bookingService.findByTripId(trip.getId()));
        }

        boolean hasActiveTrip = tripService.driverHasActiveTrip(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trips", trips);
        model.addAttribute("cars", cars);
        model.addAttribute("bookingsMap", bookingsMap);
        model.addAttribute("hasActiveTrip", hasActiveTrip);

        return "driver/dashboard";
    }

    @GetMapping("/add-trip")
    public String showAddTripForm(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);

        // Block driver from adding trip if they already have an active one
        if (tripService.driverHasActiveTrip(currentUser)) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("error",
                    "You already have an active trip. Please delete or complete your current trip before adding a new one.");
            model.addAttribute("locations", locations);
            return "driver/add-trip";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("locations", locations);
        return "driver/add-trip";
    }

    @PostMapping("/add-trip")
    public String addTrip(@RequestParam String carBrand,
                          @RequestParam String numberPlate,
                          @RequestParam(required = false) MultipartFile carImage,
                          @RequestParam String fromLocation,
                          @RequestParam String toLocation,
                          @RequestParam(required = false) String pickupStation,
                          @RequestParam String departureTime,
                          @RequestParam BigDecimal tripAmount,
                          @RequestParam Integer totalSeats,
                          @RequestParam(required = false) String description,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {

        User currentUser = userService.getCurrentUser(principal);

        // Block if driver already has active trip
        if (tripService.driverHasActiveTrip(currentUser)) {
            redirectAttributes.addFlashAttribute("error",
                    "You already have an active trip. Delete your current trip before adding a new one.");
            return "redirect:/driver/dashboard";
        }

        Car car;
        if (carService.existsByNumberPlate(numberPlate)) {
            car = carService.findByDriver(currentUser).stream()
                    .filter(c -> c.getNumberPlate().equals(numberPlate))
                    .findFirst()
                    .orElse(null);
            if (car == null) {
                redirectAttributes.addFlashAttribute("error",
                        "Number plate belongs to another driver.");
                return "redirect:/driver/add-trip";
            }
        } else {
            car = new Car();
            car.setDriver(currentUser);
            car.setCarBrand(carBrand);
            car.setNumberPlate(numberPlate);
            car.setStatus(CarStatus.APPROVED);

            if (carImage != null && !carImage.isEmpty()) {
                String imagePath = fileStorageService.storeFile(carImage);
                car.setCarImagePath("/" + imagePath);
            }
            car = carService.saveCar(car);
        }

        Trip trip = new Trip();
        trip.setCar(car);
        trip.setDriver(currentUser);
        trip.setFromLocation(fromLocation);
        trip.setToLocation(toLocation);
        trip.setPickupStation(pickupStation);
        trip.setDepartureTime(LocalDateTime.parse(departureTime));
        trip.setTripAmount(tripAmount);
        trip.setTotalSeats(totalSeats);
        trip.setAvailableSeats(totalSeats);
        trip.setDescription(description);
        trip.setStatus(TripStatus.PENDING);

        tripService.saveTrip(trip);

        redirectAttributes.addFlashAttribute("success",
                "Trip added successfully and is pending admin approval.");
        return "redirect:/driver/dashboard";
    }

    // ===== CANCEL TRIP =====
    @PostMapping("/trips/{tripId}/cancel")
    public String cancelTrip(@PathVariable Long tripId,
                             @RequestParam String cancelReason,
                             @RequestParam(required = false) String cancelReasonDetails,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Optional<Trip> tripOpt = tripService.findById(tripId);

            if (tripOpt.isEmpty() || !tripOpt.get().getDriver().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Trip not found or access denied.");
                return "redirect:/driver/dashboard";
            }
            
            Trip trip = tripOpt.get();
            long bookingsCount = bookingService.findByTripId(trip.getId()).size();
            
            // Check 3-hour rule
            if (bookingsCount > 0 && LocalDateTime.now().isAfter(trip.getDepartureTime().minusHours(3))) {
                redirectAttributes.addFlashAttribute("error", "Cannot cancel trip with bookings less than 3 hours before departure. Please contact admin.");
                return "redirect:/driver/dashboard";
            }

            tripService.cancelTrip(tripId, cancelReason, cancelReasonDetails, currentUser);
            redirectAttributes.addFlashAttribute("success", "Trip cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot cancel trip: " + e.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    // ===== MARK TRIP AS FULL =====
    @PostMapping("/trips/{tripId}/full")
    public String markTripFull(@PathVariable Long tripId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Optional<Trip> tripOpt = tripService.findById(tripId);

            if (tripOpt.isEmpty() || !tripOpt.get().getDriver().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Trip not found or access denied.");
                return "redirect:/driver/dashboard";
            }

            tripService.markAsFull(tripId);
            redirectAttributes.addFlashAttribute("success",
                    "Trip marked as full. It will show as unavailable to users.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/driver/dashboard";
    }
}