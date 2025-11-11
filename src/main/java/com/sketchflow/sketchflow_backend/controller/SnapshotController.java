package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.CanvasSnapshot;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.CanvasSnapshotRepository;
import com.sketchflow.sketchflow_backend.service.AuthService;
import com.sketchflow.sketchflow_backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/whiteboard")
@CrossOrigin(origins = "*")
public class SnapshotController {

    private static final Logger logger = Logger.getLogger(SnapshotController.class.getName());

    @Autowired
    private CanvasSnapshotRepository snapshotRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuthService authService;

    /**
     * Save canvas snapshot with JSON data (authenticated users only)
     * This endpoint accepts JSON for canvas data without file upload
     */
    @PostMapping(value = "/sessions/{sessionId}/snapshots", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveSnapshotJson(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {

        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        try {
            String snapshotId = UUID.randomUUID().toString();
            String name = request.getOrDefault("name", "Untitled Snapshot");
            String canvasData = request.get("canvasData");

            CanvasSnapshot snapshot = new CanvasSnapshot(
                snapshotId,
                sessionId,
                name,
                currentUser.getUsername(),
                LocalDateTime.now(),
                null, // No image URL for JSON endpoint
                null, // No thumbnail
                canvasData
            );

            CanvasSnapshot saved = snapshotRepository.save(snapshot);

            Map<String, Object> response = new HashMap<>();
            response.put("snapshotId", saved.getSnapshotId());
            response.put("name", saved.getName());
            response.put("createdBy", saved.getCreatedBy());
            response.put("createdAt", saved.getCreatedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.severe("Error saving snapshot: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save snapshot: " + e.getMessage()));
        }
    }

    /**
     * Save canvas snapshot with file upload (authenticated users only)
     * This endpoint accepts multipart/form-data for uploading images
     */
    @PostMapping(value = "/sessions/{sessionId}/snapshots/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> saveSnapshotWithImage(
            @PathVariable String sessionId,
            @RequestParam String name,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String canvasData) {

        // Get authenticated user
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        try {
            String snapshotId = UUID.randomUUID().toString();
            String imageUrl = null;
            String thumbnail = null;

            // Store image if provided
            if (image != null && !image.isEmpty()) {
                imageUrl = fileStorageService.storeFile(image);
                // In production, generate thumbnail here
                thumbnail = imageUrl; // Simplified for now
            }

            CanvasSnapshot snapshot = new CanvasSnapshot(
                snapshotId,
                sessionId,
                name,
                currentUser.getUsername(), // Use authenticated user
                LocalDateTime.now(),
                imageUrl,
                thumbnail,
                canvasData
            );

            CanvasSnapshot saved = snapshotRepository.save(snapshot);

            Map<String, Object> response = new HashMap<>();
            response.put("snapshotId", saved.getSnapshotId());
            response.put("name", saved.getName());
            response.put("imageUrl", saved.getImageUrl());
            response.put("thumbnail", saved.getThumbnail());
            response.put("createdAt", saved.getCreatedAt());
            response.put("createdBy", Map.of(
                "username", currentUser.getUsername(),
                "fullName", currentUser.getFullName()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get snapshots for a session
     */
    @GetMapping("/sessions/{sessionId}/snapshots")
    public ResponseEntity<Map<String, Object>> getSessionSnapshots(@PathVariable String sessionId) {
        List<CanvasSnapshot> snapshots = snapshotRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("snapshots", snapshots);
        response.put("total", snapshots.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get snapshot by ID
     */
    @GetMapping("/snapshots/{snapshotId}")
    public ResponseEntity<CanvasSnapshot> getSnapshot(@PathVariable String snapshotId) {
        return snapshotRepository.findById(snapshotId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Download snapshot
     */
    @GetMapping("/snapshots/{snapshotId}/download")
    public ResponseEntity<Map<String, Object>> downloadSnapshot(@PathVariable String snapshotId) {
        return snapshotRepository.findById(snapshotId)
            .map(snapshot -> {
                Map<String, Object> response = new HashMap<>();
                response.put("snapshotId", snapshot.getSnapshotId());
                response.put("name", snapshot.getName());
                response.put("imageUrl", snapshot.getImageUrl());
                response.put("canvasData", snapshot.getCanvasData());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete snapshot
     */
    @DeleteMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable String snapshotId) {
        if (snapshotRepository.existsById(snapshotId)) {
            snapshotRepository.deleteById(snapshotId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

