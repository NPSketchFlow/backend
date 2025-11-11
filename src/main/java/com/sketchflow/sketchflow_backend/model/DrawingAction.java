package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drawing_actions")
public class DrawingAction {

    @Id
    private String actionId;

    private String sessionId;
    private String userId;
    private String tool;
    private String color;
    private LocalDateTime timestamp;
    private String actionType;
    private Coordinates coordinates;
    private Properties properties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinates {
        private List<Point> points;
        private Point start;
        private Point end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private double x;
        private double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Properties {
        private double lineWidth;
        private boolean isEraser;
    }
}

