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
import java.util.List;
import java.util.Map;

/**
 * Passenger Controller - Handles passenger-specific operations:
 * Wallet, notifications, profile, bookings, reviews.
 */
@Slf4j
@Controller
@RequestMapping("/passenger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class PassengerController {

    private final BookingService bookingService;
    private final TripService tripService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final FileStorageService fileStorageService;

    // =========================================================
    // PASSENGER DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        // Upcoming bookings
        List<Booking> upcomingBookings = bookingRepository.findUpcomingBookings(currentUser);
        List<Booking> pastBookings = bookingRepository.findPastBookings(currentUser);

        // Wallet
        Wallet wallet = walletRepository.findByUser(currentUser)
            .orElseGet(() -> walletService.getOrCreateWallet(currentUser));

        // Stats
        long upcomingCount = upcomingBookings.size();
        long completedCount = pastBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long cancelledCount = pastBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        // Wallet stats
        BigDecimal totalSpent = walletTransactionRepository.getTotalSpent(currentUser);
        BigDecimal totalCashback = walletTransactionRepository.getTotalLoyaltyEarned(currentUser);

        // Upcoming trip for quick access
        Booking nextTrip = upcomingBookings.isEmpty() ? null : upcomingBookings.get(0);

        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("pastBookings", pastBookings);
        model.addAttribute("nextTrip", nextTrip);
        model.addAttribute("wallet", wallet);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("totalCashback", totalCashback);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("pageTitle", "Passenger Dashboard — GhanaRide");

        return "passenger/dashboard";
    }

    // =========================================================
    // MY BOOKINGS
    // =========================================================

    @GetMapping("/my-bookings")
    public String myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal,
            Model model
    ) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "bookingDate"));

        Page<Booking> bookings = bookingRepository.findByUser(currentUser, PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "bookingDate")));

        model.addAttribute("bookings", bookings);
        model.addAttribute("pageTitle", "My Bookings — GhanaRide");

        return "passenger/my-bookings";
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
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Wallet wallet = walletRepository.findByUser(currentUser)
            .orElseGet(() -> walletService.getOrCreateWallet(currentUser));

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WalletTransaction> transactions = walletTransactionRepository
            .findByUserOrderByCreatedAtDesc(currentUser, PageRequest.of(page, 10));

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("pageTitle", "My Wallet — GhanaRide");

        return "passenger/wallet";
    }

    @GetMapping("/wallet/topup")
    public String topupForm(Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Wallet wallet = walletService.getOrCreateWallet(
            userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow()
        );

        model.addAttribute("wallet", wallet);
        model.addAttribute("pageTitle", "Top Up Wallet — GhanaRide");
        return "passenger/wallet-topup";
    }

    @PostMapping("/wallet/topup")
    public String topupWallet(
            @RequestParam BigDecimal amount,
            @RequestParam String provider, // PAYSTACK, MTN_MOMO, VODAFONE_CASH
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User currentUser = userRepository.findByEmail(
                SecurityContextHolder.getContext().getAuthentication().getName()
            ).orElseThrow();

            WalletTransaction tx = walletService.topup(currentUser, amount, provider, "TOPUP-" + System.currentTimeMillis());

            redirectAttributes.addFlashAttribute("success", 
                "Wallet topped up with GH₵" + amount + " via " + provider + "!");
            return "redirect:/passenger/wallet";
        } catch (Exception e) {
            log.error("Topup failed", e);
            redirectAttributes.addFlashAttribute("error", "Top-up failed: " + e.getMessage());
            return "redirect:/passenger/wallet/topup";
        }
    }

    // =========================================================
    // PROFILE
    // =========================================================

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "Profile Settings");
        return "passenger/profile";
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
            return "passenger/profile";
        }

        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        if (profileForm.getFullName() != null) currentUser.setFullName(profileForm.getFullName());
        if (profileForm.getPhoneNumber() != null) currentUser.setPhoneNumber(profileForm.getPhoneNumber());
        if (profileForm.getDateOfBirth() != null) currentUser.setDateOfBirth(profileForm.getDateOfBirth());
        if (profileForm.getGender() != null) currentUser.setGender(profileForm.getGender());
        if (profileForm.getAddress() != null) currentUser.setAddress(profileForm.getAddress());

        if (profileImage != null && !profileImage.isEmpty()) {
            String path = fileStorageService.storeProfileImage(profileImage);
            currentUser.setProfileImagePath(path);
        }

        userRepository.save(currentUser);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/passenger/profile";
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    @GetMapping("/notifications")
    public String notifications(
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository
            .findByUserOrderByCreatedAtDesc(currentUser, PageRequest.of(page, 10));

        model.addAttribute("notifications", notifications);
        model.addAttribute("pageTitle", "Notifications — GhanaRide");
        return "passenger/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public Map<String, Object> markAsRead(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        notificationRepository.findByIdAndUserId(id, currentUser.getId())
            .ifPresent(n -> {
                n.setRead(true);
                notificationRepository.save(n);
            });

        return Map.of("success", true);
    }

    // =========================================================
    // REVIEWS
    // =========================================================

    @GetMapping("/review/{bookingId}")
    public String reviewForm(
            @PathVariable Long bookingId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty() || !bookingOpt.get().getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Booking not found or access denied.");
            return "redirect:/passenger/my-bookings";
        }

        Booking booking = bookingOpt.get();
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            redirectAttributes.addFlashAttribute("error", "Can only review completed trips.");
            return "redirect:/passenger/my-bookings";
        }

        if (reviewRepository.existsByBookingId(bookingId)) {
            redirectAttributes.addFlashAttribute("error", "You've already reviewed this trip.");
            return "redirect:/passenger/my-bookings";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("reviewForm", new ReviewFormDTO());
        model.addAttribute("pageTitle", "Review Your Trip — GhanaRide");
        return "passenger/review";
    }

    @PostMapping("/review/{bookingId}")
    public String submitReview(
            @PathVariable Long bookingId,
            @Valid @ModelAttribute("reviewForm") ReviewFormDTO reviewForm,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Review Your Trip — GhanaRide");
            return "passenger/review";
        }

        User currentUser = userRepository.findByEmail(
            SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Access denied.");
            return "redirect:/passenger/my-bookings";
        }

        if (reviewRepository.existsByBookingId(bookingId)) {
            redirectAttributes.addFlashAttribute("error", "Already reviewed.");
            return "redirect:/passenger/my-bookings";
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(currentUser);
        review.setDriver(booking.getTrip().getDriver());
        review.setRating(reviewForm.getRating());
        review.setComment(reviewForm.getComment());

        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("success", "Thank you for your review!");
        return "redirect:/passenger/my-bookings";
    }
}