package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.VoiceChat;
import com.sketchflow.sketchflow_backend.service.VoiceChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voice-chats")
@ConditionalOnProperty(name = "app.mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class VoiceChatController {

    private static final Logger log = LoggerFactory.getLogger(VoiceChatController.class);
    private final VoiceChatService voiceChatService;

    public VoiceChatController(VoiceChatService voiceChatService) {
        this.voiceChatService = voiceChatService;
    }

    /**
     Sample JSON:
     {
       "chatId": "c1",
       "senderId": "u1",
       "receiverId": "u2",
       "filePath": "voice-data/uploads/file1.wav",
       "timestamp": 1630000000000
     }
     */
    @PostMapping
    public ResponseEntity<VoiceChat> addVoiceChat(@RequestBody VoiceChat chat) {
        log.info("API: addVoiceChat payload={}", chat);
        VoiceChat saved = voiceChatService.addVoiceChat(chat);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<VoiceChat>> getAllVoiceChats() {
        log.info("API: getAllVoiceChats");
        List<VoiceChat> items = voiceChatService.getAllVoiceChats();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/conversation")
    public ResponseEntity<List<VoiceChat>> getConversation(@RequestParam("participantA") String participantA,
                                                           @RequestParam("participantB") String participantB) {
        log.info("API: getConversation participantA={} participantB={}", participantA, participantB);

        if (participantA == null || participantA.isBlank() || participantB == null || participantB.isBlank()) {
            log.warn("Rejecting getConversation due to missing participant identifiers");
            return ResponseEntity.badRequest().build();
        }

        List<VoiceChat> chats = voiceChatService.getConversation(participantA, participantB);
        return ResponseEntity.ok(chats);
    }
}
