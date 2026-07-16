package com.ghanaride.api;

import com.ghanaride.dto.response.ApiResponse;
import com.ghanaride.entity.Booking;
import com.ghanaride.entity.User;
import com.ghanaride.service.BookingService;
import com.ghanaride.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
public class BookingRestController {

    private final BookingService bookingService;
    private final UserService userService;

    @PostMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<Booking>> create(
            @PathVariable Long tripId,
            @RequestBody CreateBookingRequest req,
            Principal principal
    ) {
        User user = userService.getCurrentUser(principal);
        Booking booking = bookingService.createBooking(user, tripId, req.getSeatNumber(), req.getPaymentMethod(), req.getPassengerName(), req.getPassengerPhone());
        return ResponseEntity.ok(ApiResponse.success("Booking created", booking));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<Booking>>> myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        User user = userService.getCurrentUser(principal);
        Page<Booking> bookings = bookingService.findByUserPaginated(user, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> detail(@PathVariable Long id, Principal principal) {
        Optional<Booking> opt = bookingService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Booking b = opt.get();
        User current = userService.getCurrentUser(principal);
        if (!b.getUser().getId().equals(current.getId()) && !current.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(ApiResponse.success(b));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancel(@PathVariable Long id, Principal principal) {
        Optional<Booking> opt = bookingService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Booking b = opt.get();
        User current = userService.getCurrentUser(principal);
        if (!b.getUser().getId().equals(current.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (!bookingService.canCancelBooking(b)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot cancel – less than 2 hours to departure"));
        }
        bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled"));
    }

    @Data
    public static class CreateBookingRequest {
        private String seatNumber;
        private String paymentMethod;
        private String passengerName;
        private String passengerPhone;
    }
}
