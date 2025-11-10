package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.service.NotificationService;
import com.sketchflow.sketchflow_backend.udp.OnlineUserTracker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final OnlineUserTracker onlineUserTracker;

    public NotificationController(NotificationService notificationService, OnlineUserTracker onlineUserTracker) {
        this.notificationService = notificationService;
        this.onlineUserTracker = onlineUserTracker;
    }

    @PostMapping("/notifications/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest req) {
        try {
            if (req == null || req.type == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "type_required"));
            }

            String fileId = req.payload != null ? String.valueOf(req.payload.get("fileId")) : null;
            String senderId = req.payload != null ? String.valueOf(req.payload.get("senderId")) : null;
            int priority = req.priority != null ? req.priority : 1;

            Notification n = new Notification(req.type, fileId, senderId, System.currentTimeMillis(), priority);

            if (req.targetHost == null) {
                notificationService.sendNotification(n);
            } else {
                InetSocketAddress target = new InetSocketAddress(req.targetHost, req.targetPort != null ? req.targetPort : 0);
                notificationService.sendNotificationTo(target, n);
            }

            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/online-users")
    public ResponseEntity<List<OnlineUserTracker.OnlineUserInfo>> listOnlineUsers() {
        return ResponseEntity.ok(onlineUserTracker.listOnlineUsers());
    }

    public static class NotificationRequest {
        public String type;
        public Map<String, Object> payload;
        public String targetHost;
        public Integer targetPort;
        public Integer priority;
    }
}
