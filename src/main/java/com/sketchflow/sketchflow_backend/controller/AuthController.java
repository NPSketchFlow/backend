package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.dto.AuthResponse;
import com.sketchflow.sketchflow_backend.dto.LoginRequest;
import com.sketchflow.sketchflow_backend.dto.RegisterRequest;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sketchflow.sketchflow_backend.dto.UpdateProfileRequest;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
// CORS handled by global SecurityConfig
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        User user = authService.getCurrentUser();
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        return getResponseEntity(user);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return authService.getUserByUsername(username)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    response.put("fullName", user.getFullName());
                    response.put("avatar", user.getAvatar());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody UpdateProfileRequest request) {
        try {
            User updatedUser = authService.updateProfile(request);

            // Return a safe response map, similar to /me
            return getResponseEntity(updatedUser);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    private ResponseEntity<?> getResponseEntity(User updatedUser) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedUser.getId());
        response.put("username", updatedUser.getUsername());
        response.put("email", updatedUser.getEmail());
        response.put("fullName", updatedUser.getFullName());
        response.put("avatar", updatedUser.getAvatar());
        response.put("roles", updatedUser.getRoles());
        response.put("createdAt", updatedUser.getCreatedAt());

        return ResponseEntity.ok(response);
    }
}

