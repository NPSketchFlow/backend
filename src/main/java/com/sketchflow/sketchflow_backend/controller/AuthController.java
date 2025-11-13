package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.dto.AuthResponse;
import com.sketchflow.sketchflow_backend.dto.LoginRequest;
import com.sketchflow.sketchflow_backend.dto.RegisterRequest;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.service.AuthService;
import com.sketchflow.sketchflow_backend.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private ActivityLogService activityLogService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.register(request);

            // Log registration activity
            if (activityLogService != null) {
                activityLogService.logActivity(
                    response.getId(),
                    response.getUsername(),
                    "REGISTER",
                    "New user registered",
                    getClientIp(httpRequest),
                    "INFO"
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.login(request);

            // Log login activity
            if (activityLogService != null) {
                activityLogService.logActivity(
                    response.getId(),
                    response.getUsername(),
                    "LOGIN",
                    "User logged in successfully",
                    getClientIp(httpRequest),
                    "INFO"
                );
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log failed login attempt
            if (activityLogService != null) {
                activityLogService.logActivity(
                    "unknown",
                    request.getUsername(),
                    "LOGIN_FAILED",
                    "Failed login attempt",
                    getClientIp(httpRequest),
                    "WARNING"
                );
            }

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

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("avatar", user.getAvatar());
        response.put("roles", user.getRoles());
        response.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(response);
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

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    /**
     * NEW METHOD: Get a list of all users for the chat page.
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = authService.getAllUsers();

        // We must filter out the password and other sensitive data
        List<Map<String, Object>> safeUserList = users.stream().map(user -> {
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());

            // This is the UserResponse format the frontend expects
            response.put("username", user.getUsername());
            response.put("status", "OFFLINE"); // We can wire this up to the tracker later
            response.put("ip", "N/A");
            response.put("port", 0);
            response.put("lastSeen", user.getLastLogin() != null ? user.getLastLogin().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0);

            return response;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(safeUserList);
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

