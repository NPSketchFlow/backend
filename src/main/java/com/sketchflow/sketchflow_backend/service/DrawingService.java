package com.sketchflow.sketchflow_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class DrawingService {

    // A thread-safe set to hold all active WebSocket sessions
    private final Set<WebSocketSession> activeSessions = Collections.synchronizedSet(new HashSet<>());

    // This method will add a session to the active sessions when a client connects
    public void addSession(WebSocketSession session) {
        activeSessions.add(session);
    }

    // This method will remove a session when a client disconnects
    public void removeSession(WebSocketSession session) {
        activeSessions.remove(session);
    }

    // This method broadcasts drawing events to all connected clients
    public void broadcastDrawing(String drawingData) {
        // Iterate over all active sessions and send the drawing data
        synchronized (activeSessions) {
            for (WebSocketSession session : activeSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(drawingData));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
