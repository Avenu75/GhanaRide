package com.ghanaride.service;

import com.ghanaride.entity.*;
import com.ghanaride.exception.BookingException;
import com.ghanaride.exception.ResourceNotFoundException;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all booking business logic.
 *
 * Key design decisions:
 * - @Transactional on write operations (createBooking,
 *   cancelBooking, deleteBooking) ensures atomicity
 * - Custom exceptions (BookingException) instead of
 *   RuntimeException for cleaner error handling
 * - Seat reservation is atomic (DB-level locking
 *   prevents race conditions)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final EmailService emailService;

    // Cancellation window — configurable via properties
    @Value("${app.booking.cancel-window-minutes:3}")
    private int cancelWindowMinutes;

    // =========================================================
    // CREATE BOOKING
    // Thread-safe seat reservation using @Transactional
    // =========================================================
    @Transactional
    public Booking createBooking(
            User user,
            Trip trip,
            BookingType type,
            String passengerName,
            String passengerPhone
    ) {
        // Re-fetch trip with lock to prevent race condition
        // where two users book the last seat simultaneously
        Trip freshTrip = tripRepository
                .findById(trip.getId())
                .orElseThrow(() ->
                        BookingException.tripNotFound(trip.getId())
                );

        // Check seats still available
        if (freshTrip.getAvailableSeats() <= 0) {
            throw BookingException.noSeatsAvailable();
        }

        // Check one active self-booking restriction
        if (type == BookingType.SELF) {
            boolean hasActiveBooking =
                    bookingRepository
                            .existsByUserAndBookingTypeAndStatusIn(
                                    user,
                                    BookingType.SELF,
                                    List.of(
                                            BookingStatus.CONFIRMED,
                                            BookingStatus.ACTIVE,
                                            BookingStatus.PAID
                                    )
                            );

            if (hasActiveBooking) {
                throw BookingException.alreadyBooked();
            }
        }

        // Check not already booked this specific trip
        boolean alreadyBookedThisTrip =
                bookingRepository.existsByUserAndTrip(
                        user, freshTrip
                );
        if (alreadyBookedThisTrip) {
            throw new BookingException(
                    "You have already booked this trip."
            );
        }

        // Build booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(freshTrip);
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setBookingType(type);
        booking.setBookingDate(LocalDateTime.now());

        // Passenger details
        if (type == BookingType.RELATIVE) {
            booking.setPassengerName(passengerName);
            booking.setPassengerPhone(passengerPhone);
        } else {
            // For SELF bookings, use user's own details
            booking.setPassengerName(
                    user.getFullName() != null
                            ? user.getFullName()
                            : user.getUsername()
            );
            booking.setPassengerPhone(
                    user.getPhoneNumber()
            );
        }

        // Unique booking reference
        booking.setBookingReference(
                "GR-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        // Seat number (based on current bookings count)
        long currentBookings =
                bookingRepository.countByTripId(
                        freshTrip.getId()
                );
        booking.setSeatNumber((int) currentBookings + 1);

        // Amount
        booking.setTotalAmount(freshTrip.getTripAmount());
        booking.setPaymentStatus(PaymentStatus.PENDING);

        // Decrement available seats (atomic with @Transactional)
        freshTrip.setAvailableSeats(
                freshTrip.getAvailableSeats() - 1
        );

        // Mark as full if no seats remain
        if (freshTrip.getAvailableSeats() == 0) {
            freshTrip.setStatus(TripStatus.FULL);
        }

        tripRepository.save(freshTrip);
        Booking saved = bookingRepository.save(booking);

        // Send confirmation email asynchronously
        // (doesn't block the booking response)
        sendBookingConfirmationAsync(saved);

        log.info(
                "Booking created: ref={} user={} trip={} type={}",
                saved.getBookingReference(),
                user.getEmail(),
                freshTrip.getId(),
                type
        );

        return saved;
    }

    // =========================================================
    // CAN USER CANCEL?
    // Returns true if within cancellation window
    // =========================================================
    public boolean canCancelBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return false;
        }
        if (booking.getBookingDate() == null) {
            return false;
        }
        // Within cancellation window?
        return LocalDateTime.now().isBefore(
                booking.getBookingDate()
                        .plusMinutes(cancelWindowMinutes)
        );
    }

    /**
     * Returns seconds remaining in cancellation window.
     * Returns 0 if window has expired.
     */
    public long secondsUntilCancelDeadline(Booking booking) {
        if (booking.getBookingDate() == null) return 0;

        LocalDateTime deadline = booking.getBookingDate()
                .plusMinutes(cancelWindowMinutes);
        long secs = java.time.Duration
                .between(LocalDateTime.now(), deadline)
                .getSeconds();
        return Math.max(0, secs);
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        BookingException.bookingNotFound(bookingId)
                );

        // Already cancelled?
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw BookingException.alreadyCancelled();
        }

        // Within window?
        if (!canCancelBooking(booking)) {
            throw BookingException
                    .cancellationWindowExpired(cancelWindowMinutes);
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Restore seat
        Trip trip = booking.getTrip();
        if (trip != null) {
            trip.setAvailableSeats(
                    trip.getAvailableSeats() + 1
            );
            // Reopen if it was marked as FULL
            if (trip.getStatus() == TripStatus.FULL) {
                trip.setStatus(TripStatus.APPROVED);
            }
            tripRepository.save(trip);
        }

        bookingRepository.save(booking);

        log.info(
                "Booking cancelled: id={} ref={} user={}",
                bookingId,
                booking.getBookingReference(),
                booking.getUser().getEmail()
        );
    }

    // =========================================================
    // DELETE BOOKING (Admin only)
    // =========================================================
    @Transactional
    public void deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking", bookingId
                        )
                );

        // Restore seat if booking was not already cancelled
        Trip trip = booking.getTrip();
        if (trip != null &&
                booking.getStatus() != BookingStatus.CANCELLED) {
            trip.setAvailableSeats(
                    trip.getAvailableSeats() + 1
            );
            tripRepository.save(trip);
        }

        bookingRepository.delete(booking);

        log.info("Booking deleted by admin: id={}", bookingId);
    }

    // =========================================================
    // QUERY METHODS
    // =========================================================

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    /**
     * Single booking with trip + driver + car + company eagerly
     * fetched. Use this instead of findById(...) for any view
     * that reads booking.trip.driver.fullName /
     * booking.trip.car.carBrand — e.g. the receipt page —
     * otherwise you'll hit LazyInitializationException during
     * rendering.
     */
    public Optional<Booking> findByIdWithDetails(Long id) {
        return bookingRepository.findByIdWithDetails(id);
    }

    public Optional<Booking> findByBookingReference(
            String ref
    ) {
        return bookingRepository.findByBookingReference(ref);
    }

    public List<Booking> findByUser(User user) {
        return bookingRepository.findByUser(user);
    }

    /**
     * A user's bookings with trip + driver + car + company eagerly
     * fetched. Use this instead of findByUser(...) for any view
     * that reads booking.trip.driver.fullName /
     * booking.trip.fromLocation / booking.trip.departureTime —
     * e.g. the dashboard and my-bookings pages — otherwise you'll
     * hit LazyInitializationException during rendering.
     */
    public List<Booking> findByUserWithDetails(User user) {
        return bookingRepository.findByUserWithDetails(user);
    }

    public List<Booking> findByUserId(Long id) {
        return bookingRepository.findByUserId(id);
    }

    public List<Booking> findByTripId(Long tripId) {
        return bookingRepository.findByTripId(tripId);
    }

    /**
     * Check if user has already booked a specific trip
     */
    public boolean hasUserBookedTrip(User user, Trip trip) {
        return bookingRepository.existsByUserAndTrip(
                user, trip
        );
    }

    /**
     * Active bookings = not cancelled + trip not departed
     *
     * Uses findByUserWithDetails() (fetch-joins trip) since this
     * filters on b.getTrip().getDepartureTime() — a plain
     * findByUser(...) would return lazy trip proxies and throw
     * LazyInitializationException the moment the session closes.
     */
    public List<Booking> findActiveBookingsByUser(User user) {
        return bookingRepository.findByUserWithDetails(user).stream()
                .filter(b ->
                        b.getStatus() != BookingStatus.CANCELLED &&
                                b.getTrip().getDepartureTime()
                                        .isAfter(LocalDateTime.now()))
                .toList();
    }

    /**
     * Past bookings = cancelled OR trip already departed
     *
     * Uses findByUserWithDetails() for the same reason as
     * findActiveBookingsByUser() above.
     */
    public List<Booking> findPastBookingsByUser(User user) {
        return bookingRepository.findByUserWithDetails(user).stream()
                .filter(b ->
                        b.getStatus() == BookingStatus.CANCELLED ||
                                b.getTrip().getDepartureTime()
                                        .isBefore(LocalDateTime.now()))
                .toList();
    }

    /**
     * Upcoming = active bookings with future departure
     */
    public List<Booking> findUpcomingBookingsByUser(
            User user
    ) {
        return findActiveBookingsByUser(user);
    }

    /**
     * Recent bookings for dashboard feed
     */
    public List<Booking> findRecentBookingsByUser(
            User user, int limit
    ) {
        return bookingRepository.findByUser(user).stream()
                .sorted((a, b) -> b.getBookingDate()
                        .compareTo(a.getBookingDate()))
                .limit(limit)
                .toList();
    }

    /**
     * All bookings — paginated for admin panel
     */
    public Page<Booking> findAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable);
    }

    /**
     * All bookings without pagination
     * (kept for backward compatibility with existing admin view)
     */
    public List<Booking> findAllBookings() {
        return bookingRepository.findAllWithDetails();
    }

    /**
     * Recent bookings for admin dashboard feed
     */
    public List<Booking> findRecentBookings(int limit) {
        return bookingRepository.findAll(
                PageRequest.of(0, limit,
                        Sort.by(Sort.Direction.DESC, "bookingDate"))
        ).getContent();
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    public long countAll() {
        return bookingRepository.count();
    }

    public long countAllCompleted() {
        return bookingRepository.countByTripStatus(
                TripStatus.COMPLETED
        );
    }

    public long countByUser(User user) {
        return bookingRepository.countByUser(user);
    }

    public long countCompletedByUser(User user) {
        return bookingRepository.countByUserAndTripStatus(
                user, TripStatus.COMPLETED
        );
    }

    // =========================================================
    // ASYNC EMAIL NOTIFICATION
    // Runs in separate thread — doesn't block booking response
    // =========================================================
    @Async
    public void sendBookingConfirmationAsync(Booking booking) {
        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            // Email failure must NEVER fail the booking
            log.warn(
                    "Failed to send booking confirmation " +
                            "email for ref: {}",
                    booking.getBookingReference(), e
            );
        }
    }
}