package com.sketchflow.sketchflow_backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.service.ChatService;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatService chatService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatWebSocketHandler(ChatService chatService) { this.chatService = chatService; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        chatService.registerWebSocket(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatService.unregisterWebSocket(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode node = mapper.readTree(payload);
        String sender = node.has("sender") ? node.get("sender").asText() : "unknown";
        String content = node.has("content") ? node.get("content").asText() : "";
        String type = node.has("type") ? node.get("type").asText() : "message";
        chatService.handleIncomingMessage(sender, content, type);
    }
}
