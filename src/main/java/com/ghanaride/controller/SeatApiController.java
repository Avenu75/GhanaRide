package com.ghanaride.controller;

import com.ghanaride.entity.SeatMap;
import com.ghanaride.entity.Trip;
import com.ghanaride.repository.TripRepository;
import com.ghanaride.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class SeatApiController {

    private final SeatService seatService;
    private final TripRepository tripRepository;

    @GetMapping("/{tripId}/seats")
    public ResponseEntity<?> seats(@PathVariable Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) return ResponseEntity.notFound().build();
        List<SeatMap> map = seatService.ensureSeatMap(trip);
        var dto = map.stream().map(s -> Map.of(
                "seatNumber", s.getSeatNumber(),
                "row", s.getRowNumber(),
                "col", s.getColumnLabel(),
                "type", s.getSeatType().name(),
                "status", s.getStatus().name(),
                "extraLegroom", s.getExtraLegroom()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "tripId", tripId,
                "totalSeats", trip.getTotalSeats(),
                "available", map.stream().filter(x -> x.getStatus() == SeatMap.SeatStatus.AVAILABLE).count(),
                "seats", dto
        ));
    }

    @GetMapping("/{tripId}/live")
    public ResponseEntity<?> live(@PathVariable Long tripId) {
        // stub live GPS – world class would hook to driver app WebSocket
        return ResponseEntity.ok(Map.of(
                "tripId", tripId,
                "status", "EN_ROUTE",
                "lat", 5.6037,
                "lng", -0.1870,
                "speed_kmh", 78,
                "eta_minutes", 42,
                "next_stop", "Nkawkaw",
                "updated_at", java.time.Instant.now().toString()
        ));
    }
}
