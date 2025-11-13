package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.*;
import com.sketchflow.sketchflow_backend.repository.*;
import com.sketchflow.sketchflow_backend.security.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Admin service for managing users, monitoring system, and security
 * Part of Admin Dashboard & Security contribution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final WhiteboardSessionRepository whiteboardSessionRepository;
    private final DrawingActionRepository drawingActionRepository;
    private final ActiveUserSessionRepository activeUserSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final ActivityLogService activityLogService;

    /**
     * Get all users with pagination
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * Update user status (enable/disable)
     */
    public User updateUserStatus(String userId, boolean enabled, String adminUsername, String ipAddress) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(enabled);
            User savedUser = userRepository.save(user);

            // Log activity
            activityLogService.logActivity(
                userId,
                user.getUsername(),
                enabled ? "USER_ENABLED" : "USER_DISABLED",
                "User account " + (enabled ? "enabled" : "disabled") + " by admin: " + adminUsername,
                ipAddress,
                "WARNING"
            );

            return savedUser;
        }
        throw new RuntimeException("User not found");
    }

    /**
     * Update user roles
     */
    public User updateUserRoles(String userId, List<String> roles, String adminUsername, String ipAddress) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.getRoles().clear();
            user.getRoles().addAll(roles);
            User savedUser = userRepository.save(user);

            // Log activity
            activityLogService.logActivity(
                userId,
                user.getUsername(),
                "USER_ROLES_UPDATED",
                "User roles updated to: " + roles + " by admin: " + adminUsername,
                ipAddress,
                "WARNING"
            );

            return savedUser;
        }
        throw new RuntimeException("User not found");
    }

    /**
     * Delete user
     */
    public void deleteUser(String userId, String adminUsername, String ipAddress) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String username = user.getUsername();

            // Log activity before deletion
            activityLogService.logActivity(
                userId,
                username,
                "USER_DELETED",
                "User deleted by admin: " + adminUsername,
                ipAddress,
                "CRITICAL"
            );

            userRepository.deleteById(userId);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    /**
     * Reset user password
     */
    public void resetUserPassword(String userId, String newPassword, String adminUsername, String ipAddress) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Log activity
            activityLogService.logActivity(
                userId,
                user.getUsername(),
                "PASSWORD_RESET",
                "Password reset by admin: " + adminUsername,
                ipAddress,
                "WARNING"
            );
        } else {
            throw new RuntimeException("User not found");
        }
    }

    /**
     * Get system metrics for dashboard
     */
    public SystemMetrics getSystemMetrics() {
        SystemMetrics metrics = new SystemMetrics();

        try {
            // User metrics
            metrics.setTotalUsers(userRepository.count());
            metrics.setActiveUsers((long) activeUserSessionRepository.findAll().size());

            // Whiteboard metrics
            metrics.setTotalWhiteboardSessions(whiteboardSessionRepository.count());
            metrics.setActiveWhiteboardSessions(
                whiteboardSessionRepository.findAll().stream()
                    .filter(WhiteboardSession::isActive)
                    .count()
            );

            // Drawing actions
            metrics.setTotalDrawingActions(drawingActionRepository.count());

            // Today's activity
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            metrics.setTodayLogins(activityLogRepository.countByActionAndTimestampAfter("LOGIN", startOfDay));
            metrics.setTodayNewUsers(activityLogRepository.countByActionAndTimestampAfter("REGISTER", startOfDay));

            metrics.setLastUpdated(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error calculating system metrics: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * Get all whiteboard sessions for monitoring
     */
    public Page<WhiteboardSession> getAllWhiteboardSessions(Pageable pageable) {
        return whiteboardSessionRepository.findAll(pageable);
    }

    /**
     * Get active sessions
     */
    public List<ActiveUserSession> getActiveSessions() {
        return activeUserSessionRepository.findAll();
    }

    /**
     * Force disconnect a user session
     */
    public void forceDisconnectUser(String sessionId, String adminUsername, String ipAddress) {
        Optional<ActiveUserSession> sessionOpt = activeUserSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            ActiveUserSession session = sessionOpt.get();

            // Log activity
            activityLogService.logActivity(
                session.getUserId(),
                session.getUsername(),
                "FORCE_DISCONNECT",
                "User forcefully disconnected by admin: " + adminUsername,
                ipAddress,
                "WARNING"
            );

            activeUserSessionRepository.deleteById(sessionId);
        }
    }

    /**
     * Search users by username or email
     */
    public List<User> searchUsers(String query) {
        // Simple search - in production, use text indexing
        return userRepository.findAll().stream()
            .filter(user ->
                user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                user.getEmail().toLowerCase().contains(query.toLowerCase()) ||
                (user.getFullName() != null && user.getFullName().toLowerCase().contains(query.toLowerCase()))
            )
            .toList();
    }

    /**
     * Get user login history
     */
    public List<ActivityLog> getUserLoginHistory(String userId) {
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId).stream()
            .filter(log -> "LOGIN".equals(log.getAction()) || "LOGOUT".equals(log.getAction()))
            .toList();
    }

    /**
     * Encrypt sensitive data (demonstration of encryption feature)
     */
    public String encryptSensitiveData(String data) {
        return encryptionUtil.encrypt(data);
    }

    /**
     * Decrypt sensitive data
     */
    public String decryptSensitiveData(String encryptedData) {
        return encryptionUtil.decrypt(encryptedData);
    }
}

