package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Car/Vehicle entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"driver", "company", "trips", "seatMaps"})
@Entity
@Table(
    name = "cars",
    indexes = {
        @Index(name = "idx_car_driver", columnList = "driver_id"),
        @Index(name = "idx_car_company", columnList = "company_id"),
        @Index(name = "idx_car_plate", columnList = "plate_number", unique = true),
        @Index(name = "idx_car_status", columnList = "status")
    }
)
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "plate_number", length = 20, unique = true, nullable = false)
    private String plateNumber;

    @Column(name = "car_brand", length = 50, nullable = false)
    private String carBrand;

    @Column(name = "model", length = 50, nullable = false)
    private String model;

    @Column(name = "year")
    private Integer year;

    @Column(name = "color", length = 30)
    private String color;

    @Builder.Default
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats = 18;

    @Column(name = "vin", length = 50)
    private String vin;

    @Column(name = "chassis_number", length = 50)
    private String chassisNumber;

    @Column(name = "engine_number", length = 50)
    private String engineNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CarStatus status = CarStatus.ACTIVE;

    @Column(name = "roadworthy_expiry")
    private LocalDateTime roadworthyExpiry;

    @Column(name = "insurance_expiry")
    private LocalDateTime insuranceExpiry;

    @Column(name = "last_inspection_date")
    private LocalDateTime lastInspectionDate;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatMap> seatMaps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = CarStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getDisplayName() {
        return carBrand + " " + model + " (" + plateNumber + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Car other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}