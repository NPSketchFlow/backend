package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.dto.SessionCreateRequest;
import com.sketchflow.sketchflow_backend.model.ActiveUserSession;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.model.WhiteboardSession;
import com.sketchflow.sketchflow_backend.service.ActiveUserService;
import com.sketchflow.sketchflow_backend.service.AuthService;
import com.sketchflow.sketchflow_backend.service.WhiteboardSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/whiteboard/sessions")
@CrossOrigin(origins = "*")
public class WhiteboardSessionController {

    @Autowired
    private WhiteboardSessionService sessionService;

    @Autowired
    private ActiveUserService activeUserService;

    @Autowired
    private AuthService authService;

    /**
     * Create a new whiteboard session
     * Synchronous to ensure SecurityContext is available throughout the operation
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestBody SessionCreateRequest request) {

        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        // Set the authenticated user as creator
        request.setCreatedBy(currentUser.getUsername());

        try {
            WhiteboardSession session = sessionService.createSession(request);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("name", session.getName());
            response.put("createdBy", session.getCreatedBy());
            response.put("createdAt", session.getCreatedAt());
            response.put("shareLink", "https://app.com/whiteboard/" + session.getSessionId());
            response.put("creatorInfo", Map.of(
                "username", currentUser.getUsername(),
                "fullName", currentUser.getFullName(),
                "avatar", currentUser.getAvatar()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
        }
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
     * Get all sessions for the authenticated user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserSessions() {
        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        // Get sessions created by this user
        List<WhiteboardSession> sessions = sessionService.getUserSessions(currentUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        response.put("total", sessions.size());
        response.put("user", Map.of(
            "username", currentUser.getUsername(),
            "fullName", currentUser.getFullName()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active sessions (admin/public view)
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        List<WhiteboardSession> sessions = sessionService.getActiveSessions();

        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        response.put("total", sessions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete session (only the creator can delete)
     * Synchronous to ensure SecurityContext is available throughout the operation
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        // Verify user owns the session
        Optional<WhiteboardSession> sessionOpt = sessionService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Session not found"));
        }

        WhiteboardSession session = sessionOpt.get();
        if (!session.getCreatedBy().equals(currentUser.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only the session creator can delete it"));
        }

        try {
            sessionService.deleteSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Session deleted successfully");
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
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
     * Clear canvas (authenticated users only)
     */
    @PostMapping("/{sessionId}/clear")
    public ResponseEntity<Map<String, Object>> clearCanvas(@PathVariable String sessionId) {
        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        // Verify session exists
        Optional<WhiteboardSession> sessionOpt = sessionService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Session not found"));
        }

        // This would trigger a clear action
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Canvas cleared successfully");
        response.put("clearedBy", currentUser.getUsername());
        response.put("clearedByFullName", currentUser.getFullName());
        response.put("sessionId", sessionId);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}

