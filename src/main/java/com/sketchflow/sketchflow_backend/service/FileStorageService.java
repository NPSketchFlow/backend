package com.sketchflow.sketchflow_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path base;

    public FileStorageService(@Value("${sketchflow.voice.dir:voice-data}") String dir) throws IOException {
        this.base = Paths.get(dir);
        if(!Files.exists(base)) Files.createDirectories(base);
    }

    public void saveAsync(MultipartFile multipart, String filename, CompletionHandler<Integer, Void> handler) throws IOException {
        Path target = base.resolve(filename);
        AsynchronousFileChannel ch = AsynchronousFileChannel.open(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        byte[] bytes;
        try {
            bytes = multipart.getBytes();
        } catch(IOException e){
            ch.close();
            throw e;
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        ch.write(bb, 0, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                handler.completed(result, null);
                try { ch.close(); } catch(IOException ignored){}
            }
            @Override
            public void failed(Throwable exc, Void attachment) {
                handler.failed(exc, null);
                try { ch.close(); } catch(IOException ignored){}
            }
        });
    }

    public Path resolve(String filename){
        return base.resolve(filename);
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
        Path targetPath = base.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return "/files/" + filename;
    }

    public boolean deleteFile(String filename) {
        try {
            Path filePath = base.resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}

