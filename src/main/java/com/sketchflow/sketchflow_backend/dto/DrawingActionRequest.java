package com.sketchflow.sketchflow_backend.dto;

import com.sketchflow.sketchflow_backend.model.DrawingAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawingActionRequest {
    private String userId;
    private String tool;
    private String color;
    private String actionType;
    private DrawingAction.Coordinates coordinates;
    private DrawingAction.Properties properties;
}

