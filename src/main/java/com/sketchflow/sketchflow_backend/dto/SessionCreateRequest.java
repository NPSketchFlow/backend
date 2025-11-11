package com.sketchflow.sketchflow_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {
    private String name;
    private String createdBy;
    private int maxUsers = 50;
}

