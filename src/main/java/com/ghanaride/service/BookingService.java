package com.ghanaride.service;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.BookingStatus;
import com.ghanaride.entity.PaymentMethod;
import com.ghanaride.entity.PaymentStatus;
import com.ghanaride.entity.Trip;
import com.ghanaride.entity.User;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        booking.setPaymentStatus(PaymentStatus.PENDING); // Default until payment is made
        
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

    // ===== CAN USER CANCEL? (within 2 minutes of booking) =====
    public boolean canCancelBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) return false;
        if (booking.getBookingDate() == null) return false;
        return LocalDateTime.now().isBefore(
                booking.getBookingDate().plusMinutes(2)
        );
    }

    // ===== CANCEL BOOKING =====
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!canCancelBooking(booking)) {
            throw new RuntimeException(
                    "Cancellation window has expired. You can only cancel within 2 minutes of booking.");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Trip trip = booking.getTrip();
        if (trip != null) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
        }

        bookingRepository.save(booking);
    }

    // ===== DELETE BOOKING =====
    @Transactional
    public void deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Booking not found with id: " + bookingId));

        Trip trip = booking.getTrip();
        if (trip != null && booking.getStatus() != BookingStatus.CANCELLED) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
        }

        bookingRepository.delete(booking);
    }

    // ===== FIND ALL BOOKINGS WITH DETAILS =====
    public List<Booking> findAllBookings() {
        return bookingRepository.findAllWithDetails();
    }
}