package com.ghanaride.repository;

import com.ghanaride.entity.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatMapRepository extends JpaRepository<SeatMap, Long> {
    List<SeatMap> findByTripIdOrderByRowNumberAscColumnLabelAsc(Long tripId);
    Optional<SeatMap> findByTripIdAndSeatNumber(Long tripId, String seatNumber);
    long countByTripIdAndStatus(Long tripId, SeatMap.SeatStatus status);

    @Modifying
    @Query("update SeatMap s set s.status='AVAILABLE', s.heldBy=null, s.holdExpiresAt=null where s.status='HELD' and s.holdExpiresAt < ?1")
    int releaseExpiredHolds(LocalDateTime now);
}
