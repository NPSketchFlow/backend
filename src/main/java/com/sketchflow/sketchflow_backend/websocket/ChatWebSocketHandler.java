package com.sketchflow.sketchflow_backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.dto.WebSocketMessage;
import com.sketchflow.sketchflow_backend.model.DirectMessage;
import com.sketchflow.sketchflow_backend.service.DirectMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.sketchflow.sketchflow_backend.security.JwtTokenProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.security.Principal; // Keep this one

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = Logger.getLogger(ChatWebSocketHandler.class.getName());

    // This map stores the active session for each user (Username -> Session)
    private final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    // --- ADD THIS NEW MAP ---
    // Stores the authenticated username for a given session ID
    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    @Autowired
    private DirectMessageService dmService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- ADD THESE 3 NEW AUTOWIRED FIELDS ---
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService; // This is used by the provider

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // We no longer check for Principal. We wait for an 'AUTH' message.
        logger.info("New chat connection, awaiting auth. Session: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String username = sessionUserMap.get(session); // Check if session is authenticated

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

            if (username == null) { // If NOT authenticated
                if ("AUTH".equals(wsMessage.getType())) {
                    String token = wsMessage.getToken();
                    if (token != null && jwtTokenProvider.validateToken(token)) {
                        // Token is valid, authenticate the session
                        username = jwtTokenProvider.getUsernameFromToken(token);
                        userSessionMap.put(username, session);
                        sessionUserMap.put(session, username);
                        logger.info("User '" + username + "' authenticated and connected to Chat WS.");
                        // Send a success message back
                        session.sendMessage(new TextMessage("{\"type\": \"AUTH_SUCCESS\"}"));
                    } else {
                        logger.warning("Invalid token received. Closing session.");
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
                    }
                } else {
                    // First message was not AUTH
                    logger.warning("First message was not AUTH. Closing session.");
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Not authenticated"));
                }
                return; // Stop processing until authenticated
            }

            // If we are here, the user is authenticated
            if ("DM".equals(wsMessage.getType())) {
                String receiverUsername = wsMessage.getReceiverUsername();
                String messageContent = wsMessage.getMessageContent();

                if (receiverUsername == null || messageContent == null) {
                    logger.warning("Invalid DM received: " + message.getPayload());
                    return;
                }

                // 1. Save the message to the database
                DirectMessage savedDm = dmService.saveMessage(username, receiverUsername, messageContent);

                // 2. Send the message to the receiver if they are online
                WebSocketSession receiverSession = userSessionMap.get(receiverUsername);
                if (receiverSession != null && receiverSession.isOpen()) {
                    receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(savedDm)));
                }

                // 3. Send the message back to the sender (as confirmation)
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(savedDm)));
            }

        } catch (Exception e) {
            logger.severe("Error handling WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = sessionUserMap.remove(session); // Get user from our new map
        if (username != null) {
            userSessionMap.remove(username); // Remove from the username map
            logger.info("User '" + username + "' disconnected from Chat WebSocket. Total users: " + userSessionMap.size());
        } else {
            logger.info("Unauthenticated chat session closed. Status: " + status);
        }
    }
}