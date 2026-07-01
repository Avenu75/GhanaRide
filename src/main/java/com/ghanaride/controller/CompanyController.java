package com.ghanaride.controller;

import com.ghanaride.entity.*;
import com.ghanaride.repository.CompanyRepository;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.CarService;
import com.ghanaride.service.TripService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final UserService userService;
    private final CompanyRepository companyRepository;
    private final CarService carService;
    private final TripService tripService;
    private final BookingService bookingService;

    private Company getCompanyForCurrentUser(Principal principal) {
        User currentUser = userService.getCurrentUser(principal);
        return companyRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Company profile not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Company company = getCompanyForCurrentUser(principal);
        
        List<Car> vehicles = carService.findByCompany(company);
        List<Trip> trips = tripService.findByCompany(company);
        
        long activeTrips = trips.stream().filter(t -> t.getStatus() == TripStatus.APPROVED).count();
        long completedTrips = trips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED).count();
        
        // Simple earnings calculation: sum of (tripAmount * bookedSeats) for completed trips
        // (This would be more complex in reality, pulling from actual payments)
        double earnings = 0.0;
        for (Trip trip : trips) {
            if (trip.getStatus() == TripStatus.COMPLETED) {
                // Calculate based on booked seats (totalSeats - availableSeats)
                int bookedSeats = trip.getTotalSeats() - trip.getAvailableSeats();
                earnings += trip.getTripAmount().doubleValue() * bookedSeats;
            }
        }
        
        model.addAttribute("company", company);
        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("activeTrips", activeTrips);
        model.addAttribute("completedTrips", completedTrips);
        model.addAttribute("earnings", earnings);
        model.addAttribute("recentTrips", trips);
        
        return "company/dashboard";
    }

    // ===== VEHICLE MANAGEMENT =====
    @GetMapping("/vehicles")
    public String viewVehicles(Principal principal, Model model) {
        Company company = getCompanyForCurrentUser(principal);
        model.addAttribute("company", company);
        model.addAttribute("vehicles", carService.findByCompany(company));
        model.addAttribute("newCar", new Car());
        return "company/vehicles";
    }

    @PostMapping("/vehicles/add")
    public String addVehicle(@ModelAttribute Car car, Principal principal, RedirectAttributes redirectAttributes) {
        Company company = getCompanyForCurrentUser(principal);
        
        if (carService.existsByNumberPlate(car.getNumberPlate())) {
            redirectAttributes.addFlashAttribute("error", "Vehicle with this number plate already exists.");
            return "redirect:/company/vehicles";
        }
        
        car.setCompany(company);
        car.setStatus(CarStatus.APPROVED);
        carService.saveCar(car);
        
        redirectAttributes.addFlashAttribute("success", "Vehicle added successfully.");
        return "redirect:/company/vehicles";
    }

    @PostMapping("/vehicles/remove/{id}")
    public String removeVehicle(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Company company = getCompanyForCurrentUser(principal);
        
        Car car = carService.findById(id).orElse(null);
        if (car == null || !car.getCompany().getId().equals(company.getId())) {
            redirectAttributes.addFlashAttribute("error", "Vehicle not found.");
            return "redirect:/company/vehicles";
        }
        
        carService.deleteCar(id);
        redirectAttributes.addFlashAttribute("success", "Vehicle removed successfully.");
        return "redirect:/company/vehicles";
    }

    // ===== TRIP MANAGEMENT =====
    @GetMapping("/trips/add")
    public String showAddTripForm(Principal principal, Model model) {
        Company company = getCompanyForCurrentUser(principal);
        model.addAttribute("company", company);
        model.addAttribute("vehicles", carService.findByCompany(company));
        model.addAttribute("trip", new Trip());
        return "company/add-trip";
    }

    @PostMapping("/trips/add")
    public String addTrip(@ModelAttribute Trip trip, @RequestParam Long vehicleId, Principal principal, RedirectAttributes redirectAttributes) {
        Company company = getCompanyForCurrentUser(principal);
        
        Car vehicle = carService.findById(vehicleId).orElse(null);
        if (vehicle == null || !vehicle.getCompany().getId().equals(company.getId())) {
            redirectAttributes.addFlashAttribute("error", "Invalid vehicle selected.");
            return "redirect:/company/trips/add";
        }
        
        trip.setCompany(company);
        trip.setCar(vehicle);
        trip.setTotalSeats(trip.getAvailableSeats());
        trip.setStatus(TripStatus.PENDING); // Admin must approve
        
        tripService.saveTrip(trip);
        redirectAttributes.addFlashAttribute("success", "Trip created successfully and is pending approval.");
        return "redirect:/company/dashboard";
    }

    // ===== VIEW PASSENGERS WHO BOOKED A COMPANY TRIP =====
    @GetMapping("/trips/{tripId}/passengers")
    public String viewPassengers(@PathVariable Long tripId,
                                 Principal principal,
                                 Model model) {
        Company company = getCompanyForCurrentUser(principal);

        java.util.Optional<Trip> tripOpt = tripService.findById(tripId);
        if (tripOpt.isEmpty() ||
                (tripOpt.get().getCompany() == null ||
                 !tripOpt.get().getCompany().getId().equals(company.getId()))) {
            return "redirect:/company/dashboard?error=not_found";
        }

        Trip trip = tripOpt.get();
        java.util.List<com.ghanaride.entity.Booking> bookings = bookingService.findByTripId(tripId);

        model.addAttribute("company", company);
        model.addAttribute("trip", trip);
        model.addAttribute("bookings", bookings);
        return "company/trip-passengers";
    }
}
