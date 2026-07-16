package com.ghanaride.service;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.exception.*;
import com.ghanaride.repository.*;
import com.ghanaride.security.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Service - Handles in-app notifications and real-time updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            String actionUrl
    ) {
        Notification notification = Notification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .actionUrl(actionUrl)
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

        Notification saved = notificationRepository.save(notification);

        // Send real-time via WebSocket
        webSocketService.sendNotification(user.getId(), saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
            .getContent();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
            .ifPresent(n -> {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
            .ifPresent(notificationRepository::delete);
    }

    @Transactional
    public void createBulkNotifications(
            List<User> users,
            NotificationType type,
            String title,
            String message,
            String actionUrl
    ) {
        List<Notification> notifications = users.stream()
            .map(user -> Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build())
            .toList();

        notificationRepository.saveAll(notifications);

        // Send real-time to all users
        users.forEach(user -> webSocketService.sendNotification(user.getId(), 
            Notification.builder().type(type).title(title).message(message).actionUrl(actionUrl).build()));
    }

    // =========================================================
    // PREDEFINED NOTIFICATION TYPES
    // =========================================================

    public void notifyBookingConfirmed(Booking booking) {
        createNotification(booking.getUser(), NotificationType.BOOKING_CONFIRMED,
            "Booking Confirmed",
            "Your booking " + booking.getBookingReference() + " is confirmed.",
            "/booking/receipt/" + booking.getId());
    }

    public void notifyBookingCancelled(Booking booking, String reason) {
        createNotification(booking.getUser(), NotificationType.BOOKING_CANCELLED,
            "Booking Cancelled",
            "Booking " + booking.getBookingReference() + " was cancelled. " + reason,
            "/my-bookings");
    }

    public void notifyTripCancelled(Trip trip, String reason) {
        List<Booking> bookings = bookingRepository.findByTripAndStatusIn(trip, 
            List.of(BookingStatus.ACTIVE, BookingStatus.CONFIRMED));

        bookings.forEach(booking -> createNotification(booking.getUser(), NotificationType.TRIP_CANCELLED,
            "Trip Cancelled",
            "Your trip " + trip.getFromLocation() + " → " + trip.getToLocation() + " was cancelled. " + reason,
            "/my-bookings"));
    }

    public void notifyTripDepartingSoon(Booking booking, int minutes) {
        createNotification(booking.getUser(), NotificationType.TRIP_REMINDER,
            "Departure Reminder",
            "Your trip departs in " + minutes + " minutes. Please proceed to the pickup point.",
            "/boarding-pass/" + booking.getId());
    }

    public void notifyPaymentReceived(Booking booking) {
        createNotification(booking.getUser(), NotificationType.PAYMENT_RECEIVED,
            "Payment Received",
            "Payment of GH₵" + booking.getTotalAmount() + " received for booking " + booking.getBookingReference(),
            "/booking/receipt/" + booking.getId());
    }

    public void notifyRefundProcessed(Booking booking) {
        createNotification(booking.getUser(), NotificationType.REFUND_PROCESSED,
            "Refund Processed",
            "Refund of GH₵" + booking.getTotalAmount() + " has been processed to your wallet.",
            "/wallet");
    }

    public void notifyDriverNewBooking(Booking booking) {
        if (booking.getTrip().getDriver() != null) {
            createNotification(booking.getTrip().getDriver(), NotificationType.NEW_BOOKING,
                "New Passenger",
                "New booking for your trip " + booking.getTrip().getFromLocation() + " → " + booking.getTrip().getToLocation(),
                "/driver/trip-passengers/" + booking.getTrip().getId());
        }
    }

    public void notifyDriverTripApproved(Trip trip) {
        if (trip.getDriver() != null) {
            createNotification(trip.getDriver(), NotificationType.TRIP_APPROVED,
                "Trip Approved",
                "Your trip " + trip.getFromLocation() + " → " + trip.getToLocation() + " has been approved.",
                "/driver/dashboard");
        }
    }

    public void notifyDriverTripRejected(Trip trip, String reason) {
        if (trip.getDriver() != null) {
            createNotification(trip.getDriver(), NotificationType.TRIP_REJECTED,
                "Trip Rejected",
                "Your trip " + trip.getFromLocation() + " → " + trip.getToLocation() + " was rejected. Reason: " + reason,
                "/driver/trips");
        }
    }

    public void notifyWalletTopup(User user, BigDecimal amount) {
        createNotification(user, NotificationType.WALLET_TOPUP,
            "Wallet Topped Up",
            "GH₵" + amount + " has been added to your wallet.",
            "/wallet");
    }

    public void notifyLoyaltyEarned(User user, BigDecimal points) {
        createNotification(user, NotificationType.LOYALTY_EARNED,
            "Loyalty Points Earned",
            "You earned " + points + " loyalty points!",
            "/wallet/loyalty");
    }

    public void notifyReviewReceived(Booking booking) {
        if (booking.getTrip().getDriver() != null) {
            createNotification(booking.getTrip().getDriver(), NotificationType.REVIEW_RECEIVED,
                "New Review",
                "You received a new review from a passenger.",
                "/driver/reviews");
        }
    }

    public void notifySystemMaintenance(String message, LocalDateTime scheduledAt) {
        List<User> users = userRepository.findActiveUsers();
        createBulkNotifications(users, NotificationType.SYSTEM_MAINTENANCE,
            "Scheduled Maintenance",
            message + " (Scheduled: " + scheduledAt + ")",
            "/faq");
    }
}