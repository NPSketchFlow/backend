package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.CanvasSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanvasSnapshotRepository extends MongoRepository<CanvasSnapshot, String> {
    List<CanvasSnapshot> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    List<CanvasSnapshot> findByCreatedBy(String createdBy);
}

