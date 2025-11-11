package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.dto.SessionCreateRequest;
import com.sketchflow.sketchflow_backend.model.ActiveUserSession;
import com.sketchflow.sketchflow_backend.model.WhiteboardSession;
import com.sketchflow.sketchflow_backend.service.ActiveUserService;
import com.sketchflow.sketchflow_backend.service.WhiteboardSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/whiteboard/sessions")
@CrossOrigin(origins = "*")
public class WhiteboardSessionController {

    @Autowired
    private WhiteboardSessionService sessionService;

    @Autowired
    private ActiveUserService activeUserService;

    /**
     * Create a new whiteboard session
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createSession(
            @RequestBody SessionCreateRequest request) {

        return sessionService.createSessionAsync(request)
            .thenApply(session -> {
                Map<String, Object> response = new HashMap<>();
                response.put("sessionId", session.getSessionId());
                response.put("name", session.getName());
                response.put("createdBy", session.getCreatedBy());
                response.put("createdAt", session.getCreatedAt());
                response.put("shareLink", "https://app.com/whiteboard/" + session.getSessionId());

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                              .body(Map.of("error", ex.getMessage())));
    }

    /**
     * Get session details
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        Optional<WhiteboardSession> sessionOpt = sessionService.getSession(sessionId);

        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Session not found"));
        }

        WhiteboardSession session = sessionOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("name", session.getName());
        response.put("createdBy", session.getCreatedBy());
        response.put("createdAt", session.getCreatedAt());
        response.put("activeUsers", session.getActiveUsers() != null ? session.getActiveUsers().size() : 0);
        response.put("isActive", session.isActive());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all sessions for a user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserSessions(
            @RequestParam(required = false) String userId) {

        List<WhiteboardSession> sessions;

        if (userId != null) {
            sessions = sessionService.getUserSessions(userId);
        } else {
            sessions = sessionService.getActiveSessions();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        response.put("total", sessions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete session
     */
    @DeleteMapping("/{sessionId}")
    public CompletableFuture<ResponseEntity<Void>> deleteSession(@PathVariable String sessionId) {
        return sessionService.deleteSessionAsync(sessionId)
            .thenApply(v -> ResponseEntity.noContent().<Void>build())
            .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Get active users in a session
     */
    @GetMapping("/{sessionId}/users")
    public ResponseEntity<Map<String, Object>> getActiveUsers(@PathVariable String sessionId) {
        List<ActiveUserSession> users = activeUserService.getSessionUsers(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("total", users.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Clear canvas (will be handled via WebSocket, but also available as REST)
     */
    @PostMapping("/{sessionId}/clear")
    public ResponseEntity<Map<String, Object>> clearCanvas(
            @PathVariable String sessionId,
            @RequestParam String userId) {

        // This would trigger a clear action
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Canvas cleared successfully");
        response.put("clearedBy", userId);
        response.put("timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}

