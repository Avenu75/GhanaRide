package com.ghanaride.service;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.BookingStatus;
import com.ghanaride.entity.Trip;
import com.ghanaride.entity.TripStatus;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripExpirationScheduler {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    // Run every 10 minutes
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void expireOldTrips() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        
        List<Trip> expiredTrips = tripRepository.findByStatusAndDepartureTimeBefore(TripStatus.APPROVED, twoHoursAgo);
        // Also check FULL trips that are past departure
        expiredTrips.addAll(tripRepository.findByStatusAndDepartureTimeBefore(TripStatus.FULL, twoHoursAgo));

        if (!expiredTrips.isEmpty()) {
            log.info("Found {} trips to expire.", expiredTrips.size());
            
            for (Trip trip : expiredTrips) {
                trip.setStatus(TripStatus.EXPIRED);
                trip.setExpiredAt(LocalDateTime.now());
                tripRepository.save(trip);

                // Also update associated active bookings to EXPIRED
                List<Booking> bookings = bookingRepository.findByTripId(trip.getId());
                for (Booking booking : bookings) {
                    if (booking.getStatus() == BookingStatus.ACTIVE || booking.getStatus() == BookingStatus.CONFIRMED) {
                        booking.setStatus(BookingStatus.EXPIRED);
                        bookingRepository.save(booking);
                    }
                }
            }
        }
    }
}
