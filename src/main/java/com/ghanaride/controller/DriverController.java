package com.ghanaride.controller;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.repository.*;
import com.ghanaride.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * Driver Controller - Handles driver-specific operations.
 */
@Slf4j
@Controller
@RequestMapping("/driver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {

    private final TripService tripService;
    private final CarService carService;
    private final UserService userService;
    private final SeatService seatService;
    private final WalletService walletService;
    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    // =========================================================
    // DRIVER DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);

        // Stats
        long totalTrips = tripRepository.countByDriver(userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow());

        long approvedTrips = tripRepository.countByDriverAndStatus(
            userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow(), TripStatus.APPROVED
        );

        long totalPassengers = bookingRepository.countByDriver(
            userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow()
        );

        BigDecimal totalEarnings = tripService.calculateDriverEarnings(
            userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow()
        );

        Wallet wallet = walletService.getOrCreateWallet(
            userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow()
        );

        model.addAttribute("totalTrips", totalTrips);
        model.addAttribute("activeTrips", approvedTrips);
        model.addAttribute("totalPassengers", totalPassengers);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("wallet", wallet);
        model.addAttribute("pageTitle", "Driver Dashboard — GhanaRide");

        return "driver/dashboard";
    }

    // =========================================================
    // TRIP MANAGEMENT
    // =========================================================

    @GetMapping("/trips")
    public String myTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Trip> tripsPage;
        if (status != null && !status.isBlank()) {
            try {
                TripStatus tripStatus = TripStatus.valueOf(status.toUpperCase());
                tripsPage = tripRepository.findByDriverAndStatus(
                    userRepository.findByEmail(
                        SecurityContextHolder.getContext().getAuthentication().getName()
                    ).orElseThrow(), 
                    TripStatus.valueOf(status.toUpperCase()), 
                    PageRequest.of(page, 10)
                );
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                tripsPage = tripRepository.findByDriver(driver, PageRequest.of(page, 10));
            }
        } else {
            tripsPage = tripRepository.findByDriver(driver, PageRequest.of(page, 10));
        }

        model.addAttribute("tripsPage", tripsPage);
        model.addAttribute("trips", tripsPage.getContent());
        model.addAttribute("pageTitle", "My Trips — Driver Dashboard");

        return "driver/trips";
    }

    @GetMapping("/add-trip")
    public String showAddTripForm(Model model) {
        model.addAttribute("pageTitle", "Add New Trip — Driver Dashboard");
        model.addAttribute("tripForm", new TripFormDTO());
        return "driver/add-trip";
    }

    @PostMapping("/add-trip")
    public String addTrip(
            @Valid @ModelAttribute("tripForm") TripFormDTO tripForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "driver/add-trip";
        }

        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        // Check if driver already has an active trip
        if (tripRepository.driverHasActiveTrip(
            userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow()
        )) {
            bindingResult.rejectValue("fromLocation", "error.fromLocation", 
                "You already have an active trip. Delete or complete it first.");
            return "driver/add-trip";
        }

        try {
            Trip trip = new Trip();
            trip.setDriver(userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
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

            if (tripForm.getImageFile() != null && !tripForm.getImageFile().isEmpty()) {
                String path = fileStorageService.storeCarImage(tripForm.getImageFile());
                trip.setImagePath(path);
            }

            tripRepository.save(trip);

            redirectAttributes.addFlashAttribute("success", 
                "Trip added! Status: PENDING (awaiting admin approval).");
            return "redirect:/driver/trips";

        } catch (Exception e) {
            log.error("Failed to add trip", e);
            redirectAttributes.addFlashAttribute("error", "Failed to add trip.");
            return "redirect:/driver/add-trip";
        }
    }

    @PostMapping("/trip/{tripId}/delete")
    public String deleteTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.deleteTrip(tripId);
            redirectAttributes.addFlashAttribute("success", "Trip deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete: trip has active bookings.");
        }
        return "redirect:/driver/trips";
    }

    @PostMapping("/trip/{tripId}/cancel")
    public String cancelTrip(
            @PathVariable Long tripId,
            @RequestParam String reason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.cancelTrip(tripId, reason);
            redirectAttributes.addFlashAttribute("success", "Trip cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel trip: " + e.getMessage());
        }
        return "redirect:/driver/trips";
    }

    @PostMapping("/trip/{tripId}/complete")
    public String completeTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.markTripCompleted(tripId);
            redirectAttributes.addFlashAttribute("success", "Trip marked as completed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to complete trip: " + e.getMessage());
        }
        return "redirect:/driver/trips";
    }

    // =========================================================
    // TRIP PASSENGERS
    // =========================================================

    @GetMapping("/trip-passengers/{tripId}")
    public String tripPassengers(
            @PathVariable Long tripId,
            Principal principal,
            Model model
    ) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // Verify ownership
        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Not your trip");
        }

        List<Booking> passengers = bookingRepository.findByTripAndStatusIn(trip, 
            List.of(BookingStatus.ACTIVE, BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));

        model.addAttribute("trip", trip);
        model.addAttribute("passengers", passengers);
        model.addAttribute("pageTitle", "Passengers — " + trip.getFromLocation() + " → " + trip.getToLocation());

        return "driver/trip-passengers";
    }

    // =========================================================
    // EARNINGS
    // =========================================================

    @GetMapping("/earnings")
    public String earnings(
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "bookingDate"));
        Page<Booking> earningsPage = bookingRepository.findByDriverOrderByBookingDateDesc(driver, PageRequest.of(page, 10));

        BigDecimal totalEarnings = bookingRepository.getDriverTotalEarnings(driver);
        BigDecimal pendingEarnings = bookingRepository.getDriverPendingEarnings(driver);
        BigDecimal thisMonthEarnings = bookingRepository.getDriverEarningsThisMonth(driver);

        model.addAttribute("earningsPage", earningsPage);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("pendingEarnings", pendingEarnings);
        model.addAttribute("thisMonthEarnings", thisMonthEarnings);
        model.addAttribute("pageTitle", "Earnings — Driver Dashboard");

        return "driver/earnings";
    }

    // =========================================================
    // WALLET
    // =========================================================

    @GetMapping("/wallet")
    public String wallet(Principal principal, Model model) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Wallet wallet = walletService.getOrCreateWallet(driver);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WalletTransaction> transactions = walletTransactionRepository
            .findByUserOrderByCreatedAtDesc(driver, PageRequest.of(0, 20));

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("pageTitle", "Driver Wallet — GhanaRide");

        return "driver/wallet";
    }

    // =========================================================
    // PROFILE
    // =========================================================

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        model.addAttribute("currentUser", driver);
        model.addAttribute("pageTitle", "Driver Profile");
        return "driver/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") ProfileUpdateDTO profileForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile profileImage,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "driver/profile";
        }

        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        if (profileForm.getFullName() != null) driver.setFullName(profileForm.getFullName());
        if (profileForm.getPhoneNumber() != null) driver.setPhoneNumber(profileForm.getPhoneNumber());
        if (profileForm.getDateOfBirth() != null) driver.setDateOfBirth(profileForm.getDateOfBirth());
        if (profileForm.getGender() != null) driver.setGender(profileForm.getGender());
        if (profileForm.getAddress() != null) driver.setAddress(profileForm.getAddress());

        if (profileImage != null && !profileImage.isEmpty()) {
            String path = fileStorageService.storeProfileImage(profileImage);
            driver.setProfileImagePath(path);
        }

        userRepository.save(driver);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/driver/profile";
    }

    // =========================================================
    // VEHICLE
    // =========================================================

    @GetMapping("/vehicle")
    public String vehicle(Principal principal, Model model) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Car car = carRepository.findByDriver(driver).orElse(null);

        model.addAttribute("car", car);
        model.addAttribute("pageTitle", "My Vehicle — Driver Dashboard");
        return "driver/vehicle";
    }

    @PostMapping("/vehicle")
    public String updateVehicle(
            @Valid @ModelAttribute("carForm") CarFormDTO carForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile imageFile,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "driver/vehicle";
        }

        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Car car = carRepository.findByDriver(driver).orElse(new Car());
        car.setDriver(driver);
        car.setPlateNumber(carForm.getPlateNumber());
        car.setCarBrand(carForm.getCarBrand());
        car.setModel(carForm.getModel());
        car.setYear(carForm.getYear());
        car.setColor(carForm.getColor());
        car.setTotalSeats(carForm.getTotalSeats());
        car.setFuelType(carForm.getFuelType());

        if (imageFile != null && !imageFile.isEmpty()) {
            String path = fileStorageService.storeCarImage(imageFile);
            car.setImagePath(path);
        }

        carRepository.save(car);
        redirectAttributes.addFlashAttribute("success", "Vehicle details saved!");
        return "redirect:/driver/vehicle";
    }

    // =========================================================
    // DOCUMENTS
    // =========================================================

    @GetMapping("/documents")
    public String documents(Principal principal, Model model) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        model.addAttribute("driver", driver);
        model.addAttribute("pageTitle", "Documents — Driver Dashboard");
        return "driver/documents";
    }

    @PostMapping("/documents/upload")
    public String uploadDocument(
            @RequestParam MultipartFile file,
            @RequestParam String type, // LICENSE or ID
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        User driver = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        try {
            String path = fileStorageService.storeDriverDocument(file, type);
            if ("LICENSE".equals(type)) {
                driver.setLicenseDocumentPath(path);
            } else {
                driver.setIdDocumentPath(path);
            }
            userRepository.save(driver);
            redirectAttributes.addFlashAttribute("success", "Document uploaded successfully!");
        } catch (Exception e) {
            log.error("Document upload failed", e);
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/driver/documents";
    }
}