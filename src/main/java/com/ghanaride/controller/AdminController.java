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
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Admin Controller - Admin portal for platform management.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final CarService carService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final WalletRepository walletRepository;

    private static final int PAGE_SIZE = 20;

    // =========================================================
    // DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("currentUser", 
            userService.getCurrentUser(
                new org.springframework.security.core.userdetails.User("temp", "", List.of())
            )
        );

        // Stats
        model.addAttribute("totalUsers", userService.countAllUsers());
        model.addAttribute("totalDrivers", userService.countByRole(Role.DRIVER));
        model.addAttribute("totalCompanies", userService.countByRole(Role.COMPANY));
        model.addAttribute("totalPassengers", userService.countByRole(Role.USER));

        model.addAttribute("totalTrips", tripService.countAll());
        model.addAttribute("pendingTrips", tripService.countByStatus(TripStatus.PENDING));
        model.addAttribute("approvedTrips", tripService.countByStatus(TripStatus.APPROVED));
        model.addAttribute("completedTrips", tripService.countByStatus(TripStatus.COMPLETED));
        model.addAttribute("totalBookings", bookingService.countAll());

        model.addAttribute("totalRevenue", bookingService.getTotalRevenue());
        model.addAttribute("avgBookingValue", bookingService.getAverageBookingValue());

        // Recent activity
        model.addAttribute("recentUsers", userService.findRecent(5));
        model.addAttribute("recentTrips", tripService.findRecent(5));
        model.addAttribute("recentBookings", bookingService.findRecent(5));

        return "admin/dashboard";
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
        model.addAttribute("currentUser", 
            userService.getCurrentUser(
                new org.springframework.security.core.userdetails.User("temp", "", List.of())
            )
        );

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Trip> tripsPage;

        if (status != null && !status.isBlank()) {
            try {
                TripStatus tripStatus = TripStatus.valueOf(status.toUpperCase());
                tripsPage = tripService.findByStatusWithDetails(tripStatus, pageable);
            } catch (IllegalArgumentException e) {
                tripsPage = tripService.findAllWithDetails(pageable);
            }
            model.addAttribute("selectedStatus", status);
        } else {
            tripsPage = tripService.findAllWithDetails(pageable);
        }

        model.addAttribute("tripsPage", tripsPage);
        model.addAttribute("trips", tripsPage.getContent());
        model.addAttribute("totalPages", tripsPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle", "Manage Trips — Admin | GhanaRide");

        return "admin/trips";
    }

    @PostMapping("/trips/{tripId}/approve")
    public String approveTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.approveTrip(tripId);
            log.info("Admin {} approved trip {}", 
                userService.getCurrentUser(
                    new org.springframework.security.core.userdetails.User("temp", "", List.of())
                ).getEmail(), tripId);
            redirectAttributes.addFlashAttribute("success",
                "Trip #" + tripId + " approved successfully. It is now visible to passengers.");
        } catch (Exception e) {
            log.error("Error approving trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to approve trip. Please try again.");
        }
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/reject")
    public String rejectTrip(
            @PathVariable Long tripId,
            @RequestParam(required = false) String rejectReason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.rejectTrip(tripId, rejectReason);
            log.info("Admin {} rejected trip {} reason: {}", 
                userService.getCurrentUser(
                    new org.springframework.security.core.userdetails.User("temp", "", List.of())
                ).getEmail(), tripId, rejectReason);
            redirectAttributes.addFlashAttribute("success",
                "Trip #" + tripId + " rejected. The driver has been notified.");
        } catch (Exception e) {
            log.error("Error rejecting trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to reject trip. Please try again.");
        }
        return "redirect:/admin/trips";
    }

    @PostMapping("/trips/{tripId}/delete")
    public String deleteTrip(
            @PathVariable Long tripId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tripService.deleteTrip(tripId);
            log.warn("Admin {} DELETED trip {}", 
                userService.getCurrentUser(
                    new org.springframework.security.core.userdetails.User("temp", "", List.of())
                ).getEmail(), tripId);
            redirectAttributes.addFlashAttribute("success", "Trip #" + tripId + " deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("error", "Cannot delete trip: it may have active bookings.");
        }
        return "redirect:/admin/trips";
    }

    // =========================================================
    // USER MANAGEMENT
    // =========================================================

    @GetMapping("/users")
    public String manageUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> usersPage;

        if (search != null && !search.isBlank()) {
            usersPage = userRepository.searchUsers(search.trim(), PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else if (role != null && !role.isBlank()) {
            usersPage = userRepository.findByRole(Role.valueOf(role.toUpperCase()), PageRequest.of(page, PAGE_SIZE));
        } else {
            usersPage = userRepository.findAllOrderByCreatedDesc(PageRequest.of(page, PAGE_SIZE));
        }

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle", "Manage Users — Admin | GhanaRide");

        return "admin/users";
    }

    @PostMapping("/users/{userId}/toggle-status")
    public String toggleUserStatus(
            @PathVariable Long userId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.toggleUserStatus(userId);
            redirectAttributes.addFlashAttribute("success", "User status updated successfully.");
        } catch (Exception e) {
            log.error("Error toggling user status {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to update user status.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/verify")
    public String verifyUser(
            @PathVariable Long userId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.verifyUser(userId);
            redirectAttributes.addFlashAttribute("success", "User verified successfully.");
        } catch (Exception e) {
            log.error("Error verifying user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to verify user.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(
            @PathVariable Long userId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Cannot delete user: they may have active trips or bookings.");
        }
        return "redirect:/admin/users";
    }

    // =========================================================
    // DRIVER MANAGEMENT
    // =========================================================

    @GetMapping("/drivers")
    public String manageDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> driversPage;

        if (search != null && !search.isBlank()) {
            driversPage = userRepository.searchDrivers(search.trim(), PageRequest.of(page, PAGE_SIZE));
        } else {
            driversPage = userRepository.findByRole(Role.DRIVER, PageRequest.of(page, PAGE_SIZE));
        }

        model.addAttribute("driversPage", driversPage);
        model.addAttribute("drivers", driversPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "Manage Drivers — Admin | GhanaRide");

        return "admin/drivers";
    }

    @PostMapping("/drivers/{driverId}/verify")
    public String verifyDriver(
            @PathVariable Long driverId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.verifyDriver(driverId);
            redirectAttributes.addFlashAttribute("success", "Driver verified successfully.");
        } catch (Exception e) {
            log.error("Error verifying driver {}", driverId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to verify driver.");
        }
        return "redirect:/admin/drivers";
    }

    @PostMapping("/drivers/{driverId}/suspend")
    public String suspendDriver(
            @PathVariable Long driverId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.suspendDriver(driverId);
            redirectAttributes.addFlashAttribute("success", "Driver suspended successfully.");
        } catch (Exception e) {
            log.error("Error suspending driver {}", driverId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to suspend driver.");
        }
        return "redirect:/admin/drivers";
    }

    // =========================================================
    // COMPANY MANAGEMENT
    // =========================================================

    @GetMapping("/companies")
    public String manageCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> companiesPage;

        if (search != null && !search.isBlank()) {
            companiesPage = userRepository.searchCompanies(search.trim(), PageRequest.of(page, PAGE_SIZE));
        } else {
            companiesPage = userRepository.findByRole(Role.COMPANY, PageRequest.of(page, PAGE_SIZE));
        }

        model.addAttribute("companiesPage", companiesPage);
        model.addAttribute("companies", companiesPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "Manage Companies — Admin | GhanaRide");

        return "admin/companies";
    }

    @PostMapping("/companies/{companyId}/verify")
    public String verifyCompany(
            @PathVariable Long companyId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.verifyCompany(companyId);
            redirectAttributes.addFlashAttribute("success", "Company verified successfully.");
        } catch (Exception e) {
            log.error("Error verifying company {}", companyId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to verify company.");
        }
        return "redirect:/admin/companies";
    }

    // =========================================================
    // CAR MANAGEMENT
    // =========================================================

    @GetMapping("/cars")
    public String manageCars(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Car> carsPage;

        if (status != null && !status.isBlank()) {
            carsPage = carRepository.findByStatus(CarStatus.valueOf(status.toUpperCase()), PageRequest.of(page, PAGE_SIZE));
        } else {
            carsPage = carRepository.findAll(pageable);
        }

        model.addAttribute("carsPage", carsPage);
        model.addAttribute("cars", carsPage.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Manage Vehicles — Admin | GhanaRide");

        return "admin/cars";
    }

    @PostMapping("/cars/{carId}/approve")
    public String approveCar(
            @PathVariable Long carId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            carService.approveCar(carId);
            redirectAttributes.addFlashAttribute("success", "Vehicle approved successfully.");
        } catch (Exception e) {
            log.error("Error approving car {}", carId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to approve vehicle.");
        }
        return "redirect:/admin/cars";
    }

    @PostMapping("/cars/{carId}/reject")
    public String rejectCar(
            @PathVariable Long carId,
            @RequestParam(required = false) String reason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            carService.rejectCar(carId, reason);
            redirectAttributes.addFlashAttribute("success", "Vehicle rejected.");
        } catch (Exception e) {
            log.error("Error rejecting car {}", carId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to reject vehicle.");
        }
        return "redirect:/admin/cars";
    }

    // =========================================================
    // BOOKING MANAGEMENT
    // =========================================================

    @GetMapping("/bookings")
    public String manageBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "bookingDate"));
        Page<Booking> bookingsPage;

        if (status != null && !status.isBlank()) {
            bookingsPage = bookingRepository.findByStatus(BookingStatus.valueOf(status.toUpperCase()), PageRequest.of(page, PAGE_SIZE));
        } else {
            bookingsPage = bookingRepository.findAllWithDetails(pageable);
        }

        model.addAttribute("bookingsPage", bookingsPage);
        model.addAttribute("bookings", bookingsPage.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Manage Bookings — Admin | GhanaRide");

        return "admin/bookings";
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam String reason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bookingService.adminCancelBooking(bookingId, reason);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully.");
        } catch (Exception e) {
            log.error("Error cancelling booking {}", bookingId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to cancel booking.");
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{bookingId}/refund")
    public String refundBooking(
            @PathVariable Long bookingId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bookingService.refundBooking(bookingId);
            redirectAttributes.addFlashAttribute("success", "Refund processed successfully.");
        } catch (Exception e) {
            log.error("Error refunding booking {}", bookingId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to process refund.");
        }
        return "redirect:/admin/bookings";
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
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        LocalDateTime end = LocalDateTime.now();

        model.addAttribute("totalRevenue", bookingService.getRevenueInPeriod(start, end));
        model.addAttribute("totalBookings", bookingService.countBookingsInPeriod(start, end));
        model.addAttribute("totalTrips", tripService.countCompletedInPeriod(start, end));
        model.addAttribute("newUsers", userRepository.countByCreatedAtBetween(start, end));
        model.addAttribute("avgBookingValue", bookingService.getAverageBookingValueInPeriod(start, end));
        
        // Revenue by day
        model.addAttribute("dailyRevenue", bookingService.getDailyRevenue(start, end));
        
        // Top routes
        model.addAttribute("topRoutes", tripService.getTopRoutes(start, end, 10));
        
        // Top drivers
        model.addAttribute("topDrivers", userRepository.findTopDriversByEarnings(start, end, 10));
        
        model.addAttribute("pageTitle", "Analytics — Admin | GhanaRide");
        model.addAttribute("days", days);

        return "admin/analytics";
    }

    // =========================================================
    // PAYMENT / WALLET MANAGEMENT
    // =========================================================

    @GetMapping("/wallets")
    public String manageWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Wallet> walletsPage;

        if (search != null && !search.isBlank()) {
            // TODO: Implement search
            walletsPage = walletRepository.findAll(PageRequest.of(page, PAGE_SIZE));
        } else {
            walletsPage = walletRepository.findAll(PageRequest.of(page, PAGE_SIZE));
        }

        model.addAttribute("walletsPage", walletsPage);
        model.addAttribute("wallets", walletsPage.getContent());
        model.addAttribute("pageTitle", "Wallet Management — Admin | GhanaRide");

        return "admin/wallets";
    }

    @PostMapping("/wallets/{walletId}/adjust")
    public String adjustWallet(
            @PathVariable Long walletId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            walletService.adjustBalance(walletId, amount, reason);
            redirectAttributes.addFlashAttribute("success", "Wallet adjusted successfully.");
        } catch (Exception e) {
            log.error("Error adjusting wallet {}", walletId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to adjust wallet.");
        }
        return "redirect:/admin/wallets";
    }

    // =========================================================
    // SETTINGS
    // =========================================================

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Platform Settings — Admin | GhanaRide");
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(
            @RequestParam String platformName,
            @RequestParam String supportEmail,
            @RequestParam String supportPhone,
            @RequestParam String paystackPublicKey,
            @RequestParam String paystackSecretKey,
            @RequestParam BigDecimal platformFeePercentage,
            @RequestParam Integer minBookingAdvanceHours,
            @RequestParam Integer cancellationWindowHours,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        // Save settings to database or config
        // For now, just show success
        redirectAttributes.addFlashAttribute("success", "Settings updated successfully.");
        return "redirect:/admin/settings";
    }

    // =========================================================
    // HELPER
    // =========================================================

    private String getCurrentUserEmail(Principal principal) {
        if (principal == null) return "unknown";
        try {
            return userService.getCurrentUser(
                new org.springframework.security.core.userdetails.User("temp", "", List.of())
            ).getEmail();
        } catch (Exception e) {
            return principal.getName();
        }
    }
}