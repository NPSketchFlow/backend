package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.udp.UdpServer;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private static final String UPLOAD_DIR = "voice-data/uploads";
    private final UdpServer udpServer;

    public VoiceController(UdpServer udpServer) {
        this.udpServer = udpServer;
    }

    // Endpoint for uploading voice files
    @PostMapping("/upload")
    public ResponseEntity<String> uploadVoice(@RequestParam("file") MultipartFile file,
                                              @RequestParam("senderId") String senderId) {
        try {
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

            // Save the file using NIO FileChannel (streaming without loading whole file into memory)
            Path filePath = uploadPath.resolve(originalFilename);
            try (InputStream inputStream = file.getInputStream();
                 ReadableByteChannel inChannel = Channels.newChannel(inputStream);
                 var fileChannel = Files.newByteChannel(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
                long total = 0;
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        fileChannel.write(buffer);
                    }
                    total += buffer.position();
                    buffer.clear();
                }
            }

            // Publish a "NEW_VOICE" notification (simple JSON metadata)
            String metadata = String.format("{\"fileId\":\"%s\",\"senderId\":\"%s\",\"timestamp\":%d}",
                    originalFilename, senderId, System.currentTimeMillis());
            // udpServer may be null if not available in tests; guard null
            if (udpServer != null) {
                udpServer.broadcast(metadata.getBytes(StandardCharsets.UTF_8));
            }

            return ResponseEntity.ok("File uploaded successfully: " + originalFilename);
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
