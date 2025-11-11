package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.WhiteboardSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhiteboardSessionRepository extends MongoRepository<WhiteboardSession, String> {
    List<WhiteboardSession> findByCreatedBy(String createdBy);
    List<WhiteboardSession> findByIsActiveTrue();
    List<WhiteboardSession> findByActiveUsersContaining(String userId);
}

