package com.sketchflow.sketchflow_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableMongoRepositories(basePackages = "com.sketchflow.sketchflow_backend.repository")
public class MongoConfig {
    // Let Spring Boot auto-configuration handle MongoDB connection
    // The tlsAllowInvalidCertificates parameter in the URI will be respected
    /* * -----------------------------------------------------------
     * ADD THE OBJECTMAPPER BEAN HERE
     * -----------------------------------------------------------
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register JavaTimeModule for Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

