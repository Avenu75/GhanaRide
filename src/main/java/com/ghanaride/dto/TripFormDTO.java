package com.ghanaride.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Form DTO for creating/editing trips (Driver).
 */
@Data
@NoArgsConstructor
public class TripFormDTO {

    @NotBlank(message = "Departure city is required")
    private String fromLocation;

    @NotBlank(message = "Destination city is required")
    private String toLocation;

    @NotBlank(message = "Pickup station is required")
    private String pickupStation;

    @NotNull(message = "Departure date and time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Trip amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least GH₵1.00")
    private BigDecimal tripAmount;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "At least 1 seat required")
    @Max(value = 50, message = "Maximum 50 seats allowed")
    private Integer totalSeats;

    private String description;

    // For image upload (handled separately via MultipartFile in controller)
    @jakarta.validation.constraints.NotNull(message = "Image is required")
    private MultipartFile imageFile;

    // For editing existing trips
    private Long tripId;

    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }
    public String getPickupStation() { return pickupStation; }
    public void setPickupStation(String pickupStation) { this.pickupStation = pickupStation; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public BigDecimal getTripAmount() { return tripAmount; }
    public void setTripAmount(BigDecimal tripAmount) { this.tripAmount = tripAmount; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
}