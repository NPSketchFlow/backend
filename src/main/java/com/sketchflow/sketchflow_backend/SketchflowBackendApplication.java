package com.sketchflow.sketchflow_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.sketchflow.sketchflow_backend.repository")
public class SketchflowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SketchflowBackendApplication.class, args);
	}

}
