package com.ghanaride.service;

import com.ghanaride.entity.Car;
import com.ghanaride.entity.Company;
import com.ghanaride.entity.User;
import com.ghanaride.exception.ResourceNotFoundException;
import com.ghanaride.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Handles vehicle (car) management.
 * Used by both individual drivers and companies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    // =========================================================
    // SAVE / UPDATE
    // =========================================================
    @Transactional
    public Car saveCar(Car car) {
        // Normalize number plate
        if (car.getPlateNumber() != null) {
            car.setPlateNumber(
                    car.getPlateNumber().trim().toUpperCase()
            );
        }
        Car saved = carRepository.save(car);
        log.info("Car saved: plate={} driver={}",
                saved.getPlateNumber(),
                saved.getDriver() != null
                        ? saved.getDriver().getEmail() : "company"
        );
        return saved;
    }

    // =========================================================
    // QUERIES
    // =========================================================
    public List<Car> findByDriver(User driver) {
        return carRepository.findByDriver(driver);
    }

    public List<Car> findByCompany(Company company) {
        return carRepository.findByCompany(company);
    }

    public List<Car> findByDriverId(Long id) {
        return carRepository.findByDriverId(id);
    }

    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    public boolean existsByNumberPlate(String plate) {
        if (plate == null || plate.isBlank()) return false;
        return carRepository.existsByPlateNumber(
                plate.trim().toUpperCase()
        );
    }

    public List<Car> findAllCars() {
        return carRepository.findAll();
    }

    // =========================================================
    // DELETE
    // =========================================================
    @Transactional
    public void deleteCar(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Car", carId)
                );

        log.warn("Car deleted: id={} plate={}",
                carId, car.getPlateNumber());

        carRepository.delete(car);
    }
}