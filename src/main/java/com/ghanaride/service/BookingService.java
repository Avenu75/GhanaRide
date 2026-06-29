package com.ghanaride.service;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.BookingStatus;
import com.ghanaride.entity.Trip;
import com.ghanaride.entity.User;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;

    @Transactional
    public Booking createBooking(User user, Trip trip) {
        if (trip.getAvailableSeats() <= 0) {
            throw new RuntimeException("No seats available");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("GR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        long currentBookings = bookingRepository.countByTripId(trip.getId());
        booking.setSeatNumber((int) currentBookings + 1);
        booking.setTotalAmount(trip.getTripAmount());

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        tripRepository.save(trip);

        return bookingRepository.save(booking);
    }

    public List<Booking> findByUser(User user) {
        return bookingRepository.findByUser(user);
    }

    public List<Booking> findByUserId(Long id) {
        return bookingRepository.findByUserId(id);
    }

    public List<Booking> findByTripId(Long tripId) {
        return bookingRepository.findByTripId(tripId);
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Optional<Booking> findByBookingReference(String ref) {
        return bookingRepository.findByBookingReference(ref);
    }

    public long countAll() {
        return bookingRepository.count();
    }

    // ===== DELETE BOOKING =====
    @Transactional
    public void deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        // Restore the seat back to the trip when booking is deleted
        Trip trip = booking.getTrip();
        if (trip != null) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
        }

        bookingRepository.delete(booking);
    }

    // ===== FIND ALL BOOKINGS =====
    public List<Booking> findAllBookings() {
        return bookingRepository.findAll();
    }

    // ===== CANCEL BOOKING =====
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        booking.setStatus(BookingStatus.CANCELLED);

        // Restore seat back to trip
        Trip trip = booking.getTrip();
        if (trip != null) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
        }

        bookingRepository.save(booking);
    }
}