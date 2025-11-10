package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.udp.OnlineUserTracker;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class NotificationService {

    private final OnlineUserTracker onlineUserTracker;
    private static final int FALLBACK_PORT = 9876; // used when no online users found for local testing

    public NotificationService(OnlineUserTracker onlineUserTracker) {
        this.onlineUserTracker = onlineUserTracker;
    }

    public void notifyNewVoice(String fileId, String senderId) {
        Notification n = new Notification("NEW_VOICE", fileId, senderId, System.currentTimeMillis(), 1);
        sendNotification(n);
    }

    public void sendNotification(Notification n) {
        try {
            List<OnlineUserTracker.OnlineUserInfo> users = onlineUserTracker.listOnlineUsers();
            if (users == null || users.isEmpty()) {
                System.out.println("[NotificationService] No online users found, sending to localhost fallback");
                sendNotificationTo(new InetSocketAddress("127.0.0.1", FALLBACK_PORT), n);
                return;
            }

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(false);
                for (OnlineUserTracker.OnlineUserInfo u : users) {
                    if (u == null) continue;
                    String host = u.getIp();
                    int port = u.getPort();
                    if (host == null || port <= 0) continue;
                    InetSocketAddress target = new InetSocketAddress(host, port);
                    try {
                        sendUsingSocket(socket, target, n);
                    } catch (Exception ex) {
                        System.out.println("[NotificationService] Failed to send to " + host + ":" + port + " - " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[NotificationService] Error in sendNotification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendNotificationTo(InetSocketAddress target, Notification n) {
        if (target == null) return;
        try (DatagramSocket socket = new DatagramSocket()) {
            sendUsingSocket(socket, target, n);
        } catch (Exception e) {
            System.out.println("[NotificationService] Error sending to target " + target + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper that does the actual packet send using the provided socket
    private void sendUsingSocket(DatagramSocket socket, InetSocketAddress target, Notification n) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("type", n.getType());
        payload.put("fileId", n.getFileId());
        payload.put("senderId", n.getSenderId());
        payload.put("timestamp", n.getTimestamp());
        payload.put("priority", n.getPriority());

        byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);

        InetAddress addr;
        try {
            addr = InetAddress.getByName(target.getHostString());
        } catch (Exception e) {
            // fallback: try ip from target.getAddress()
            addr = target.getAddress();
            if (addr == null) throw e;
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, addr, target.getPort());
        socket.send(packet);
        System.out.println("[NotificationService] Sent notification to " + addr.getHostAddress() + ":" + target.getPort() + " payload=" + payload);
    }
}
