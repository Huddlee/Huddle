package com.huddlee.backendspringboot.services.signalingServices;

import com.huddlee.backendspringboot.dtos.RedisMessage;
import com.huddlee.backendspringboot.dtos.WsResponse;
import com.huddlee.backendspringboot.models.ResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisMessageListenerTest {

    @Mock
    private ObjectMapper mapper;

    @Mock
    private RoomRegistry roomRegistry;

    @Mock
    private Message message;

    @Mock
    private WebSocketSession session;

    private RedisMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new RedisMessageListener(mapper, roomRegistry);
    }

    @Test
    void onMessage_ShouldSendWebSocketMessage_WhenSessionIsOpen() throws Exception {
        // Arrange
        String payload = "{\"type\":\"OFFER\",\"message\":\"sdp\",\"from\":\"userA\",\"to\":\"sessionB\"}";
        byte[] payloadBytes = payload.getBytes();
        when(message.getBody()).thenReturn(payloadBytes);

        RedisMessage redisMessage = new RedisMessage(ResponseType.OFFER, "sdp", "userA", "sessionB");
        when(mapper.readValue(payload, RedisMessage.class)).thenReturn(redisMessage);

        when(roomRegistry.getSessionFromSid("sessionB")).thenReturn(session);
        when(session.isOpen()).thenReturn(true);
        when(mapper.writeValueAsString(any(WsResponse.class))).thenReturn("{\"response\":\"mock\"}");

        // Act
        listener.onMessage(message, null);

        // Assert
        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(textMessageCaptor.capture());
        assertEquals("{\"response\":\"mock\"}", textMessageCaptor.getValue().getPayload());

        ArgumentCaptor<WsResponse> wsResponseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(wsResponseCaptor.capture());
        assertEquals(ResponseType.OFFER, wsResponseCaptor.getValue().getResponseType());
        assertEquals("sdp", wsResponseCaptor.getValue().getMessage());
        assertEquals("userA", wsResponseCaptor.getValue().getFrom());
    }

    @Test
    void onMessage_ShouldNotSendMessage_WhenSessionIsNull() throws Exception {
        String payload = "{\"to\":\"sessionB\"}";
        when(message.getBody()).thenReturn(payload.getBytes());
        RedisMessage redisMessage = new RedisMessage(ResponseType.OFFER, "sdp", "userA", "sessionB");
        when(mapper.readValue(payload, RedisMessage.class)).thenReturn(redisMessage);

        // Session not found
        when(roomRegistry.getSessionFromSid("sessionB")).thenReturn(null);

        listener.onMessage(message, null);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void onMessage_ShouldNotSendMessage_WhenSessionIsClosed() throws Exception {
        String payload = "{\"to\":\"sessionB\"}";
        when(message.getBody()).thenReturn(payload.getBytes());
        RedisMessage redisMessage = new RedisMessage(ResponseType.OFFER, "sdp", "userA", "sessionB");
        when(mapper.readValue(payload, RedisMessage.class)).thenReturn(redisMessage);

        when(roomRegistry.getSessionFromSid("sessionB")).thenReturn(session);
        // Session is found but closed
        when(session.isOpen()).thenReturn(false);

        listener.onMessage(message, null);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void onMessage_ShouldCatchAndLogException_WhenParsingFails() throws Exception {
        when(message.getBody()).thenReturn("invalid-json".getBytes());
        when(mapper.readValue("invalid-json", RedisMessage.class)).thenThrow(new RuntimeException("Parsing error"));

        // Should not throw exception outwards
        listener.onMessage(message, null);

        verify(roomRegistry, never()).getSessionFromSid(anyString());
    }
}