package com.sketchflow.sketchflow_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        private double lineWidth;
        // isEraser field removed - frontend no longer sends it
    }
}

