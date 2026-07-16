package com.ghanaride.service;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.exception.*;
import com.ghanaride.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seat Service - Seat management for trips.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatMapRepository seatMapRepository;
    private final TripRepository tripRepository;

    // =========================================================
    // SEAT MAP GENERATION
    // =========================================================

    @Transactional
    public List<SeatMap> ensureSeatMap(Trip trip) {
        List<SeatMap> existing = seatMapRepository.findByTripIdOrderByRowNumberAscColumnLabelAsc(trip.getId());
        if (!existing.isEmpty()) return existing;

        int totalSeats = trip.getTotalSeats() != null ? trip.getTotalSeats() : 18;
        // Ghana trotro / bus: 4 per row (2-2), last row 5
        String[] cols = {"A","B","C","D"};
        List<SeatMap> seats = new ArrayList<>();
        int seatCount = 0;
        int row = 1;
        while (seatCount < totalSeats) {
            for (String col : cols) {
                if (seatCount >= totalSeats) break;
                SeatMap.SeatType type = col.equals("A") || col.equals("D") ? SeatMap.SeatType.WINDOW : SeatMap.SeatType.AISLE;
                boolean extraLegroom = row == 1;
                seats.add(SeatMap.builder()
                    .trip(trip)
                    .seatNumber(row + col)
                    .rowNumber(row)
                    .columnLabel(col)
                    .seatType(type)
                    .extraLegroom(extraLegroom)
                    .status(SeatMap.SeatStatus.AVAILABLE)
                    .build());
                seatCount++;
            }
            row++;
        }
        return seatMapRepository.saveAll(seats);
    }

    public List<SeatMap> getSeatMap(Long tripId) {
        return seatMapRepository.findByTripIdOrderByRowNumberAscColumnLabelAsc(tripId);
    }

    // =========================================================
    // SEAT OPERATIONS
    // =========================================================

    public Optional<String> getFirstAvailableSeat(Long tripId) {
        return seatMapRepository.findByTripIdOrderByRowNumberAscColumnLabelAsc(tripId)
            .stream()
            .filter(s -> s.getStatus() == SeatMap.SeatStatus.AVAILABLE)
            .map(SeatMap::getSeatNumber)
            .findFirst();
    }

    @Transactional
    public boolean holdSeat(Long tripId, String seatNumber, Booking booking, int holdMinutes) {
        var seatOpt = seatMapRepository.findByTripIdAndSeatNumber(tripId, seatNumber);
        if (seatOpt.isEmpty()) return false;
        SeatMap seat = seatOpt.get();
        if (seat.getStatus() != SeatMap.SeatStatus.AVAILABLE) return false;

        seat.setStatus(SeatMap.SeatStatus.HELD);
        seat.setHeldBy(booking);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(holdMinutes));
        seatMapRepository.save(seat);
        return true;
    }

    @Transactional
    public void confirmSeat(Long tripId, String seatNumber) {
        seatMapRepository.findByTripIdAndSeatNumber(tripId, seatNumber).ifPresent(seat -> {
            seat.setStatus(SeatMap.SeatStatus.BOOKED);
            seat.setHeldBy(null);
            seat.setHoldExpiresAt(null);
            seatMapRepository.save(seat);
        });
    }

    @Transactional
    public void releaseSeat(Long tripId, String seatNumber) {
        seatMapRepository.findByTripIdAndSeatNumber(tripId, seatNumber).ifPresent(seat -> {
            seat.setStatus(SeatMap.SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHoldExpiresAt(null);
            seatMapRepository.save(seat);
        });
    }

    @Scheduled(fixedRate = 60000) // every 1 min
    @Transactional
    public void releaseExpired() {
        int n = seatMapRepository.releaseExpiredHolds(LocalDateTime.now());
        if (n > 0) log.info("Released {} expired seat holds", n);
    }
}