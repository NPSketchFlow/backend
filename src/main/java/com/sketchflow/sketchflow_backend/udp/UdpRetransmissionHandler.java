package com.sketchflow.sketchflow_backend.udp;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Component
public class UdpRetransmissionHandler {

    private static final Logger logger = Logger.getLogger(UdpRetransmissionHandler.class.getName());

    public boolean sendWithRetransmission(DatagramSocket socket, DatagramPacket packet, InetSocketAddress target, int maxRetries, long baseTimeoutMs) {
        int retries = 0;
        long timeout = baseTimeoutMs;
        byte[] ackBuffer = new byte[1024];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);

        while (retries <= maxRetries) {
            try {
                // Send the packet
                socket.send(packet);
                logger.info("Sent packet to " + target + " (Attempt: " + (retries + 1) + ")");

                // Set timeout and wait for ACK
                socket.setSoTimeout((int) timeout);
                socket.receive(ackPacket);

                // Check if the ACK matches the sequence ID
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8);
                if (ackMessage.contains("ACK")) { // Simplified ACK check
                    logger.info("Received ACK from " + ackPacket.getAddress() + ":" + ackPacket.getPort());
                    return true;
                }

            } catch (SocketTimeoutException e) {
                logger.warning("Timeout waiting for ACK (Attempt: " + (retries + 1) + ", Timeout: " + timeout + "ms)");
                retries++;
                timeout *= 2; // Exponential backoff
            } catch (IOException e) {
                logger.severe("Error sending or receiving packet: " + e.getMessage());
                return false;
            }
        }

        logger.severe("Failed to receive ACK after " + maxRetries + " retries.");
        return false;
    }
}
