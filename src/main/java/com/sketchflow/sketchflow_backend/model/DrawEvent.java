package com.sketchflow.sketchflow_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "drawing_events")
public class DrawEvent {

    // Getters and Setters
    @Id
    private String id;
    private String action; // e.g., "draw"
    private int x;
    private int y;
    private String color;
    private String tool;
    private String timestamp; // Optional field to store when the event occurred

}
