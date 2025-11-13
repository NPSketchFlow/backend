package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "direct_messages")
public class DirectMessage {

    @Id
    private String id;

    // Use usernames for simplicity, as they are unique
    @Indexed
    private String senderUsername;
    @Indexed
    private String receiverUsername;

    private String messageContent;

    @Indexed
    private LocalDateTime timestamp;

    // A composite key to help find conversations
    @Indexed
    private String conversationId;

    private boolean read = false;

    // Constructor
    public DirectMessage(String senderUsername, String receiverUsername, String messageContent) {
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.messageContent = messageContent;
        this.timestamp = LocalDateTime.now();

        // Create a consistent, sorted ID for the conversation
        // This makes "userA -> userB" and "userB -> userA" part of the same thread
        if (senderUsername.compareTo(receiverUsername) > 0) {
            this.conversationId = receiverUsername + ":" + senderUsername;
        } else {
            this.conversationId = senderUsername + ":" + receiverUsername;
        }
    }
}