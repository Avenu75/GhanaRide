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

    private final List<String> locations = Arrays.asList("Accra", "Cape Coast", "Kumasi", "Takoradi", "Tamale", "Sunyani", "Ho", "Koforidua", "Tema", "Winneba");

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        List<Trip> trips = tripService.findByDriver(currentUser);
        List<Car> cars = carService.findByDriver(currentUser);
        
        Map<Long, List<Booking>> bookingsMap = new HashMap<>();
        for (Trip trip : trips) {
            bookingsMap.put(trip.getId(), bookingService.findByTripId(trip.getId()));
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("trips", trips);
        model.addAttribute("cars", cars);
        model.addAttribute("bookingsMap", bookingsMap);

        return "driver/dashboard";
    }

    @GetMapping("/add-trip")
    public String showAddTripForm(Principal principal, Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser(principal));
        model.addAttribute("locations", locations);
        return "driver/add-trip";
    }

    @PostMapping("/add-trip")
    public String addTrip(@RequestParam String carBrand,
                          @RequestParam String numberPlate,
                          @RequestParam(required = false) MultipartFile carImage,
                          @RequestParam String fromLocation,
                          @RequestParam String toLocation,
                          @RequestParam String departureTime,
                          @RequestParam BigDecimal tripAmount,
                          @RequestParam Integer totalSeats,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {

        User currentUser = userService.getCurrentUser(principal);

        Car car;
        if (carService.existsByNumberPlate(numberPlate)) {
            car = carService.findByDriver(currentUser).stream()
                    .filter(c -> c.getNumberPlate().equals(numberPlate))
                    .findFirst()
                    .orElse(null); // Assuming number plates are unique per driver for simplicity here
            if (car == null) {
                // Another driver has this plate, handle error appropriately in real app
                redirectAttributes.addFlashAttribute("error", "Number plate belongs to another driver.");
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
        trip.setDepartureTime(LocalDateTime.parse(departureTime));
        trip.setTripAmount(tripAmount);
        trip.setTotalSeats(totalSeats);
        trip.setAvailableSeats(totalSeats);
        trip.setStatus(TripStatus.PENDING);

        tripService.saveTrip(trip);

        redirectAttributes.addFlashAttribute("success", "Trip added successfully and is pending admin approval.");
        return "redirect:/driver/dashboard";
    }
}
