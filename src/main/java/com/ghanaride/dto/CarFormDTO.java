package com.ghanaride.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Car Form DTO for driver/company vehicle management.
 */
@Data
@NoArgsConstructor
public class CarFormDTO {

    @NotBlank(message = "Plate number is required")
    @Size(max = 20, message = "Plate number must be 20 characters or less")
    @Pattern(regexp = "^[A-Z0-9\\s-]+$", message = "Plate number can only contain letters, numbers, spaces, and hyphens")
    private String plateNumber;

    @NotBlank(message = "Car brand is required")
    @Size(max = 50, message = "Brand name must be 50 characters or less")
    private String carBrand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model name must be 50 characters or less")
    private String model;

    @Min(value = 1990, message = "Year must be 1990 or later")
    @Max(value = 2030, message = "Year cannot be in the future")
    private Integer year;

    @Size(max = 30, message = "Color must be 30 characters or less")
    private String color;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "At least 1 seat required")
    @Max(value = 50, message = "Maximum 50 seats")
    private Integer totalSeats;

    @Size(max = 20, message = "Fuel type must be 20 characters or less")
    private String fuelType;
}