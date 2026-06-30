package com.ghanaride.service;

import com.ghanaride.entity.Booking;
import com.ghanaride.entity.BookingStatus;
import com.ghanaride.entity.PaymentMethod;
import com.ghanaride.entity.PaymentStatus;
import com.ghanaride.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;

    @Transactional
    public void processCashPayment(Booking booking) {
        booking.setPaymentMethod(PaymentMethod.CASH);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        bookingRepository.save(booking);
    }

    @Transactional
    public Booking verifyPaystackPayment(String reference) {
        // In a real integration, this would make an HTTP call to Paystack API:
        // GET https://api.paystack.co/transaction/verify/{reference}
        // with Header: Authorization: Bearer {secret_key}
        
        Optional<Booking> bookingOpt = bookingRepository.findByBookingReference(reference);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("Booking not found for reference: " + reference);
        }
        
        Booking booking = bookingOpt.get();
        booking.setPaymentMethod(PaymentMethod.PAYSTACK);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.PAID);
        booking.setTransactionReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setPaymentDate(LocalDateTime.now());
        
        return bookingRepository.save(booking);
    }
}
