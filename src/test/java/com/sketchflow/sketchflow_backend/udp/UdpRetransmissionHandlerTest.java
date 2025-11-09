package com.sketchflow.sketchflow_backend.udp;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UdpRetransmissionHandlerTest {

    private static final Logger logger = Logger.getLogger(UdpRetransmissionHandlerTest.class.getName());

    @Test
    public void testSendWithRetransmissionSuccess() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        UdpRetransmissionHandler handler = new UdpRetransmissionHandler();

        // Prepare a test packet
        String message = "Test Packet";
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        InetSocketAddress target = new InetSocketAddress("127.0.0.1", 9876);
        DatagramPacket packet = new DatagramPacket(data, data.length, target);

        // Test the sendWithRetransmission method
        boolean success = handler.sendWithRetransmission(socket, packet, target, 3, 1000);

        // Assert that the method returns true (ACK received)
        assertTrue(success, "Expected ACK to be received");
    }

    @Test
    public void testSendWithRetransmissionFailure() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        UdpRetransmissionHandler handler = new UdpRetransmissionHandler();

        // Prepare a test packet to an unreachable port
        String message = "Test Packet";
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        InetSocketAddress target = new InetSocketAddress("127.0.0.1", 9999); // Unreachable port
        DatagramPacket packet = new DatagramPacket(data, data.length, target);

        // Test the sendWithRetransmission method
        boolean success = handler.sendWithRetransmission(socket, packet, target, 3, 1000);

        // Assert that the method returns false (no ACK received)
        assertFalse(success, "Expected no ACK to be received");
    }
}
