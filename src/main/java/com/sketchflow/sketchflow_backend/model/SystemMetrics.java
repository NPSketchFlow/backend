package com.sketchflow.sketchflow_backend.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {
    private Long totalUsers = 0L;
    private Long activeUsers = 0L;
    private Long totalWhiteboardSessions = 0L;
    private Long activeWhiteboardSessions = 0L;
    private Long totalMessages = 0L;
    private Long totalVoiceMessages = 0L;
    private Long totalDrawingActions = 0L;
    private Long todayLogins = 0L;
    private Long todayNewUsers = 0L;
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
