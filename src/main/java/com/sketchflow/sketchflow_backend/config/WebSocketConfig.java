package com.sketchflow.sketchflow_backend.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SimpleTextSocketHandler(), "/ws/sketch")
                .setAllowedOrigins("*");  // Allow all origins or specify the allowed ones
    }
}
