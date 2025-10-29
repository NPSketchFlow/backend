package com.sketchflow.sketchflow_backend.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DrawEventDTO {

    private String action;
    private int x;
    private int y;
    private String color;
    private String tool;
    private String timestamp;

    // Constructors, Getters and Setters
    public DrawEventDTO(String action, int x, int y, String color, String tool, String timestamp) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.color = color;
        this.tool = tool;
        this.timestamp = timestamp;
    }

}
