package com.huddlee.backendspringboot.services.signalingServices;

import com.huddlee.backendspringboot.dtos.RedisMessage;
import com.huddlee.backendspringboot.dtos.WsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageListener implements MessageListener {

    private final ObjectMapper mapper;
    private final RoomRegistry roomRegistry;

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        try {
            String payload = new String(message.getBody());
            RedisMessage redisMessage = mapper.readValue(payload, RedisMessage.class);
            WebSocketSession session = roomRegistry.getSessionFromSid(redisMessage.getTo());

            if (session != null && session.isOpen()) {
                WsResponse wsResponse = new WsResponse(redisMessage.getType(), redisMessage.getMessage(), redisMessage.getFrom());
                session.sendMessage(new TextMessage(mapper.writeValueAsString(wsResponse)));
            }
        }
        catch (Exception e) {
            log.error("Error in RedisMessageListener: {}", e.getMessage());
        }
    }
}
