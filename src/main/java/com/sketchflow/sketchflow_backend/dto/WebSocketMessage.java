package com.sketchflow.sketchflow_backend.dto;

import com.sketchflow.sketchflow_backend.model.DrawingAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type;
    private String userId;
    private String username;
    private String avatar;
    private String actionId;
    private String tool;
    private String color;
    private DrawingAction.Coordinates coordinates;
    private Long timestamp; // Changed from LocalDateTime to Long (epoch milliseconds)
    private CursorPosition position;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CursorPosition {
        private double x;
        private double y;
    }
}

