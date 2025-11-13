package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User addUser(User user) {
        log.info("Saving user: {}", user);
        User saved = userRepository.save(user);
        log.info("Saved user: {}", saved);
        return saved;
    }

    public List<User> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return users;
    }

    public List<User> getUsersByStatus(String status) {
        log.info("Fetching users by status={}", status);
        List<User> users = userRepository.findAll()
                .stream()
                .filter(u -> u.getStatus() != null && u.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        log.info("Found {} users with status {}", users.size(), status);
        return users;
    }

    public void updatePresence(String userId, String status, String ip, int port, long lastSeen) {
        try {
            // userId is mapped to username in this application
            Optional<User> existing = userRepository.findByUsername(userId);
            User target = existing.orElseGet(() -> {
                log.info("Creating new user record for presence update, userId={}", userId);
                User placeholder = new User();
                // set username as identifier
                placeholder.setUsername(userId);
                return placeholder;
            });

            target.setStatus(status);
            target.setIp(ip);
            target.setPort(port);
            target.setLastSeen(lastSeen);

            userRepository.save(target);
        } catch (Exception ex) {
            log.warn("Failed to update presence for user {}: {}", userId, ex.getMessage());
        }
    }
}
