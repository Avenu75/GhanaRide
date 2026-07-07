package com.ghanaride.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private String tripId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}
