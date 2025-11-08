package com.sketchflow.sketchflow_backend.config;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MongoConnectionChecker {

    private static final Logger logger = LoggerFactory.getLogger(MongoConnectionChecker.class);
    private final MongoClient mongoClient;

    public MongoConnectionChecker(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @PostConstruct
    public void testConnection() {
        try {
            mongoClient.listDatabaseNames().first();
            logger.info("✅ Successfully connected to MongoDB");
        } catch (MongoException e) {
            logger.error("❌ Failed to connect to MongoDB! Check URI, credentials, or network.", e);
        }
    }
}
