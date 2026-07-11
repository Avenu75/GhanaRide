package com.ghanaride.controller;

import com.ghanaride.model.ChatMessage;
import com.ghanaride.model.LocationUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RealTimeController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming chat messages.
     * Clients send to /app/chat
     * We broadcast to /topic/chat/{tripId}
     */
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        if (chatMessage.getType() == null) {
            chatMessage.setType(ChatMessage.MessageType.CHAT);
        }
        log.info("Chat message on trip {}: {}", chatMessage.getTripId(), chatMessage.getContent());
        messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getTripId(), chatMessage);
    }

    /**
     * Handle incoming driver location updates.
     * Driver client sends to /app/location
     * We broadcast to /topic/location/{tripId}
     */
    @MessageMapping("/location")
    public void processLocationUpdate(@Payload LocationUpdate locationUpdate) {
        // Broadcast location to passengers tracking this trip
        log.debug("Location update for trip {}: lat={}, lng={}",
                locationUpdate.getTripId(), locationUpdate.getLatitude(), locationUpdate.getLongitude());
        messagingTemplate.convertAndSend("/topic/location/" + locationUpdate.getTripId(), locationUpdate);
    }
}
