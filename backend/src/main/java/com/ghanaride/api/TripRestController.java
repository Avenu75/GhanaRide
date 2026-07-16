package com.ghanaride.api;

import com.ghanaride.dto.response.ApiResponse;
import com.ghanaride.dto.response.TripDTO;
import com.ghanaride.entity.Trip;
import com.ghanaride.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
public class TripRestController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        var pageable = PageRequest.of(page, size);
        Page<Trip> trips = status != null ? tripService.findByStatus(com.ghanaride.entity.TripStatus.valueOf(status), pageable) : tripService.findAllTrips(pageable);
        Page<TripDTO> dtoPage = trips.map(t -> {
            TripDTO dto = TripDTO.fromEntity(t);
            // enrich car info lazily to avoid N+1? Simplified
            return dto;
        });
        return ResponseEntity.ok(ApiResponse.success(dtoPage));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TripDTO>>> search(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String date
    ) {
        List<Trip> trips = tripService.searchAvailableTrips(from, to, date);
        List<TripDTO> dtos = trips.stream().map(trip -> {
            TripDTO dto = TripDTO.fromEntity(trip);
            if (trip.getCar() != null) {
                dto.setCar(com.ghanaride.dto.response.CarDTO.builder()
                        .id(trip.getCar().getId())
                        .plateNumber(trip.getCar().getPlateNumber())
                        .carBrand(trip.getCar().getCarBrand())
                        .model(trip.getCar().getModel())
                        .totalSeats(trip.getCar().getTotalSeats())
                        .status(trip.getCar().getStatus())
                        .imagePath(trip.getCar().getImagePath())
                        .build());
            }
            if (trip.getDriver() != null) {
                dto.setDriver(com.ghanaride.dto.response.UserDTO.fromEntity(trip.getDriver()));
            }
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripDTO>> getTrip(@PathVariable Long id) {
        return tripService.findById(id)
                .map(trip -> ResponseEntity.ok(ApiResponse.success(TripDTO.fromEntity(trip))))
                .orElse(ResponseEntity.notFound().build());
    }
}
