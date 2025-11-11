package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.ActiveUserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveUserSessionRepository extends MongoRepository<ActiveUserSession, String> {
    List<ActiveUserSession> findBySessionId(String sessionId);
    Optional<ActiveUserSession> findByUserIdAndSessionId(String userId, String sessionId);
    void deleteBySessionId(String sessionId);
    void deleteByUserIdAndSessionId(String userId, String sessionId);
    List<ActiveUserSession> findByLastActivityBefore(LocalDateTime dateTime);
}

