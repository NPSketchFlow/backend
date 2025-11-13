package com.sketchflow.sketchflow_backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * Thread-safe WebSocket session manager
 * Manages connections per whiteboard session using concurrent data structures
 */
@Component
public class WebSocketSessionManager {

    private static final Logger logger = Logger.getLogger(WebSocketSessionManager.class.getName());

    // Map: sessionId -> Set of WebSocket sessions
    private final Map<String, Set<WebSocketSession>> sessionConnections = new ConcurrentHashMap<>();

    // Map: WebSocket session ID -> whiteboard sessionId
    private final Map<String, String> sessionMapping = new ConcurrentHashMap<>();

    // Map: WebSocket session ID -> userId
    private final Map<String, String> userMapping = new ConcurrentHashMap<>();

    /**
     * Add a WebSocket connection to a whiteboard session
     */
    public synchronized void addSession(String whiteboardSessionId, WebSocketSession wsSession, String userId) {
        sessionConnections.computeIfAbsent(whiteboardSessionId, k -> new CopyOnWriteArraySet<>())
                         .add(wsSession);
        sessionMapping.put(wsSession.getId(), whiteboardSessionId);
        userMapping.put(wsSession.getId(), userId);

        logger.info("Added WebSocket session " + wsSession.getId() +
                   " to whiteboard session " + whiteboardSessionId +
                   " for user " + userId +
                   ". Total connections in session: " + sessionConnections.get(whiteboardSessionId).size());
    }

    /**
     * Remove a WebSocket connection
     */
    public synchronized void removeSession(WebSocketSession wsSession) {
        String wsSessionId = wsSession.getId();
        String whiteboardSessionId = sessionMapping.remove(wsSessionId);
        String userId = userMapping.remove(wsSessionId);

        if (whiteboardSessionId != null) {
            Set<WebSocketSession> sessions = sessionConnections.get(whiteboardSessionId);
            if (sessions != null) {
                sessions.remove(wsSession);
                logger.info("Removed WebSocket session " + wsSessionId +
                           " from whiteboard session " + whiteboardSessionId +
                           " for user " + userId +
                           ". Remaining connections: " + sessions.size());

                // Clean up empty session sets
                if (sessions.isEmpty()) {
                    sessionConnections.remove(whiteboardSessionId);
                }
            }
        }
    }

    /**
     * Get all WebSocket sessions for a whiteboard session
     */
    public Set<WebSocketSession> getSessionConnections(String whiteboardSessionId) {
        return sessionConnections.getOrDefault(whiteboardSessionId, Collections.emptySet());
    }

    /**
     * Get whiteboard session ID for a WebSocket session
     */
    public String getWhiteboardSessionId(WebSocketSession wsSession) {
        return sessionMapping.get(wsSession.getId());
    }

    /**
     * Get user ID for a WebSocket session
     */
    public String getUserId(WebSocketSession wsSession) {
        return userMapping.get(wsSession.getId());
    }

    /**
     * Get total connection count for a whiteboard session
     */
    public int getConnectionCount(String whiteboardSessionId) {
        Set<WebSocketSession> sessions = sessionConnections.get(whiteboardSessionId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get all active whiteboard session IDs
     */
    public Set<String> getActiveSessionIds() {
        return sessionConnections.keySet();
    }

    /**
     * Check if session is open
     */
    public boolean isSessionOpen(WebSocketSession wsSession) {
        return wsSession != null && wsSession.isOpen();
    }

    /**
     * Close all connections for a whiteboard session
     */
    public synchronized void closeAllSessionConnections(String whiteboardSessionId) {
        Set<WebSocketSession> sessions = sessionConnections.remove(whiteboardSessionId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                    sessionMapping.remove(session.getId());
                    userMapping.remove(session.getId());
                } catch (IOException e) {
                    logger.warning("Error closing WebSocket session: " + e.getMessage());
                }
            }
            logger.info("Closed " + sessions.size() + " WebSocket connections for whiteboard session " + whiteboardSessionId);
        }
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWhiteboardSessions", sessionConnections.size());
        stats.put("totalWebSocketConnections", sessionMapping.size());

        Map<String, Integer> sessionCounts = new HashMap<>();
        for (Map.Entry<String, Set<WebSocketSession>> entry : sessionConnections.entrySet()) {
            sessionCounts.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("connectionsPerSession", sessionCounts);

        return stats;
    }
}

