package com.sketchflow.sketchflow_backend.config;

import com.sketchflow.sketchflow_backend.websocket.VoiceChatHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class VoiceChatWebSocketConfig implements WebSocketConfigurer {

    private final VoiceChatHandler voiceChatHandler;

    public VoiceChatWebSocketConfig(VoiceChatHandler voiceChatHandler) {
        this.voiceChatHandler = voiceChatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceChatHandler, "/ws/voice")
            .setAllowedOrigins("*");
    }
}
