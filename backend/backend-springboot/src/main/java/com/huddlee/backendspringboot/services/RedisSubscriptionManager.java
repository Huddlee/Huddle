package com.huddlee.backendspringboot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriptionManager {

    @Value("${redis.channel.name}")
    private String channelName;
    private final RedisMessageListenerContainer container;
    private final RedisMessageListener listener;

    private final ConcurrentHashMap<String, Integer> localRoomCount = new ConcurrentHashMap<>();

    public void subscribeToRoom(String roomCode) {
        localRoomCount.compute(roomCode, (key, count) -> {
            if (count == null || count == 0) {
                // First local user in this room -> Subscribe to the Redis channel
                container.addMessageListener(listener, new ChannelTopic(channelName + roomCode));
                log.info("Subscribed to Redis channel: {}", channelName + roomCode);
                return 1;
            }
            // Increment the counter
            return count + 1;
        });
    }

    public void unsubscribeFromRoom(String roomCode) {
        localRoomCount.computeIfPresent(roomCode, (key, count) -> {
            if (count <= 1) {
                // Last local user left -> Unsubscribe from the Redis channel
                container.removeMessageListener(listener, new ChannelTopic(channelName + roomCode));
                log.info("Unsubscribed from Redis channel: {}", channelName + roomCode);
                return null; // Removes the key from the map
            }
            // Decrement the counter
            return count - 1;
        });
    }

}
