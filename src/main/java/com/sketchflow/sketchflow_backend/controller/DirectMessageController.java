package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.DirectMessage;
import com.sketchflow.sketchflow_backend.service.AuthService;
import com.sketchflow.sketchflow_backend.service.DirectMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm")
@CrossOrigin(origins = "*") // Use your specific frontend origin in production
public class DirectMessageController {

    @Autowired
    private DirectMessageService dmService;

    @Autowired
    private AuthService authService; // To get the currently logged-in user

    /**
     * Get the full conversation history with another user.
     */
    @GetMapping("/history/{otherUsername}")
    public ResponseEntity<List<DirectMessage>> getConversationHistory(@PathVariable String otherUsername) {
        String currentUsername = authService.getCurrentUser().getUsername();
        List<DirectMessage> history = dmService.getConversationHistory(currentUsername, otherUsername);
        return ResponseEntity.ok(history);
    }

    /**
     * REPLACED METHOD:
     * Get the list of all conversations (last message & unread count for each).
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<DirectMessageService.ConversationDTO>> getConversations() {
        String currentUsername = authService.getCurrentUser().getUsername();
        List<DirectMessageService.ConversationDTO> conversations = dmService.getConversations(currentUsername);
        return ResponseEntity.ok(conversations);
    }
    /**
     * NEW ENDPOINT:
     * Mark all messages in a conversation with another user as read.
     */
    @PostMapping("/read/{otherUsername}")
    public ResponseEntity<Void> markAsRead(@PathVariable String otherUsername) {
        String currentUsername = authService.getCurrentUser().getUsername();
        dmService.markAsRead(currentUsername, otherUsername);
        return ResponseEntity.ok().build();
    }
}