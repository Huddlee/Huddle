package com.huddlee.backendspringboot.services.signalingServices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSubscriptionManagerTest {

    @Mock
    private RedisMessageListenerContainer container;

    @Mock
    private RedisMessageListener listener;

    private RedisSubscriptionManager subscriptionManager;

    private final String CHANNEL_PREFIX = "redis-channel:";

    @BeforeEach
    void setUp() {
        subscriptionManager = new RedisSubscriptionManager(container, listener);
        ReflectionTestUtils.setField(subscriptionManager, "channelName", CHANNEL_PREFIX);
    }

    @Test
    void subscribeToRoom_ShouldAddListener_WhenFirstUserSubscribes() {
        String roomCode = "ROOM123";

        subscriptionManager.subscribeToRoom(roomCode);

        // Verifies listener was registered
        verify(container, times(1)).addMessageListener(eq(listener), any(ChannelTopic.class));
    }

    @Test
    void subscribeToRoom_ShouldNotAddListenerAgain_WhenSubsequentUsersSubscribe() {
        String roomCode = "ROOM123";

        subscriptionManager.subscribeToRoom(roomCode); // 1st user
        subscriptionManager.subscribeToRoom(roomCode); // 2nd user
        subscriptionManager.subscribeToRoom(roomCode); // 3rd user

        // Should still only be registered once
        verify(container, times(1)).addMessageListener(eq(listener), any(ChannelTopic.class));
    }

    @Test
    void unsubscribeFromRoom_ShouldRemoveListener_WhenLastUserLeaves() {
        String roomCode = "ROOM123";

        // Setup: One user subscribes
        subscriptionManager.subscribeToRoom(roomCode);

        // Act: User unsubscribes
        subscriptionManager.unsubscribeFromRoom(roomCode);

        verify(container, times(1)).removeMessageListener(eq(listener), any(ChannelTopic.class));
    }

    @Test
    void unsubscribeFromRoom_ShouldNotRemoveListener_WhenOtherUsersRemain() {
        String roomCode = "ROOM123";

        // Setup: Two users subscribe
        subscriptionManager.subscribeToRoom(roomCode);
        subscriptionManager.subscribeToRoom(roomCode);

        // Act: One user unsubscribes
        subscriptionManager.unsubscribeFromRoom(roomCode);

        // Listener should NOT be removed yet (Explicitly typed to avoid ambiguous method call error)
        verify(container, never()).removeMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }

    @Test
    void unsubscribeFromRoom_ShouldHandleNullOrBlankRoomCodesGracefully() {
        subscriptionManager.unsubscribeFromRoom(null);
        subscriptionManager.unsubscribeFromRoom("");
        subscriptionManager.unsubscribeFromRoom("   ");

        // Explicitly typed to avoid ambiguous method call error
        verify(container, never()).removeMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }
}