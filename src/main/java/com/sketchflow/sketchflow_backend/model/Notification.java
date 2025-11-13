package com.sketchflow.sketchflow_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;
    private String type;
    private String fileId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private int priority;
    private boolean read;
    private Map<String, Object> metadata = new HashMap<>();

    public Notification() {
    }

    public Notification(String type, String fileId, String senderId, long timestamp, int priority) {
        this(type, fileId, senderId, null, null, timestamp, priority, false, null);
    }

    public Notification(String type,
                        String fileId,
                        String senderId,
                        String receiverId,
                        String message,
                        long timestamp,
                        int priority,
                        boolean read,
                        Map<String, Object> metadata) {
        this.type = type;
        this.fileId = fileId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.priority = priority;
        this.read = read;
        if (metadata != null) {
            this.metadata = new HashMap<>(metadata);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", fileId='" + fileId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", priority=" + priority +
                ", read=" + read +
                ", metadata=" + metadata +
                '}';
    }
}
