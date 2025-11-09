package com.sketchflow.sketchflow_backend.udp;

import org.springframework.stereotype.Component;

import java.net.SocketAddress;
import java.time.Instant;
import java.util.Map;
@Component
public class HeartbeatManager {
    public void updatePresence(SocketAddress sa, Instant now) {
        // Implement presence update logic
    }

    public void expireOld(Map<SocketAddress, Instant> presence) {
        // Implement expiration logic
    }
}