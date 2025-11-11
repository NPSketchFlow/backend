package com.sketchflow.sketchflow_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "voice_chats")
public class VoiceChat {

    @Id
    private String chatId;
    private String senderId;
    private String receiverId;
    private String filePath;
    private long timestamp;

    public VoiceChat() {}

    public VoiceChat(String chatId, String senderId, String receiverId, String filePath, long timestamp) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.filePath = filePath;
        this.timestamp = timestamp;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "VoiceChat{" +
                "chatId='" + chatId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

