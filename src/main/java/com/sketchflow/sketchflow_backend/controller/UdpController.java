package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.udp.UdpServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/udp")
public class UdpController {
    private final UdpServer udpServer;

    public UdpController(UdpServer udpServer){
        this.udpServer = udpServer;
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notifyClients(@RequestBody byte[] payload){
        udpServer.sendNotification(payload);
        return ResponseEntity.accepted().body("Notification queued");
    }
}

