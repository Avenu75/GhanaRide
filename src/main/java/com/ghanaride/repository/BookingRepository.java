package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUser(User user);

    List<Booking> findByTripId(Long tripId);

    Optional<Booking> findByBookingReference(String bookingReference);

    long countByTripId(Long tripId);

    @Modifying
    @Transactional
    void deleteByTripId(Long tripId);
}