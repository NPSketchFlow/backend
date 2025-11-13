package com.sketchflow.sketchflow_backend.nio;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * NIO-based test client for Whiteboard NIO Server
 * Demonstrates client-side NIO socket programming
 */
public class WhiteboardNioClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SocketChannel socketChannel;
    private boolean running = false;

    public void connect() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(HOST, PORT));
        socketChannel.configureBlocking(false);
        running = true;

        System.out.println("Connected to Whiteboard NIO Server at " + HOST + ":" + PORT);

        // Start reader thread
        new Thread(this::readMessages).start();
    }

    public void sendMessage(Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        ByteBuffer buffer = ByteBuffer.wrap((json + "\n").getBytes(StandardCharsets.UTF_8));

        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }

        System.out.println("Sent: " + json);
    }

    private void readMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        StringBuilder messageBuffer = new StringBuilder();

        while (running) {
            try {
                buffer.clear();
                int bytesRead = socketChannel.read(buffer);

                if (bytesRead == -1) {
                    System.out.println("Server closed connection");
                    break;
                }

                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String received = new String(data, StandardCharsets.UTF_8);
                    messageBuffer.append(received);

                    // Process complete messages (terminated by newline)
                    String messages = messageBuffer.toString();
                    if (messages.contains("\n")) {
                        String[] lines = messages.split("\n");
                        for (int i = 0; i < lines.length - 1; i++) {
                            processMessage(lines[i]);
                        }
                        messageBuffer = new StringBuilder(lines[lines.length - 1]);
                    }
                }

                Thread.sleep(10);

            } catch (Exception e) {
                System.err.println("Error reading: " + e.getMessage());
            }
        }
    }

    private void processMessage(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            System.out.println("Received: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    public void close() throws IOException {
        running = false;
        if (socketChannel != null) {
            socketChannel.close();
        }
    }

    public static void main(String[] args) {
        WhiteboardNioClient client = new WhiteboardNioClient();

        try {
            client.connect();

            Scanner scanner = new Scanner(System.in);
            System.out.println("\nCommands:");
            System.out.println("  sync <sessionId> <userId> - Join a session");
            System.out.println("  draw <actionId> - Send drawing action");
            System.out.println("  ping - Send ping");
            System.out.println("  quit - Exit");

            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("quit")) {
                    break;
                }

                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();

                Map<String, Object> message = new HashMap<>();

                switch (command) {
                    case "sync":
                        if (parts.length < 3) {
                            System.out.println("Usage: sync <sessionId> <userId>");
                            continue;
                        }
                        message.put("type", "SYNC_REQUEST");
                        message.put("sessionId", parts[1]);
                        message.put("userId", parts[2]);
                        break;

                    case "draw":
                        String actionId = parts.length > 1 ? parts[1] : UUID.randomUUID().toString();
                        message.put("type", "DRAWING_ACTION");
                        message.put("actionId", actionId);
                        message.put("tool", "pen");
                        message.put("color", "#FF0000");
                        message.put("timestamp", System.currentTimeMillis());
                        break;

                    case "ping":
                        message.put("type", "PING");
                        message.put("timestamp", System.currentTimeMillis());
                        break;

                    default:
                        System.out.println("Unknown command: " + command);
                        continue;
                }

                client.sendMessage(message);
            }

            client.close();
            scanner.close();
            System.out.println("Client disconnected");

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

