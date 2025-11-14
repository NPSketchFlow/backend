package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.dto.DrawingActionRequest;
import com.sketchflow.sketchflow_backend.model.DrawingAction;
import com.sketchflow.sketchflow_backend.repository.DrawingActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Service
public class DrawingActionService {

    private static final Logger logger = Logger.getLogger(DrawingActionService.class.getName());

    // Thread pool for high-throughput drawing action processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    // Batch processing queue with blocking capabilities
    private final BlockingQueue<DrawingAction> actionQueue = new LinkedBlockingQueue<>(10000);

    // Batch size for bulk inserts
    private static final int BATCH_SIZE = 100;

    @Autowired
    private DrawingActionRepository actionRepository;

    @Autowired
    private WhiteboardSessionService sessionService;

    public DrawingActionService() {
        // Start batch processor thread
        startBatchProcessor();
    }

    /**
     * Save drawing action asynchronously with batching
     */
    public CompletableFuture<DrawingAction> saveActionAsync(String sessionId, DrawingActionRequest request) {
        // Capture SecurityContext from current thread
        SecurityContext context = SecurityContextHolder.getContext();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Set SecurityContext in async thread
                SecurityContextHolder.setContext(context);

                String actionId = UUID.randomUUID().toString();

                DrawingAction action = new DrawingAction();
                action.setActionId(actionId);
                action.setSessionId(sessionId);
                action.setUserId(request.getUserId());
                action.setTool(request.getTool());
                action.setColor(request.getColor());
                action.setActionType(request.getActionType());
                action.setCoordinates(request.getCoordinates());
                action.setProperties(request.getProperties());
                action.setTimestamp(LocalDateTime.now());

                try {
                    // Add to batch queue for optimized persistence
                    actionQueue.offer(action, 1, TimeUnit.SECONDS);
                    logger.fine("Queued drawing action: " + actionId);
                } catch (InterruptedException e) {
                    logger.warning("Failed to queue action: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }

                // Update session activity
                sessionService.updateSessionActivity(sessionId);

                return action;
            } finally {
                // Clean up SecurityContext
                SecurityContextHolder.clearContext();
            }
        }, executorService);
    }

    /**
     * Save action immediately (for critical operations)
     */
    public DrawingAction saveActionImmediately(String sessionId, DrawingActionRequest request) {
        String actionId = UUID.randomUUID().toString();

        DrawingAction action = new DrawingAction();
        action.setActionId(actionId);
        action.setSessionId(sessionId);
        action.setUserId(request.getUserId());
        action.setTool(request.getTool());
        action.setColor(request.getColor());
        action.setActionType(request.getActionType());
        action.setCoordinates(request.getCoordinates());
        action.setProperties(request.getProperties());
        action.setTimestamp(LocalDateTime.now());

        DrawingAction saved = actionRepository.save(action);
        logger.info("Saved drawing action immediately: " + actionId);

        return saved;
    }

    /**
     * Get paginated drawing actions for a session
     */
    public Page<DrawingAction> getSessionActions(String sessionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return actionRepository.findBySessionIdOrderByTimestampAsc(sessionId, pageable);
    }

    /**
     * Get all actions for a session
     */
    public List<DrawingAction> getAllSessionActions(String sessionId) {
        return actionRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    /**
     * Clear all actions for a session
     */
    public CompletableFuture<Void> clearSessionActionsAsync(String sessionId) {
        // Capture SecurityContext from current thread
        SecurityContext context = SecurityContextHolder.getContext();

        return CompletableFuture.runAsync(() -> {
            try {
                // Set SecurityContext in async thread
                SecurityContextHolder.setContext(context);

                actionRepository.deleteBySessionId(sessionId);
                logger.info("Cleared all actions for session: " + sessionId);
            } finally {
                // Clean up SecurityContext
                SecurityContextHolder.clearContext();
            }
        }, executorService);
    }

    /**
     * Delete a single drawing action by actionId
     */
    public CompletableFuture<Void> deleteActionAsync(String actionId) {
        // Capture SecurityContext from current thread
        SecurityContext context = SecurityContextHolder.getContext();

        return CompletableFuture.runAsync(() -> {
            try {
                // Set SecurityContext in async thread
                SecurityContextHolder.setContext(context);

                actionRepository.deleteByActionId(actionId);
                logger.info("Deleted drawing action: " + actionId);
            } finally {
                // Clean up SecurityContext
                SecurityContextHolder.clearContext();
            }
        }, executorService);
    }

    /**
     * Delete a single drawing action by sessionId and actionId
     */
    public CompletableFuture<Void> deleteActionAsync(String sessionId, String actionId) {
        // Capture SecurityContext from current thread
        SecurityContext context = SecurityContextHolder.getContext();

        return CompletableFuture.runAsync(() -> {
            try {
                // Set SecurityContext in async thread
                SecurityContextHolder.setContext(context);

                actionRepository.deleteBySessionIdAndActionId(sessionId, actionId);
                logger.info("Deleted drawing action " + actionId + " from session: " + sessionId);
            } finally {
                // Clean up SecurityContext
                SecurityContextHolder.clearContext();
            }
        }, executorService);
    }

    /**
     * Batch processor for optimized database writes
     */
    private void startBatchProcessor() {
        Thread batchThread = new Thread(() -> {
            List<DrawingAction> batch = new CopyOnWriteArrayList<>();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for actions or timeout
                    DrawingAction action = actionQueue.poll(5, TimeUnit.SECONDS);

                    if (action != null) {
                        batch.add(action);
                    }

                    // Save batch when size reached or timeout occurred
                    if (batch.size() >= BATCH_SIZE || (action == null && !batch.isEmpty())) {
                        actionRepository.saveAll(batch);
                        logger.info("Saved batch of " + batch.size() + " drawing actions");
                        batch.clear();
                    }

                } catch (InterruptedException e) {
                    logger.info("Batch processor interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.severe("Error in batch processor: " + e.getMessage());
                }
            }

            // Save remaining actions
            if (!batch.isEmpty()) {
                actionRepository.saveAll(batch);
                logger.info("Saved final batch of " + batch.size() + " actions");
            }
        });

        batchThread.setDaemon(true);
        batchThread.setName("DrawingAction-BatchProcessor");
        batchThread.start();
        logger.info("Started drawing action batch processor thread");
    }

    /**
     * Get action count for session
     */
    public long getActionCount(String sessionId) {
        return actionRepository.countBySessionId(sessionId);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

