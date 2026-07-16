package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.ghanaride.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Trip entity.
 * 
 * CRITICAL: All view-rendering queries MUST use JOIN FETCH for driver, company, car
 * to prevent LazyInitializationException when Thymeleaf accesses lazy associations
 * after the Hibernate session closes.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // =========================================================
    // BASIC FINDERS (legacy - use with caution)
    // =========================================================
    
    @Transactional(readOnly = true)
    List<Trip> findByStatus(TripStatus status);

    @Transactional(readOnly = true)
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    /** 
     * WARNING: Does NOT fetch driver/company/car. 
     * Only use for internal service logic, NOT for view rendering.
     */
    @Transactional(readOnly = true)
    List<Trip> findByStatusIn(List<TripStatus> statuses);

    @Transactional(readOnly = true)
    List<Trip> findByStatusAndAvailableSeatsGreaterThan(TripStatus status, int seats);

    @Transactional(readOnly = true)
    List<Trip> findByDriver(User driver);

    @Transactional(readOnly = true)
    List<Trip> findByCompany(Company company);

    @Transactional(readOnly = true)
    List<Trip> findByDriverId(Long driverId);

    // =========================================================
    // UPCOMING TRIPS - Public browse page (FIXED: with JOIN FETCH)
    // =========================================================

    /**
     * All upcoming approved trips with available seats.
     * Uses JOIN FETCH so template can safely access trip.driver, trip.company, trip.car
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        LEFT JOIN FETCH t.car
        WHERE t.status = 'APPROVED'
        AND t.availableSeats > 0
        AND t.departureTime > CURRENT_TIMESTAMP
        ORDER BY t.departureTime ASC
    """)
    List<Trip> findApprovedUpcomingWithDetails();

    /**
     * Preview for homepage (limited number)
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        LEFT JOIN FETCH t.car
        WHERE t.status = 'APPROVED'
        AND t.availableSeats > 0
        AND t.departureTime > CURRENT_TIMESTAMP
        ORDER BY t.departureTime ASC
    """)
    List<Trip> findApprovedUpcomingPreview(int limit);

    // =========================================================
    // PASSENGER DASHBOARD - Approved + Full trips (FIXED)
    // =========================================================
    
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        LEFT JOIN FETCH t.car
        WHERE t.status IN :statuses
        ORDER BY t.departureTime ASC
    """)
    List<Trip> findByStatusInWithDetails(@Param("statuses") List<TripStatus> statuses);

    // =========================================================
    // SEARCH - Flexible filters with JOIN FETCH
    // =========================================================

    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.car
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        WHERE t.status = 'APPROVED'
        AND t.availableSeats > 0
        AND t.departureTime > CURRENT_TIMESTAMP
        AND (:from IS NULL OR :from = '' OR LOWER(t.fromLocation) LIKE LOWER(CONCAT('%', :from, '%')))
        AND (:to IS NULL OR :to = '' OR LOWER(t.toLocation) LIKE LOWER(CONCAT('%', :to, '%')))
        AND (:startDate IS NULL OR t.departureTime >= :startDate)
        AND (:endDate IS NULL OR t.departureTime <= :endDate)
        ORDER BY t.departureTime ASC
    """)
    List<Trip> searchTrips(
        @Param("from") String from,
        @Param("to") String to,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // =========================================================
    // ADMIN PAGINATED QUERIES (with JOIN FETCH + countQuery)
    // =========================================================

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
        countQuery = "SELECT COUNT(t) FROM Trip t WHERE t.status = :status"
    )
    Page<Trip> findByStatusWithDetails(@Param("status") TripStatus status, Pageable pageable);

    @Transactional(readOnly = true)
    @Query(
        value = """
            SELECT t FROM Trip t
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.company
            LEFT JOIN FETCH t.car
            WHERE t.driver = :driver
            ORDER BY t.departureTime DESC
        """,
        countQuery = "SELECT COUNT(t) FROM Trip t WHERE t.driver = :driver"
    )
    Page<Trip> findByDriverWithDetails(@Param("driver") User driver, Pageable pageable);

    @Transactional(readOnly = true)
    @Query(
        value = """
            SELECT t FROM Trip t
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.company
            LEFT JOIN FETCH t.car
            WHERE t.company = :company
            ORDER BY t.departureTime DESC
        """,
        countQuery = "SELECT COUNT(t) FROM Trip t WHERE t.company = :company"
    )
    Page<Trip> findByCompanyWithDetails(@Param("company") Company company, Pageable pageable);

    // =========================================================
    // RECENT TRIPS - Admin dashboard
    // =========================================================
    
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        LEFT JOIN FETCH t.car
        ORDER BY t.createdAt DESC
    """)
    List<Trip> findRecentTripsWithDetails(int limit);

    // =========================================================
    // DRIVER QUERIES
    // =========================================================

    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(t) > 0 FROM Trip t
        WHERE t.driver = :driver
        AND t.status IN ('PENDING', 'APPROVED', 'FULL')
    """)
    boolean driverHasActiveTrip(@Param("driver") User driver);

    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM Trip t
        LEFT JOIN FETCH t.car
        WHERE t.driver = :driver
        ORDER BY t.departureTime DESC
    """)
    List<Trip> findByDriverOrderByDepartureDesc(@Param("driver") User driver);

    // =========================================================
    // STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    long countByStatus(TripStatus status);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver = :driver")
    long countByDriver(@Param("driver") User driver);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.company = :company")
    long countByCompany(@Param("company") Company company);

    @Transactional(readOnly = true)
    @Query("""
        SELECT COALESCE(SUM(t.tripAmount), 0)
        FROM Trip t
        WHERE t.status = 'COMPLETED'
    """)
    BigDecimal calculateTotalRevenue();

    // =========================================================
    // DELETE OPERATIONS
    // =========================================================

    @Modifying
    @Transactional
    void deleteByDriverId(Long driverId);

    @Modifying
    @Transactional
    void deleteByCompanyId(Long companyId);

    // =========================================================
    // EXPIRATION SCHEDULER
    // =========================================================

    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM Trip t
        WHERE t.status IN ('APPROVED', 'FULL')
        AND t.departureTime < :before
    """)
    List<Trip> findByStatusAndDepartureTimeBefore(
        @Param("before") LocalDateTime before
    );

    @Transactional(readOnly = true)
    List<Trip> findByStatusAndDepartureTimeBetween(
        TripStatus status, 
        LocalDateTime start, 
        LocalDateTime end
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE Trip t SET t.status = 'EXPIRED', t.updatedAt = CURRENT_TIMESTAMP
        WHERE t.status IN ('APPROVED', 'FULL') AND t.departureTime < :before
    """)
    int expireTripsBefore(@Param("before") LocalDateTime before);

    int countByDriverAndStatusIn(User driver, List<TripStatus> pending);
}