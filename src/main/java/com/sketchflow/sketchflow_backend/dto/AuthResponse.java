package com.sketchflow.sketchflow_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String avatar;
    private Set<String> roles;

    public AuthResponse(String token, String id, String username, String email, String fullName, String avatar, Set<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.avatar = avatar;
        this.roles = roles;
    }
}

