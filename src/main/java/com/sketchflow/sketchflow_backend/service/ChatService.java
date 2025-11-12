package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.dto.WebSocketMessage;
import com.sketchflow.sketchflow_backend.model.ChatMessage;
import com.sketchflow.sketchflow_backend.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // Use an executor for non-blocking database saves
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Saves a chat message to the database asynchronously.
     */
    public ChatMessage saveMessage(WebSocketMessage wsMessage, String sessionId) {
        ChatMessage chatMessage = new ChatMessage(
                sessionId,
                wsMessage.getUserId(),
                wsMessage.getUsername(),
                wsMessage.getMessageContent(), // We will add this field to WebSocketMessage
                LocalDateTime.now()
        );

        // Save asynchronously to not block the WebSocket thread
        executor.submit(() -> {
            chatMessageRepository.save(chatMessage);
        });

        // Return the message immediately for broadcasting
        return chatMessage;
    }

    /**
     * Retrieves all chat messages for a given session.
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}