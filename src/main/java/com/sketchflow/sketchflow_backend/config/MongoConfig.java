package com.sketchflow.sketchflow_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.sketchflow.sketchflow_backend.repository")
public class MongoConfig {
    // Let Spring Boot auto-configuration handle MongoDB connection
    // The tlsAllowInvalidCertificates parameter in the URI will be respected
}

