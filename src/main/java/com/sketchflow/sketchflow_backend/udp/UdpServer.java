package com.sketchflow.sketchflow_backend.udp;

import com.sketchflow.sketchflow_backend.metrics.NetworkMetrics;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
public class UdpServer {

    private static final Logger logger = Logger.getLogger(UdpServer.class.getName());
    private static final int DEFAULT_PORT = 9876;

    private DatagramSocket socket;
    private final Set<InetSocketAddress> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${sketchflow.udp.port:9876}")
    private int udpPort;

    private final OnlineUserTracker onlineUserTracker;
    private final NetworkMetrics networkMetrics;

    public UdpServer(OnlineUserTracker onlineUserTracker, NetworkMetrics networkMetrics) {
        this.onlineUserTracker = onlineUserTracker;
        this.networkMetrics = networkMetrics;
    }

    @PostConstruct
    public void start() {
        int portToUse = udpPort > 0 ? udpPort : DEFAULT_PORT;
        try {
            socket = new DatagramSocket(portToUse);
            logger.info("UDP Server started on port: " + portToUse);

            executor.submit(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[2048];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        // Decode packet
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                        int checksum = PacketUtils.crc32(data);
                        InetAddress senderAddress = packet.getAddress();
                        int senderPort = packet.getPort();

                        logger.info("Received packet from " + senderAddress + ":" + senderPort + ", checksum: " + checksum);

                        // Add sender to known clients
                        InetSocketAddress clientAddress = new InetSocketAddress(senderAddress, senderPort);
                        clients.add(clientAddress);

                        // Update metrics
                        if (networkMetrics != null) networkMetrics.incrementPacketsReceived();

                        // If payload appears to be a heartbeat JSON containing userId and timestamp, try to update tracker
                        try {
                            // naive parse: look for "userId" and "timestamp" strings
                            String payload = new String(data, StandardCharsets.UTF_8);
                            if (payload.contains("userId") && payload.contains("timestamp")) {
                                // crude parsing to extract values
                                String userId = extractJsonField(payload, "userId");
                                String tsStr = extractJsonField(payload, "timestamp");
                                long clientTs = System.currentTimeMillis();
                                try { clientTs = Long.parseLong(tsStr); } catch (Exception ignored) {}
                                if (onlineUserTracker != null) onlineUserTracker.onHeartbeat(clientAddress, userId, clientTs);
                            }
                        } catch (Exception ignored) {}

                        // Send ACK
                        String ackMessage = "ACK";
                        byte[] ackData = ackMessage.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, senderAddress, senderPort);
                        socket.send(ackPacket);
                        if (networkMetrics != null) networkMetrics.incrementAcksSent();

                        logger.info("Sent ACK to " + senderAddress + ":" + senderPort);
                    } catch (IOException e) {
                        logger.warning("Error receiving or processing packet: " + e.getMessage());
                    }
                }
            });
        } catch (SocketException e) {
            logger.severe("Failed to start UDP server: " + e.getMessage());
        }
    }

    public void broadcast(byte[] data) {
        for (InetSocketAddress client : clients) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, client.getAddress(), client.getPort());
                socket.send(packet);
                logger.info("Broadcasted packet to " + client.getAddress() + ":" + client.getPort());
            } catch (IOException e) {
                logger.warning("Failed to broadcast to " + client.getAddress() + ":" + client.getPort() + ", error: " + e.getMessage());
            }
        }
    }

    public void sendNotification(byte[] payload) {
        // Placeholder for future implementation
        logger.info("sendNotification called with payload of size: " + payload.length);
    }

    // Add a method to trigger broadcasting
    public void triggerBroadcast(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        broadcast(data);
        logger.info("Triggered broadcast with message: " + message);
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    // Very small helper to extract simple JSON string/number fields (not robust) used only for heartbeat basic parsing
    private String extractJsonField(String json, String field) {
        String needle = "\"" + field + "\"";
        int idx = json.indexOf(needle);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return null;
        int start = colon + 1;
        // skip spaces
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        char c = json.charAt(start);
        if (c == '"') {
            int end = json.indexOf('"', start + 1);
            if (end == -1) return null;
            return json.substring(start + 1, end);
        } else {
            // number
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            return json.substring(start, end);
        }
    }
}