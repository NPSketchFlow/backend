package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // Find all messages for a specific session, ordered by time
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
}