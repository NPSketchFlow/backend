package com.sketchflow.sketchflow_backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = Logger.getLogger(JwtAuthenticationFilter.class.getName());

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // EXTREMELY AGGRESSIVE LOGGING - FORCE OUTPUT
        System.err.println("╔═══════════════════════════════════════════════════════════════════");
        System.err.println("║ 🔐🔐🔐 JWT FILTER EXECUTING - doFilterInternal() CALLED 🔐🔐🔐");
        System.err.println("║ URI: " + request.getRequestURI());
        System.err.println("║ Method: " + request.getMethod());
        System.err.println("║ Thread: " + Thread.currentThread().getName());
        System.err.println("╚═══════════════════════════════════════════════════════════════════");

        // FORCE CONSOLE OUTPUT
        System.out.println("========================================");
        System.out.println("🔍 JWT FILTER EXECUTING for: " + request.getRequestURI());
        System.out.println("   Method: " + request.getMethod());
        System.out.println("========================================");

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                System.out.println("✅ JWT token FOUND in Authorization header");
                System.out.println("   Token starts with: " + jwt.substring(0, Math.min(30, jwt.length())) + "...");
                logger.info("JWT token found for request: " + request.getRequestURI());

                boolean isValid = false;
                try {
                    System.out.println("🔐 Starting token validation...");
                    isValid = tokenProvider.validateToken(jwt);
                    System.out.println("   Validation result: " + isValid);
                } catch (Exception e) {
                    System.err.println("❌ EXCEPTION during token validation: " + e.getClass().getName());
                    System.err.println("   Message: " + e.getMessage());
                    logger.severe("Exception during token validation: " + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }

                if (isValid) {
                    try {
                        String username = tokenProvider.getUsernameFromToken(jwt);
                        System.out.println("✅ Token validated! Username: " + username);
                        logger.info("JWT token validated successfully for user: " + username);

                        System.out.println("📝 Loading UserDetails from database...");
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        System.out.println("✅ UserDetails loaded: " + userDetails.getUsername());
                        System.out.println("   Authorities: " + userDetails.getAuthorities());
                        logger.info("UserDetails loaded: " + userDetails.getUsername() + ", authorities: " + userDetails.getAuthorities());

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("🎉 ✅ AUTHENTICATION SET IN SECURITY CONTEXT!");
                        logger.info("✅ Authentication successfully set in security context for user: " + username);
                    } catch (Exception e) {
                        System.err.println("❌ ERROR loading user or setting authentication:");
                        System.err.println("   " + e.getClass().getName() + ": " + e.getMessage());
                        logger.severe("Error loading user or setting authentication: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("❌ TOKEN VALIDATION FAILED!");
                    logger.warning("❌ JWT token validation failed for request: " + request.getRequestURI());
                }
            } else {
                System.out.println("⚠️ NO JWT TOKEN found in Authorization header!");
                logger.warning("⚠️ No JWT token found in Authorization header for request: " + request.getRequestURI());
            }
        } catch (Exception ex) {
            System.err.println("❌ UNEXPECTED ERROR in JWT filter:");
            System.err.println("   " + ex.getClass().getName() + ": " + ex.getMessage());
            logger.severe("❌ Unexpected error in JWT filter: " + ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("========================================");
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        boolean shouldSkip = false; // By default, don't skip any path

        // CRITICAL LOGGING
        System.err.println("╔═══════════════════════════════════════════════════════════════════");
        System.err.println("║ 🔍 shouldNotFilter() called for: " + path);
        System.err.println("║ Method: " + request.getMethod());
        System.err.println("║ Will skip filter? " + shouldSkip);
        System.err.println("╚═══════════════════════════════════════════════════════════════════");

        return shouldSkip;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

