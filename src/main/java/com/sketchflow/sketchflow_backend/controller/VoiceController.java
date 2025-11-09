package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.udp.UdpServer;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private static final String UPLOAD_DIR = "voice-data/uploads";
    private static final String DOWNLOAD_DIR = "voice-data/downloads";
    private UdpServer udpServer;

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

            // Save the file using NIO FileChannel
            Path filePath = uploadPath.resolve(originalFilename);
            try (var inputStream = file.getInputStream();
                 var fileChannel = Files.newByteChannel(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.allocate(1024);
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer.array())) != -1) {
                    buffer.limit(bytesRead);
                    fileChannel.write(buffer);
                    buffer.clear();
                }
            }

            // Publish a "NEW_VOICE" notification
            String metadata = String.format("{\"fileId\":\"%s\",\"senderId\":\"%s\",\"timestamp\":%d}",
                    originalFilename, senderId, System.currentTimeMillis());
            udpServer.broadcast(metadata.getBytes(StandardCharsets.UTF_8));

            return ResponseEntity.ok("File uploaded successfully: " + originalFilename);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file.");
        }
    }

    // Endpoint for downloading voice files
    @GetMapping("/download/{filename}")
    public ResponseEntity<InputStreamResource> downloadVoice(@PathVariable("filename") String filename) {
        try {
            // Ensure the download directory exists
            Path downloadPath = Paths.get(DOWNLOAD_DIR);
            if (!Files.exists(downloadPath)) {
                Files.createDirectories(downloadPath);
            }

            // Read the file using NIO FileChannel
            Path filePath = downloadPath.resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            var fileChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
            var inputStream = java.nio.channels.Channels.newInputStream(fileChannel);
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
