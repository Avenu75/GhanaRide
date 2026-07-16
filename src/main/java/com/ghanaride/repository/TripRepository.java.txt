package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByStatus(TripStatus status);
    List<Trip> findByDriverId(Long driverId);
    List<Trip> findByDriver(User driver);
    List<Trip> findByStatusAndAvailableSeatsGreaterThan(TripStatus status, int seats);
    long countByStatus(TripStatus status);

    // ===== FIND BY MULTIPLE STATUSES =====
    List<Trip> findByStatusIn(List<TripStatus> statuses);

    List<Trip> findByStatusAndDepartureTimeBefore(TripStatus status, java.time.LocalDateTime time);

    List<Trip> findByCompany(Company company);

    // ===== DELETE BY DRIVER =====
    @Modifying
    @Transactional
    void deleteByDriverId(Long driverId);
}