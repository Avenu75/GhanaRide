package com.ghanaride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ghanaride.entity.Notification;

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
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
            log.debug("Sent notification to user {} via WebSocket", userId);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}", userId, e);
        }
    }

    /**
     * Send a booking update to a user.
     */
    public void sendBookingUpdate(Long userId, Object payload) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/bookings",
            payload
        );
    }

    /**
     * Send a wallet update to a user.
     */
    public void sendWalletUpdate(Long userId, Object payload) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/wallet",
            payload
        );
    }

    /**
     * Send a trip update to a user.
     */
    public void sendTripUpdate(Long userId, Object payload) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/trips",
            payload
        );
    }

    /**
     * Broadcast to all connected users (admin use).
     */
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Send to a specific topic for a user's active session.
     */
    public void sendToUserSession(Long userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            destination,
            payload
        );
    }
}