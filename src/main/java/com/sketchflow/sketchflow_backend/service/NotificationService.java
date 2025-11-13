package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.repository.NotificationRepository;
import com.sketchflow.sketchflow_backend.udp.OnlineUserTracker;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationService {

    private final OnlineUserTracker onlineUserTracker;
    private final NotificationRepository notificationRepository;
    private static final int FALLBACK_PORT = 9876; // used when no online users found for local testing
    private static final int DEBUG_FORCE_PORT = 60000; // debug: always send a copy here to help testing

    public NotificationService(OnlineUserTracker onlineUserTracker,
                               @Autowired(required = false) NotificationRepository notificationRepository) {
        this.onlineUserTracker = onlineUserTracker;
        this.notificationRepository = notificationRepository;
    }

    public Notification notifyNewVoice(String fileId, String senderId) {
        Notification notification = new Notification("NEW_VOICE", fileId, senderId, System.currentTimeMillis(), 1);
        notification.setMessage("New voice message available");
        return sendNotification(notification);
    }

    public Notification recordTextMessage(String senderId, String receiverId, String message, Map<String, Object> metadata) {
        Notification notification = new Notification("TEXT_MESSAGE", null, senderId, receiverId, message, System.currentTimeMillis(), 1, false, metadata);
        return sendNotification(notification);
    }

    public Notification recordPresenceChange(String userId, String status) {
        Notification notification = new Notification("USER_STATUS", null, userId, null,
                String.format("%s is now %s", userId, status), System.currentTimeMillis(), 0, false, null);
        return persist(notification);
    }

    public Notification sendNotification(Notification notification) {
        Notification persisted = persist(notification);
        broadcast(persisted);
        return persisted;
    }

    public List<Notification> listNotifications(String receiverId) {
        if (notificationRepository == null) {
            return List.of();
        }
        if (receiverId == null || receiverId.isBlank()) {
            return notificationRepository.findAllOrdered();
        }
        return notificationRepository.findByReceiverIdOrderByTimestampDesc(receiverId);
    }

    public List<Notification> listUnread(String receiverId) {
        if (notificationRepository == null || receiverId == null || receiverId.isBlank()) {
            return List.of();
        }
        return notificationRepository.findByReceiverIdAndReadFalseOrderByTimestampDesc(receiverId);
    }

    public long countUnread(String receiverId) {
        if (notificationRepository == null || receiverId == null || receiverId.isBlank()) {
            return 0;
        }
        return notificationRepository.countByReceiverIdAndReadFalse(receiverId);
    }

    public Notification markAsRead(String notificationId) {
        if (notificationRepository == null || notificationId == null) {
            return null;
        }
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                })
                .orElse(null);
    }

    public void markAllAsRead(String receiverId) {
        if (notificationRepository == null || receiverId == null) {
            return;
        }
        List<Notification> unread = notificationRepository.findByReceiverIdAndReadFalseOrderByTimestampDesc(receiverId);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @EventListener
    public void onPresenceChange(PresenceChangeEvent event) {
        if (event == null) {
            return;
        }
        Notification notification = new Notification(
                "USER_STATUS",
                null,
                event.userId(),
                null,
                String.format("%s is now %s", event.userId(), event.status()),
                event.timestamp(),
                0,
                false,
                Map.of(
                        "ip", event.ip(),
                        "port", event.port(),
                        "status", event.status()
                )
        );
        persist(notification);

        // When user comes online, send them their missed notifications
        if ("ONLINE".equals(event.status())) {
            sendMissedNotifications(event.userId());
        }
    }

    /**
     * Send all unread notifications to a user who just came online
     */
    public void sendMissedNotifications(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            List<Notification> unreadNotifications = listUnread(userId);
            if (unreadNotifications.isEmpty()) {
                System.out.println("[NotificationService] No missed notifications for user: " + userId);
                return;
            }

            System.out.println("[NotificationService] Sending " + unreadNotifications.size() + " missed notifications to user: " + userId);

            // Find the user's current address
            List<OnlineUserTracker.OnlineUserInfo> onlineUsers = onlineUserTracker.listOnlineUsers();
            InetSocketAddress userAddress = null;
            for (OnlineUserTracker.OnlineUserInfo userInfo : onlineUsers) {
                if (userId.equals(userInfo.getUserId())) {
                    userAddress = new InetSocketAddress(userInfo.getIp(), userInfo.getPort());
                    break;
                }
            }

            if (userAddress == null) {
                System.out.println("[NotificationService] User " + userId + " address not found, cannot send missed notifications");
                return;
            }

            // Send each missed notification
            try (DatagramSocket socket = new DatagramSocket()) {
                for (Notification notification : unreadNotifications) {
                    try {
                        sendUsingSocket(socket, userAddress, notification);
                        // Don't mark as read yet - let the client acknowledge receipt
                    } catch (Exception e) {
                        System.out.println("[NotificationService] Failed to send missed notification " + notification.getId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[NotificationService] Error sending missed notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Notification persist(Notification notification) {
        if (notification.getTimestamp() == 0L) {
            notification.setTimestamp(System.currentTimeMillis());
        }
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
        }
        if (notification.getMetadata() != null) {
            notification.setMetadata(notification.getMetadata());
        }
        if (notificationRepository == null) {
            return notification;
        }
        return notificationRepository.save(notification);
    }

    private void broadcast(Notification notification) {
        try {
            List<OnlineUserTracker.OnlineUserInfo> users = onlineUserTracker.listOnlineUsers();
            if (users == null || users.isEmpty()) {
                System.out.println("[NotificationService] No online users found, sending to localhost fallback");
                sendNotificationTo(new InetSocketAddress("127.0.0.1", FALLBACK_PORT), notification);
                sendDebugCopy(notification);
                return;
            }

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(false);
                for (OnlineUserTracker.OnlineUserInfo online : users) {
                    if (online == null || online.getIp() == null || online.getPort() <= 0) {
                        continue;
                    }
                    InetSocketAddress target = new InetSocketAddress(online.getIp(), online.getPort());
                    try {
                        sendUsingSocket(socket, target, notification);
                    } catch (Exception ex) {
                        System.out.println("[NotificationService] Failed to send to " + online.getIp() + ":" + online.getPort() + " - " + ex.getMessage());
                    }
                }
                sendDebugCopyUsingSocket(socket, notification);
            }
        } catch (Exception e) {
            System.out.println("[NotificationService] Error in broadcast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Notification sendNotificationTo(InetSocketAddress target, Notification notification) {
        if (target == null) {
            return notification;
        }
        Notification persisted = persist(notification);
        try (DatagramSocket socket = new DatagramSocket()) {
            sendUsingSocket(socket, target, persisted);
            sendDebugCopyUsingSocket(socket, persisted);
        } catch (Exception e) {
            System.out.println("[NotificationService] Error sending to target " + target + " : " + e.getMessage());
            e.printStackTrace();
        }
        return persisted;
    }

    private void sendUsingSocket(DatagramSocket socket, InetSocketAddress target, Notification notification) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("id", notification.getId());
        payload.put("type", notification.getType());
        payload.put("fileId", notification.getFileId());
        payload.put("senderId", notification.getSenderId());
        payload.put("receiverId", notification.getReceiverId());
        payload.put("message", notification.getMessage());
        payload.put("timestamp", notification.getTimestamp());
        payload.put("priority", notification.getPriority());
        payload.put("read", notification.isRead());
        if (notification.getMetadata() != null && !notification.getMetadata().isEmpty()) {
            payload.put("metadata", new JSONObject(notification.getMetadata()));
        }

        byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);

        InetAddress addr;
        try {
            addr = InetAddress.getByName(target.getHostString());
        } catch (Exception e) {
            InetAddress fallback = target.getAddress();
            if (fallback == null) {
                throw e;
            }
            addr = fallback;
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, addr, target.getPort());
        socket.send(packet);
        System.out.println("[NotificationService] Sent notification to " + addr.getHostAddress() + ":" + target.getPort() + " payload=" + payload);
    }

    private void sendDebugCopy(Notification notification) {
        try (DatagramSocket socket = new DatagramSocket()) {
            sendDebugCopyUsingSocket(socket, notification);
        } catch (Exception e) {
            System.out.println("[NotificationService] Failed to send debug copy: " + e.getMessage());
        }
    }

    private void sendDebugCopyUsingSocket(DatagramSocket socket, Notification notification) {
        try {
            InetSocketAddress debugTarget = new InetSocketAddress("127.0.0.1", DEBUG_FORCE_PORT);
            JSONObject payload = new JSONObject();
            payload.put("id", notification.getId());
            payload.put("type", notification.getType());
            payload.put("fileId", notification.getFileId());
            payload.put("senderId", notification.getSenderId());
            payload.put("receiverId", notification.getReceiverId());
            payload.put("message", notification.getMessage());
            payload.put("timestamp", notification.getTimestamp());
            payload.put("priority", notification.getPriority());
            payload.put("read", notification.isRead());
            if (notification.getMetadata() != null && !notification.getMetadata().isEmpty()) {
                payload.put("metadata", new JSONObject(notification.getMetadata()));
            }

            byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, debugTarget.getAddress(), debugTarget.getPort());
            socket.send(packet);
            System.out.println("[NotificationService][DEBUG] Sent debug copy to 127.0.0.1:" + DEBUG_FORCE_PORT + " payload=" + payload);
        } catch (Exception e) {
            System.out.println("[NotificationService] Failed to send debug copy via socket: " + e.getMessage());
        }
    }

    public record PresenceChangeEvent(String userId, String status, String ip, int port, long timestamp) {
        public PresenceChangeEvent {
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(status, "status");
        }
    }
}
