package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugAuthController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Debug endpoint to validate and decode JWT token
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        Map<String, Object> response = new HashMap<>();

        try {
            boolean isValid = jwtTokenProvider.validateToken(token);
            response.put("valid", isValid);

            if (isValid) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                response.put("username", username);
                response.put("message", "Token is valid");
            } else {
                response.put("message", "Token validation failed");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Test endpoint that requires authentication
     */
    @GetMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuth() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You are authenticated!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}

