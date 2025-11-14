package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.ActivityLog;
import com.sketchflow.sketchflow_backend.model.SystemMetrics;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.model.WhiteboardSession;
import com.sketchflow.sketchflow_backend.service.ActivityLogService;
import com.sketchflow.sketchflow_backend.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Dashboard Controller - Secure endpoints for admin operations
 * Part of Admin Dashboard & Security contribution
 * Demonstrates: SSL/TLS security, role-based access control, activity logging
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminController {

    private final AdminService adminService;
    private final ActivityLogService activityLogService;

    /**
     * Get system metrics and statistics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemMetrics> getSystemMetrics(Authentication authentication, HttpServletRequest request) {
        log.info("Admin {} accessing system metrics", authentication.getName());

        // Log admin activity
        activityLogService.logActivity(
            authentication.getName(),
            authentication.getName(),
            "VIEW_METRICS",
            "Viewed system metrics",
            getClientIp(request),
            "INFO"
        );

        SystemMetrics metrics = adminService.getSystemMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get all users with pagination
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Admin {} accessing user list", authentication.getName());

        activityLogService.logActivity(
            authentication.getName(),
            authentication.getName(),
            "VIEW_USERS",
            "Viewed user list - page: " + page,
            getClientIp(request),
            "INFO"
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = adminService.getAllUsers(pageable);

        // Remove sensitive data before sending
        users.forEach(user -> user.setPassword(null));

        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(
            @PathVariable String userId,
            Authentication authentication,
            HttpServletRequest request) {

        return adminService.getUserById(userId)
            .map(user -> {
                user.setPassword(null); // Remove sensitive data

                activityLogService.logActivity(
                    authentication.getName(),
                    authentication.getName(),
                    "VIEW_USER",
                    "Viewed user details: " + user.getUsername(),
                    getClientIp(request),
                    "INFO"
                );

                return ResponseEntity.ok(user);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user status (enable/disable)
     */
    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Boolean> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        boolean enabled = request.getOrDefault("enabled", true);

        User updatedUser = adminService.updateUserStatus(
            userId,
            enabled,
            authentication.getName(),
            getClientIp(httpRequest)
        );

        updatedUser.setPassword(null);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Update user roles
     */
    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserRoles(
            @PathVariable String userId,
            @RequestBody Map<String, List<String>> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        List<String> roles = request.get("roles");

        User updatedUser = adminService.updateUserRoles(
            userId,
            roles,
            authentication.getName(),
            getClientIp(httpRequest)
        );

        updatedUser.setPassword(null);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable String userId,
            Authentication authentication,
            HttpServletRequest request) {

        adminService.deleteUser(userId, authentication.getName(), getClientIp(request));

        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Reset user password
     */
    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String newPassword = request.get("newPassword");

        adminService.resetUserPassword(
            userId,
            newPassword,
            authentication.getName(),
            getClientIp(httpRequest)
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Search users
     */
    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam String query,
            Authentication authentication) {

        List<User> users = adminService.searchUsers(query);
        users.forEach(user -> user.setPassword(null));

        return ResponseEntity.ok(users);
    }

    /**
     * Get activity logs with pagination
     */
    @GetMapping("/activity-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ActivityLog>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String severity,
            Authentication authentication,
            HttpServletRequest request) {

        activityLogService.logActivity(
            authentication.getName(),
            authentication.getName(),
            "VIEW_ACTIVITY_LOGS",
            "Viewed activity logs",
            getClientIp(request),
            "INFO"
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> logs;

        if (userId != null) {
            logs = activityLogService.getUserLogs(userId, pageable);
        } else if (action != null) {
            logs = activityLogService.getLogsByAction(action, pageable);
        } else if (severity != null) {
            logs = activityLogService.getLogsBySeverity(severity, pageable);
        } else {
            logs = activityLogService.getAllLogs(pageable);
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * Get recent activity logs
     */
    @GetMapping("/activity-logs/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActivityLog>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {

        List<ActivityLog> logs = activityLogService.getRecentLogs(limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get all whiteboard sessions
     */
    @GetMapping("/whiteboard-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WhiteboardSession>> getWhiteboardSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest request) {

        activityLogService.logActivity(
            authentication.getName(),
            authentication.getName(),
            "VIEW_WHITEBOARD_SESSIONS",
            "Viewed whiteboard sessions",
            getClientIp(request),
            "INFO"
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<WhiteboardSession> sessions = adminService.getAllWhiteboardSessions(pageable);

        return ResponseEntity.ok(sessions);
    }

    /**
     * Get active user sessions
     */
    @GetMapping("/active-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getActiveSessions(
            Authentication authentication,
            HttpServletRequest request) {

        activityLogService.logActivity(
            authentication.getName(),
            authentication.getName(),
            "VIEW_ACTIVE_SESSIONS",
            "Viewed active user sessions",
            getClientIp(request),
            "INFO"
        );

        return ResponseEntity.ok(adminService.getActiveSessions());
    }

    /**
     * Force disconnect a user
     */
    @DeleteMapping("/active-sessions/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> forceDisconnect(
            @PathVariable String sessionId,
            Authentication authentication,
            HttpServletRequest request) {

        adminService.forceDisconnectUser(
            sessionId,
            authentication.getName(),
            getClientIp(request)
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "User disconnected successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get user login history
     */
    @GetMapping("/users/{userId}/login-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActivityLog>> getUserLoginHistory(@PathVariable String userId) {
        List<ActivityLog> history = adminService.getUserLoginHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Test encryption endpoint (demonstrates encryption feature)
     */
    @PostMapping("/test/encrypt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testEncryption(@RequestBody Map<String, String> request) {
        String data = request.get("data");
        String encrypted = adminService.encryptSensitiveData(data);

        Map<String, String> response = new HashMap<>();
        response.put("original", data);
        response.put("encrypted", encrypted);
        response.put("decrypted", adminService.decryptSensitiveData(encrypted));

        return ResponseEntity.ok(response);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

