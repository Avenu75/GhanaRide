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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles all trip business logic.
 *
 * Caching strategy:
 * - Popular routes cached (homepage fare estimator)
 * - Cache evicted when trips are added/modified
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
    @CacheEvict(value = {"routes", "popularRoutes"},
            allEntries = true)
    public Trip saveTrip(Trip trip) {
        Trip saved = tripRepository.save(trip);
        log.info(
                "Trip saved: id={} {} → {} driver={}",
                saved.getId(),
                saved.getFromLocation(),
                saved.getToLocation(),
                saved.getDriver() != null
                        ? saved.getDriver().getEmail()
                        : "company"
        );
        return saved;
    }

    // =========================================================
    // QUERIES
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
     * in the template — prevents LazyInitializationException.
     */
    public Page<Trip> findAllTripsWithDetails(Pageable pageable) {
        return tripRepository.findAllWithDetails(pageable);
    }

    public Page<Trip> findByStatus(
            TripStatus status, Pageable pageable
    ) {
        return tripRepository.findByStatus(
                status, pageable
        );
    }

    /**
     * Trips by status paginated with driver/company/car eagerly fetched.
     * Use this instead of findByStatus(Pageable) for view rendering
     * that accesses lazy-loaded relationships.
     */
    public Page<Trip> findByStatusWithDetails(
            TripStatus status, Pageable pageable
    ) {
        return tripRepository.findByStatusWithDetails(
                status, pageable
        );
    }

    public List<Trip> findApprovedTrips() {
        return tripRepository
                .findByStatusAndAvailableSeatsGreaterThan(
                        TripStatus.APPROVED, 0
                );
    }

    /**
     * Approved + full trips for the passenger dashboard.
     *
     * Uses TripRepository.findByStatusInWithDetails(), which
     * fetch-joins driver, company, and car. Required so
     * dashboard.html can safely read trip.driver.fullName /
     * trip.car.carBrand after the request-scoped Hibernate
     * session has closed.
     *
     * Do NOT switch this back to the plain findByStatusIn(...) —
     * that returns lazy proxies and will throw
     * LazyInitializationException during view rendering (this is
     * exactly what caused the original /dashboard 500).
     */
    public List<Trip> findApprovedAndFullTrips() {
        return tripRepository.findByStatusInWithDetails(
                List.of(TripStatus.APPROVED, TripStatus.FULL)
        );
    }

    /**
     * All upcoming approved trips (for public browse page)
     */
    @Cacheable("routes")
    public List<Trip> findAllApprovedUpcoming() {
        return tripRepository
                .findByStatusAndDepartureTimeAfter(
                        TripStatus.APPROVED,
                        LocalDateTime.now()
                );
    }

    /**
     * Preview for homepage (limited number)
     */
    public List<Trip> findUpcomingApprovedPreview(
            int limit
    ) {
        return tripRepository.findAll(
                        PageRequest.of(
                                0, limit,
                                Sort.by(Sort.Direction.ASC, "departureTime")
                        )
                ).stream()
                .filter(t ->
                        t.getStatus() == TripStatus.APPROVED &&
                                t.getDepartureTime().isAfter(
                                        LocalDateTime.now()
                                ) &&
                                t.getAvailableSeats() > 0
                )
                .toList();
    }

    /**
     * Search trips by from/to/date
     */
    public List<Trip> searchAvailableTrips(
            String from, String to, String date
    ) {
        LocalDateTime startOfDay = null;
        LocalDateTime endOfDay   = null;

        if (date != null && !date.isBlank()) {
            try {
                LocalDate searchDate =
                        LocalDate.parse(date);
                startOfDay = searchDate.atStartOfDay();
                endOfDay   = searchDate.atTime(
                        LocalTime.MAX
                );
            } catch (Exception e) {
                log.warn(
                        "Invalid date format in search: {}",
                        date
                );
            }
        }

        // Use repository query with optional filters
        return tripRepository.searchTrips(
                from, to, startOfDay, endOfDay
        );
    }

    public List<Trip> findByDriver(User driver) {
        return tripRepository.findByDriver(driver);
    }

    public List<Trip> findByCompany(Company company) {
        return tripRepository.findByCompany(company);
    }

    public List<Trip> findByDriverId(Long id) {
        return tripRepository.findByDriverId(id);
    }

    public List<Trip> findPendingTrips() {
        return tripRepository.findByStatus(
                TripStatus.PENDING
        );
    }

    /**
     * Recent trips for admin dashboard.
     *
     * Uses TripRepository.findAllWithDetails(), which fetch-joins
     * driver, car, and company. This is required so the
     * admin/trips.html template can safely read
     * trip.driver.fullName / trip.car.carBrand / trip.company
     * after the request-scoped Hibernate session has closed.
     *
     * Do NOT switch this back to a plain findAll(pageable) or any
     * non-fetch-joined query — that returns lazy proxies and will
     * throw LazyInitializationException during view rendering
     * (this is exactly what caused the original /admin/trips 500).
     */
    public List<Trip> findRecentTrips(int limit) {
        Pageable pageable = PageRequest.of(
                0, limit,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return tripRepository.findAllWithDetails(pageable)
                .getContent();
    }

    // =========================================================
    // APPROVE TRIP
    // =========================================================
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public Trip approveTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trip", id)
                );

        if (trip.getStatus() == TripStatus.APPROVED) {
            log.warn("Trip {} already approved", id);
            return trip;
        }

        trip.setStatus(TripStatus.APPROVED);
        Trip saved = tripRepository.save(trip);

        log.info(
                "Trip approved: id={} {} → {}",
                id,
                trip.getFromLocation(),
                trip.getToLocation()
        );

        // Notify driver of approval (async email)
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trip", id)
                );

        trip.setStatus(TripStatus.REJECTED);
        if (reason != null && !reason.isBlank()) {
            trip.setCancelReasonDetails(reason);
        }

        Trip saved = tripRepository.save(trip);

        log.info(
                "Trip rejected: id={} reason={}",
                id, reason
        );

        // Notify driver of rejection (async email)
        notifyDriverTripRejected(trip, reason);

        return saved;
    }

    // =========================================================
    // CANCEL TRIP (with passenger notifications)
    // =========================================================
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public Trip cancelTrip(
            Long tripId,
            String reason,
            String details,
            User cancelledBy
    ) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", tripId
                        )
                );

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelReason(reason);
        trip.setCancelReasonDetails(details);
        trip.setCancelledBy(cancelledBy);

        tripRepository.save(trip);

        // Cancel all active bookings
        // Use batch update for efficiency
        List<Booking> bookings =
                bookingRepository.findByTripId(tripId);

        List<Booking> toCancel = bookings.stream()
                .filter(b ->
                        b.getStatus() == BookingStatus.ACTIVE ||
                                b.getStatus() == BookingStatus.CONFIRMED ||
                                b.getStatus() == BookingStatus.PAID
                )
                .toList();

        toCancel.forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            // Notify passenger (async)
            notifyPassengerTripCancelled(b, reason);
        });

        bookingRepository.saveAll(toCancel);

        log.info(
                "Trip {} cancelled. {} passengers notified.",
                tripId, toCancel.size()
        );

        return trip;
    }

    // =========================================================
    // MARK AS FULL
    // =========================================================
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public Trip markAsFull(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", tripId
                        )
                );
        trip.setStatus(TripStatus.FULL);
        trip.setAvailableSeats(0);
        return tripRepository.save(trip);
    }

    // =========================================================
    // MARK AS FAILED TO SHOW
    // =========================================================
    @Transactional
    public Trip markAsFailedToShow(
            Long tripId,
            String reason,
            User markedBy
    ) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", tripId
                        )
                );

        trip.setStatus(TripStatus.FAILED_TO_SHOW);
        trip.setCancelReason("Driver Failed To Show");
        trip.setCancelReasonDetails(reason);
        trip.setCancelledBy(markedBy);

        tripRepository.save(trip);

        // Cancel bookings and notify passengers
        List<Booking> bookings =
                bookingRepository.findByTripId(tripId);

        List<Booking> toCancel = bookings.stream()
                .filter(b ->
                        b.getStatus() == BookingStatus.ACTIVE ||
                                b.getStatus() == BookingStatus.CONFIRMED ||
                                b.getStatus() == BookingStatus.PAID
                )
                .toList();

        toCancel.forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            notifyPassengerTripCancelled(
                    b, "Driver failed to show"
            );
        });

        bookingRepository.saveAll(toCancel);

        log.warn(
                "Trip {} marked as failed to show. " +
                        "{} passengers notified.",
                tripId, toCancel.size()
        );

        return trip;
    }

    // =========================================================
    // DELETE TRIP (Hard delete — Admin only)
    // =========================================================
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", tripId
                        )
                );

        // Delete bookings first (FK constraint)
        bookingRepository.deleteByTripId(tripId);
        tripRepository.delete(trip);

        log.warn("Trip {} HARD DELETED", tripId);
    }

    // =========================================================
    // CHECK DRIVER HAS ACTIVE TRIP
    // =========================================================
    public boolean driverHasActiveTrip(User driver) {
        return tripRepository.findByDriver(driver)
                .stream()
                .anyMatch(t ->
                        t.getStatus() == TripStatus.PENDING ||
                                t.getStatus() == TripStatus.APPROVED ||
                                t.getStatus() == TripStatus.FULL
                );
    }

    // =========================================================
    // CALCULATE COMPANY EARNINGS
    // Moved from CompanyController to service layer
    // =========================================================
    public double calculateCompanyEarnings(
            Company company
    ) {
        return tripRepository.findByCompany(company)
                .stream()
                .filter(t ->
                        t.getStatus() == TripStatus.COMPLETED
                )
                .mapToDouble(t -> {
                    int bookedSeats =
                            t.getTotalSeats() -
                                    t.getAvailableSeats();
                    return t.getTripAmount()
                            .doubleValue() * bookedSeats;
                })
                .sum();
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

    // =========================================================
    // ASYNC NOTIFICATIONS
    // =========================================================
    private void notifyDriverTripApproved(Trip trip) {
        try {
            if (trip.getDriver() != null &&
                    trip.getDriver().getEmail() != null) {
                // TODO: Add sendTripApprovedEmail to EmailService
                log.info(
                        "Driver notified of approval: {}",
                        trip.getDriver().getEmail()
                );
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to notify driver of approval", e
            );
        }
    }

    private void notifyDriverTripRejected(
            Trip trip, String reason
    ) {
        try {
            if (trip.getDriver() != null &&
                    trip.getDriver().getEmail() != null) {
                // TODO: Add sendTripRejectedEmail to EmailService
                log.info(
                        "Driver notified of rejection: {}",
                        trip.getDriver().getEmail()
                );
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to notify driver of rejection", e
            );
        }
    }

    private void notifyPassengerTripCancelled(
            Booking booking, String reason
    ) {
        try {
            if (booking.getUser() != null &&
                    booking.getUser().getEmail() != null) {
                // TODO: Add sendTripCancelledEmail to EmailService
                log.info(
                        "Passenger notified of cancellation: {}",
                        booking.getUser().getEmail()
                );
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to notify passenger of " +
                            "cancellation", e
            );
        }
    }
}