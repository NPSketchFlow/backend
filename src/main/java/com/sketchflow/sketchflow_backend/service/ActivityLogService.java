package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.ActivityLog;
import com.sketchflow.sketchflow_backend.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing activity logs and audit trails
 * Part of Admin Dashboard & Security contribution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Log a user activity
     */
    public void logActivity(String userId, String username, String action, String resourceType, String ipAddress) {
        try {
            ActivityLog activityLog = new ActivityLog(userId, username, action, resourceType, ipAddress);
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Failed to log activity: {}", e.getMessage());
        }
    }

    /**
     * Log activity with description
     */
    public void logActivity(String userId, String username, String action, String description, String ipAddress, String severity) {
        try {
            ActivityLog activityLog = new ActivityLog(userId, username, action, description, ipAddress);
            activityLog.setSeverity(severity);
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Failed to log activity: {}", e.getMessage());
        }
    }

    /**
     * Log detailed activity
     */
    public void logActivity(ActivityLog activityLog) {
        try {
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Failed to log activity: {}", e.getMessage());
        }
    }

    /**
     * Get all activity logs with pagination
     */
    public Page<ActivityLog> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Get activity logs for a specific user
     */
    public Page<ActivityLog> getUserLogs(String userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * Get activity logs by action type
     */
    public Page<ActivityLog> getLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }

    /**
     * Get activity logs within a time range
     */
    public Page<ActivityLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return activityLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
    }

    /**
     * Get activity logs by severity
     */
    public Page<ActivityLog> getLogsBySeverity(String severity, Pageable pageable) {
        return activityLogRepository.findBySeverityOrderByTimestampDesc(severity, pageable);
    }

    /**
     * Get recent activity count
     */
    public Long getRecentActivityCount(LocalDateTime since) {
        return activityLogRepository.countByTimestampAfter(since);
    }

    /**
     * Get count of specific action since timestamp
     */
    public Long getActionCount(String action, LocalDateTime since) {
        return activityLogRepository.countByActionAndTimestampAfter(action, since);
    }

    /**
     * Get recent logs (last 100)
     */
    public List<ActivityLog> getRecentLogs(int limit) {
        return activityLogRepository.findAllByOrderByTimestampDesc(
            org.springframework.data.domain.PageRequest.of(0, limit)
        ).getContent();
    }
}

