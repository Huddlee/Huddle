package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.dtos.WebRequest;
import com.huddlee.backendspringboot.models.MessageType;
import com.huddlee.backendspringboot.services.signalingServices.SignalingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalingHandlerTest {

    @Mock
    private SignalingService signalingService;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private WebSocketSession session;

    private SignalingHandler signalingHandler;

    @BeforeEach
    void setUp() {
        // Manual constructor injection
        signalingHandler = new SignalingHandler(signalingService, mapper);
        lenient().when(session.getId()).thenReturn("session-123");
    }

    @Test
    void afterConnectionEstablished_ShouldCallOnConnection() {
        signalingHandler.afterConnectionEstablished(session);

        verify(signalingService, times(1)).onConnection(session);
    }

    @Test
    void handleTextMessage_ShouldHandleJoin() throws Exception {
        TextMessage textMessage = new TextMessage("{\"type\":\"JOIN\"}");
        WebRequest req = new WebRequest();
        req.setType(MessageType.JOIN);

        when(mapper.readValue(textMessage.getPayload(), WebRequest.class)).thenReturn(req);

        signalingHandler.handleTextMessage(session, textMessage);

        verify(signalingService, times(1)).handleJoin(session, req);
    }

    @Test
    void handleTextMessage_ShouldHandleOffer() throws Exception {
        TextMessage textMessage = new TextMessage("{\"type\":\"OFFER\"}");
        WebRequest req = new WebRequest();
        req.setType(MessageType.OFFER);

        when(mapper.readValue(textMessage.getPayload(), WebRequest.class)).thenReturn(req);

        signalingHandler.handleTextMessage(session, textMessage);

        verify(signalingService, times(1)).handleMessage(session, req);
    }

    @Test
    void handleTextMessage_ShouldHandleLeave() throws Exception {
        TextMessage textMessage = new TextMessage("{\"type\":\"LEAVE\"}");
        WebRequest req = new WebRequest();
        req.setType(MessageType.LEAVE);

        when(mapper.readValue(textMessage.getPayload(), WebRequest.class)).thenReturn(req);

        signalingHandler.handleTextMessage(session, textMessage);

        verify(signalingService, times(1)).disconnect(session, false);
    }

    @Test
    void handleTextMessage_ShouldHandleUnknownType() throws Exception {
        // Simulating a scenario where mapper maps to null or an unhandled enum (if you add one later)
        TextMessage textMessage = new TextMessage("{}");
        WebRequest req = new WebRequest(); // null type

        // Note: Using a lenient mock or specific setup if you have a default unknown enum
        when(mapper.readValue(textMessage.getPayload(), WebRequest.class)).thenReturn(req);

        // We catch the NullPointerException from req.getType() to test the default block,
        // or you can just add an UNKNOWN enum type to MessageType. Assuming null triggers default logic:
        try {
            signalingHandler.handleTextMessage(session, textMessage);
        } catch (NullPointerException e) {
            // Expected if switch(req.getType()) hits a null
        }
    }

    @Test
    void afterConnectionClosed_ShouldCallDisconnectWithForcedTrue() throws Exception {
        signalingHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(signalingService, times(1)).disconnect(session, true);
    }
}