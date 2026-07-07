package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Represents a vehicle used for trips.
 * Belongs to either an individual driver OR a company.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"driver", "company"})
@Entity
@Table(
        name = "cars",
        indexes = {
                @Index(name = "idx_car_number_plate",
                        columnList = "number_plate"),
                @Index(name = "idx_car_driver",
                        columnList = "driver_id"),
                @Index(name = "idx_car_company",
                        columnList = "company_id"),
                @Index(name = "idx_car_status",
                        columnList = "status")
        }
)
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Individual driver (null for company vehicles)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    // Company (null for individual driver vehicles)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(
            name = "car_brand",
            length = 100,
            nullable = false
    )
    private String carBrand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(
            name = "number_plate",
            length = 20,
            unique = true,
            nullable = false
    )
    private String numberPlate;

    @Column(name = "car_image_path")
    private String carImagePath;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "year")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            length = 20,
            nullable = false
    )
    private CarStatus status = CarStatus.APPROVED;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = CarStatus.APPROVED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // CONVENIENCE
    // =========================================================

    /**
     * Display name: "Toyota Camry (GR-1234-22)"
     */
    public String getDisplayName() {
        return carBrand +
                (model != null ? " " + model : "") +
                " (" + numberPlate + ")";
    }

    // =========================================================
    // EQUALS & HASHCODE (ID-based)
    // =========================================================
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