package com.sketchflow.sketchflow_backend.controller;

import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testdb")
@ConditionalOnProperty(name = "app.mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class TestDbController {

    private final UserRepository userRepository;

    public TestDbController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public User addUser(@RequestBody User user) {
        return userRepository.save(user);
    }
}
