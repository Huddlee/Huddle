package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.dtos.WebRequest;
import com.huddlee.backendspringboot.services.signalingServices.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingHandler extends TextWebSocketHandler {

    private final SignalingService signalingService;
    private final ObjectMapper mapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Add this connection and username into the memory
        signalingService.onConnection(session);
        log.info("WS connected: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        WebRequest req = mapper.readValue(message.getPayload(), WebRequest.class);

        switch (req.getType()) {
            case JOIN: signalingService.handleJoin(session, req); break;
            case ICE, OFFER, ANSWER: signalingService.handleMessage(session, req); break;
            case LEAVE: signalingService.disconnect(session, false); break;
            default: signalingService.unknownMessageType(session); break;
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        signalingService.disconnect(session, true);
        log.info("WS disconnected: {}, status: {}", session.getId(), status);
    }
}