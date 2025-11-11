package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.dto.SessionCreateRequest;
import com.sketchflow.sketchflow_backend.model.WhiteboardSession;
import com.sketchflow.sketchflow_backend.repository.ActiveUserSessionRepository;
import com.sketchflow.sketchflow_backend.repository.DrawingActionRepository;
import com.sketchflow.sketchflow_backend.repository.WhiteboardSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
public class WhiteboardSessionService {

    private static final Logger logger = Logger.getLogger(WhiteboardSessionService.class.getName());

    // Thread pool for async operations
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    private WhiteboardSessionRepository sessionRepository;

    @Autowired
    private ActiveUserSessionRepository activeUserSessionRepository;

    @Autowired
    private DrawingActionRepository drawingActionRepository;

    /**
     * Create a new whiteboard session asynchronously
     */
    public CompletableFuture<WhiteboardSession> createSessionAsync(SessionCreateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String sessionId = UUID.randomUUID().toString();
            WhiteboardSession session = new WhiteboardSession(
                sessionId,
                request.getName(),
                request.getCreatedBy(),
                request.getMaxUsers()
            );

            WhiteboardSession saved = sessionRepository.save(session);
            logger.info("Created whiteboard session: " + sessionId + " by user: " + request.getCreatedBy());
            return saved;
        }, executorService);
    }

    /**
     * Get session by ID
     */
    public Optional<WhiteboardSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * Get all sessions for a user
     */
    public List<WhiteboardSession> getUserSessions(String userId) {
        return sessionRepository.findByCreatedBy(userId);
    }

    /**
     * Get all active sessions
     */
    public List<WhiteboardSession> getActiveSessions() {
        return sessionRepository.findByIsActiveTrue();
    }

    /**
     * Delete session and all associated data
     */
    @Transactional
    public CompletableFuture<Void> deleteSessionAsync(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Delete session
                sessionRepository.deleteById(sessionId);

                // Delete all drawing actions
                drawingActionRepository.deleteBySessionId(sessionId);

                // Delete active user sessions
                activeUserSessionRepository.deleteBySessionId(sessionId);

                logger.info("Deleted session and all associated data: " + sessionId);
            } catch (Exception e) {
                logger.severe("Error deleting session: " + sessionId + ", error: " + e.getMessage());
                throw new RuntimeException("Failed to delete session", e);
            }
        }, executorService);
    }

    /**
     * Add user to session
     */
    public synchronized boolean addUserToSession(String sessionId, String userId) {
        Optional<WhiteboardSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            WhiteboardSession session = sessionOpt.get();
            if (session.getActiveUsers().size() < session.getMaxUsers()) {
                session.addActiveUser(userId);
                sessionRepository.save(session);
                logger.info("Added user " + userId + " to session " + sessionId);
                return true;
            } else {
                logger.warning("Session " + sessionId + " is full. Cannot add user " + userId);
                return false;
            }
        }
        return false;
    }

    /**
     * Remove user from session
     */
    public synchronized void removeUserFromSession(String sessionId, String userId) {
        Optional<WhiteboardSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            WhiteboardSession session = sessionOpt.get();
            session.removeActiveUser(userId);
            sessionRepository.save(session);
            logger.info("Removed user " + userId + " from session " + sessionId);
        }
    }

    /**
     * Update session activity timestamp
     */
    public void updateSessionActivity(String sessionId) {
        Optional<WhiteboardSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            WhiteboardSession session = sessionOpt.get();
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    /**
     * Cleanup inactive sessions
     */
    public CompletableFuture<Integer> cleanupInactiveSessionsAsync(int inactiveHours) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime threshold = LocalDateTime.now().minusHours(inactiveHours);
            List<WhiteboardSession> allSessions = sessionRepository.findAll();
            int deletedCount = 0;

            for (WhiteboardSession session : allSessions) {
                if (session.getUpdatedAt().isBefore(threshold)) {
                    deleteSessionAsync(session.getSessionId());
                    deletedCount++;
                }
            }

            logger.info("Cleaned up " + deletedCount + " inactive sessions");
            return deletedCount;
        }, executorService);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

