package com.ghanaride.repository;
import com.ghanaride.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByStatus(TripStatus status);
    List<Trip> findByDriverId(Long driverId);
    List<Trip> findByDriver(User driver);
    List<Trip> findByStatusAndAvailableSeatsGreaterThan(TripStatus status, int seats);
    long countByStatus(TripStatus status);
}
