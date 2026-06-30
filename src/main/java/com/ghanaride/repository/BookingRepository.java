package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUser(User user);
    
    boolean existsByUserAndBookingTypeAndStatusIn(User user, BookingType bookingType, List<BookingStatus> statuses);

    List<Booking> findByTripId(Long tripId);

    Optional<Booking> findByBookingReference(String bookingReference);

    long countByTripId(Long tripId);

    @Modifying
    @Transactional
    void deleteByTripId(Long tripId);

    // ===== FETCH ALL BOOKINGS WITH RELATED DATA =====
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.user " +
            "JOIN FETCH b.trip t " +
            "JOIN FETCH t.driver " +
            "JOIN FETCH t.car")
    List<Booking> findAllWithDetails();
}