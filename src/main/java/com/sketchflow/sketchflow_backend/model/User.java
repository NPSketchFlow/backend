package com.sketchflow.sketchflow_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String userId;
    private String username;
    private String status;
    private String ip;
    private int port;
    private long lastSeen;

    public User() {}

    public User(String userId, String username, String status, String ip, int port, long lastSeen) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.ip = ip;
        this.port = port;
        this.lastSeen = lastSeen;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", status='" + status + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", lastSeen=" + lastSeen +
                '}';
    }
}

