package com.ghanaride.service;

import com.ghanaride.entity.Car;
import com.ghanaride.entity.User;
import com.ghanaride.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    public Car saveCar(Car car) {
        return carRepository.save(car);
    }

    public List<Car> findByDriver(User driver) {
        return carRepository.findByDriver(driver);
    }

    public List<Car> findByDriverId(Long id) {
        return carRepository.findByDriverId(id);
    }

    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    public boolean existsByNumberPlate(String plate) {
        return carRepository.existsByNumberPlate(plate);
    }

    // ===== DELETE CAR =====
    @Transactional
    public void deleteCar(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + carId));
        carRepository.delete(car);
    }

    // ===== FIND ALL CARS =====
    public List<Car> findAllCars() {
        return carRepository.findAll();
    }
}