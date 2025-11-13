package com.sketchflow.sketchflow_backend.repository;

import com.sketchflow.sketchflow_backend.model.DirectMessage;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends MongoRepository<DirectMessage, String> {

    List<DirectMessage> findByConversationId(String conversationId, Sort sort);

    // ... (existing method)
    List<DirectMessage> findBySenderUsernameOrReceiverUsername(String senderUsername, String receiverUsername);

    /**
     * NEW: Counts unread messages for a user in a specific conversation
     */
    long countByConversationIdAndReceiverUsernameAndReadFalse(String conversationId, String receiverUsername, boolean read);

    /**
     * NEW: Finds all unread messages for a user in a specific conversation
     */
    List<DirectMessage> findByConversationIdAndReceiverUsernameAndReadFalse(String conversationId, String receiverUsername, boolean read);
}