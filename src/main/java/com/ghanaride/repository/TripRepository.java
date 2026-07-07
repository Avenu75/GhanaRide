package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Trip entity.
 *
 * Key queries:
 * - searchTrips(): Full-text style search with optional
 *   from/to/date filters (used by browse page)
 * - findByStatusAndDepartureTimeAfter(): All upcoming
 *   approved trips (used by public browse page)
 * - Paginated queries for admin panel
 */
@Repository
public interface TripRepository
        extends JpaRepository<Trip, Long> {

    // =========================================================
    // BASIC FINDERS
    // =========================================================

    @Transactional(readOnly = true)
    List<Trip> findByStatus(TripStatus status);

    @Transactional(readOnly = true)
    Page<Trip> findByStatus(
            TripStatus status, Pageable pageable
    );

    /**
     * NOTE: kept for backwards compatibility, but this does NOT
     * fetch driver/company/car. If a view needs to render
     * trip.driver.fullName, trip.car.carBrand, etc., use
     * findByStatusInWithDetails(...) instead, or you will hit
     * LazyInitializationException once the request-scoped
     * Hibernate session closes before rendering.
     */
    @Transactional(readOnly = true)
    List<Trip> findByStatusIn(List<TripStatus> statuses);

    /**
     * Trips by multiple statuses, with JOIN FETCH for
     * driver/company/car. Used by BookingController#dashboard
     * (passenger dashboard, APPROVED + FULL trips) so the
     * template can safely read trip.driver.fullName,
     * trip.car.carBrand, etc. without a LazyInitializationException.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        LEFT JOIN FETCH t.car
        WHERE t.status IN :statuses
        ORDER BY t.departureTime ASC
        """)
    List<Trip> findByStatusInWithDetails(
            @Param("statuses") List<TripStatus> statuses
    );

    @Transactional(readOnly = true)
    @Query("""
    SELECT t FROM Trip t
    LEFT JOIN FETCH t.car
    LEFT JOIN FETCH t.company
    WHERE t.driver = :driver
    """)
    List<Trip> findByDriver(@Param("driver") User driver);

    @Transactional(readOnly = true)
    List<Trip> findByDriverId(Long driverId);

    @Transactional(readOnly = true)
    List<Trip> findByCompany(Company company);

    @Transactional(readOnly = true)
    List<Trip> findByStatusAndAvailableSeatsGreaterThan(
            TripStatus status, int seats
    );

    // =========================================================
    // UPCOMING TRIPS
    // =========================================================

    /**
     * All upcoming approved trips.
     * Used for public trip browse page.
     */
    @Transactional(readOnly = true)
    List<Trip> findByStatusAndDepartureTimeAfter(
            TripStatus status,
            LocalDateTime after
    );

    /**
     * Trips departing within a time window.
     * Used for departure reminders.
     */
    @Transactional(readOnly = true)
    List<Trip> findByStatusAndDepartureTimeBetween(
            TripStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Trips that have passed their departure time.
     * Used by expiration scheduler.
     */
    @Transactional(readOnly = true)
    List<Trip> findByStatusAndDepartureTimeBefore(
            TripStatus status,
            LocalDateTime before
    );

    // =========================================================
    // SEARCH
    // Flexible search with optional from/to/date filters.
    // All filters are optional (null = ignored).
    // Returns only APPROVED trips with available seats.
    // =========================================================
    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM Trip t
        LEFT JOIN FETCH t.car
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        WHERE t.status = 'APPROVED'
        AND t.availableSeats > 0
        AND t.departureTime > CURRENT_TIMESTAMP
        AND (:from IS NULL OR :from = ''
             OR LOWER(t.fromLocation)
                LIKE LOWER(CONCAT('%', :from, '%')))
        AND (:to IS NULL OR :to = ''
             OR LOWER(t.toLocation)
                LIKE LOWER(CONCAT('%', :to, '%')))
        AND (:startDate IS NULL
             OR t.departureTime >= :startDate)
        AND (:endDate IS NULL
             OR t.departureTime <= :endDate)
        ORDER BY t.departureTime ASC
        """)
    List<Trip> searchTrips(
            @Param("from") String from,
            @Param("to") String to,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =========================================================
    // ADMIN PAGINATED QUERIES (with JOIN FETCH)
    // =========================================================

    /**
     * All trips paginated for admin panel.
     * Uses countQuery to avoid JOIN FETCH pagination warning.
     *
     * This is the fetch-joined query the admin/trips.html template
     * and AdminController#manageTrips should use — it eagerly loads
     * driver, company, and car so the view can safely read
     * trip.driver.fullName / trip.car.carBrand / trip.company
     * without hitting LazyInitializationException after the
     * request-scoped session closes.
     */
    @Transactional(readOnly = true)
    @Query(
            value = """
            SELECT t FROM Trip t
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.company
            LEFT JOIN FETCH t.car
            ORDER BY t.createdAt DESC
            """,
            countQuery = "SELECT COUNT(t) FROM Trip t"
    )
    Page<Trip> findAllWithDetails(Pageable pageable);

    /**
     * Trips by status paginated, with JOIN FETCH for driver/company/car.
     * Used by AdminController#manageTrips when a status filter is active.
     * Prevents LazyInitializationException on trip.driver.fullName etc.
     */
    @Transactional(readOnly = true)
    @Query(
            value = """
            SELECT t FROM Trip t
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.company
            LEFT JOIN FETCH t.car
            WHERE t.status = :status
            ORDER BY t.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM Trip t
            WHERE t.status = :status
            """
    )
    Page<Trip> findByStatusWithDetails(
            @Param("status") TripStatus status,
            Pageable pageable
    );

    /**
     * Trips by status paginated (no JOIN FETCH — legacy/simple usage).
     */
    @Transactional(readOnly = true)
    @Query(
            value = """
            SELECT t FROM Trip t
            WHERE t.status = :status
            ORDER BY t.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM Trip t
            WHERE t.status = :status
            """
    )
    Page<Trip> findByStatusPaginated(
            @Param("status") TripStatus status,
            Pageable pageable
    );

    // =========================================================
    // DRIVER QUERIES
    // =========================================================

    /**
     * Check if driver has any active trips.
     * Used to prevent adding multiple trips.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(t) > 0 FROM Trip t
        WHERE t.driver = :driver
        AND t.status IN ('PENDING', 'APPROVED', 'FULL')
        """)
    boolean driverHasActiveTrip(
            @Param("driver") User driver
    );

    /**
     * Driver's trips ordered by departure time.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM Trip t
        WHERE t.driver = :driver
        ORDER BY t.departureTime DESC
        """)
    List<Trip> findByDriverOrderByDepartureDesc(
            @Param("driver") User driver
    );

    // =========================================================
    // STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    long countByStatus(TripStatus status);

    /**
     * Count trips for a specific driver.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(t) FROM Trip t
        WHERE t.driver = :driver
        """)
    long countByDriver(@Param("driver") User driver);

    /**
     * Count trips for a specific company.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(t) FROM Trip t
        WHERE t.company = :company
        """)
    long countByCompany(
            @Param("company") Company company
    );

    /**
     * Total revenue from completed trips.
     * Admin financial dashboard.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COALESCE(SUM(t.tripAmount), 0)
        FROM Trip t
        WHERE t.status = 'COMPLETED'
        """)
    java.math.BigDecimal calculateTotalRevenue();

    // =========================================================
    // DELETE OPERATIONS
    // =========================================================

    @Modifying
    @Transactional
    void deleteByDriverId(Long driverId);

    /**
     * Delete all trips for a company.
     * Called when deleting a company account.
     */
    @Modifying
    @Transactional
    void deleteByCompanyId(Long companyId);
}