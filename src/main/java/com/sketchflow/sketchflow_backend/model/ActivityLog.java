package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_logs")
public class ActivityLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String username;

    @Indexed
    private String action; // LOGIN, LOGOUT, CREATE_ROOM, JOIN_ROOM, SEND_MESSAGE, DRAW, VOICE_MESSAGE, etc.

    private String resourceType; // USER, ROOM, MESSAGE, WHITEBOARD, VOICE

    private String resourceId;

    private String ipAddress;

    private String userAgent;

    private String description;

    @Indexed
    private LocalDateTime timestamp;

    private String severity; // INFO, WARNING, ERROR, CRITICAL

    private String metadata; // JSON string for additional data

    public ActivityLog(String userId, String username, String action, String resourceType, String ipAddress) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.resourceType = resourceType;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
        this.severity = "INFO";
    }
}

