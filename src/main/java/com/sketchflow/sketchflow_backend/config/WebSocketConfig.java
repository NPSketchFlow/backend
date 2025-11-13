package com.sketchflow.sketchflow_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sketchflow.sketchflow_backend.websocket.WhiteboardWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.sketchflow.sketchflow_backend.websocket.ChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WhiteboardWebSocketHandler whiteboardWebSocketHandler;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Use origin patterns rather than wildcard when credentials are allowed elsewhere
        registry.addHandler(whiteboardWebSocketHandler, "/api/whiteboard/sessions/{sessionId}/ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*"); // safer CORS patterns
    }
        // This is the existing route for the whiteboard
        registry.addHandler(whiteboardWebSocketHandler, "/api/whiteboard/sessions/{sessionId}/ws")
                .setAllowedOrigins("*");

        // ADD THIS NEW ROUTE for the DM chat
        registry.addHandler(chatWebSocketHandler, "/api/chat/ws")
                .setAllowedOrigins("*");
    }

//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        // Register JavaTimeModule for Java 8 date/time support
//        mapper.registerModule(new JavaTimeModule());
//        // Disable writing dates as timestamps
//        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        return mapper;
//    }
}
