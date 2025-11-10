package com.sketchflow.sketchflow_backend.udp;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void start() {
        try {
            socket = new DatagramSocket(DEFAULT_PORT);
            logger.info("UDP Server started on port: " + DEFAULT_PORT);

            executor.submit(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        // Decode packet
                        byte[] data = packet.getData();
                        int checksum = PacketUtils.crc32(data);
                        InetAddress senderAddress = packet.getAddress();
                        int senderPort = packet.getPort();

                        logger.info("Received packet from " + senderAddress + ":" + senderPort + ", checksum: " + checksum);

                        // Add sender to known clients
                        InetSocketAddress clientAddress = new InetSocketAddress(senderAddress, senderPort);
                        clients.add(clientAddress);

                        // Send ACK
                        String ackMessage = "ACK";
                        byte[] ackData = ackMessage.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, senderAddress, senderPort);
                        socket.send(ackPacket);

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
}