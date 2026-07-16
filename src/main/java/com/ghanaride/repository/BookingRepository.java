package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Booking Repository - All booking queries.
 */
@Repository
@Transactional(readOnly = true)
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // =========================================================
    // BASIC FINDERS
    // =========================================================

    Optional<Booking> findByBookingReference(String reference);

    List<Booking> findByUser(User user);

    Page<Booking> findByUser(User user, Pageable pageable);

    List<Booking> findByTrip(Trip trip);

    List<Booking> findByTripAndStatusIn(Trip trip, List<BookingStatus> statuses);

    boolean existsByUserAndTripAndStatusIn(User user, Trip trip, List<BookingStatus> statuses);

    // =========================================================
    // FETCH JOINED QUERIES (for view rendering)
    // =========================================================

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        LEFT JOIN FETCH b.user u
        WHERE b.user = :user
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByUserWithDetails(@Param("user") User user);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        WHERE b.user = :user
        ORDER BY b.bookingDate DESC
    """)
    Page<Booking> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        WHERE b.bookingReference = :ref
    """)
    Optional<Booking> findByReferenceWithDetails(@Param("ref") String reference);

    // =========================================================
    // STATUS-BASED QUERIES
    // =========================================================

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByStatusIn(List<BookingStatus> statuses);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        WHERE b.status IN :statuses
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByStatusInWithDetails(@Param("statuses") List<BookingStatus> statuses);

    // =========================================================
    // PAYMENT STATUS QUERIES
    // =========================================================

    List<Booking> findByPaymentStatus(PaymentStatus status);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        WHERE b.paymentStatus = :status
        ORDER BY b.paymentDate DESC
    """)
    List<Booking> findByPaymentStatusWithDetails(@Param("status") PaymentStatus status);

    // =========================================================
    // UPCOMING / PAST BOOKINGS
    // =========================================================

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        WHERE b.user = :user
        AND b.status NOT IN ('CANCELLED')
        AND t.departureTime > CURRENT_TIMESTAMP
        ORDER BY t.departureTime ASC
    """)
    List<Booking> findUpcomingBookings(@Param("user") User user);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        WHERE b.user = :user
        AND (b.status = 'CANCELLED' OR t.departureTime <= CURRENT_TIMESTAMP)
        ORDER BY t.departureTime DESC
    """)
    List<Booking> findPastBookings(@Param("user") User user);

    // =========================================================
    // DRIVER / COMPANY QUERIES
    // =========================================================

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.car ca
        LEFT JOIN FETCH b.user u
        WHERE t.driver = :driver
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByDriver(@Param("driver") User driver);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.car ca
        LEFT JOIN FETCH b.user u
        WHERE t.company = :company
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByCompany(@Param("company") Company company);

    // =========================================================
    // ADMIN / REPORTING
    // =========================================================

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        LEFT JOIN FETCH b.user u
        ORDER BY b.bookingDate DESC
    """)
    Page<Booking> findAllWithDetails(Pageable pageable);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        LEFT JOIN FETCH t.car ca
        LEFT JOIN FETCH b.user u
        WHERE b.paymentStatus = 'PAID'
        ORDER BY b.paymentDate DESC
    """)
    Page<Booking> findPaidBookingsWithDetails(Pageable pageable);

    // =========================================================
    // STATISTICS
    // =========================================================

    long countByUser(User user);

    long countByUserAndStatus(User user, BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.trip = :trip AND b.status != 'CANCELLED'")
    long countActiveByTrip(@Param("trip") Trip trip);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.paymentStatus = 'PAID'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.paymentStatus = 'PAID' AND b.paymentDate >= :since")
    BigDecimal getRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.user = :user AND b.paymentStatus = 'PAID'")
    BigDecimal getUserTotalSpent(@Param("user") User user);
}