package com.ghanaride.service;

import com.ghanaride.entity.Car;
import com.ghanaride.entity.User;
import com.ghanaride.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
