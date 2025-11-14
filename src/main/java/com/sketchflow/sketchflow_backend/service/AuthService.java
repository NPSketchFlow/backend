package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.dto.AuthResponse;
import com.sketchflow.sketchflow_backend.dto.LoginRequest;
import com.sketchflow.sketchflow_backend.dto.RegisterRequest;
import com.sketchflow.sketchflow_backend.dto.UpdateProfileRequest;
import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import com.sketchflow.sketchflow_backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create new user
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            user.setAvatar(request.getAvatar());
        } else {
            // Generate default avatar URL
            user.setAvatar("https://ui-avatars.com/api/?name=" + request.getFullName().replace(" ", "+") + "&background=random");
        }

        user = userRepository.save(user);

        // Generate JWT token
        String token = tokenProvider.generateTokenFromUsername(user.getUsername());

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                user.getRoles()
        );
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);

        // Update last login
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                user.getRoles()
        );
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if authentication exists and is not anonymous
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {

            logger.warning("⚠️ No authenticated user found in security context");
            if (authentication != null) {
                logger.warning("   Authentication type: " + authentication.getClass().getName());
                logger.warning("   Is authenticated: " + authentication.isAuthenticated());
            }
            return null;
        }

        String username = authentication.getName();
        logger.info("✅ Current authenticated user: " + username);
        return userRepository.findByUsername(username).orElse(null);
    }

    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Update the current authenticated user's profile.
     *
     * @param request The request containing the new profile data
     * @return The updated User object
     */
    public User updateProfile(UpdateProfileRequest request) {
        // 1. Get the currently authenticated user
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        // 2. Update the fields if they are provided in the request
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            currentUser.setFullName(request.getFullName());
        }

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            currentUser.setAvatar(request.getAvatar());
        }

        // 3. Save the updated user back to the database
        return userRepository.save(currentUser);
    }
}

