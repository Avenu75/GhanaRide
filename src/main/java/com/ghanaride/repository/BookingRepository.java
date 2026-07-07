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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Booking entity.
 *
 * Performance notes:
 * - All read queries use @Transactional(readOnly=true)
 *   → Hibernate skips dirty checking = faster
 * - findAllWithDetails() uses LEFT JOIN FETCH
 *   → Handles company trips where driver is null
 * - Counts use @Query instead of findAll().size()
 *   → Avoids loading entities just to count them
 */
@Repository
public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    // =========================================================
    // FIND BY USER
    // =========================================================

    /**
     * NOTE: kept for backwards compatibility, but this does NOT
     * fetch trip/driver/car. If a view needs to render
     * booking.trip.driver.fullName, booking.trip.car.carBrand,
     * etc., use findByUserWithDetails(...) instead, or you will
     * hit LazyInitializationException once the request-scoped
     * Hibernate session closes before rendering.
     */
    @Transactional(readOnly = true)
    List<Booking> findByUser(User user);

    /**
     * A user's bookings with trip + driver + car + company
     * eagerly fetched. Used by BookingController#myBookings and
     * #dashboard so my-bookings.html can safely read
     * booking.trip.driver.fullName / booking.trip.car.carBrand /
     * booking.trip.fromLocation etc. without hitting
     * LazyInitializationException.
     *
     * Do NOT switch callers back to the plain findByUser(...) —
     * that returns lazy proxies for trip/driver/car and will
     * throw LazyInitializationException during view rendering
     * (this is exactly what caused the /my-bookings 500).
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.car
        LEFT JOIN FETCH t.company
        WHERE b.user = :user
        ORDER BY b.bookingDate DESC
        """)
    List<Booking> findByUserWithDetails(
            @Param("user") User user
    );

    @Transactional(readOnly = true)
    List<Booking> findByUserId(Long userId);

    @Transactional(readOnly = true)
    Page<Booking> findByUser(User user, Pageable pageable);

    // =========================================================
    // FIND BY TRIP
    // =========================================================
    @Transactional(readOnly = true)
    List<Booking> findByTripId(Long tripId);

    // =========================================================
    // FIND BY REFERENCE
    // =========================================================
    @Transactional(readOnly = true)
    Optional<Booking> findByBookingReference(
            String bookingReference
    );

    // =========================================================
    // FIND BY ID (single booking, with details)
    // =========================================================

    /**
     * Single booking by id with trip + driver + car + company
     * eagerly fetched. Used by BookingController#viewReceipt so
     * receipt.html can safely read booking.trip.driver.fullName /
     * booking.trip.driver.phoneNumber / booking.trip.car.carBrand
     * without hitting LazyInitializationException.
     *
     * Do NOT switch the receipt page back to the plain
     * findById(...) inherited from JpaRepository — that returns
     * lazy proxies for trip/driver/car and will throw
     * LazyInitializationException during view rendering (this is
     * exactly what would cause a /booking/receipt/{id} 500).
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver
        LEFT JOIN FETCH t.car
        LEFT JOIN FETCH t.company
        WHERE b.id = :id
        """)
    Optional<Booking> findByIdWithDetails(
            @Param("id") Long id
    );

    // =========================================================
    // EXISTENCE CHECKS
    // =========================================================

    /**
     * Check if user has active booking of specific type.
     * Used to enforce one-active-booking rule.
     */
    @Transactional(readOnly = true)
    boolean existsByUserAndBookingTypeAndStatusIn(
            User user,
            BookingType bookingType,
            List<BookingStatus> statuses
    );

    /**
     * Check if user already booked a specific trip.
     * Prevents double-booking same trip.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.user = :user
        AND b.trip = :trip
        AND b.status != 'CANCELLED'
        """)
    boolean existsByUserAndTrip(
            @Param("user") User user,
            @Param("trip") Trip trip
    );

    // =========================================================
    // COUNTS
    // =========================================================

    @Transactional(readOnly = true)
    long countByTripId(Long tripId);

    /**
     * Count all bookings for a user.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.user = :user
        """)
    long countByUser(@Param("user") User user);

    /**
     * Count completed bookings for a user.
     * A booking is "completed" when its trip is COMPLETED.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.user = :user
        AND b.trip.status = :tripStatus
        """)
    long countByUserAndTripStatus(
            @Param("user") User user,
            @Param("tripStatus") TripStatus tripStatus
    );

    /**
     * Count all bookings where trip has given status.
     * Used for homepage stats (total completed rides).
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.trip.status = :tripStatus
        AND b.status != 'CANCELLED'
        """)
    long countByTripStatus(
            @Param("tripStatus") TripStatus tripStatus
    );

    // =========================================================
    // ADMIN: FIND ALL WITH DETAILS
    // Uses LEFT JOIN FETCH to handle:
    // - Company trips (driver is null)
    // - Trips without car images
    // FIX: Was INNER JOIN which failed for company trips
    // =========================================================
    @Transactional(readOnly = true)
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.trip t
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.company c
        JOIN FETCH t.car car
        ORDER BY b.bookingDate DESC
        """)
    List<Booking> findAllWithDetails();

    /**
     * Paginated version for admin panel.
     *
     * NOTE: Cannot use JOIN FETCH with pagination
     * (Hibernate warns about HHH90003004).
     * Use separate queries or @EntityGraph instead.
     */
    @Transactional(readOnly = true)
    @Query(
            value = """
            SELECT b FROM Booking b
            ORDER BY b.bookingDate DESC
            """,
            countQuery = "SELECT COUNT(b) FROM Booking b"
    )
    Page<Booking> findAllBookingsPaginated(
            Pageable pageable
    );

    /**
     * Recent bookings for admin dashboard feed.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.trip
        ORDER BY b.bookingDate DESC
        """)
    List<Booking> findRecentBookings(Pageable pageable);

    // =========================================================
    // DELETE OPERATIONS (write — no readOnly)
    // =========================================================

    /**
     * Delete all bookings for a trip.
     * Called before deleting a trip (FK constraint).
     */
    @Modifying
    @Transactional
    void deleteByTripId(Long tripId);

    /**
     * Delete all bookings for a user.
     * Called before deleting a user account.
     */
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    // =========================================================
    // PAYMENT QUERIES
    // =========================================================

    /**
     * Find bookings by payment status.
     * Useful for admin reconciliation.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.paymentStatus = :paymentStatus
        ORDER BY b.bookingDate DESC
        """)
    List<Booking> findByPaymentStatus(
            @Param("paymentStatus") PaymentStatus paymentStatus
    );

    /**
     * Find bookings with payment reference.
     */
    @Transactional(readOnly = true)
    Optional<Booking> findByTransactionReference(
            String transactionReference
    );

    // =========================================================
    // STATISTICS QUERIES
    // =========================================================

    /**
     * Total revenue from paid bookings.
     * Used for admin financial dashboard.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0)
        FROM Booking b
        WHERE b.paymentStatus = 'PAID'
        """)
    java.math.BigDecimal calculateTotalRevenue();

    /**
     * Revenue for a specific driver.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0)
        FROM Booking b
        WHERE b.trip.driver = :driver
        AND b.paymentStatus = 'PAID'
        """)
    java.math.BigDecimal calculateDriverRevenue(
            @Param("driver") User driver
    );
}