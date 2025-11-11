package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.VoiceChat;
import com.sketchflow.sketchflow_backend.service.VoiceChatService;
import com.sketchflow.sketchflow_backend.udp.UdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private static final String UPLOAD_DIR = "voice-data/uploads";
    private static final Logger log = LoggerFactory.getLogger(VoiceController.class);
    private final UdpServer udpServer;
    private final VoiceChatService voiceChatService;

    public VoiceController(UdpServer udpServer, VoiceChatService voiceChatService) {
        this.udpServer = udpServer;
        this.voiceChatService = voiceChatService;
    }

    // Endpoint for uploading voice files
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVoice(@RequestParam("file") MultipartFile file,
                                         @RequestParam("senderId") String senderId,
                                         @RequestParam(value = "receiverId", required = false) String receiverId) {
        try {
            if (senderId == null || senderId.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender is required.");
            }

            if (receiverId != null && receiverId.isBlank()) {
                receiverId = null;
            }

            // Ensure the upload directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Validate file name
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file name.");
            }

            String storedFilename = UUID.randomUUID() + "-" + originalFilename.replaceAll("\\s+", "_");

            // Save the file using NIO FileChannel (streaming without loading whole file into memory)
            Path filePath = uploadPath.resolve(storedFilename);
            try (InputStream inputStream = file.getInputStream();
                 ReadableByteChannel inChannel = Channels.newChannel(inputStream);
                 var fileChannel = Files.newByteChannel(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        fileChannel.write(buffer);
                    }
                    buffer.clear();
                }
            }

            // Publish a "NEW_VOICE" notification (simple JSON metadata)
            long timestamp = System.currentTimeMillis();
            String metadata = String.format("{\"fileId\":\"%s\",\"senderId\":\"%s\",\"timestamp\":%d}",
                    storedFilename, senderId, timestamp);
            // udpServer may be null if not available in tests; guard null
            if (udpServer != null) {
                udpServer.broadcast(metadata.getBytes(StandardCharsets.UTF_8));
            }

            VoiceChat savedChat = voiceChatService.addVoiceChat(
                    new VoiceChat(UUID.randomUUID().toString(), senderId, receiverId, filePath.toString(), timestamp)
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "uploaded");
            response.put("fileId", storedFilename);
            response.put("downloadUrl", "/api/voice/download/" + storedFilename);
            response.put("voiceChat", savedChat);

            log.info("Uploaded voice file {} for sender {} (receiver {})", storedFilename, senderId, receiverId);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file. " + e.getMessage());
        }
    }

    // Endpoint for downloading voice files
    @GetMapping("/download/{filename}")
    public ResponseEntity<InputStreamResource> downloadVoice(@PathVariable("filename") String filename) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            var fileChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
            var inputStream = java.nio.channels.Channels.newInputStream(fileChannel);
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentLength(Files.size(filePath))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
