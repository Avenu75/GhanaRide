package com.ghanaride.controller;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.repository.*;
import com.ghanaride.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Company Controller - Handles company/fleet operator operations.
 */
@Slf4j
@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyController {

    private final TripService tripService;
    private final CarService carService;
    private final UserService userService;
    private final BookingService bookingService;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    private final WalletRepository walletRepository;
    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;

    // =========================================================
    // COMPANY DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        // Stats
        long totalTrips = tripRepository.countByCompany(company);
        long activeTrips = tripRepository.countByCompanyAndStatus(company, TripStatus.APPROVED);
        long pendingTrips = tripRepository.countByCompanyAndStatus(company, TripStatus.PENDING);
        long totalCars = carRepository.countByCompany(userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow().getCompany());

        long totalPassengers = bookingRepository.countByCompany(company);
        BigDecimal totalRevenue = bookingRepository.getCompanyTotalRevenue(company);
        BigDecimal thisMonthRevenue = bookingRepository.getCompanyRevenueThisMonth(company);

        model.addAttribute("company", company);
        model.addAttribute("totalTrips", totalTrips);
        model.addAttribute("activeTrips", activeTrips);
        model.addAttribute("pendingTrips", pendingTrips);
        model.addAttribute("totalCars", totalCars);
        model.addAttribute("totalPassengers", totalPassengers);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("thisMonthRevenue", thisMonthRevenue);
        model.addAttribute("pageTitle", "Company Dashboard — GhanaRide");

        return "company/dashboard";
    }

    // =========================================================
    // TRIP MANAGEMENT
    // =========================================================

    @GetMapping("/trips")
    public String manageTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Trip> tripsPage;
        if (status != null && !status.isBlank()) {
            tripsPage = tripRepository.findByCompanyAndStatus(
                companyRepository.findByUser(userRepository.findByEmail(
                    SecurityContextHolder.getContext().getAuthentication().getName()
                ).orElseThrow()).orElseThrow(),
                TripStatus.valueOf(status.toUpperCase()),
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } else {
            tripsPage = tripRepository.findByCompanyOrderByCreatedAtDesc(
                companyRepository.findByUser(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow()
                ).orElseThrow(),
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }

        model.addAttribute("tripsPage", tripsPage);
        model.addAttribute("trips", tripsPage.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Manage Trips — Company Portal");

        return "company/trips";
    }

    @GetMapping("/trips/add")
    public String showAddTripForm(Model model) {
        model.addAttribute("locations", TripService.LOCATIONS);
        model.addAttribute("tripForm", new TripFormDTO());
        model.addAttribute("pageTitle", "Add New Trip — Company Portal");
        return "company/add-trip";
    }

    @PostMapping("/trips/add")
    public String addTrip(
            @Valid @ModelAttribute("tripForm") TripFormDTO tripForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile imageFile,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("locations", TripService.LOCATIONS);
            return "company/add-trip";
        }

        try {
            User company = userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow();

            Trip trip = new Trip();
            trip.setCompany(companyRepository.findByUser(
                userRepository.findByEmail(
                    SecurityContextHolder.getContext().getAuthentication().getName()
                ).orElseThrow()
            ).orElseThrow());
            trip.setFromLocation(tripForm.getFromLocation());
            trip.setToLocation(tripForm.getToLocation());
            trip.setPickupStation(tripForm.getPickupStation());
            trip.setDepartureTime(tripForm.getDepartureTime());
            trip.setTripAmount(tripForm.getTripAmount());
            trip.setAvailableSeats(tripForm.getTotalSeats());
            trip.setTotalSeats(tripForm.getTotalSeats());
            trip.setDescription(tripForm.getDescription());
            trip.setStatus(TripStatus.PENDING);

            if (imageFile != null && !imageFile.isEmpty()) {
                String path = fileStorageService.storeCarImage(imageFile);
                trip.setImagePath(path);
            }

            Trip saved = tripRepository.save(trip);
            log.info("Trip added by company: {} -> {}", tripForm.getFromLocation(), tripForm.getToLocation());

            redirectAttributes.addFlashAttribute("success", 
                "Trip added! Status: PENDING (awaiting admin approval).");
            return "redirect:/company/trips";

        } catch (Exception e) {
            log.error("Failed to add trip", e);
            redirectAttributes.addFlashAttribute("error", "Failed to add trip: " + e.getMessage());
            return "redirect:/company/trips/add";
        }
    }

    @PostMapping("/trips/{tripId}/delete")
    public String deleteTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.deleteTrip(tripId);
            redirectAttributes.addFlashAttribute("success", "Trip deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete: " + e.getMessage());
        }
        return "redirect:/company/trips";
    }

    // =========================================================
    // CAR / FLEET MANAGEMENT
    // =========================================================

    @GetMapping("/cars")
    public String manageCars(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Car> carsPage;

        if (status != null && !status.isBlank()) {
            carsPage = carRepository.findByCompanyAndStatus(
                companyRepository.findByUser(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow()
                ).orElseThrow(),
                CarStatus.valueOf(status.toUpperCase()),
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } else {
            carsPage = carRepository.findByCompanyOrderByCreatedAtDesc(
                companyRepository.findByUser(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow()
                ).orElseThrow(),
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }

        model.addAttribute("carsPage", carsPage);
        model.addAttribute("cars", carsPage.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Manage Fleet — Company Portal");

        return "company/cars";
    }

    @GetMapping("/cars/add")
    public String showAddCarForm(Model model) {
        model.addAttribute("carForm", new CarFormDTO());
        model.addAttribute("pageTitle", "Add Vehicle — Company Portal");
        return "company/add-car";
    }

    @PostMapping("/cars/add")
    public String addCar(
            @Valid @ModelAttribute("carForm") CarFormDTO carForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile imageFile,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "company/add-car";
        }

        try {
            User company = userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow();

            Car car = new Car();
            car.setCompany(companyRepository.findByUser(
                userRepository.findByEmail(
                    SecurityContextHolder.getContext().getAuthentication().getName()
                ).orElseThrow()
            ).orElseThrow());
            car.setPlateNumber(carForm.getPlateNumber());
            car.setCarBrand(carForm.getCarBrand());
            car.setModel(carForm.getModel());
            car.setYear(carForm.getYear());
            car.setColor(carForm.getColor());
            car.setTotalSeats(carForm.getTotalSeats());
            car.setFuelType(carForm.getFuelType());
            car.setStatus(CarStatus.ACTIVE);

            if (imageFile != null && !imageFile.isEmpty()) {
                String path = fileStorageService.storeCarImage(imageFile);
                car.setImagePath(path);
            }

            carRepository.save(car);

            redirectAttributes.addFlashAttribute("success", "Vehicle added to fleet!");
            return "redirect:/company/cars";

        } catch (Exception e) {
            log.error("Failed to add car", e);
            redirectAttributes.addFlashAttribute("error", "Failed to add vehicle: " + e.getMessage());
            return "redirect:/company/cars/add";
        }
    }

    @PostMapping("/cars/{carId}/delete")
    public String deleteCar(
            @PathVariable Long carId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            carService.deleteCar(carId);
            redirectAttributes.addFlashAttribute("success", "Vehicle removed from fleet.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete: " + e.getMessage());
        }
        return "redirect:/company/cars";
    }

    // =========================================================
    // BOOKINGS / PASSENGERS
    // =========================================================

    @GetMapping("/bookings")
    public String manageBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "bookingDate"));
        Page<Booking> bookingsPage;

        if (status != null && !status.isBlank()) {
            bookingsPage = bookingRepository.findByCompanyAndStatus(
                companyRepository.findByUser(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow()
                ).orElseThrow(),
                BookingStatus.valueOf(status.toUpperCase()),
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "bookingDate"))
            );
        } else {
            bookingsPage = bookingRepository.findByCompanyOrderByBookingDateDesc(
                companyRepository.findByUser(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow()
                ).orElseThrow(),
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "bookingDate"))
            );
        }

        model.addAttribute("bookingsPage", bookingsPage);
        model.addAttribute("bookings", bookingsPage.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Passenger Bookings — Company Portal");

        return "company/bookings";
    }

    @GetMapping("/trip-passengers/{tripId}")
    public String tripPassengers(
            @PathVariable Long tripId,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // Verify ownership
        if (!trip.getCompany().getId().equals(
            companyRepository.findByUser(
                userRepository.findByEmail(
                    SecurityContextHolder.getContext().getAuthentication().getName()
                ).orElseThrow()
            ).orElseThrow().getId()
        )) {
            throw new AccessDeniedException("Not your trip");
        }

        List<Booking> bookings = bookingRepository.findByTripAndStatusIn(trip, 
            List.of(BookingStatus.ACTIVE, BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));

        model.addAttribute("trip", trip);
        model.addAttribute("bookings", bookings);
        model.addAttribute("pageTitle", "Passengers — " + trip.getFromLocation() + " → " + trip.getToLocation());

        return "company/trip-passengers";
    }

    // =========================================================
    // REVENUE / ANALYTICS
    // =========================================================

    @GetMapping("/analytics")
    public String analytics(
            @RequestParam(defaultValue = "30") int days,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        LocalDateTime start = LocalDateTime.now().minusDays(days);
        LocalDateTime end = LocalDateTime.now();

        BigDecimal revenue = bookingRepository.getCompanyRevenueInPeriod(company, start, end);
        long bookings = bookingRepository.countByCompanyInPeriod(company, start, end);
        long trips = tripRepository.countCompletedByCompanyInPeriod(company, start, end);

        model.addAttribute("revenue", revenue);
        model.addAttribute("bookings", bookings);
        model.addAttribute("trips", trips);
        model.addAttribute("days", days);
        model.addAttribute("pageTitle", "Analytics — Company Portal");

        return "company/analytics";
    }

    // =========================================================
    // DRIVERS MANAGEMENT
    // =========================================================

    @GetMapping("/drivers")
    public String manageDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> driversPage;

        if (search != null && !search.isBlank()) {
            driversPage = userRepository.searchDriversInCompany(company, search, PageRequest.of(page, 20));
        } else {
            driversPage = userRepository.findByCompanyAndRole(
                companyRepository.findByUser(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow()
                ).orElseThrow(),
                Role.DRIVER,
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }

        model.addAttribute("driversPage", driversPage);
        model.addAttribute("drivers", driversPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "Manage Drivers — Company Portal");

        return "company/drivers";
    }

    @PostMapping("/drivers/{driverId}/approve")
    public String approveDriver(
            @PathVariable Long driverId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.verifyDriver(driverId);
            redirectAttributes.addFlashAttribute("success", "Driver approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve driver: " + e.getMessage());
        }
        return "redirect:/company/drivers";
    }

    @PostMapping("/drivers/{driverId}/suspend")
    public String suspendDriver(
            @PathVariable Long driverId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.suspendUser(driverId);
            redirectAttributes.addFlashAttribute("success", "Driver suspended.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to suspend driver.");
        }
        return "redirect:/company/drivers";
    }

    // =========================================================
    // WALLET
    // =========================================================

    @GetMapping("/wallet")
    public String wallet(
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        User company = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Wallet wallet = walletRepository.findByUser(company)
            .orElseGet(() -> walletService.getOrCreateWallet(company));

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WalletTransaction> transactions = walletTransactionRepository
            .findByUserOrderByCreatedAtDesc(company, PageRequest.of(page, 20));

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("pageTitle", "Company Wallet — GhanaRide");

        return "company/wallet";
    }
}