package com.ghanaride.exception;

/**
 * Thrown when a booking business rule is violated.
 * Extends IllegalStateException so controllers can
 * catch it specifically without catching all exceptions.
 */
public class BookingException extends IllegalStateException {

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }

    // =========================================================
    // Factory methods for common booking errors
    // Keeps error messages consistent across the app
    // =========================================================

    public static BookingException noSeatsAvailable() {
        return new BookingException(
                "No seats available on this trip. " +
                        "Please choose a different trip."
        );
    }

    public static BookingException alreadyBooked() {
        return new BookingException(
                "You already have an active ride. " +
                        "Please complete or cancel your current " +
                        "trip before booking another."
        );
    }

    public static BookingException cancellationWindowExpired(
            int minutes
    ) {
        return new BookingException(
                "Cancellation window has expired. " +
                        "Bookings can only be cancelled within " +
                        minutes + " minutes of booking."
        );
    }

    public static BookingException tripNotFound(Long tripId) {
        return new BookingException(
                "Trip not found: " + tripId
        );
    }

    public static BookingException bookingNotFound(
            Long bookingId
    ) {
        return new BookingException(
                "Booking not found: " + bookingId
        );
    }

    public static BookingException alreadyCancelled() {
        return new BookingException(
                "This booking has already been cancelled."
        );
    }
}