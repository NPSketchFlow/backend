package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.VoiceChat;
import com.sketchflow.sketchflow_backend.repository.VoiceChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class VoiceChatService {

    private static final Logger log = LoggerFactory.getLogger(VoiceChatService.class);
    private final VoiceChatRepository voiceChatRepository;

    public VoiceChatService(VoiceChatRepository voiceChatRepository) {
        this.voiceChatRepository = voiceChatRepository;
    }

    public VoiceChat addVoiceChat(VoiceChat chat) {
        log.info("Saving voice chat: {}", chat);
        VoiceChat saved = voiceChatRepository.save(chat);
        log.info("Saved voice chat: {}", saved);
        return saved;
    }

    public List<VoiceChat> getAllVoiceChats() {
        log.info("Fetching all voice chats");
        List<VoiceChat> items = voiceChatRepository.findAll();
        log.info("Found {} voice chats", items.size());
        return items;
    }

    public List<VoiceChat> getConversation(String participantA, String participantB) {
        log.info("Fetching conversation between {} and {}", participantA, participantB);
        List<VoiceChat> items = voiceChatRepository.findConversationBetween(participantA, participantB);
        items.sort(Comparator.comparingLong(VoiceChat::getTimestamp));
        log.info("Retrieved {} voice chats for conversation {} <-> {}", items.size(), participantA, participantB);
        return items;
    }
}
