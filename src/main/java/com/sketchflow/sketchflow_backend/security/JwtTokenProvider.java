package com.sketchflow.sketchflow_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Logger;

@Component
public class JwtTokenProvider {

    private static final Logger logger = Logger.getLogger(JwtTokenProvider.class.getName());

    @Value("${app.jwt.secret:sketchflow-secret-key-for-jwt-token-generation-minimum-256-bits-required}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userDetails.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            System.out.println("🔐 JwtTokenProvider.validateToken() called");
            System.out.println("   Secret length: " + jwtSecret.length() + " characters");
            logger.info("Validating JWT token...");

            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);

            System.out.println("✅ JWT token validation SUCCESS!");
            logger.info("JWT token validated successfully");
            return true;
        } catch (SecurityException ex) {
            System.err.println("❌ Invalid JWT SIGNATURE!");
            System.err.println("   This means the token was signed with a DIFFERENT secret key!");
            System.err.println("   Error: " + ex.getMessage());
            logger.severe("Invalid JWT signature: " + ex.getMessage());
            ex.printStackTrace();
        } catch (MalformedJwtException ex) {
            System.err.println("❌ Malformed JWT token!");
            System.err.println("   Error: " + ex.getMessage());
            logger.severe("Invalid JWT token: " + ex.getMessage());
            ex.printStackTrace();
        } catch (ExpiredJwtException ex) {
            System.err.println("❌ JWT token EXPIRED!");
            System.err.println("   Expired at: " + ex.getClaims().getExpiration());
            logger.severe("Expired JWT token: " + ex.getMessage());
            logger.severe("Token expired at: " + ex.getClaims().getExpiration());
        } catch (UnsupportedJwtException ex) {
            System.err.println("❌ Unsupported JWT token!");
            System.err.println("   Error: " + ex.getMessage());
            logger.severe("Unsupported JWT token: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IllegalArgumentException ex) {
            System.err.println("❌ JWT claims string is empty!");
            System.err.println("   Error: " + ex.getMessage());
            logger.severe("JWT claims string is empty: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("❌ Unknown JWT validation error!");
            System.err.println("   Error: " + ex.getClass().getName() + ": " + ex.getMessage());
            logger.severe("Unknown JWT validation error: " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }
}

