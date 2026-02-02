package com.huddlee.backendspringboot.config;

import com.huddlee.backendspringboot.controllers.SignalingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final SignalingHandler signalingHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws")
                .addInterceptors(new UserHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}