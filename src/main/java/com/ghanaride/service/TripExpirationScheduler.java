package com.ghanaride.service;

import com.ghanaride.entity.*;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled job that expires old trips.
 *
 * Runs every 10 minutes and marks trips as EXPIRED
 * if their departure time was more than 2 hours ago
 * and they are still in APPROVED or FULL status.
 *
 * Also marks associated bookings as EXPIRED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripExpirationScheduler {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    // =========================================================
    // EXPIRE OLD TRIPS
    // Runs every 10 minutes
    // =========================================================
    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void expireOldTrips() {
        // Trips departed more than 2 hours ago
        // are considered expired
        LocalDateTime cutoff =
                LocalDateTime.now().minusHours(2);

        List<Trip> expiredTrips = new ArrayList<>();
        expiredTrips.addAll(
                tripRepository.findByStatusAndDepartureTimeBefore(
                        TripStatus.APPROVED, cutoff
                )
        );
        expiredTrips.addAll(
                tripRepository.findByStatusAndDepartureTimeBefore(
                        TripStatus.FULL, cutoff
                )
        );

        if (expiredTrips.isEmpty()) return;

        log.info(
                "Expiring {} trips past their departure time",
                expiredTrips.size()
        );

        for (Trip trip : expiredTrips) {
            try {
                trip.setStatus(TripStatus.EXPIRED);
                trip.setExpiredAt(LocalDateTime.now());
                tripRepository.save(trip);

                // Expire associated active bookings
                List<Booking> bookings =
                        bookingRepository.findByTripId(
                                trip.getId()
                        );

                List<Booking> toExpire = bookings.stream()
                        .filter(b ->
                                b.getStatus() ==
                                        BookingStatus.ACTIVE ||
                                        b.getStatus() ==
                                                BookingStatus.CONFIRMED
                        )
                        .toList();

                toExpire.forEach(b ->
                        b.setStatus(BookingStatus.EXPIRED)
                );

                if (!toExpire.isEmpty()) {
                    bookingRepository.saveAll(toExpire);
                }

                log.debug(
                        "Expired trip: id={} {} → {} " +
                                "bookings expired: {}",
                        trip.getId(),
                        trip.getFromLocation(),
                        trip.getToLocation(),
                        toExpire.size()
                );

            } catch (Exception e) {
                // Don't fail the whole batch
                // if one trip fails
                log.error(
                        "Failed to expire trip: id={}",
                        trip.getId(), e
                );
            }
        }

        log.info(
                "Trip expiration complete. {} trips expired.",
                expiredTrips.size()
        );
    }

    // =========================================================
    // SEND DEPARTURE REMINDERS
    // Runs every hour — sends reminder 2 hours before trip
    // =========================================================
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void sendDepartureReminders() {
        LocalDateTime twoHoursFromNow =
                LocalDateTime.now().plusHours(2);
        LocalDateTime twoHoursAndFifteenMin =
                LocalDateTime.now().plusHours(2)
                        .plusMinutes(15);

        // Find trips departing in approximately 2 hours
        List<Trip> upcomingTrips =
                tripRepository
                        .findByStatusAndDepartureTimeBetween(
                                TripStatus.APPROVED,
                                twoHoursFromNow,
                                twoHoursAndFifteenMin
                        );

        for (Trip trip : upcomingTrips) {
            List<Booking> bookings =
                    bookingRepository.findByTripId(
                            trip.getId()
                    );

            for (Booking booking : bookings) {
                if (booking.getStatus() ==
                        BookingStatus.ACTIVE ||
                        booking.getStatus() ==
                                BookingStatus.CONFIRMED ||
                        booking.getStatus() ==
                                BookingStatus.PAID) {

                    // TODO: Send reminder email/SMS
                    log.debug(
                            "Reminder due for booking: {} " +
                                    "user: {}",
                            booking.getBookingReference(),
                            booking.getUser().getEmail()
                    );
                }
            }
        }
    }
}