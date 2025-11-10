package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.service.NotificationService;
import com.sketchflow.sketchflow_backend.udp.UdpServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final UdpServer udpServer;
    private final NotificationService notificationService;

    public DebugController(UdpServer udpServer, NotificationService notificationService) {
        this.udpServer = udpServer;
        this.notificationService = notificationService;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody Map<String, Object> body) {
        String message = (body != null && body.get("message") != null) ? String.valueOf(body.get("message")) : "debug-broadcast";
        try {
            udpServer.triggerBroadcast(message);
            return ResponseEntity.ok(Map.of("status", "broadcast_sent", "message", message));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/notify-new-voice")
    public ResponseEntity<?> notifyNewVoice(@RequestBody Map<String, Object> body) {
        String fileId = body != null && body.get("fileId") != null ? String.valueOf(body.get("fileId")) : "testFile123";
        String senderId = body != null && body.get("senderId") != null ? String.valueOf(body.get("senderId")) : "debug-sender";
        try {
            notificationService.notifyNewVoice(fileId, senderId);
            return ResponseEntity.ok(Map.of("status", "notify_sent", "fileId", fileId, "senderId", senderId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/udp-clients")
    public ResponseEntity<?> udpClients() {
        try {
            return ResponseEntity.ok(Map.of("clients", udpServer.getKnownClients()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
