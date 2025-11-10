package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.udp.UdpServer;
import com.sketchflow.sketchflow_backend.udp.UdpRetransmissionHandler;
import com.sketchflow.sketchflow_backend.udp.PacketUtils;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    private final UdpServer udpServer;
    private final UdpRetransmissionHandler retransmissionHandler;
    private int priority = 1; // Default priority

    public NotificationService(UdpServer udpServer, UdpRetransmissionHandler retransmissionHandler) {
        this.udpServer = udpServer;
        this.retransmissionHandler = retransmissionHandler;
    }

    public void sendNotification(Notification notification) {
        try {
            byte[] serializedData = PacketUtils.serialize(notification);
            udpServer.broadcast(serializedData);
            logger.info("Notification broadcasted: " + notification);
        } catch (Exception e) {
            logger.severe("Failed to broadcast notification: " + e.getMessage());
        }
    }

    public void sendNotificationTo(InetSocketAddress target, Notification notification) {
        try {
            byte[] serializedData = PacketUtils.serialize(notification);
            boolean success = retransmissionHandler.sendWithRetransmission(
                    udpServer.getSocket(),
                    PacketUtils.createPacket(serializedData, target),
                    target,
                    3, // maxRetries
                    1000 // baseTimeoutMs
            );
            if (success) {
                logger.info("Notification sent to " + target + ": " + notification);
            } else {
                logger.warning("Failed to send notification to " + target);
            }
        } catch (Exception e) {
            logger.severe("Error sending notification to " + target + ": " + e.getMessage());
        }
    }

    public void notifyNewVoice(String fileId, String senderId) {
        Notification notification = new Notification("NEW_VOICE", fileId, senderId, System.currentTimeMillis(), priority);
        sendNotification(notification);
    }
}
