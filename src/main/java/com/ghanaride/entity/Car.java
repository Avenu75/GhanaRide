package com.ghanaride.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vehicle entity.
 */
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

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats = 18;

    @Column(name = "vin", length = 50)
    private String vin;

    @Column(name = "chassis_number", length = 50)
    private String chassisNumber;

    @Column(name = "engine_number", length = 50)
    private String engineNumber;

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

    public Car() {
    }

    public Car(Long id, User driver, Company company, String plateNumber, String carBrand, String model, Integer year,
               String color, Integer totalSeats, String vin, String chassisNumber, String engineNumber,
               CarStatus status, LocalDateTime roadworthyExpiry, LocalDateTime insuranceExpiry,
               LocalDateTime lastInspectionDate, String imagePath, String description, List<Trip> trips,
               List<SeatMap> seatMaps, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.driver = driver;
        this.company = company;
        this.plateNumber = plateNumber;
        this.carBrand = carBrand;
        this.model = model;
        this.year = year;
        this.color = color;
        this.totalSeats = totalSeats;
        this.vin = vin;
        this.chassisNumber = chassisNumber;
        this.engineNumber = engineNumber;
        this.status = status;
        this.roadworthyExpiry = roadworthyExpiry;
        this.insuranceExpiry = insuranceExpiry;
        this.lastInspectionDate = lastInspectionDate;
        this.imagePath = imagePath;
        this.description = description;
        this.trips = trips;
        this.seatMaps = seatMaps;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Car car = new Car();

        public Builder id(Long id) { car.setId(id); return this; }
        public Builder driver(User driver) { car.setDriver(driver); return this; }
        public Builder company(Company company) { car.setCompany(company); return this; }
        public Builder plateNumber(String plateNumber) { car.setPlateNumber(plateNumber); return this; }
        public Builder carBrand(String carBrand) { car.setCarBrand(carBrand); return this; }
        public Builder model(String model) { car.setModel(model); return this; }
        public Builder year(Integer year) { car.setYear(year); return this; }
        public Builder color(String color) { car.setColor(color); return this; }
        public Builder totalSeats(Integer totalSeats) { car.setTotalSeats(totalSeats); return this; }
        public Builder vin(String vin) { car.setVin(vin); return this; }
        public Builder chassisNumber(String chassisNumber) { car.setChassisNumber(chassisNumber); return this; }
        public Builder engineNumber(String engineNumber) { car.setEngineNumber(engineNumber); return this; }
        public Builder status(CarStatus status) { car.setStatus(status); return this; }
        public Builder roadworthyExpiry(LocalDateTime roadworthyExpiry) { car.setRoadworthyExpiry(roadworthyExpiry); return this; }
        public Builder insuranceExpiry(LocalDateTime insuranceExpiry) { car.setInsuranceExpiry(insuranceExpiry); return this; }
        public Builder lastInspectionDate(LocalDateTime lastInspectionDate) { car.setLastInspectionDate(lastInspectionDate); return this; }
        public Builder imagePath(String imagePath) { car.setImagePath(imagePath); return this; }
        public Builder description(String description) { car.setDescription(description); return this; }
        public Builder trips(List<Trip> trips) { car.setTrips(trips); return this; }
        public Builder seatMaps(List<SeatMap> seatMaps) { car.setSeatMaps(seatMaps); return this; }
        public Builder createdAt(LocalDateTime createdAt) { car.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { car.setUpdatedAt(updatedAt); return this; }
        public Car build() { return car; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getCarBrand() { return carBrand; }
    public void setCarBrand(String carBrand) { this.carBrand = carBrand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getChassisNumber() { return chassisNumber; }
    public void setChassisNumber(String chassisNumber) { this.chassisNumber = chassisNumber; }
    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }
    public CarStatus getStatus() { return status; }
    public void setStatus(CarStatus status) { this.status = status; }
    public LocalDateTime getRoadworthyExpiry() { return roadworthyExpiry; }
    public void setRoadworthyExpiry(LocalDateTime roadworthyExpiry) { this.roadworthyExpiry = roadworthyExpiry; }
    public LocalDateTime getInsuranceExpiry() { return insuranceExpiry; }
    public void setInsuranceExpiry(LocalDateTime insuranceExpiry) { this.insuranceExpiry = insuranceExpiry; }
    public LocalDateTime getLastInspectionDate() { return lastInspectionDate; }
    public void setLastInspectionDate(LocalDateTime lastInspectionDate) { this.lastInspectionDate = lastInspectionDate; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Trip> getTrips() { return trips; }
    public void setTrips(List<Trip> trips) { this.trips = trips; }
    public List<SeatMap> getSeatMaps() { return seatMaps; }
    public void setSeatMaps(List<SeatMap> seatMaps) { this.seatMaps = seatMaps; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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