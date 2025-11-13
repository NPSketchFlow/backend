package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
// CORS handled by global SecurityConfig - removed @CrossOrigin to avoid conflicts
public class JwtTestController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Provide a safe default so application context can start even if property is not set
    @Value("${app.jwt.secret:sketchflow-secret-key-for-jwt-token-generation-minimum-256-bits-required}")
    private String jwtSecret;

    /**
     * Test endpoint to manually decode and validate JWT token
     */
    @PostMapping("/decode-token")
    public ResponseEntity<Map<String, Object>> decodeToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        Map<String, Object> response = new HashMap<>();

        try {
            // Log the secret being used
            System.out.println("=== JWT SECRET INFO ===");
            System.out.println("Secret length: " + jwtSecret.length());
            System.out.println("Secret (first 50 chars): " + jwtSecret.substring(0, Math.min(50, jwtSecret.length())));

            // Try to decode with current secret
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            response.put("success", true);
            response.put("username", claims.getSubject());
            response.put("issuedAt", claims.getIssuedAt());
            response.put("expiration", claims.getExpiration());
            response.put("isExpired", claims.getExpiration().getTime() < System.currentTimeMillis());
            response.put("secretLength", jwtSecret.length());

            // Also test with JwtTokenProvider
            boolean isValid = jwtTokenProvider.validateToken(token);
            response.put("jwtProviderValidation", isValid);

            if (isValid) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                response.put("extractedUsername", username);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getClass().getSimpleName());
            response.put("message", e.getMessage());
            response.put("secretLength", jwtSecret.length());

            System.err.println("=== JWT DECODE ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.ok(response);
        }
    }

    /**
     * Test endpoint to check authentication flow
     */
    @GetMapping("/auth-status")
    public ResponseEntity<Map<String, Object>> checkAuthStatus() {
        Map<String, Object> response = new HashMap<>();

        // Check if SecurityContext has authentication
        org.springframework.security.core.context.SecurityContext context =
            org.springframework.security.core.context.SecurityContextHolder.getContext();

        org.springframework.security.core.Authentication auth = context.getAuthentication();

        response.put("hasAuthentication", auth != null);
        if (auth != null) {
            response.put("isAuthenticated", auth.isAuthenticated());
            response.put("principal", auth.getPrincipal().toString());
            response.put("authorities", auth.getAuthorities().toString());
        }

        return ResponseEntity.ok(response);
    }
}
