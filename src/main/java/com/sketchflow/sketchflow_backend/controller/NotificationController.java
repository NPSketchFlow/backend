package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.service.NotificationService;
import com.sketchflow.sketchflow_backend.udp.OnlineUserTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

import java.net.InetSocketAddress;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final OnlineUserTracker onlineUserTracker;

    @Autowired // <-- ADD THIS
    private UserRepository userRepository; // <-- ADD THIS

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
    public ResponseEntity<List<UserResponse>> listOnlineUsers() {
        // 1. Get the list of online users (which only has userId)
        List<OnlineUserTracker.OnlineUserInfo> onlineUsers = onlineUserTracker.listOnlineUsers();

        // 2. For each user, find their full details from the User database
        List<UserResponse> fullUserList = onlineUsers.stream()
                .map(onlineUser -> {
                    // Find the full user details by their ID
                    User user = userRepository.findById(onlineUser.getUserId()).orElse(null);

                    if (user == null) {
                        // This might happen if a user is deleted but still has a heartbeat
                        return null;
                    }

                    // 3. Create a UserResponse object
                    return new UserResponse(
                            user.getId(),
                            user.getUsername(),
                            onlineUser.getStatus(),
                            onlineUser.getIp(),
                            onlineUser.getPort(),
                            onlineUser.getLastSeenTimestamp()
                    );
                })
                .filter(response -> response != null) // Filter out any null users
                .collect(Collectors.toList());

        return ResponseEntity.ok(fullUserList);
    }
    // We need to define the UserResponse class.
// ADD THIS HELPER CLASS inside NotificationController
    public static class UserResponse {
        public String userId;
        public String username;
        public String status;
        public String ip;
        public int port;
        public long lastSeen;

        public UserResponse(String userId, String username, String status, String ip, int port, long lastSeen) {
            this.userId = userId;
            this.username = username;
            this.status = status;
            this.ip = ip;
            this.port = port;
            this.lastSeen = lastSeen;
        }
    }

    public static class NotificationRequest {
        public String type;
        public Map<String, Object> payload;
        public String targetHost;
        public Integer targetPort;
        public Integer priority;
    }
}
