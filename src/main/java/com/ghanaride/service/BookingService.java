package com.ghanaride.service;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.exception.*;
import com.ghanaride.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Booking Service - Core booking operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final SeatService seatService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final TripService tripService;

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @Transactional
    public Booking createBooking(
            User user,
            Long tripId,
            String seatNumber,
            String paymentMethod,
            String passengerName,
            String passengerPhone
    ) {
        // Validate trip
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        validateBookingEligibility(trip, user);

        // Seat selection
        String finalSeatNumber = seatNumber;
        if (finalSeatNumber == null || finalSeatNumber.isBlank()) {
            finalSeatNumber = seatService.getFirstAvailableSeat(tripId)
                .orElseThrow(() -> new IllegalStateException("No seats available"));
        }

        // Hold seat for payment
        if (!seatService.holdSeat(tripId, finalSeatNumber, null, 7)) {
            throw new IllegalStateException("Seat " + finalSeatNumber + " is no longer available");
        }

        // Determine booking type
        BookingType bookingType = (passengerName != null && !passengerName.isBlank())
            ? BookingType.RELATIVE
            : BookingType.SELF;

        // Create booking
        Booking booking = Booking.builder()
            .user(user)
            .trip(trip)
            .bookingReference(generateBookingReference())
            .seatNumber(Integer.parseInt(finalSeatNumber.replaceAll("[^0-9]", "")))
            .bookingDate(LocalDateTime.now())
            .status(BookingStatus.PENDING_PAYMENT)
            .totalAmount(trip.getTripAmount())
            .bookingType(bookingType)
            .passengerName(bookingType == BookingType.RELATIVE ? passengerName : user.getFullName())
            .passengerPhone(bookingType == BookingType.RELATIVE ? passengerPhone : user.getPhoneNumber())
            .paymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()))
            .paymentStatus(PaymentStatus.PENDING)
            .build();

        Booking saved = bookingRepository.save(booking);

        // Process payment based on method
        boolean paymentSuccess = false;
        switch (PaymentMethod.valueOf(paymentMethod.toUpperCase())) {
            case WALLET -> {
                paymentSuccess = walletService.payWithWallet(user, trip.getTripAmount(),
                    "Booking " + booking.getBookingReference(), booking.getBookingReference());
                if (paymentSuccess) {
                    booking.setPaymentStatus(PaymentStatus.PAID);
                    booking.setTransactionReference("WALLET-" + booking.getBookingReference());
                }
            }
            case PAYSTACK -> {
                // Paystack handled on frontend, booking stays PENDING
                booking.setPaymentStatus(PaymentStatus.PENDING);
            }
            case CASH -> {
                booking.setPaymentStatus(PaymentStatus.PENDING);
            }
        }

        // Update trip seats if payment successful or cash
        if (paymentSuccess || PaymentMethod.CASH.name().equals(paymentMethod)) {
            tripService.decrementAvailableSeats(tripId);
            seatService.confirmSeat(tripId, finalSeatNumber);
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            // Payment pending - seat held for 7 min, will be released by scheduler if not confirmed
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
        }

        Booking finalBooking = bookingRepository.save(booking);

        log.info("Booking created: {} for user {} on trip {}",
            finalBooking.getBookingReference(), user.getEmail(), tripId);

        return finalBooking;
    }

    private void validateBookingEligibility(Trip trip, User user) {
        if (trip.getStatus() != TripStatus.APPROVED) {
            throw new IllegalStateException("Trip is not available for booking");
        }
        if (trip.getAvailableSeats() == null || trip.getAvailableSeats() <= 0) {
            throw new IllegalStateException("No seats available");
        }
        if (trip.getDepartureTime() == null || trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Trip has already departed");
        }
        if (trip.getDriver() != null && trip.getDriver().getId().equals(user.getId())) {
            throw new IllegalStateException("You cannot book your own trip");
        }
        if (bookingRepository.hasActiveBookingForTrip(user, trip)) {
            throw new IllegalStateException("You already have an active booking for this trip");
        }
    }

    // =========================================================
    // BOOKING LOOKUP
    // =========================================================

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Optional<Booking> findByReference(String reference) {
        return bookingRepository.findByBookingReference(reference);
    }

    public Optional<Booking> findByReferenceWithDetails(String reference) {
        return bookingRepository.findByReferenceWithDetails(reference);
    }

    public List<Booking> findByUserWithDetails(User user) {
        return bookingRepository.findByUserWithDetails(user);
    }

    public Page<Booking> findByUserPaginated(User user, Pageable pageable) {
        return bookingRepository.findByUserWithDetails(user, pageable);
    }

    // =========================================================
    // PAYMENT CONFIRMATION (from Paystack webhook)
    // =========================================================

    @Transactional
    public void confirmPayment(String bookingReference, String transactionReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("Payment already confirmed for {}", bookingReference);
            return;
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setTransactionReference(transactionReference);
        booking.setPaymentDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);

        // Confirm seat and decrement trip seats
        if (booking.getSeatMap() != null) {
            seatService.confirmSeat(booking.getTrip().getId(), booking.getSeatMap().getSeatNumber());
        }
        tripService.decrementAvailableSeats(booking.getTrip().getId());

        bookingRepository.save(booking);

        // Send receipt
        sendBookingConfirmation(booking);

        log.info("Payment confirmed for booking {}", bookingReference);
    }

    // =========================================================
    // CANCELLATION
    // =========================================================

    @Transactional
    public void cancelBooking(Long bookingId, User currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You can only cancel your own bookings");
        }

        if (!bookingService.canCancelBooking(booking)) {
            throw new BookingException("This booking can no longer be cancelled. " +
                "Cancellations must be made at least 2 hours before departure. " +
                "Please contact support for help.");
        }

        // Release seat
        seatService.releaseSeat(booking.getTrip().getId(),
            booking.getSeatMap() != null ? booking.getSeatMap().getSeatNumber() : String.valueOf(booking.getSeatNumber()));
        tripService.incrementAvailableSeats(booking.getTrip().getId());

        // Update booking
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Process refund if paid
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            processRefund(booking);
        }

        // Notify
        notificationService.createNotification(
            currentUser,
            NotificationType.BOOKING_CANCELLED,
            "Booking Cancelled",
            "Booking " + booking.getBookingReference() + " has been cancelled. " +
                (booking.getPaymentStatus() == PaymentStatus.PAID ? "Refund will be processed to your wallet." : ""),
            "/my-bookings"
        );

        log.info("Booking {} cancelled by user {}", bookingId, currentUser.getEmail());
    }

    private boolean canCancelBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            return false;
        }
        if (booking.getTrip().getDepartureTime() == null) return false;
        return booking.getTrip().getDepartureTime().isAfter(LocalDateTime.now().plusHours(2));
    }

    private void processRefund(Booking booking) {
        switch (booking.getPaymentMethod()) {
            case WALLET -> {
                walletService.refund(booking.getUser(), booking.getTotalAmount(), booking.getBookingReference());
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            case PAYSTACK, MTN_MOMO, VODAFONE_CASH -> {
                // Paystack refund API call would go here
                booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            }
            case CASH -> {
                booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            }
        }
        bookingRepository.save(booking);
    }

    // =========================================================
    // RECEIPT / BOARDING PASS
    // =========================================================

    private void sendBookingConfirmation(Booking booking) {
        // EmailService.sendBookingConfirmationEmail(booking);
        notificationService.createNotification(booking.getUser(), NotificationType.BOOKING_CONFIRMED,
            "Booking Confirmed",
            "Your booking " + booking.getBookingReference() + " is confirmed. " +
            "Trip: " + booking.getTrip().getFromLocation() + " → " + booking.getTrip().getToLocation(),
            "/booking/receipt/" + booking.getId());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String generateBookingReference() {
        String prefix = "GR";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
    }
}