package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "whiteboard_sessions")
public class WhiteboardSession {

    @Id
    private String sessionId;

    private String name;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private int maxUsers;
    private List<String> activeUsers;

    public WhiteboardSession(String sessionId, String name, String createdBy, int maxUsers) {
        this.sessionId = sessionId;
        this.name = name;
        this.createdBy = createdBy;
        this.maxUsers = maxUsers;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.activeUsers = new ArrayList<>();
    }

    public void addActiveUser(String userId) {
        if (activeUsers == null) {
            activeUsers = new ArrayList<>();
        }
        if (!activeUsers.contains(userId) && activeUsers.size() < maxUsers) {
            activeUsers.add(userId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeActiveUser(String userId) {
        if (activeUsers != null) {
            activeUsers.remove(userId);
            this.updatedAt = LocalDateTime.now();
        }
    }
}

