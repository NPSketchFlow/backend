package com.sketchflow.sketchflow_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    // We only allow changing these fields
    private String fullName;
    private String avatar;
}