package com.ghanaride.service;

import com.ghanaride.entity.*;
import com.ghanaride.exception.ResourceNotFoundException;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ghanaride.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles all trip business logic.
 * 
 * Caching Strategy:
 * - "routes": Popular routes for homepage fare estimator
 * - "popularRoutes": Cached search results
 * - Cache evicted on trip save/modify/approve/reject
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    // =========================================================
    // SAVE TRIP
    // =========================================================

    @Transactional
    @CacheEvict(value = {"routes", "popularRoutes"}, allEntries = true)
    public Trip saveTrip(Trip trip) {
        Trip saved = tripRepository.save(trip);
        log.info("Trip saved: id={} {} → {} driver={}", 
            saved.getId(), saved.getFromLocation(), saved.getToLocation(),
            saved.getDriver() != null ? saved.getDriver().getEmail() : "company");
        return saved;
    }

    // =========================================================
    // QUERIES - All view-rendering methods use JOIN FETCH
    // =========================================================

    public Optional<Trip> findById(Long id) {
        return tripRepository.findById(id);
    }

    public List<Trip> findAllTrips() {
        return tripRepository.findAll();
    }

    public Page<Trip> findAllTrips(Pageable pageable) {
        return tripRepository.findAll(pageable);
    }

    /**
     * All trips paginated with driver/company/car eagerly fetched.
     * Use this instead of findAllTrips(Pageable) for any view that
     * accesses trip.driver.fullName / trip.car.carBrand / trip.company
     */
    public Page<Trip> findAllTripsWithDetails(Pageable pageable) {
        return tripRepository.findAllWithDetails(pageable);
    }

    public Page<Trip> findByStatus(TripStatus status, Pageable pageable) {
        return tripRepository.findByStatus(status, pageable);
    }

    /**
     * Trips by status paginated with driver/company/car eagerly fetched.
     * Use this instead of findByStatus(Pageable) for view rendering.
     */
    public Page<Trip> findByStatusWithDetails(TripStatus status, Pageable pageable) {
        return tripRepository.findByStatusWithDetails(status, pageable);
    }

    /**
     * Approved trips with available seats (legacy - no JOIN FETCH).
     * Use findApprovedTripsWithDetails() for view rendering.
     */
    public List<Trip> findApprovedTrips() {
        return tripRepository.findByStatusAndAvailableSeatsGreaterThan(TripStatus.APPROVED, 0);
    }

    /**
     * Approved + Full trips for passenger dashboard.
     * Uses JOIN FETCH so dashboard.html can safely read trip.driver.fullName etc.
     */
    public List<Trip> findApprovedAndFullTrips() {
        return tripRepository.findByStatusInWithDetails(
            List.of(TripStatus.APPROVED, TripStatus.FULL)
        );
    }

    /**
     * All upcoming approved trips for public browse page (/rides).
     * FIXED: Uses JOIN FETCH to prevent LazyInitializationException.
     */
    @Cacheable("routes")
    public List<Trip> findAllApprovedUpcoming() {
        return tripRepository.findApprovedUpcomingWithDetails();
    }

    /**
     * Preview for homepage (limited number)
     */
    public List<Trip> findUpcomingApprovedPreview(int limit) {
        return tripRepository.findApprovedUpcomingPreview(limit);
    }

    /**
     * Search trips by from/to/date with JOIN FETCH.
     * All filters optional (null = ignored).
     * Returns only APPROVED trips with available seats.
     */
    public List<Trip> searchAvailableTrips(String from, String to, String date) {
        LocalDateTime startOfDay = null;
        LocalDateTime endOfDay = null;

        if (date != null && !date.isBlank()) {
            try {
                LocalDate searchDate = LocalDate.parse(date);
                startOfDay = searchDate.atStartOfDay();
                endOfDay = searchDate.atTime(LocalTime.MAX);
            } catch (Exception e) {
                log.warn("Invalid date format in search: {}", date);
            }
        }

        return tripRepository.searchTrips(from, to, startOfDay, endOfDay);
    }

    public List<Trip> findByDriver(User driver) {
        return tripRepository.findByDriver(driver);
    }

    public boolean driverHasActiveTrip(User driver) {
        return tripRepository.countByDriverAndStatusIn(driver, List.of(TripStatus.PENDING, TripStatus.APPROVED, TripStatus.FULL)) > 0;
    }

    public List<Trip> findByCompany(Company company) {
        return tripRepository.findByCompany(company);
    }

    public List<Trip> findByDriverId(Long id) {
        return tripRepository.findByDriverId(id);
    }

    public List<Trip> findPendingTrips() {
        return tripRepository.findByStatus(TripStatus.PENDING);
    }

    /**
     * Recent trips for admin dashboard with JOIN FETCH.
     */
    public List<Trip> findRecentTrips(int limit) {
        return tripRepository.findRecentTripsWithDetails(limit);
    }

    // =========================================================
    // APPROVE TRIP
    // =========================================================

    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public Trip approveTrip(Long id) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", id));

        if (trip.getStatus() == TripStatus.APPROVED) {
            log.warn("Trip {} already approved", id);
            return trip;
        }

        trip.setStatus(TripStatus.APPROVED);
        Trip saved = tripRepository.save(trip);

        log.info("Trip approved: id={} {} → {}", id, trip.getFromLocation(), trip.getToLocation());

        // Notify driver of approval (async)
        notifyDriverTripApproved(trip);

        return saved;
    }

    // =========================================================
    // REJECT TRIP
    // =========================================================

    @Transactional
    public Trip rejectTrip(Long id) {
        return rejectTrip(id, null);
    }

    @Transactional
    public Trip rejectTrip(Long id, String reason) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", id));

        trip.setStatus(TripStatus.REJECTED);
        if (reason != null && !reason.isBlank()) {
            trip.setCancelReasonDetails(reason);
        }

        Trip saved = tripRepository.save(trip);

        log.info("Trip rejected: id={} reason={}", id, reason);

        // Notify driver of rejection (async)
        notifyDriverTripRejected(trip, reason);

        return saved;
    }

    // =========================================================
    // CANCEL TRIP (with passenger notifications)
    // =========================================================

    @Transactional
    public Trip cancelTrip(Long id, String reason) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", id));

        if (trip.getStatus() == TripStatus.CANCELLED) {
            log.warn("Trip {} already cancelled", id);
            return trip;
        }

        trip.setStatus(TripStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            trip.setCancelReasonDetails(reason);
        }

        Trip saved = tripRepository.save(trip);

        log.info("Trip cancelled: id={} reason={}", id, reason);

        // Notify passengers (async)
        notifyPassengersTripCancelled(trip, reason);

        return saved;
    }

    // =========================================================
    // MARK AS FULL
    // =========================================================

    @Transactional
    public void markAsFull(Long id) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
        trip.setStatus(TripStatus.FULL);
        tripRepository.save(trip);
        log.info("Trip marked as FULL: id={}", id);
    }

    // =========================================================
    // SEAT MANAGEMENT
    // =========================================================

    @Transactional
    public void decrementAvailableSeats(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        if (trip.getAvailableSeats() == null || trip.getAvailableSeats() <= 0) {
            throw new IllegalStateException("No available seats on this trip");
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        
        // Auto-transition to FULL when last seat booked
        if (trip.getAvailableSeats() == 0) {
            trip.setStatus(TripStatus.FULL);
        }

        tripRepository.save(trip);
        log.debug("Decremented seats for trip {}: now {}", tripId, trip.getAvailableSeats());
    }

    @Transactional
    public void incrementAvailableSeats(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        int maxSeats = trip.getTotalSeats() != null ? trip.getTotalSeats() : 18;
        if (trip.getAvailableSeats() != null && trip.getAvailableSeats() < maxSeats) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            
            // Transition from FULL back to APPROVED if seats available
            if (trip.getStatus() == TripStatus.FULL) {
                trip.setStatus(TripStatus.APPROVED);
            }
            
            tripRepository.save(trip);
            log.debug("Incremented seats for trip {}: now {}", tripId, trip.getAvailableSeats());
        }
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    public long countAll() {
        return tripRepository.count();
    }

    public long countByStatus(TripStatus status) {
        return tripRepository.countByStatus(status);
    }

    public double calculateCompanyEarnings(Company company) {
        List<Trip> trips = tripRepository.findByCompany(company);
        double total = 0;
        for (Trip trip : trips) {
            if (trip.getTripAmount() != null) {
                int bookedSeats = (trip.getTotalSeats() != null ? trip.getTotalSeats() : 18) - 
                                  (trip.getAvailableSeats() != null ? trip.getAvailableSeats() : 0);
                total += trip.getTripAmount().doubleValue() * Math.max(0, bookedSeats);
            }
        }
        return total;
    }

    // =========================================================
    // ASYNC NOTIFICATIONS
    // =========================================================

    private void notifyDriverTripApproved(Trip trip) {
        try {
            if (trip.getDriver() != null && trip.getDriver().getEmail() != null) {
                // TODO: Add sendTripApprovedEmail to EmailService
                log.info("Driver notified of approval: {}", trip.getDriver().getEmail());
            }
        } catch (Exception e) {
            log.warn("Failed to notify driver of approval", e);
        }
    }

    private void notifyDriverTripRejected(Trip trip, String reason) {
        try {
            if (trip.getDriver() != null && trip.getDriver().getEmail() != null) {
                // TODO: Add sendTripRejectedEmail to EmailService
                log.info("Driver notified of rejection: {}", trip.getDriver().getEmail());
            }
        } catch (Exception e) {
            log.warn("Failed to notify driver of rejection", e);
        }
    }

    private void notifyPassengersTripCancelled(Trip trip, String reason) {
        try {
            List<Booking> bookings = bookingRepository
                .findByTripAndStatusIn(trip, List.of(BookingStatus.ACTIVE, BookingStatus.CONFIRMED));
            
            for (Booking booking : bookings) {
                if (booking.getUser() != null && booking.getUser().getEmail() != null) {
                    // TODO: Add sendTripCancelledEmail to EmailService
                    log.info("Passenger notified of cancellation: {}", booking.getUser().getEmail());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify passengers of cancellation", e);
        }
    }

    // =========================================================
    // SCHEDULED TASKS
    // =========================================================

    /**
     * Runs every 5 minutes to expire trips past departure time.
     * Transitions APPROVED/FULL → EXPIRED for departed trips.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void expireDepartedTrips() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = tripRepository.expireTripsBefore(now);
        if (expiredCount > 0) {
            log.info("Expired {} departed trips", expiredCount);
        }
    }

    /**
     * Runs daily at 2 AM to clean up old EXPIRED/CANCELLED trips
     * (keeps last 90 days for audit).
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldTrips() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        // Implementation would delete or archive old trips
        log.debug("Trip cleanup job ran, cutoff: {}", cutoff);
    }

    public void markAsFailedToShow(Long tripId, String failReason, User currentUser) {
    }

    public void deleteTrip(Long tripId) {
    }
}