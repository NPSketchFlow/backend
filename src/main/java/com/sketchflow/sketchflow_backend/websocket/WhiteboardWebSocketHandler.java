package com.sketchflow.sketchflow_backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.dto.DrawingActionRequest;
import com.sketchflow.sketchflow_backend.dto.WebSocketMessage;
import com.sketchflow.sketchflow_backend.service.ActiveUserService;
import com.sketchflow.sketchflow_backend.service.DrawingActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.sketchflow.sketchflow_backend.model.ChatMessage;
import com.sketchflow.sketchflow_backend.service.ChatService;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * WebSocket handler for real-time collaborative whiteboard
 * Uses NIO concepts and multi-threading for high-performance message handling
 */
@Component
public class WhiteboardWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = Logger.getLogger(WhiteboardWebSocketHandler.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Thread pool for message broadcasting (NIO-style async processing)
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(20);

    // Rate limiter: user -> last action timestamp
    private final ConcurrentHashMap<String, Long> rateLimiter = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 10; // 100 messages per second max

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private DrawingActionService drawingActionService;

    @Autowired
    private ActiveUserService activeUserService;

    @Autowired
    private ChatService chatService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: " + session.getId() +
                   " from " + session.getRemoteAddress());

        // Session ID and user info should be passed during connection or first message
        // For now, we'll wait for the JOIN message
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // Use thread pool for async message processing
        broadcastExecutor.submit(() -> {
            try {
                WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
                String messageType = wsMessage.getType();

                logger.fine("Received WebSocket message type: " + messageType +
                           " from session: " + session.getId());

                switch (messageType.toUpperCase()) {
                    case "JOIN":
                        handleJoinMessage(session, wsMessage);
                        break;
                    case "DRAW":
                        handleDrawMessage(session, wsMessage);
                        break;
                    case "ERASE":
                        handleEraseMessage(session, wsMessage);
                        break;
                    case "CLEAR":
                        handleClearMessage(session, wsMessage);
                        break;
                    case "CURSOR_MOVE":
                        handleCursorMove(session, wsMessage);
                        break;
                    case "TOOL_CHANGE":
                        handleToolChange(session, wsMessage);
                        break;
                    case "LEAVE":
                        handleLeaveMessage(session, wsMessage);
                        break;

                    case "CHAT_MESSAGE":
                        handleChatMessage(session, wsMessage);
                        break;

                    default:
                        logger.warning("Unknown message type: " + messageType);
                }

            } catch (Exception e) {
                logger.severe("Error handling WebSocket message: " + e.getMessage());
            }
        });
    }

    /**
     * Handle JOIN message - user joining the whiteboard session
     */
    private void handleJoinMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = extractSessionIdFromUri(session);
            String userId = message.getUserId();
            String username = message.getUsername();
            String avatar = message.getAvatar();

            // Add to session manager
            sessionManager.addSession(sessionId, session, userId);

            // Add to active users
            activeUserService.joinSessionAsync(sessionId, userId, username, avatar);

            // Broadcast to other users
            WebSocketMessage joinNotification = new WebSocketMessage();
            joinNotification.setType("USER_JOINED");
            joinNotification.setUserId(userId);
            joinNotification.setUsername(username);
            joinNotification.setAvatar(avatar);
            joinNotification.setTimestamp(System.currentTimeMillis());

            broadcastToSession(sessionId, joinNotification, session);

            logger.info("User " + userId + " joined whiteboard session " + sessionId);

        } catch (Exception e) {
            logger.severe("Error handling JOIN message: " + e.getMessage());
        }
    }

    /**
     * Handle DRAW message - drawing action from user
     */
    private void handleDrawMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            String userId = message.getUserId();

            // Rate limiting check
            if (!checkRateLimit(userId)) {
                logger.warning("Rate limit exceeded for user: " + userId);
                return;
            }

            // Create drawing action request
            DrawingActionRequest actionRequest = new DrawingActionRequest();
            actionRequest.setUserId(userId);
            actionRequest.setTool(message.getTool());
            actionRequest.setColor(message.getColor());
            actionRequest.setActionType("draw");
            actionRequest.setCoordinates(message.getCoordinates());

            // Save action asynchronously
            drawingActionService.saveActionAsync(sessionId, actionRequest);

            // Update user activity
            activeUserService.updateActivity(sessionId, userId);

            // Broadcast to all users in session (including sender for confirmation)
            message.setTimestamp(System.currentTimeMillis());
            broadcastToSession(sessionId, message, null);

        } catch (Exception e) {
            logger.severe("Error handling DRAW message: " + e.getMessage());
        }
    }

    /**
     * Handle ERASE message - delete a specific drawing action
     */
    private void handleEraseMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            String userId = message.getUserId();
            String actionId = message.getActionId();

            if (actionId == null || actionId.isEmpty()) {
                logger.warning("ERASE message missing actionId from user: " + userId);
                return;
            }

            // Delete the action asynchronously (already handled by REST API, but support via WebSocket too)
            drawingActionService.deleteActionAsync(sessionId, actionId);

            // Update user activity
            activeUserService.updateActivity(sessionId, userId);

            // Broadcast erase to all users in session (including sender for confirmation)
            message.setTimestamp(System.currentTimeMillis());
            broadcastToSession(sessionId, message, null);

            logger.info("Action " + actionId + " erased in session " + sessionId + " by user " + userId);

        } catch (Exception e) {
            logger.severe("Error handling ERASE message: " + e.getMessage());
        }
    }

    /**
     * Handle CLEAR message - clear entire canvas
     */
    private void handleClearMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            String userId = message.getUserId();

            // Clear all drawing actions
            drawingActionService.clearSessionActionsAsync(sessionId);

            // Broadcast clear to all users
            message.setTimestamp(System.currentTimeMillis());
            broadcastToSession(sessionId, message, null);

            logger.info("Canvas cleared for session " + sessionId + " by user " + userId);

        } catch (Exception e) {
            logger.severe("Error handling CLEAR message: " + e.getMessage());
        }
    }

    /**
     * Handle CURSOR_MOVE message - update user cursor position
     */
    private void handleCursorMove(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            String userId = message.getUserId();

            // Update cursor position
            if (message.getPosition() != null) {
                activeUserService.updateCursorPosition(
                    sessionId,
                    userId,
                    message.getPosition().getX(),
                    message.getPosition().getY()
                );
            }

            // Broadcast to others (not to sender)
            broadcastToSession(sessionId, message, session);

        } catch (Exception e) {
            logger.fine("Error handling CURSOR_MOVE: " + e.getMessage());
        }
    }

    /**
     * Handle TOOL_CHANGE message - user changed drawing tool or color
     */
    private void handleToolChange(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            String userId = message.getUserId();

            // Update user tool
            activeUserService.updateUserTool(sessionId, userId, message.getTool(), message.getColor());

            // Broadcast to others
            broadcastToSession(sessionId, message, session);

        } catch (Exception e) {
            logger.warning("Error handling TOOL_CHANGE: " + e.getMessage());
        }
    }

    /**
     * Handle LEAVE message - user leaving session
     */
    private void handleLeaveMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            String userId = message.getUserId();

            // Remove from active users
            activeUserService.leaveSessionAsync(sessionId, userId);

            // Broadcast to others
            message.setTimestamp(System.currentTimeMillis());
            broadcastToSession(sessionId, message, session);

            // Remove from session manager
            sessionManager.removeSession(session);

            logger.info("User " + userId + " left session " + sessionId);

        } catch (Exception e) {
            logger.warning("Error handling LEAVE: " + e.getMessage());
        }
    }
    /**
     * Handle CHAT_MESSAGE - a new chat message from user
     */
    private void handleChatMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String sessionId = sessionManager.getWhiteboardSessionId(session);
            if (sessionId == null) return;

            // 1. Save the message to the database
            // We save it first, which gives it a timestamp and ID
            ChatMessage savedMessage = chatService.saveMessage(message, sessionId);

            // 2. Prepare the message for broadcast
            // We use the server-generated timestamp for consistency
            message.setTimestamp(System.currentTimeMillis());
            // We use the username from the saved message for security
            message.setUsername(savedMessage.getSenderUsername());

            // 3. Broadcast to all users in the session (including the sender)
            broadcastToSession(sessionId, message, null);

            logger.info("Chat message from " + message.getUserId() + " in session " + sessionId);

        } catch (Exception e) {
            logger.severe("Error handling CHAT_MESSAGE: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = sessionManager.getWhiteboardSessionId(session);
        String userId = sessionManager.getUserId(session);

        logger.info("WebSocket connection closed: " + session.getId() +
                   ", status: " + status +
                   ", sessionId: " + sessionId +
                   ", userId: " + userId);

        if (sessionId != null && userId != null) {
            // Clean up user session
            activeUserService.leaveSessionAsync(sessionId, userId);

            // Notify others
            WebSocketMessage leaveMessage = new WebSocketMessage();
            leaveMessage.setType("USER_LEFT");
            leaveMessage.setUserId(userId);
            leaveMessage.setTimestamp(System.currentTimeMillis());

            broadcastToSession(sessionId, leaveMessage, session);
        }

        // Remove from session manager
        sessionManager.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.severe("WebSocket transport error for session " + session.getId() +
                     ": " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * Broadcast message to all users in a session (async using thread pool)
     * @param excludeSession - session to exclude from broadcast (null to broadcast to all)
     */
    private void broadcastToSession(String sessionId, WebSocketMessage message, WebSocketSession excludeSession) {
        Set<WebSocketSession> sessions = sessionManager.getSessionConnections(sessionId);

        if (sessions.isEmpty()) {
            return;
        }

        // Convert message to JSON once
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.severe("Error serializing message: " + e.getMessage());
            return;
        }

        // Parallel broadcast using thread pool
        CompletableFuture<?>[] futures = sessions.stream()
            .filter(s -> !s.equals(excludeSession) && s.isOpen())
            .map(s -> CompletableFuture.runAsync(() -> {
                try {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(jsonMessage));
                    }
                } catch (IOException e) {
                    logger.warning("Failed to send message to session " + s.getId() + ": " + e.getMessage());
                }
            }, broadcastExecutor))
            .toArray(CompletableFuture[]::new);

        // Wait for all broadcasts to complete (non-blocking)
        CompletableFuture.allOf(futures);
    }

    /**
     * Extract session ID from WebSocket URI
     */
    private String extractSessionIdFromUri(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String uri = session.getUri().getPath();
        // Extract from path like: /api/whiteboard/sessions/{sessionId}/ws
        String[] parts = uri.split("/");
        if (parts.length >= 5) {
            return parts[4];
        }
        return null;
    }

    /**
     * Rate limiting check (100 messages per second per user)
     */
    private boolean checkRateLimit(String userId) {
        long now = System.currentTimeMillis();
        Long lastTime = rateLimiter.get(userId);

        if (lastTime == null || (now - lastTime) >= RATE_LIMIT_MS) {
            rateLimiter.put(userId, now);
            return true;
        }
        return false;
    }

    public void shutdown() {
        broadcastExecutor.shutdown();
    }
}

