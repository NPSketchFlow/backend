package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.service.NotificationService;
import com.sketchflow.sketchflow_backend.udp.OnlineUserTracker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

            Notification notification = new Notification();
            notification.setType(req.type);
            notification.setSenderId(firstNonNull(req.senderId, req.extractFromPayload("senderId")));
            notification.setReceiverId(firstNonNull(req.receiverId, req.extractFromPayload("receiverId")));
            notification.setMessage(firstNonNull(req.message, req.extractFromPayload("message")));
            notification.setFileId(firstNonNull(req.fileId, req.extractFromPayload("fileId")));
            notification.setPriority(req.priority != null ? req.priority : 1);
            notification.setMetadata(req.metadata != null ? new HashMap<>(req.metadata) : req.payload != null ? new HashMap<>(req.payload) : new HashMap<>());

            Notification persisted;
            if (req.targetHost == null || req.targetHost.isBlank()) {
                persisted = notificationService.sendNotification(notification);
            } else {
                InetSocketAddress target = new InetSocketAddress(req.targetHost, req.targetPort != null ? req.targetPort : 0);
                persisted = notificationService.sendNotificationTo(target, notification);
            }

            return ResponseEntity.ok(Map.of("status", "sent", "notification", persisted));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> listNotifications(@RequestParam(value = "receiverId", required = false) String receiverId) {
        return ResponseEntity.ok(notificationService.listNotifications(receiverId));
    }

    @GetMapping("/notifications/unread")
    public ResponseEntity<List<Notification>> listUnread(@RequestParam("receiverId") String receiverId) {
        return ResponseEntity.ok(notificationService.listUnread(receiverId));
    }

    @GetMapping("/notifications/unread/count")
    public ResponseEntity<Map<String, Object>> countUnread(@RequestParam("receiverId") String receiverId) {
        long count = notificationService.countUnread(receiverId);
        return ResponseEntity.ok(Map.of("receiverId", receiverId, "count", count));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") String id) {
        Notification notification = notificationService.markAsRead(id);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<?> markAllAsRead(@RequestBody Map<String, String> body) {
        String receiverId = body != null ? body.get("receiverId") : null;
        if (receiverId == null || receiverId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "receiverId_required"));
        }
        notificationService.markAllAsRead(receiverId);
        return ResponseEntity.ok(Map.of("status", "updated", "receiverId", receiverId));
    }

    @GetMapping("/online-users")
    public ResponseEntity<List<OnlineUserTracker.OnlineUserInfo>> listOnlineUsers() {
        return ResponseEntity.ok(onlineUserTracker.listOnlineUsers());
    }

    public static class NotificationRequest {
        public String type;
        public String message;
        public String senderId;
        public String receiverId;
        public String fileId;
        public Map<String, Object> metadata;
        public Map<String, Object> payload;
        public String targetHost;
        public Integer targetPort;
        public Integer priority;

        private String extractFromPayload(String key) {
            return Optional.ofNullable(payload)
                    .map(map -> map.get(key))
                    .map(Object::toString)
                    .orElse(null);
        }
    }

    private static String firstNonNull(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }
}
