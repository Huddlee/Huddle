package com.huddlee.backendspringboot.config;

import com.huddlee.backendspringboot.controllers.SignalingHandler;
import com.huddlee.backendspringboot.services.signalingServices.UserHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${signaling.session.max.idle.time}")
    private long maxIdleTime;

    private final SignalingHandler signalingHandler;
    private final UserHandshakeInterceptor userHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws")
                .addInterceptors(userHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Set the idle timeout to 30 seconds (30,000 milliseconds)
        container.setMaxSessionIdleTimeout(maxIdleTime);
        return container;
    }

}