package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.dto.DrawingActionRequest;
import com.sketchflow.sketchflow_backend.model.DrawingAction;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.service.AuthService;
import com.sketchflow.sketchflow_backend.service.DrawingActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/whiteboard/sessions/{sessionId}/actions")
// CORS handled by global SecurityConfig
public class DrawingActionController {

    @Autowired
    private DrawingActionService drawingActionService;

    @Autowired
    private AuthService authService;

    /**
     * Save a drawing action (authenticated users only)
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, Object>>> saveAction(
            @PathVariable String sessionId,
            @RequestBody DrawingActionRequest request) {

        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"))
            );
        }

        // Set the authenticated user as the action creator
        request.setUserId(currentUser.getId());

        return drawingActionService.saveActionAsync(sessionId, request)
            .thenApply(action -> {
                Map<String, Object> response = new HashMap<>();
                response.put("actionId", action.getActionId());
                response.put("timestamp", action.getTimestamp());
                response.put("userId", currentUser.getId());
                response.put("username", currentUser.getUsername());

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                              .body(Map.of("error", ex.getMessage())));
    }

    /**
     * Get drawing history for a session (paginated)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSessionActions(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Page<DrawingAction> actionsPage = drawingActionService.getSessionActions(sessionId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("actions", actionsPage.getContent());
        response.put("total", actionsPage.getTotalElements());
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", actionsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all actions for a session (for loading complete canvas state)
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllSessionActions(@PathVariable String sessionId) {
        List<DrawingAction> actions = drawingActionService.getAllSessionActions(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("actions", actions);
        response.put("total", actions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all actions for a session (authenticated users only)
     */
    @DeleteMapping
    public CompletableFuture<ResponseEntity<Map<String, Object>>> clearActions(@PathVariable String sessionId) {
        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"))
            );
        }

        return drawingActionService.clearSessionActionsAsync(sessionId)
            .thenApply(v -> {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "All drawing actions cleared");
                response.put("sessionId", sessionId);
                response.put("clearedBy", currentUser.getUsername());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                Map<String, Object> error = new HashMap<>();
                error.put("error", ex.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            });
    }

    /**
     * Get action statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getActionStats(@PathVariable String sessionId) {
        long count = drawingActionService.getActionCount(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("totalActions", count);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}

