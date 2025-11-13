package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.VoiceChat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VoiceChatRepository extends MongoRepository<VoiceChat, String> {
    @Query("{ $or: [ { 'senderId': ?0, 'receiverId': ?1 }, { 'senderId': ?1, 'receiverId': ?0 } ] }")
    java.util.List<VoiceChat> findConversationBetween(String participantA, String participantB);
}

