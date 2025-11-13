package com.sketchflow.sketchflow_backend.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {
    // Spring Boot auto-configuration handles MongoDB connection.
    // Removed @EnableMongoRepositories to avoid duplicate repository registration
}
