package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.dtos.WebRequest;
import com.huddlee.backendspringboot.services.signalingServices.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingHandler extends TextWebSocketHandler {

    private final SignalingService signalingService;
    private final ObjectMapper mapper;

    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Add this connection and username into the memory
        signalingService.onConnection(session);
        // Add this to be pinged
        activeSessions.add(session);
        log.info("WS connected: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            WebRequest req = mapper.readValue(message.getPayload(), WebRequest.class);

            switch (req.getType()) {
                case JOIN: signalingService.handleJoin(session, req); break;
                case ICE, OFFER, ANSWER: signalingService.handleMessage(session, req); break;
                case LEAVE: signalingService.disconnect(session, false); break;
                default: signalingService.unknownMessageType(session); break;
            }
        }
        catch (Exception e) {
            signalingService.unknownMessageType(session);
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        signalingService.disconnect(session, true);
        // remove from the active sessions
        activeSessions.remove(session);
        log.info("WS disconnected: {}, status: {}", session.getId(), status);
    }

    @Scheduled(fixedRateString = "${signaling.session.ping.rate}")
    public void sendPingToAll() {
        PingMessage ping = new PingMessage();

        for (WebSocketSession session : activeSessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(ping);
                } catch (IOException e) {
                    log.warn("Failed to send ping to {}", session.getId());
                }
            }
        }
    }
}