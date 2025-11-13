package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.DirectMessage;
import com.sketchflow.sketchflow_backend.repository.DirectMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.stream.Collectors;

import java.util.List;

@Service
public class DirectMessageService {
    // --- ADD THIS DTO CLASS ---
    @Data
    @AllArgsConstructor
    public static class ConversationDTO {
        public DirectMessage lastMessage;
        public long unreadCount;
        public String otherUser; // The username of the person you're talking to
    }
    // --- END OF DTO ---

    @Autowired
    private DirectMessageRepository dmRepository;

    @Autowired
    private MongoTemplate mongoTemplate; // For complex queries

    /**
     * Saves a new DM and returns the saved entity.
     */
    public DirectMessage saveMessage(String senderUsername, String receiverUsername, String messageContent) {
        DirectMessage dm = new DirectMessage(senderUsername, receiverUsername, messageContent);
        return dmRepository.save(dm);
    }

    /**
     * Gets the full chat history between two users.
     */
    public List<DirectMessage> getConversationHistory(String user1, String user2) {
        String conversationId;
        if (user1.compareTo(user2) > 0) {
            conversationId = user2 + ":" + user1;
        } else {
            conversationId = user1 + ":" + user2;
        }
        return dmRepository.findByConversationId(conversationId, Sort.by(Sort.Direction.ASC, "timestamp"));
    }

    /**
     * REPLACED METHOD:
     * Gets a list of all conversations for a user, with the *last* message
     * and the count of unread messages for each.
     */
    public List<ConversationDTO> getConversations(String username) {
        // 1. Find all messages sent or received by the user
        Criteria userCriteria = new Criteria().orOperator(
                Criteria.where("senderUsername").is(username),
                Criteria.where("receiverUsername").is(username)
        );
        MatchOperation matchStage = Aggregation.match(userCriteria);

        // 2. Sort them by time (newest first)
        SortOperation sortStage = Aggregation.sort(Sort.by(Sort.Direction.DESC, "timestamp"));

        // 3. Group by the 'conversationId' and pick the *first* (newest) message
        GroupOperation groupStage = Aggregation.group("conversationId")
                .first("$$ROOT").as("lastMessage");

        // 4. Replace the root document with the last message
        ReplaceRootOperation replaceRootStage = Aggregation.replaceRoot("lastMessage");

        // 5. Sort again to have the newest conversations at the top
        SortOperation finalSortStage = Aggregation.sort(Sort.by(Sort.Direction.DESC, "timestamp"));

        // Build and run the aggregation
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                sortStage,
                groupStage,
                replaceRootStage,
                finalSortStage
        );

        AggregationResults<DirectMessage> results = mongoTemplate.aggregate(
                aggregation, "direct_messages", DirectMessage.class
        );

        // 6. Now, for each conversation, get the unread count
        List<DirectMessage> lastMessages = results.getMappedResults();

        return lastMessages.stream().map(dm -> {
            // Determine who the "other user" is
            String otherUser = dm.getSenderUsername().equals(username)
                    ? dm.getReceiverUsername()
                    : dm.getSenderUsername();

            // Count unread messages *for me* (the receiver) in this convo
            long unreadCount = dmRepository.countByConversationIdAndReceiverUsernameAndReadFalse(
                    dm.getConversationId(),
                    username,
                    false
            );

            return new ConversationDTO(dm, unreadCount, otherUser);
        }).collect(Collectors.toList());
    }

    /**
     * NEW METHOD:
     * Marks all messages in a conversation as read by the specified user.
     */
    public void markAsRead(String readerUsername, String otherUsername) {
        // 1. Re-create the conversation ID
        String conversationId;
        if (readerUsername.compareTo(otherUsername) > 0) {
            conversationId = otherUsername + ":" + readerUsername;
        } else {
            conversationId = readerUsername + ":" + otherUsername;
        }

        // 2. Find all messages in this convo *sent to me* that are unread
        List<DirectMessage> unreadMessages = dmRepository.findByConversationIdAndReceiverUsernameAndReadFalse(
                conversationId,
                readerUsername,
                false
        );

        if (unreadMessages.isEmpty()) {
            return; // Nothing to do
        }

        // 3. Mark all as read and save
        unreadMessages.forEach(msg -> msg.setRead(true));
        dmRepository.saveAll(unreadMessages);
    }
}