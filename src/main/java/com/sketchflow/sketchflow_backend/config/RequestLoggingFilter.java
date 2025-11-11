package com.sketchflow.sketchflow_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // Run before security filters
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String threadName = Thread.currentThread().getName();
            String method = httpRequest.getMethod();
            String uri = httpRequest.getRequestURI();
            String authHeader = httpRequest.getHeader("Authorization");

            // Force output with System.err to ensure it shows up
//            System.err.println("╔════════════════════════════════════════");
//            System.err.println("║ 🚨 REQUEST LOGGING FILTER - BEFORE SECURITY");
//            System.err.println("║ Thread: " + threadName);
//            System.err.println("║ Method: " + method);
//            System.err.println("║ URI: " + uri);
//            System.err.println("║ Auth Header: " + (authHeader != null ? "Present (Bearer token)" : "NOT PRESENT"));
//            System.err.println("╚════════════════════════════════════════");
//
//            System.out.println("╔════════════════════════════════════════");
//            System.out.println("║ 🌐 INCOMING REQUEST");
//            System.out.println("║ Thread: " + threadName);
//            System.out.println("║ Method: " + method);
//            System.out.println("║ URI: " + uri);
//            System.out.println("║ Auth Header: " + (authHeader != null ? "Present (Bearer token)" : "NOT PRESENT"));
//            System.out.println("╚════════════════════════════════════════");
        }

        long startTime = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - startTime;

        System.err.println("⏱️ Request completed in " + duration + "ms on thread: " + Thread.currentThread().getName());
    }
}

