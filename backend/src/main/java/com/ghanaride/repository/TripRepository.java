package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Transactional(readOnly = true)
    List<Trip> findByStatus(TripStatus status);

    @Transactional(readOnly = true)
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

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

    // ---- FIXED: Preview with Pageable + default int overload ----
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
    List<Trip> findApprovedUpcomingPreview(Pageable pageable);

    default List<Trip> findApprovedUpcomingPreview(int limit) {
        return findApprovedUpcomingPreview(PageRequest.of(0, limit));
    }

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
    @Query("""
        SELECT COUNT(t) > 0 FROM Trip t
        WHERE t.driver = :driver
        AND t.status IN ('PENDING', 'APPROVED', 'FULL')
    """)
    boolean driverHasActiveTrip(@Param("driver") User driver);

    @Transactional(readOnly = true)
    long countByStatus(TripStatus status);

    @Modifying
    @Transactional
    void deleteByDriverId(Long driverId);

    @Modifying
    @Transactional
    void deleteByCompanyId(Long companyId);

    // ---- RECENT TRIPS (FIXED: Pageable + int overload) ----
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.company
        LEFT JOIN FETCH t.car
        ORDER BY t.createdAt DESC
    """)
    List<Trip> findRecentTripsWithDetails(Pageable pageable);

    default List<Trip> findRecentTripsWithDetails(int limit) {
        return findRecentTripsWithDetails(PageRequest.of(0, limit));
    }

    // ---- EXPIRATION QUERIES (FIXED: Both overloads working) ----
    // Single param version used by new code
    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM Trip t
        WHERE t.status IN ('APPROVED', 'FULL')
        AND t.departureTime < :before
    """)
    List<Trip> findByStatusAndDepartureTimeBefore(
            @Param("before") LocalDateTime before
    );

    // Two param version for legacy scheduler - NOW FIXED
    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM Trip t
        WHERE t.status = :status
        AND t.departureTime < :before
    """)
    List<Trip> findByStatusAndDepartureTimeBefore(
            @Param("status") TripStatus status,
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
