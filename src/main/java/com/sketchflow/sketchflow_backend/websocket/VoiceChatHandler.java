package com.sketchflow.sketchflow_backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class VoiceChatHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceChatHandler.class);

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // username -> session
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public VoiceChatHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract both userId and username (support either param)
        Map<String, String> params = extractQueryParams(session.getUri());
        String userId = params.getOrDefault("userId", params.get("username"));
        String username = params.get("username");
        String decodedUserId = userId != null ? URLDecoder.decode(userId, StandardCharsets.UTF_8) : null;
        String decodedUsername = username != null ? URLDecoder.decode(username, StandardCharsets.UTF_8) : null;
        logger.info("WebSocket connection opened for userId={} username={}", decodedUserId, decodedUsername);

        if (decodedUserId == null && (decodedUsername == null || decodedUsername.isEmpty())) {
            logger.warn("No userId/username provided in WebSocket connection, closing session {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // choose session key: prefer userId if provided, else username
        String sessionKey = decodedUserId != null ? decodedUserId : decodedUsername;
        // store session under both id and username when available to make targeting robust
        if (decodedUserId != null && decodedUsername != null && !decodedUsername.isBlank()) {
            sessions.put(decodedUserId, session);
            sessions.put(decodedUsername, session);
        } else if (sessionKey != null) {
            sessions.put(sessionKey, session);
        }

        // Update user online status in MongoDB
        Optional<User> maybeUser = Optional.empty();
        if (decodedUserId != null) {
            maybeUser = userRepository.findById(decodedUserId);
        }
        if (maybeUser.isEmpty() && decodedUsername != null) {
            maybeUser = userRepository.findByUsername(decodedUsername);
        }
        User user;
        if (maybeUser.isPresent()) {
            user = maybeUser.get();
        } else {
            user = new User();
            // if we have an explicit id, set it so the document uses that id
            if (decodedUserId != null) user.setId(decodedUserId);
            user.setUsername(decodedUsername != null ? decodedUsername : decodedUserId);
            user.setEmail((decodedUsername != null ? decodedUsername : decodedUserId) + "@example.com");
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setOnline(true);
        user.setLastActive(LocalDateTime.now());
        user.setLoginTime(LocalDateTime.now());
        userRepository.save(user);

        // Broadcast status to all connected clients
        broadcastUserStatus(user.getId(), user.getUsername(), "online");
        broadcastOnlineUsersList();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // We might accept ping/pong or simple messages in future. For now log.
        logger.debug("Received WS message: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // extract params and prefer userId if present
        Map<String, String> params = extractQueryParams(session.getUri());
        String userId = params.getOrDefault("userId", params.get("username"));
        String username = params.get("username");
        String decodedUserId = userId != null ? URLDecoder.decode(userId, StandardCharsets.UTF_8) : null;
        String decodedUsername = username != null ? URLDecoder.decode(username, StandardCharsets.UTF_8) : null;
        logger.info("WebSocket connection closed for userId={} username={} status={}", decodedUserId, decodedUsername, status);

        String sessionKey = decodedUserId != null ? decodedUserId : decodedUsername;
        if (sessionKey != null) {
            // remove both possible keys
            if (decodedUserId != null) sessions.remove(decodedUserId);
            if (decodedUsername != null) sessions.remove(decodedUsername);

            if (decodedUserId != null) {
                userRepository.findById(decodedUserId).ifPresent(u -> {
                    u.setOnline(false);
                    u.setLastActive(LocalDateTime.now());
                    u.setLogoutTime(LocalDateTime.now());
                    userRepository.save(u);
                });
            } else if (decodedUsername != null) {
                userRepository.findByUsername(decodedUsername).ifPresent(u -> {
                    u.setOnline(false);
                    u.setLastActive(LocalDateTime.now());
                    u.setLogoutTime(LocalDateTime.now());
                    userRepository.save(u);
                });
            }

            broadcastUserStatus(decodedUserId, decodedUsername, "offline");
            broadcastOnlineUsersList();
        }
    }

    private void broadcastUserStatus(String userId, String username, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "USER_STATUS");
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("status", status);
        payload.put("timestamp", LocalDateTime.now().toString());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            logger.error("Failed to serialize user status", e);
            return;
        }

        sessions.values().forEach(s -> {
            try {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.warn("Failed to send status to session", e);
            }
        });
        logger.info("Broadcasted USER_STATUS for {}({}) -> {} to {} sessions", username, userId, status, sessions.size());
    }

    public void broadcastOnlineUsersList() {
        List<User> onlineUsers = userRepository.findByOnlineTrue();

        List<Map<String, Object>> users = onlineUsers.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", u.getId());
            m.put("username", u.getUsername());
            m.put("status", u.isOnline() ? "online" : "offline");
            m.put("lastActive", u.getLastActive() != null ? u.getLastActive().toString() : null);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ONLINE_USERS");
        payload.put("users", users);
        payload.put("timestamp", LocalDateTime.now().toString());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            logger.error("Failed to serialize online users list", e);
            return;
        }

        sessions.values().forEach(s -> {
            try {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.warn("Failed to send online list to session", e);
            }
        });

        logger.info("Broadcasted online-users list to {} sessions", sessions.size());
    }

    public void broadcastNotification(com.sketchflow.sketchflow_backend.model.Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NOTIFICATION");
        payload.put("id", notification.getId());
        payload.put("message", notification.getMessage());
        payload.put("fileId", notification.getFileId());
        payload.put("senderId", notification.getSenderId());
        // Resolve sender username for UI convenience (may be null)
        try {
            if (notification.getSenderId() != null && userRepository != null) {
                var maybeSender = userRepository.findById(notification.getSenderId());
                if (maybeSender.isPresent()) {
                    payload.put("senderUsername", maybeSender.get().getUsername());
                } else {
                    var maybeByName = userRepository.findByUsername(notification.getSenderId());
                    if (maybeByName.isPresent()) {
                        payload.put("senderUsername", maybeByName.get().getUsername());
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("Failed to resolve sender username for notification {}: {}", notification.getId(), ex.getMessage());
        }
        payload.put("receiverId", notification.getReceiverId());
        payload.put("timestamp", notification.getTimestamp());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            logger.error("Failed to serialize notification", e);
            return;
        }

        // If this notification targets a receiverId and that session exists, send only to that session
        String targetReceiver = notification.getReceiverId();
        if (targetReceiver != null && !targetReceiver.isBlank()) {
            WebSocketSession s = sessions.get(targetReceiver);
            // if not found, try resolving via DB: maybe receiverId is an id and session stored under username or vice versa
            if ((s == null || !s.isOpen()) && userRepository != null) {
                try {
                    var maybeUserById = userRepository.findById(targetReceiver);
                    if (maybeUserById.isPresent()) {
                        String username = maybeUserById.get().getUsername();
                        if (username != null) s = sessions.get(username);
                    } else {
                        var maybeUserByUsername = userRepository.findByUsername(targetReceiver);
                        if (maybeUserByUsername.isPresent()) {
                            String id = maybeUserByUsername.get().getId();
                            if (id != null) s = sessions.get(id);
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to resolve targetReceiver {} via DB: {}", targetReceiver, ex.getMessage());
                }
            }

            if (s != null) {
                try {
                    if (s.isOpen()) s.sendMessage(new TextMessage(json));
                    logger.info("Sent notification {} to target session {}", notification.getId(), targetReceiver);
                } catch (IOException e) {
                    logger.warn("Failed to send notification to target session {}", targetReceiver, e);
                }
                return;
            }
        }

        // otherwise broadcast to all
        sessions.values().forEach(s -> {
            try {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.warn("Failed to send notification to session", e);
            }
        });

        logger.info("Broadcasted notification {} to {} sessions", notification.getId(), sessions.size());
    }

    private String extractUsername(URI uri) {
        Map<String, String> params = extractQueryParams(uri);
        return params.getOrDefault("username", params.get("userId"));
    }

    private Map<String, String> extractQueryParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        if (uri == null) return map;
        String query = uri.getQuery();
        if (query == null || query.isBlank()) return map;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}
