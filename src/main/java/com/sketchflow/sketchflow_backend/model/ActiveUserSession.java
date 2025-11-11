package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "active_user_sessions")
public class ActiveUserSession {

    @Id
    private String id;

    private String userId;
    private String sessionId;
    private String username;
    private String avatar;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActivity;
    private CursorPosition cursorPosition;
    private String currentTool;
    private String currentColor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CursorPosition {
        private double x;
        private double y;
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}

