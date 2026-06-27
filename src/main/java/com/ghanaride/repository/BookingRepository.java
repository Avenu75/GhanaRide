package com.ghanaride.repository;
import com.ghanaride.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUser(User user);
    List<Booking> findByTripId(Long tripId);
    Optional<Booking> findByBookingReference(String bookingReference);
    long countByTripId(Long tripId);
}
