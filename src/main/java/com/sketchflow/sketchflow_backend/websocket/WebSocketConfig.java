package com.sketchflow.sketchflow_backend.websocket;

import com.sketchflow.sketchflow_backend.service.ChatService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatService chatService;

    public WebSocketConfig(ChatService chatService) { this.chatService = chatService; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(chatService), "/ws/chat")
                .setAllowedOrigins("*");
    }
}
