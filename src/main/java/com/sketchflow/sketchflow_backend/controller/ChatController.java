package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.ChatMessage;
import com.sketchflow.sketchflow_backend.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // Prefix for all endpoints
public class ChatController {

    private final ChatService chatService;

    // Constructor for dependency injection
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Endpoint to retrieve chat history
    @GetMapping("/history")
    public List<ChatMessage> history() {
        return chatService.getHistory(); // Returns the latest chat messages
    }

    // Endpoint to save and broadcast a new chat message
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody ChatMessage message) {
        chatService.handleIncomingMessage(message.getSender(), message.getContent(), message.getType());
        return ResponseEntity.ok("Message sent and broadcasted successfully.");
    }

    // Optional - Save a chat message without broadcasting
    @PostMapping("/save")
    public ChatMessage saveMessage(@RequestBody ChatMessage message) {
        return chatService.saveMessage(message); // Saves the message and returns it
    }
}