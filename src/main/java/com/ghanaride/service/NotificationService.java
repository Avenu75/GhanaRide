package com.ghanaride.service;

import com.ghanaride.entity.Notification;
import com.ghanaride.entity.User;
import com.ghanaride.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public Notification push(User user, Notification.Type type, String title, String message, String actionUrl) {
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .channel(Notification.Channel.IN_APP)
                .build();
        Notification saved = notificationRepository.save(n);

        // best-effort email
        try {
            if (user.getEmail() != null) {
                emailService.sendNotificationEmail(user.getEmail(), title, message, actionUrl);
            }
        } catch (Exception e) {
            log.debug("Email notify failed: {}", e.getMessage());
        }
        return saved;
    }

    public Page<Notification> inbox(Long userId, int page) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, 20));
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    // convenience helpers
    public void bookingConfirmed(User user, String bookingRef) {
        push(user, Notification.Type.BOOKING_CONFIRMED,
            "Booking confirmed ✓",
            "Your GhanaRide booking "+bookingRef+" is confirmed. Boarding QR is ready.",
            "/my-bookings");
    }

    public void tripReminder(User user, String route) {
        push(user, Notification.Type.TRIP_REMINDER,
            "Trip tomorrow",
            "Reminder: "+route+" departs tomorrow. Arrive 20 min early.",
            "/my-bookings");
    }

    public void refundProcessed(User user, String amount) {
        push(user, Notification.Type.REFUND_PROCESSED,
            "Refund processed",
            "GH₵"+amount+" has been refunded instantly to your GhanaRide Wallet.",
            "/wallet");
    }
}
