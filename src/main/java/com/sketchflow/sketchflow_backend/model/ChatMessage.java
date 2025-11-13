package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;
    private String sessionId;
    private String senderId;
    private String senderUsername;
    private String messageContent;
    private LocalDateTime timestamp;

    public ChatMessage(String sessionId, String senderId, String senderUsername, String messageContent, LocalDateTime timestamp) {
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
    }
}