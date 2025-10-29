package com.sketchflow.sketchflow_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleTextSocketHandler extends TextWebSocketHandler {

    // ObjectMapper to convert objects to JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Create a JSON object with the drawing data
        String payload = message.getPayload();

        // Create a new JSON response with a structure that makes sense for your use case
        DrawingResponse response = new DrawingResponse("draw", payload);

        // Convert the object to JSON and send it back as a message
        String jsonResponse = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonResponse));
    }

    // Inner class to represent the structure of the response message
    @Setter
    @Getter
    public static class DrawingResponse {
        // Getters and setters
        private String action;
        private String data;

        public DrawingResponse(String action, String data) {
            this.action = action;
            this.data = data;
        }

    }
}
