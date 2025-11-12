package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.ChatMessage;
import com.sketchflow.sketchflow_backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Use your specific frontend origin in production
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Get chat history for a session
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<Map<String, Object>> getSessionChatHistory(@PathVariable String sessionId) {

        List<ChatMessage> history = chatService.getChatHistory(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "history", history,
                "count", history.size()
        ));
    }
}