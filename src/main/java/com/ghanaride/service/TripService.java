package com.ghanaride.service;

import com.ghanaride.entity.Trip;
import com.ghanaride.entity.TripStatus;
import com.ghanaride.entity.User;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public Trip saveTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    public List<Trip> findAllTrips() {
        return tripRepository.findAll();
    }

    public List<Trip> findApprovedTrips() {
        return tripRepository.findByStatusAndAvailableSeatsGreaterThan(TripStatus.APPROVED, 0);
    }

    public List<Trip> findPendingTrips() {
        return tripRepository.findByStatus(TripStatus.PENDING);
    }

    public List<Trip> findByDriverId(Long id) {
        return tripRepository.findByDriverId(id);
    }

    public List<Trip> findByDriver(User driver) {
        return tripRepository.findByDriver(driver);
    }

    public Optional<Trip> findById(Long id) {
        return tripRepository.findById(id);
    }

    public Trip approveTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus(TripStatus.APPROVED);
        return tripRepository.save(trip);
    }

    public Trip rejectTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus(TripStatus.REJECTED);
        return tripRepository.save(trip);
    }

    public long countByStatus(TripStatus status) {
        return tripRepository.countByStatus(status);
    }

    public long countAll() {
        return tripRepository.count();
    }

    // ===== DELETE TRIP =====
    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));
        bookingRepository.deleteByTripId(tripId);
        tripRepository.delete(trip);
    }

    // ===== MARK TRIP AS FULL =====
    @Transactional
    public Trip markAsFull(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus(TripStatus.FULL);
        trip.setAvailableSeats(0);
        return tripRepository.save(trip);
    }

    // ===== CHECK IF DRIVER HAS ACTIVE TRIP =====
    public boolean driverHasActiveTrip(User driver) {
        List<Trip> trips = tripRepository.findByDriver(driver);
        return trips.stream().anyMatch(t ->
                t.getStatus() == TripStatus.PENDING ||
                        t.getStatus() == TripStatus.APPROVED ||
                        t.getStatus() == TripStatus.FULL
        );
    }

    // ===== FIND ALL APPROVED AND FULL TRIPS FOR USER DASHBOARD =====
    public List<Trip> findApprovedAndFullTrips() {
        return tripRepository.findByStatusIn(
                List.of(TripStatus.APPROVED, TripStatus.FULL)
        );
    }
}