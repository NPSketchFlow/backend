package com.sketchflow.sketchflow_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.model.ChatMessage;
import com.sketchflow.sketchflow_backend.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final ChatMessageRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, WebSocketSession> webSessions = new ConcurrentHashMap<>();
    private final Set<TcpClientConnection> tcpClients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Constructor injection for the repository
    public ChatService(ChatMessageRepository repo) {
        this.repo = repo;
    }

    // Save and broadcast a message
    public void handleIncomingMessage(String sender, String content, String type) {
        ChatMessage message = new ChatMessage(sender, content, Instant.now(), type);
        repo.save(message); // Save the message

        // Prepare the payload for broadcasting
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("content", content);
        payload.put("timestamp", message.getTimestamp().toString());
        payload.put("type", type);

        broadcast(payload); // Broadcast the message to clients
    }

    // Save a message without broadcasting
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(Instant.now()); // Set timestamp if it's not provided
        }
        return repo.save(chatMessage); // Save and return the message
    }

    // Get the latest 200 messages sorted by timestamp
    public List<ChatMessage> getHistory() {
        List<ChatMessage> latest = repo.findTop200ByOrderByTimestampDesc(); // Fetch the latest 200 messages
        latest.sort(Comparator.comparing(ChatMessage::getTimestamp)); // Sort by timestamp in ascending order
        return latest;
    }

    // Register and manage WebSocket clients
    public void registerWebSocket(WebSocketSession session) {
        webSessions.put(session.getId(), session);
    }

    public void unregisterWebSocket(WebSocketSession session) {
        webSessions.remove(session.getId());
    }

    // Register and manage TCP clients
    public void registerTcpClient(TcpClientConnection conn) {
        tcpClients.add(conn);
    }

    public void unregisterTcpClient(TcpClientConnection conn) {
        tcpClients.remove(conn);
        conn.close();
    }

    // Broadcast message to WebSocket and TCP clients
    private void broadcast(Object payload) {
        try {
            String json = mapper.writeValueAsString(payload); // Convert payload to JSON string

            // Send to WebSocket clients
            webSessions.values().forEach(ws -> {
                if (ws.isOpen()) {
                    try {
                        ws.sendMessage(new TextMessage(json));
                    } catch (IOException ignored) {
                    }
                }
            });

            // Send to TCP clients
            tcpClients.forEach(conn -> conn.send(json + "\n"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Nested class for managing TCP client connections
    public static class TcpClientConnection {
        private final java.io.PrintWriter writer;

        public TcpClientConnection(java.io.OutputStream out) {
            this.writer = new java.io.PrintWriter(out, true);
        }

        public void send(String s) {
            try {
                writer.println(s);
                writer.flush();
            } catch (Exception ignored) {
            }
        }

        public void close() {
            try {
                writer.close();
            } catch (Exception ignored) {
            }
        }
    }
}