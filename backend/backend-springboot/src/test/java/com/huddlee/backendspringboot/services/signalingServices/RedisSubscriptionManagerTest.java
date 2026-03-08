package com.huddlee.backendspringboot.services.signalingServices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSubscriptionManagerTest {

    @Mock
    private RedisMessageListenerContainer container;

    @Mock
    private RedisMessageListener listener;

    private RedisSubscriptionManager subscriptionManager;

    private final String CHANNEL_PREFIX = "redis:";
    private final String ROOM_CODE = "ROOM123";

    @BeforeEach
    void setUp() {
        subscriptionManager = new RedisSubscriptionManager(container, listener);
        ReflectionTestUtils.setField(subscriptionManager, "channelName", CHANNEL_PREFIX);
    }

    @Test
    void subscribeToRoom_ShouldAddListener_OnFirstUser() {
        subscriptionManager.subscribeToRoom(ROOM_CODE);

        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container, times(1)).addMessageListener(eq(listener), topicCaptor.capture());

        assertEquals(CHANNEL_PREFIX + ROOM_CODE, topicCaptor.getValue().getTopic());
    }

    @Test
    void subscribeToRoom_ShouldNotAddListener_OnSubsequentUsers() {
        // First user subscribes
        subscriptionManager.subscribeToRoom(ROOM_CODE);
        // Second user subscribes to the same room
        subscriptionManager.subscribeToRoom(ROOM_CODE);

        // Verify the listener was only added ONCE
        verify(container, times(1)).addMessageListener(eq(listener), any(ChannelTopic.class));
    }

    @Test
    void unsubscribeFromRoom_ShouldRemoveListener_WhenLastUserLeaves() {
        // One user joins
        subscriptionManager.subscribeToRoom(ROOM_CODE);

        // The only user leaves
        subscriptionManager.unsubscribeFromRoom(ROOM_CODE);

        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container, times(1)).removeMessageListener(eq(listener), topicCaptor.capture());
        assertEquals(CHANNEL_PREFIX + ROOM_CODE, topicCaptor.getValue().getTopic());
    }

    @Test
    void unsubscribeFromRoom_ShouldNotRemoveListener_WhenOtherUsersRemain() {
        // Two users join
        subscriptionManager.subscribeToRoom(ROOM_CODE);
        subscriptionManager.subscribeToRoom(ROOM_CODE);

        // One user leaves
        subscriptionManager.unsubscribeFromRoom(ROOM_CODE);

        // Verify the listener was NOT removed because count is still 1
        verify(container, never()).removeMessageListener(eq(listener), any(ChannelTopic.class));
    }

    @Test
    void unsubscribeFromRoom_ShouldDoNothing_IfRoomDoesNotExist() {
        // Attempt to unsubscribe from a room that no one joined locally
        subscriptionManager.unsubscribeFromRoom("NON_EXISTENT_ROOM");

        // Verify no interactions with the container
        verify(container, never()).removeMessageListener(any(), any(ChannelTopic.class));
    }
}