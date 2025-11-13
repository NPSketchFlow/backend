package com.sketchflow.sketchflow_backend.controller;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final MongoTemplate mongoTemplate;

    public HealthController(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", Instant.now().toString());

        boolean mongoUp = false;
        String mongoMessage = "MongoDB disabled";

        if (mongoTemplate != null) {
            try {
                Document result = mongoTemplate.executeCommand(new Document("ping", 1));
                mongoUp = "1.0".equals(String.valueOf(result.get("ok")));
                mongoMessage = mongoUp ? "MongoDB reachable" : "MongoDB ping failed";
            } catch (Exception ex) {
                log.warn("MongoDB ping failed", ex);
                mongoMessage = ex.getMessage();
            }
        }

        status.put("mongo", Map.of(
                "enabled", mongoTemplate != null,
                "status", mongoUp ? "UP" : "DOWN",
                "message", mongoMessage
        ));

        return ResponseEntity.ok(status);
    }
}
