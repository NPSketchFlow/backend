package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Register this controller only when Mongo-backed services are enabled
@RestController
@RequestMapping("/api/users")
@ConditionalOnProperty(name = "app.mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     Sample JSON:
     {
       "userId": "u1",
       "username": "alice",
       "status": "ONLINE",
       "ip": "127.0.0.1",
       "port": 9877,
       "lastSeen": 1630000000000
     }
     */
    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user) {
        log.info("API: addUser payload={}", user);
        User saved = userService.addUser(user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("API: getAllUsers");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/online")
    public ResponseEntity<List<User>> getOnlineUsersFromDb() {
        log.info("API: getOnlineUsersFromDb");
        List<User> users = userService.getUsersByStatus("ONLINE");
        return ResponseEntity.ok(users);
    }
}
