package com.ghanaride.dto.response;

import com.ghanaride.entity.Trip;
import com.ghanaride.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {
    private Long id;
    private String fromLocation;
    private String toLocation;
    private String pickupStation;
    private String dropOffStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal tripAmount;
    private Integer availableSeats;
    private Integer totalSeats;
    private String description;
    private TripStatus status;
    private CarDTO car;
    private UserDTO driver;
    private LocalDateTime createdAt;
    private Long bookingsCount;

    public static TripDTO fromEntity(Trip trip) {
        return TripDTO.builder()
                .id(trip.getId())
                .fromLocation(trip.getFromLocation())
                .toLocation(trip.getToLocation())
                .pickupStation(trip.getPickupStation())
                .dropOffStation(trip.getDropOffStation())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .tripAmount(trip.getTripAmount())
                .availableSeats(trip.getAvailableSeats())
                .totalSeats(trip.getTotalSeats())
                .description(trip.getDescription())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .bookingsCount(trip.getBookings() != null ? (long) trip.getBookings().size() : 0L)
                .build();
    }
}
