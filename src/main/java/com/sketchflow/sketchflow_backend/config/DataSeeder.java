package com.sketchflow.sketchflow_backend.config;

import com.sketchflow.sketchflow_backend.model.Notification;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.model.VoiceChat;
import com.sketchflow.sketchflow_backend.repository.NotificationRepository;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import com.sketchflow.sketchflow_backend.repository.VoiceChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final Path UPLOAD_DIR = Paths.get("voice-data", "uploads");

    private final UserRepository userRepository;
    private final VoiceChatRepository voiceChatRepository;
    private final NotificationRepository notificationRepository;

    public DataSeeder(UserRepository userRepository,
                      VoiceChatRepository voiceChatRepository,
                      NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.voiceChatRepository = voiceChatRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        ensureUploadSamples();
        seedUsers();
        seedVoiceChats();
        seedNotifications();
    }

    private void ensureUploadSamples() {
        try {
            Files.createDirectories(UPLOAD_DIR);
            Path sampleFile = UPLOAD_DIR.resolve("sample-voice-1.webm");
            if (Files.notExists(sampleFile)) {
                log.info("Creating placeholder voice sample at {}", sampleFile.toAbsolutePath());
                Files.writeString(sampleFile, "SketchFlow sample voice payload", StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            log.warn("Failed to create upload sample files", ex);
        }
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("User collection already populated – skipping seed");
            return;
        }

        long now = Instant.now().toEpochMilli();
        List<User> users = List.of(
                new User("user-1", "Alice Johnson", "ONLINE", "127.0.0.1", 9876, now),
                new User("user-2", "Bob Smith", "ONLINE", "127.0.0.1", 9877, now),
        new User("user-3", "Charlie Brown", "AWAY", "127.0.0.1", 9878, now - 600_000),
        new User("user-4", "Dana Scott", "OFFLINE", "127.0.0.1", 0, now - 1_200_000),
        new User("user-5", "Evan Taylor", "OFFLINE", "127.0.0.1", 0, now - 3_600_000)
        );

        userRepository.saveAll(users);
        log.info("Seeded {} users", users.size());
    }

    private void seedVoiceChats() {
        if (voiceChatRepository.count() > 0) {
            log.info("Voice chat collection already populated – skipping seed");
            return;
        }

        long now = Instant.now().toEpochMilli();
        List<VoiceChat> chats = List.of(
                new VoiceChat(UUID.randomUUID().toString(), "user-1", "user-2", "voice-data/uploads/sample-voice-1.webm", now - 900_000),
                new VoiceChat(UUID.randomUUID().toString(), "user-2", "user-1", "voice-data/uploads/sample-voice-1.webm", now - 300_000)
        );

        voiceChatRepository.saveAll(chats);
        log.info("Seeded {} voice chats", chats.size());
    }

    private void seedNotifications() {
        if (notificationRepository.count() > 0) {
            log.info("Notification collection already populated – skipping seed");
            return;
        }

        long now = Instant.now().toEpochMilli();
        List<Notification> notifications = List.of(
                new Notification("USER_STATUS", null, "user-1", null,
                        "Alice Johnson is now ONLINE", now - 300_000, 0, false,
                        java.util.Map.of("status", "ONLINE")),
                new Notification("NEW_VOICE", "sample-voice-1.webm", "user-1", "user-2",
                        "Alice sent a voice note to Bob", now - 180_000, 1, false,
                        java.util.Map.of("duration", 24)),
                new Notification("USER_STATUS", null, "user-3", null,
                        "Charlie Brown is now AWAY", now - 120_000, 0, false,
                        java.util.Map.of("status", "AWAY"))
        );

        notifications.forEach(n -> {
            if (n.getId() == null || n.getId().isBlank()) {
                n.setId(java.util.UUID.randomUUID().toString());
            }
        });

        notificationRepository.saveAll(notifications);
        log.info("Seeded {} notifications", notifications.size());
    }
}
