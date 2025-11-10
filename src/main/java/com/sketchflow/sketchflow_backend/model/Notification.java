package com.sketchflow.sketchflow_backend.model;

public class Notification {

    private String type;
    private String fileId;
    private String senderId;
    private long timestamp;
    private int priority;

    public Notification(String type, String fileId, String senderId, long timestamp, int priority) {
        this.type = type;
        this.fileId = fileId;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.priority = priority;
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

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", fileId='" + fileId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", timestamp=" + timestamp +
                ", priority=" + priority +
                '}';
    }
}
