package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findTop200ByOrderByTimestampDesc();
}
