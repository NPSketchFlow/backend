package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.ActiveUserSession;
import com.sketchflow.sketchflow_backend.repository.ActiveUserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Service
public class ActiveUserService {

    private static final Logger logger = Logger.getLogger(ActiveUserService.class.getName());

    // Thread pool for user management
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // Scheduled executor for periodic cleanup
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private ActiveUserSessionRepository activeUserRepository;

    @Autowired
    private WhiteboardSessionService sessionService;

    public ActiveUserService() {
        // Start periodic cleanup of inactive users (every 1 minute)
        startPeriodicCleanup();
    }

    /**
     * Add user to session
     */
    public CompletableFuture<ActiveUserSession> joinSessionAsync(
            String sessionId,
            String userId,
            String username,
            String avatar) {

        return CompletableFuture.supplyAsync(() -> {
            // Check if user already in session
            Optional<ActiveUserSession> existing =
                activeUserRepository.findByUserIdAndSessionId(userId, sessionId);

            if (existing.isPresent()) {
                // Update activity and return existing session
                ActiveUserSession session = existing.get();
                session.updateActivity();
                return activeUserRepository.save(session);
            }

            // Create new active user session
            String id = UUID.randomUUID().toString();
            ActiveUserSession userSession = new ActiveUserSession();
            userSession.setId(id);
            userSession.setUserId(userId);
            userSession.setSessionId(sessionId);
            userSession.setUsername(username);
            userSession.setAvatar(avatar);
            userSession.setJoinedAt(LocalDateTime.now());
            userSession.setLastActivity(LocalDateTime.now());
            userSession.setCursorPosition(new ActiveUserSession.CursorPosition(0, 0));
            userSession.setCurrentTool("pen");
            userSession.setCurrentColor("#3B82F6");

            ActiveUserSession saved = activeUserRepository.save(userSession);

            // Add user to session
            sessionService.addUserToSession(sessionId, userId);

            logger.info("User " + userId + " joined session " + sessionId);
            return saved;
        }, executorService);
    }

    /**
     * Remove user from session
     */
    public CompletableFuture<Void> leaveSessionAsync(String sessionId, String userId) {
        return CompletableFuture.runAsync(() -> {
            activeUserRepository.deleteByUserIdAndSessionId(userId, sessionId);
            sessionService.removeUserFromSession(sessionId, userId);
            logger.info("User " + userId + " left session " + sessionId);
        }, executorService);
    }

    /**
     * Update user cursor position
     */
    public void updateCursorPosition(String sessionId, String userId, double x, double y) {
        executorService.submit(() -> {
            Optional<ActiveUserSession> userSessionOpt =
                activeUserRepository.findByUserIdAndSessionId(userId, sessionId);

            if (userSessionOpt.isPresent()) {
                ActiveUserSession userSession = userSessionOpt.get();
                userSession.setCursorPosition(new ActiveUserSession.CursorPosition(x, y));
                userSession.updateActivity();
                activeUserRepository.save(userSession);
            }
        });
    }

    /**
     * Update user tool and color
     */
    public void updateUserTool(String sessionId, String userId, String tool, String color) {
        executorService.submit(() -> {
            Optional<ActiveUserSession> userSessionOpt =
                activeUserRepository.findByUserIdAndSessionId(userId, sessionId);

            if (userSessionOpt.isPresent()) {
                ActiveUserSession userSession = userSessionOpt.get();
                userSession.setCurrentTool(tool);
                userSession.setCurrentColor(color);
                userSession.updateActivity();
                activeUserRepository.save(userSession);
            }
        });
    }

    /**
     * Update user activity timestamp
     */
    public void updateActivity(String sessionId, String userId) {
        executorService.submit(() -> {
            Optional<ActiveUserSession> userSessionOpt =
                activeUserRepository.findByUserIdAndSessionId(userId, sessionId);

            if (userSessionOpt.isPresent()) {
                ActiveUserSession userSession = userSessionOpt.get();
                userSession.updateActivity();
                activeUserRepository.save(userSession);
            }
        });
    }

    /**
     * Get all active users in a session
     */
    public List<ActiveUserSession> getSessionUsers(String sessionId) {
        return activeUserRepository.findBySessionId(sessionId);
    }

    /**
     * Periodic cleanup of inactive users (inactive for > 5 minutes)
     */
    private void startPeriodicCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
                List<ActiveUserSession> inactiveUsers =
                    activeUserRepository.findByLastActivityBefore(threshold);

                for (ActiveUserSession user : inactiveUsers) {
                    leaveSessionAsync(user.getSessionId(), user.getUserId());
                }

                if (!inactiveUsers.isEmpty()) {
                    logger.info("Cleaned up " + inactiveUsers.size() + " inactive users");
                }
            } catch (Exception e) {
                logger.severe("Error in periodic cleanup: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);

        logger.info("Started periodic inactive user cleanup");
    }

    public void shutdown() {
        executorService.shutdown();
        cleanupExecutor.shutdown();
    }
}

