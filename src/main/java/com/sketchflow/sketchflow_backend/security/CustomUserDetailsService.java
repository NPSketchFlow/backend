package com.sketchflow.sketchflow_backend.security;

import com.sketchflow.sketchflow_backend.model.User;
import com.sketchflow.sketchflow_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = Logger.getLogger(CustomUserDetailsService.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("👤 CustomUserDetailsService.loadUserByUsername() called");
        System.out.println("   Looking for username: " + username);
        logger.info("Loading user by username: " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.err.println("❌ USER NOT FOUND in database: " + username);
                    logger.severe("User not found with username: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        System.out.println("✅ User FOUND in database!");
        System.out.println("   Username: " + user.getUsername());
        System.out.println("   Enabled: " + user.isEnabled());
        System.out.println("   Roles: " + user.getRoles());
        logger.info("User found: " + user.getUsername() + ", enabled: " + user.isEnabled() + ", roles: " + user.getRoles());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isAccountNonLocked(),
                getAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}

