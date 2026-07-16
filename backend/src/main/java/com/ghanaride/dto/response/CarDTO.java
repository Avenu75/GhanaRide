package com.ghanaride.dto.response;

import com.ghanaride.entity.Car;
import com.ghanaride.entity.CarStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarDTO {
    private Long id;
    private String plateNumber;
    private String carBrand;
    private String model;
    private Integer year;
    private String color;
    private Integer totalSeats;
    private CarStatus status;
    private String imagePath;
}
