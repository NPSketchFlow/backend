package com.sketchflow.sketchflow_backend.repository;



import com.sketchflow.sketchflow_backend.model.DrawEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DrawEventRepository extends MongoRepository<DrawEvent, String> {
    // Custom query methods (if needed) can be added here
}
