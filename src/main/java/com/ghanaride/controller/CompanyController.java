package com.ghanaride.controller;

import com.ghanaride.entity.*;
import com.ghanaride.repository.CompanyRepository;
import com.ghanaride.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Company/Fleet portal controller.
 * Handles vehicle management, trip creation,
 * and company dashboard.
 */
@Slf4j
@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
public class CompanyController {

    private final UserService userService;
    private final CompanyRepository companyRepository;
    private final CarService carService;
    private final TripService tripService;
    private final BookingService bookingService;

    // =========================================================
    // HELPER — Get Company for Current User
    // =========================================================
    private Company getCompanyForCurrentUser(Principal principal) {
        User currentUser = userService.getCurrentUser(principal);
        return companyRepository.findByUser(currentUser)
                .orElseThrow(() -> {
                    log.error("Company profile not found for user: {}",
                            currentUser.getEmail());
                    return new RuntimeException(
                            "Company profile not found. " +
                                    "Please contact support."
                    );
                });
    }

    // =========================================================
    // DASHBOARD
    // =========================================================
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        try {
            Company company =
                    getCompanyForCurrentUser(principal);
            List<Car> vehicles =
                    carService.findByCompany(company);
            List<Trip> trips =
                    tripService.findByCompany(company);

            long activeTrips = trips.stream()
                    .filter(t -> t.getStatus() == TripStatus.APPROVED)
                    .count();
            long pendingTrips = trips.stream()
                    .filter(t -> t.getStatus() == TripStatus.PENDING)
                    .count();
            long completedTrips = trips.stream()
                    .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                    .count();

            // Move earnings calculation to service layer
            // (here we just call the service)
            double earnings =
                    tripService.calculateCompanyEarnings(company);

            model.addAttribute("company", company);
            model.addAttribute("totalVehicles", vehicles.size());
            model.addAttribute("activeTrips", activeTrips);
            model.addAttribute("pendingTrips", pendingTrips);
            model.addAttribute("completedTrips", completedTrips);
            model.addAttribute("earnings", earnings);
            model.addAttribute("recentTrips",
                    trips.stream().limit(10).toList());
            model.addAttribute("pageTitle",
                    company.getCompanyName() +
                            " — Company Dashboard | GhanaRide");

            return "company/dashboard";

        } catch (RuntimeException e) {
            log.error("Error loading company dashboard for {}",
                    principal.getName(), e);
            model.addAttribute("error",
                    "Unable to load company dashboard. " +
                            "Please contact support.");
            return "error/500";
        }
    }

    // =========================================================
    // VEHICLE MANAGEMENT
    // =========================================================
    @GetMapping("/vehicles")
    public String viewVehicles(
            Principal principal,
            Model model
    ) {
        Company company = getCompanyForCurrentUser(principal);
        model.addAttribute("company", company);
        model.addAttribute("vehicles",
                carService.findByCompany(company));
        model.addAttribute("newCar", new Car());
        model.addAttribute("pageTitle",
                "Manage Vehicles — " +
                        company.getCompanyName() + " | GhanaRide");
        return "company/vehicles";
    }

    @PostMapping("/vehicles/add")
    public String addVehicle(
            @ModelAttribute Car car,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Company company =
                    getCompanyForCurrentUser(principal);

            // Check duplicate number plate
            if (carService.existsByNumberPlate(
                    car.getNumberPlate())) {
                redirectAttributes.addFlashAttribute("error",
                        "A vehicle with number plate '" +
                                car.getNumberPlate() +
                                "' is already registered.");
                return "redirect:/company/vehicles";
            }

            car.setCompany(company);
            car.setStatus(CarStatus.APPROVED);
            car.setNumberPlate(
                    car.getNumberPlate().trim().toUpperCase()
            );
            carService.saveCar(car);

            log.info("Company {} added vehicle: {}",
                    company.getCompanyName(),
                    car.getNumberPlate());

            redirectAttributes.addFlashAttribute("success",
                    "Vehicle '" + car.getNumberPlate() +
                            "' added successfully.");

        } catch (Exception e) {
            log.error("Error adding vehicle for company: {}",
                    principal.getName(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to add vehicle. Please try again.");
        }
        return "redirect:/company/vehicles";
    }

    @PostMapping("/vehicles/remove/{id}")
    public String removeVehicle(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Company company =
                    getCompanyForCurrentUser(principal);

            Car car = carService.findById(id).orElse(null);
            if (car == null ||
                    car.getCompany() == null ||
                    !car.getCompany().getId()
                            .equals(company.getId())) {
                redirectAttributes.addFlashAttribute("error",
                        "Vehicle not found or access denied.");
                return "redirect:/company/vehicles";
            }

            carService.deleteCar(id);

            log.info("Company {} removed vehicle: {}",
                    company.getCompanyName(),
                    car.getNumberPlate());

            redirectAttributes.addFlashAttribute("success",
                    "Vehicle removed successfully.");

        } catch (Exception e) {
            log.error("Error removing vehicle {} for company: {}",
                    id, principal.getName(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to remove vehicle. " +
                            "It may have active trips.");
        }
        return "redirect:/company/vehicles";
    }

    // =========================================================
    // TRIP MANAGEMENT
    // =========================================================
    @GetMapping("/trips/add")
    public String showAddTripForm(
            Principal principal,
            Model model
    ) {
        Company company = getCompanyForCurrentUser(principal);
        model.addAttribute("company", company);
        model.addAttribute("vehicles",
                carService.findByCompany(company));
        model.addAttribute("trip", new Trip());
        model.addAttribute("pageTitle",
                "Add Trip — " +
                        company.getCompanyName() + " | GhanaRide");
        return "company/add-trip";
    }

    @PostMapping("/trips/add")
    public String addTrip(
            @ModelAttribute Trip trip,
            @RequestParam Long vehicleId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Company company =
                    getCompanyForCurrentUser(principal);

            Car vehicle =
                    carService.findById(vehicleId).orElse(null);
            if (vehicle == null ||
                    vehicle.getCompany() == null ||
                    !vehicle.getCompany().getId()
                            .equals(company.getId())) {
                redirectAttributes.addFlashAttribute("error",
                        "Invalid vehicle selected.");
                return "redirect:/company/trips/add";
            }

            // Validate departure is in the future
            if (trip.getDepartureTime() != null &&
                    trip.getDepartureTime()
                            .isBefore(java.time.LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error",
                        "Departure time must be in the future.");
                return "redirect:/company/trips/add";
            }

            trip.setCompany(company);
            trip.setCar(vehicle);
            trip.setTotalSeats(trip.getAvailableSeats());
            trip.setStatus(TripStatus.PENDING);

            tripService.saveTrip(trip);

            log.info("Company {} created trip: {} → {} on {}",
                    company.getCompanyName(),
                    trip.getFromLocation(),
                    trip.getToLocation(),
                    trip.getDepartureTime());

            redirectAttributes.addFlashAttribute("success",
                    "Trip created successfully and is pending " +
                            "admin approval.");
            return "redirect:/company/dashboard";

        } catch (Exception e) {
            log.error("Error adding trip for company: {}",
                    principal.getName(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to create trip. Please try again.");
            return "redirect:/company/trips/add";
        }
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
        try {
            Company company =
                    getCompanyForCurrentUser(principal);

            Optional<Trip> tripOpt =
                    tripService.findById(tripId);

            if (tripOpt.isEmpty() ||
                    tripOpt.get().getCompany() == null ||
                    !tripOpt.get().getCompany().getId()
                            .equals(company.getId())) {
                log.warn("Company {} attempted to view " +
                                "passengers of trip {} they don't own",
                        company.getCompanyName(), tripId);
                return "redirect:/company/dashboard";
            }

            Trip trip = tripOpt.get();
            List<Booking> bookings =
                    bookingService.findByTripId(tripId);

            model.addAttribute("company", company);
            model.addAttribute("trip", trip);
            model.addAttribute("bookings", bookings);
            model.addAttribute("pageTitle",
                    "Passengers — Trip to " +
                            trip.getToLocation() + " | GhanaRide");

            return "company/trip-passengers";

        } catch (Exception e) {
            log.error("Error viewing passengers for trip {}",
                    tripId, e);
            return "redirect:/company/dashboard";
        }
    }
}