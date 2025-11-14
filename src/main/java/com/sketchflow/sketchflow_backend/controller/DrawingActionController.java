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
@CrossOrigin(origins = "*")
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

        // CRITICAL: Capture user and SecurityContext BEFORE async operation
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"))
            );
        }

        // Capture user details synchronously
        final String userId = currentUser.getId();
        final String username = currentUser.getUsername();

        // Set the authenticated user as the action creator
        request.setUserId(userId);

        return drawingActionService.saveActionAsync(sessionId, request)
            .thenApply(action -> {
                Map<String, Object> response = new HashMap<>();
                response.put("actionId", action.getActionId());
                response.put("timestamp", action.getTimestamp());
                response.put("userId", userId);
                response.put("username", username);

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
        // CRITICAL: Capture user BEFORE async operation
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"))
            );
        }

        // Capture username synchronously
        final String username = currentUser.getUsername();

        return drawingActionService.clearSessionActionsAsync(sessionId)
            .thenApply(v -> {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "All drawing actions cleared");
                response.put("sessionId", sessionId);
                response.put("clearedBy", username);
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                Map<String, Object> error = new HashMap<>();
                error.put("error", ex.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            });
    }

    /**
     * Delete a single drawing action (authenticated users only)
     * Endpoint: DELETE /api/whiteboard/sessions/{sessionId}/actions/{actionId}
     */
    @DeleteMapping("/{actionId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteAction(
            @PathVariable String sessionId,
            @PathVariable String actionId) {

        // CRITICAL: Capture user BEFORE async operation
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"))
            );
        }

        // Capture username synchronously
        final String username = currentUser.getUsername();

        return drawingActionService.deleteActionAsync(sessionId, actionId)
            .thenApply(v -> {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Drawing action deleted successfully");
                response.put("sessionId", sessionId);
                response.put("actionId", actionId);
                response.put("deletedBy", username);
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

