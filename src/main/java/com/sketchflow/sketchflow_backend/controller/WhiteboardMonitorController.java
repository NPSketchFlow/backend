package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.nio.WhiteboardNioServer;
import com.sketchflow.sketchflow_backend.websocket.WebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/whiteboard/monitor")
@CrossOrigin(origins = "*")
public class WhiteboardMonitorController {

    @Autowired
    private WebSocketSessionManager webSocketSessionManager;

    @Autowired
    private WhiteboardNioServer nioServer;

    /**
     * Get system statistics and health
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        // WebSocket statistics
        stats.put("websocket", webSocketSessionManager.getStatistics());

        // NIO server statistics
        stats.put("nioServer", nioServer.getStatistics());

        // JVM statistics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvmStats = new HashMap<>();
        jvmStats.put("totalMemory", runtime.totalMemory());
        jvmStats.put("freeMemory", runtime.freeMemory());
        jvmStats.put("maxMemory", runtime.maxMemory());
        jvmStats.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        jvmStats.put("availableProcessors", runtime.availableProcessors());
        stats.put("jvm", jvmStats);

        // Thread statistics
        Map<String, Object> threadStats = new HashMap<>();
        threadStats.put("activeThreads", Thread.activeCount());
        threadStats.put("currentThread", Thread.currentThread().getName());
        stats.put("threads", threadStats);

        stats.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Whiteboard Backend");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }
}

