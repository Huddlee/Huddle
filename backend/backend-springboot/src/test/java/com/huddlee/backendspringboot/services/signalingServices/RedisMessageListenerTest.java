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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisMessageListenerTest {

    @Mock
    private ObjectMapper mapper;

    @Mock
    private RoomRegistry roomRegistry;

    @Mock
    private WebSocketSession session;

    @Mock
    private Message redisMessageObj;

    private RedisMessageListener redisMessageListener;

    private final String TARGET_SID = "session-123";

    @BeforeEach
    void setUp() {
        redisMessageListener = new RedisMessageListener(mapper, roomRegistry);
    }

    @Test
    void onMessage_ShouldSendWebSocketMessage_WhenSessionIsOpen() throws Exception {
        // 1. Prepare dummy data
        String jsonPayload = "{\"type\":\"OFFER\",\"message\":\"sdp-data\",\"from\":\"user1\",\"to\":\"" + TARGET_SID + "\"}";
        byte[] payloadBytes = jsonPayload.getBytes();

        RedisMessage parsedMessage = new RedisMessage(ResponseType.OFFER, "sdp-data", "user1", TARGET_SID);
        String mappedResponseStr = "{\"type\":\"OFFER\",\"message\":\"sdp-data\",\"from\":\"user1\"}";

        // 2. Mock behavior
        when(redisMessageObj.getBody()).thenReturn(payloadBytes);
        when(mapper.readValue(jsonPayload, RedisMessage.class)).thenReturn(parsedMessage);
        when(roomRegistry.getSessionFromSid(TARGET_SID)).thenReturn(session);
        when(session.isOpen()).thenReturn(true);
        when(mapper.writeValueAsString(any(WsResponse.class))).thenReturn(mappedResponseStr);

        // 3. Execute
        redisMessageListener.onMessage(redisMessageObj, null);

        // 4. Verify
        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(textMessageCaptor.capture());

        assertEquals(mappedResponseStr, textMessageCaptor.getValue().getPayload());
    }

    @Test
    void onMessage_ShouldNotSendMessage_WhenSessionIsNull() throws Exception {
        String jsonPayload = "{\"to\":\"" + TARGET_SID + "\"}";
        RedisMessage parsedMessage = new RedisMessage(ResponseType.OFFER, "sdp", "user1", TARGET_SID);

        when(redisMessageObj.getBody()).thenReturn(jsonPayload.getBytes());
        when(mapper.readValue(jsonPayload, RedisMessage.class)).thenReturn(parsedMessage);
        when(roomRegistry.getSessionFromSid(TARGET_SID)).thenReturn(null); // Session not found

        redisMessageListener.onMessage(redisMessageObj, null);

        // Verify we never attempted to send a message
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void onMessage_ShouldNotSendMessage_WhenSessionIsClosed() throws Exception {
        String jsonPayload = "{\"to\":\"" + TARGET_SID + "\"}";
        RedisMessage parsedMessage = new RedisMessage(ResponseType.OFFER, "sdp", "user1", TARGET_SID);

        when(redisMessageObj.getBody()).thenReturn(jsonPayload.getBytes());
        when(mapper.readValue(jsonPayload, RedisMessage.class)).thenReturn(parsedMessage);
        when(roomRegistry.getSessionFromSid(TARGET_SID)).thenReturn(session);
        when(session.isOpen()).thenReturn(false); // Session is closed

        redisMessageListener.onMessage(redisMessageObj, null);

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void onMessage_ShouldHandleExceptionGracefully() throws Exception {
        // Return malformed JSON or trigger a mapper exception
        byte[] badPayload = "invalid-json".getBytes();
        when(redisMessageObj.getBody()).thenReturn(badPayload);
        when(mapper.readValue("invalid-json", RedisMessage.class)).thenThrow(new RuntimeException("Parse error"));

        // Execute - this should hit the catch block and log the error, but NOT crash the application
        redisMessageListener.onMessage(redisMessageObj, null);

        // Verify we bailed out early and never touched the registry or sessions
        verify(roomRegistry, never()).getSessionFromSid(anyString());
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}