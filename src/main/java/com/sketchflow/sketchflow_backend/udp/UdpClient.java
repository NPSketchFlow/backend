package com.sketchflow.sketchflow_backend.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class UdpClient {

    private static final Logger logger = Logger.getLogger(UdpClient.class.getName());

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // Localhost
        int serverPort = 9876; // Default port of UdpServer

        try (DatagramSocket socket = new DatagramSocket()) {
            // Send a test packet to the server
            String message = "Hello, UDP Server!";
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);

            DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverInetAddress, serverPort);
            socket.send(sendPacket);
            logger.info("Sent packet to server: " + message);

            // Receive the ACK from the server
            byte[] buffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            String ackMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
            logger.info("Received ACK from server: " + ackMessage);
        } catch (Exception e) {
            logger.severe("Error in UDP Client: " + e.getMessage());
        }

        // Listen for broadcast messages
        new Thread(() -> {
            try (DatagramSocket listenSocket = new DatagramSocket(9877)) { // Listening on a different port
                byte[] buffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    listenSocket.receive(receivePacket);
                    String broadcastMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                    logger.info("Received broadcast message: " + broadcastMessage);
                }
            } catch (Exception e) {
                logger.severe("Error in listening for broadcast messages: " + e.getMessage());
            }
        }).start();
    }
}
