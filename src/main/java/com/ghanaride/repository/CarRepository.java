package com.ghanaride.repository;
import com.ghanaride.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByDriver(User driver);
    List<Car> findByDriverId(Long driverId);
    List<Car> findByCompany(Company company);
    Optional<Car> findByNumberPlate(String numberPlate);
    boolean existsByNumberPlate(String numberPlate);
}
