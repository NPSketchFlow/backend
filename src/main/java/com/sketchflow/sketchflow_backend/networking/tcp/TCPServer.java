package com.sketchflow.sketchflow_backend.networking.tcp;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class TCPServer {

    @PostConstruct
    public void start() {
        new Thread(() -> {
            System.out.println("TCP Server Started...");
            // Future: Add serverSocket and multithreading logic here
        }).start();
    }
}
