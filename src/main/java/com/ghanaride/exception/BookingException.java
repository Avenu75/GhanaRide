package com.ghanaride.exception;

/**
 * Booking business rule violation exception.
 */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}