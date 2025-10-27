package com.sketchflow.sketchflow_backend.model;



import lombok.Data;

@Data
public class DrawEvent {
    private String userId;
    private String drawingAction; // e.g., 'draw', 'erase'
    private int x;
    private int y;
    private String color;
    private int size;
}
