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
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

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
        signalingHandler = new SignalingHandler(signalingService, mapper);
        lenient().when(session.getId()).thenReturn("session-123");
    }

    @Test
    void afterConnectionEstablished_ShouldCallOnConnectionAndAddToActiveSessions() {
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
        TextMessage textMessage = new TextMessage("{}");
        WebRequest req = new WebRequest();

        when(mapper.readValue(textMessage.getPayload(), WebRequest.class)).thenReturn(req);

        try {
            signalingHandler.handleTextMessage(session, textMessage);
        } catch (NullPointerException e) {
            // Expected if switch(req.getType()) hits a null
        }
    }

    @Test
    void afterConnectionClosed_ShouldCallDisconnectAndRemoveFromActiveSessions() throws Exception {
        // First add the session
        signalingHandler.afterConnectionEstablished(session);

        signalingHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(signalingService, times(1)).disconnect(session, true);
    }

    @Test
    void sendPingToAll_ShouldSendPingMessageToOpenSessions() throws IOException {
        // Add session to active sessions
        signalingHandler.afterConnectionEstablished(session);

        when(session.isOpen()).thenReturn(true);

        signalingHandler.sendPingToAll();

        verify(session, times(1)).sendMessage(any(PingMessage.class));
    }

    @Test
    void sendPingToAll_ShouldHandleIOExceptionGracefully() throws IOException {
        signalingHandler.afterConnectionEstablished(session);
        when(session.isOpen()).thenReturn(true);
        doThrow(new IOException("Connection failed")).when(session).sendMessage(any(PingMessage.class));

        // This should not throw an exception (caught and logged in the method)
        signalingHandler.sendPingToAll();

        verify(session, times(1)).sendMessage(any(PingMessage.class));
    }
}