package com.sketchflow.sketchflow_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple debug controller to verify backend is running
 */
@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class DebugStatusController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ok");
        status.put("message", "Backend is running successfully");
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "Sketchflow Backend");
        status.put("version", "1.0.0");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        return ResponseEntity.ok(health);
    }
}
