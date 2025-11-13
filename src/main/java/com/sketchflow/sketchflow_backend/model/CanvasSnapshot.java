package com.sketchflow.sketchflow_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "canvas_snapshots")
public class CanvasSnapshot {

    @Id
    private String snapshotId;

    private String sessionId;
    private String name;
    private String createdBy;
    private LocalDateTime createdAt;
    private String imageUrl;
    private String thumbnail;
    private String canvasData;
}

