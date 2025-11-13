package com.sketchflow.sketchflow_backend.udp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.metrics.NetworkMetrics;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

                        // Copy exact payload bytes
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                        int checksum = PacketUtils.crc32(data);
                        InetAddress senderAddress = packet.getAddress();
                        int senderPort = packet.getPort();

                        String payloadText = new String(data, StandardCharsets.UTF_8).trim();
                        logger.info("Received packet from " + senderAddress + ":" + senderPort + ", checksum: " + checksum + ", payload='" + payloadText + "'");

                        // Add sender to known clients set (we still store the address)
                        InetSocketAddress clientAddress = new InetSocketAddress(senderAddress, senderPort);
                        clients.add(clientAddress);
                        logger.info("Known clients count=" + clients.size());

                        // Update metrics
                        if (networkMetrics != null) networkMetrics.incrementPacketsReceived();

                        // Try to parse JSON payload and handle heartbeat messages robustly
                        try {
                            Map<String, Object> json = OBJECT_MAPPER.readValue(payloadText, Map.class);
                            Object typeObj = json.get("type");
                            String type = typeObj != null ? String.valueOf(typeObj) : null;
                            if ("HEARTBEAT".equalsIgnoreCase(type)) {
                                String userId = json.get("userId") != null ? String.valueOf(json.get("userId")) : "unknown";
                                long clientTs = System.currentTimeMillis();
                                try {
                                    Object tsVal = json.get("timestamp");
                                    if (tsVal != null) clientTs = Long.parseLong(String.valueOf(tsVal));
                                } catch (Exception ignored) {}

                                if (onlineUserTracker != null) {
                                    onlineUserTracker.onHeartbeat(clientAddress, userId, clientTs);
                                    logger.info("Registered heartbeat: user='" + userId + "' at " + clientAddress);
                                }
                            }
                        } catch (Exception e) {
                            // not a JSON heartbeat or parse failed; log at info to help debugging
                            logger.info("Payload is not a valid HEARTBEAT JSON or parse failed: " + e.getMessage());
                        }

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

    // Return a snapshot copy of known clients for debugging / REST endpoints
    public List<String> getKnownClients() {
        List<String> list = new ArrayList<>();
        for (InetSocketAddress addr : clients) {
            list.add(addr.getAddress().getHostAddress() + ":" + addr.getPort());
        }
        return list;
    }

}
