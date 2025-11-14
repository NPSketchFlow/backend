package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.DrawingAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrawingActionRepository extends MongoRepository<DrawingAction, String> {
    List<DrawingAction> findBySessionIdOrderByTimestampAsc(String sessionId);
    Page<DrawingAction> findBySessionIdOrderByTimestampAsc(String sessionId, Pageable pageable);
    long countBySessionId(String sessionId);
    void deleteBySessionId(String sessionId);
    void deleteByActionId(String actionId);
    void deleteBySessionIdAndActionId(String sessionId, String actionId);
}

