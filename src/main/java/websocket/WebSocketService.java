package com.ghanaride.security;

import com.ghanaride.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket Service - Sends real-time updates to connected clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a notification to a specific user.
     */
    public void sendNotification(Long userId, Notification notification) {
        String destination = "/user/" + userId + "/queue/notifications";
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
            log.debug("Sent WebSocket notification to user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}", userId, e);
        }
    }

    /**
     * Send a booking update to a specific user.
     */
    public void sendBookingUpdate(Long userId, String bookingRef, String status, String message) {
        String destination = "/user/" + userId + "/queue/bookings";
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/bookings",
                Map.of(
                    "type", "booking_update",
                    "bookingRef", bookingRef,
                    "status", status,
                    "message", message,
                    "timestamp", java.time.LocalDateTime.now().toString()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send booking update to user {}", userId, e);
        }
    }

    /**
     * Send a trip update to a specific user.
     */
    public void sendTripUpdate(Long userId, Long tripId, String message) {
        String destination = "/user/" + userId + "/queue/trips";
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/trips",
                Map.of(
                    "type", "trip_update",
                    "tripId", tripId,
                    "message", message,
                    "timestamp", java.time.LocalDateTime.now().toString()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send trip update to user {}", userId, e);
        }
    }

    /**
     * Send a wallet update to a specific user.
     */
    public void sendWalletUpdate(Long userId, String newBalance) {
        String destination = "/user/" + userId + "/queue/wallet";
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/wallet",
                Map.of(
                    "type", "wallet_update",
                    "newBalance", newBalance,
                    "timestamp", java.time.LocalDateTime.now().toString()
                )
            );
        } catch (Exception e) {
            log.warn("Failed to send wallet update to user {}", userId, e);
        }
    }

    /**
     * Send a notification to a specific user.
     */
    public void sendNotification(Long userId, Map<String, Object> notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
            log.debug("Sent notification to user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}", userId, e);
        }
    }

    /**
     * Broadcast to all connected admins.
     */
    public void broadcastToAdmins(String destination, Object payload) {
        messagingTemplate.convertAndSend("/topic/admin/" + destination, payload);
    }
}