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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Seat Map Repository - Seat management queries.
 */
@Repository
@Transactional(readOnly = true)
public interface SeatMapRepository extends JpaRepository<SeatMap, Long> {

    List<SeatMap> findByTripIdOrderByRowNumberAscColumnLabelAsc(Long tripId);

    Optional<SeatMap> findByTripIdAndSeatNumber(Long tripId, String seatNumber);

    List<SeatMap> findByTripIdAndStatus(Long tripId, SeatMap.SeatStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE SeatMap s SET s.status = 'AVAILABLE', s.heldBy = null, s.holdExpiresAt = null WHERE s.status = 'HELD' AND s.holdExpiresAt < :now")
    int releaseExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM SeatMap s WHERE s.trip.id = :tripId AND s.status = 'AVAILABLE'")
    long countAvailableSeats(@Param("tripId") Long tripId);

    @Query("SELECT COUNT(s) FROM SeatMap s WHERE s.trip.id = :tripId AND s.status = 'BOOKED'")
    long countBookedSeats(@Param("tripId") Long tripId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SeatMap WHERE trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);
}