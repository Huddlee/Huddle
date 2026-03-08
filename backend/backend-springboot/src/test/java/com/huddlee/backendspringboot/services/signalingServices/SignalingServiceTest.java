package com.huddlee.backendspringboot.services.signalingServices;

import com.huddlee.backendspringboot.dtos.RedisMessage;
import com.huddlee.backendspringboot.dtos.WebRequest;
import com.huddlee.backendspringboot.dtos.WsResponse;
import com.huddlee.backendspringboot.models.MessageType;
import com.huddlee.backendspringboot.models.ResponseType;
import com.huddlee.backendspringboot.models.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalingServiceTest {

    @Mock
    private RoomRegistry roomRegistry;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private RedisSubscriptionManager subscriptionManager;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private WebSocketSession session;

    private SignalingService signalingService;

    private final String SESSION_ID = "session-123";
    private final String USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        // 1. Manual constructor injection for absolute safety
        signalingService = new SignalingService(roomRegistry, mapper, subscriptionManager, redisTemplate);

        // 2. Inject @Value properties as requested
        ReflectionTestUtils.setField(signalingService, "MAX_PEERS", 4);
        ReflectionTestUtils.setField(signalingService, "channelName", "redis:");

        // 3. Common session mock setup (lenient because not all tests use them)
        lenient().when(session.getId()).thenReturn(SESSION_ID);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", USER_ID);
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(session.isOpen()).thenReturn(true);

        // Ensure mapper returns a valid string to avoid NullPointerExceptions in TextMessage constructor
        lenient().when(mapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");
    }

    // --- Connection Tests ---

    @Test
    void onConnection_ShouldCloseSession_IfUserAlreadyConnected() throws Exception {
        when(roomRegistry.uidInSession(USER_ID)).thenReturn(true);

        signalingService.onConnection(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(roomRegistry, never()).registerConnection(anyString(), any(WebSocketSession.class));
    }

    @Test
    void onConnection_ShouldRegisterAndSendUserId_IfNewUser() throws Exception {
        when(roomRegistry.uidInSession(USER_ID)).thenReturn(false);
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        signalingService.onConnection(session);

        verify(roomRegistry).registerConnection(USER_ID, session);
        verify(session).sendMessage(any(TextMessage.class));

        // Verify correct payload was sent
        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.USER_ID, responseCaptor.getValue().getResponseType());
        assertEquals(USER_ID, responseCaptor.getValue().getMessage());
    }

    // --- Join Tests ---

    @Test
    void handleJoin_ShouldSendError_IfAlreadyInRoom() {
        when(roomRegistry.sessionInRoom(session)).thenReturn(true);
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        WebRequest req = new WebRequest();
        req.setRoomCode("ROOM1");

        signalingService.handleJoin(session, req);

        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getResponseType());
        assertEquals("Already in a room", responseCaptor.getValue().getMessage());
    }

    @Test
    void handleJoin_ShouldSendError_IfRoomDoesNotExist() {
        when(roomRegistry.sessionInRoom(session)).thenReturn(false);
        when(roomRegistry.getRoom("ROOM1")).thenReturn(null);
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        WebRequest req = new WebRequest();
        req.setRoomCode("ROOM1");

        signalingService.handleJoin(session, req);

        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getResponseType());
        assertEquals("Room does not exist", responseCaptor.getValue().getMessage());
    }

    @Test
    void handleJoin_ShouldSendError_IfRoomIsFull() {
        String roomCode = "ROOM1";
        // MAX_PEERS is 4, so we populate 4 existing users
        Room room = new Room(roomCode, new ArrayList<>(Arrays.asList("u1", "u2", "u3", "u4")));

        when(roomRegistry.sessionInRoom(session)).thenReturn(false);
        when(roomRegistry.getRoom(roomCode)).thenReturn(room);
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        WebRequest req = new WebRequest();
        req.setRoomCode(roomCode);

        signalingService.handleJoin(session, req);

        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getResponseType());
        assertEquals("Room is full", responseCaptor.getValue().getMessage());
    }

    @Test
    void handleJoin_ShouldSuccessfullyJoinAndNotifyPeers() throws Exception {
        String roomCode = "ROOM1";
        Room room = new Room(roomCode, new ArrayList<>(List.of("existingUser")));

        when(roomRegistry.sessionInRoom(session)).thenReturn(false);
        when(roomRegistry.getRoom(roomCode)).thenReturn(room);
        when(roomRegistry.sidToUid(SESSION_ID)).thenReturn(USER_ID);

        // Mock returning the updated peer list after join
        when(roomRegistry.getPeers(roomCode)).thenReturn(List.of("existingUser", USER_ID));
        when(roomRegistry.uidToSid("existingUser")).thenReturn("existingSession");

        // Mock routing: curr user is local, existing peer is remote (Redis)
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);
        when(roomRegistry.isLocalConnection("existingSession")).thenReturn(false);
        when(roomRegistry.sidToRc("existingSession")).thenReturn(roomCode);

        WebRequest req = new WebRequest();
        req.setRoomCode(roomCode);

        signalingService.handleJoin(session, req);

        // Verify state changes
        assertTrue(room.getUsers().contains(USER_ID));
        verify(roomRegistry).saveRoom(room);
        verify(roomRegistry).saveUidToRc(USER_ID, roomCode);
        verify(subscriptionManager).subscribeToRoom(roomCode);

        // Verify local message to joined user (PEER_LIST)
        verify(session).sendMessage(any(TextMessage.class));

        // Verify Redis publication to existing peer (PEER_JOIN)
        verify(redisTemplate).convertAndSend(eq("redis:ROOM1"), anyString());
    }

    // --- Handle Message Tests ---

    @Test
    void handleMessage_ShouldSendError_IfNotInRoom() {
        when(roomRegistry.sidToRc(SESSION_ID)).thenReturn(null); // Not in a room
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        WebRequest req = new WebRequest();
        req.setType(MessageType.OFFER);

        signalingService.handleMessage(session, req);

        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getResponseType());
        assertEquals("Not in a room", responseCaptor.getValue().getMessage());
    }

    @Test
    void handleMessage_ShouldRouteMessageToLocalTargetPeer() throws Exception {
        String roomCode = "ROOM1";
        String targetUser = "user-789";
        String targetSession = "session-789";

        WebRequest req = new WebRequest();
        req.setType(MessageType.OFFER);
        req.setTo(targetUser);
        req.setMessage("sdp-offer-data");

        Room room = new Room(roomCode, new ArrayList<>(List.of(USER_ID, targetUser)));

        when(roomRegistry.sidToRc(SESSION_ID)).thenReturn(roomCode);
        when(roomRegistry.getRoom(roomCode)).thenReturn(room);
        when(roomRegistry.uidToSid(targetUser)).thenReturn(targetSession);
        when(roomRegistry.sidToUid(SESSION_ID)).thenReturn(USER_ID);

        // Simulate target peer being on the same local server
        when(roomRegistry.isLocalConnection(targetSession)).thenReturn(true);
        WebSocketSession mockTargetSession = mock(WebSocketSession.class);
        when(mockTargetSession.isOpen()).thenReturn(true);
        when(roomRegistry.getSessionFromSid(targetSession)).thenReturn(mockTargetSession);

        signalingService.handleMessage(session, req);

        // Verify message was sent to the target peer's session
        verify(mockTargetSession).sendMessage(any(TextMessage.class));

        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.OFFER, responseCaptor.getValue().getResponseType());
        assertEquals("sdp-offer-data", responseCaptor.getValue().getMessage());
        assertEquals(USER_ID, responseCaptor.getValue().getFrom());
    }

    @Test
    void handleMessage_ShouldRouteMessageToRemotePeerViaRedis() {
        String roomCode = "ROOM1";
        String targetUser = "user-789";
        String targetSession = "session-789";

        WebRequest req = new WebRequest();
        req.setType(MessageType.ANSWER);
        req.setTo(targetUser);
        req.setMessage("sdp-answer-data");

        Room room = new Room(roomCode, new ArrayList<>(List.of(USER_ID, targetUser)));

        when(roomRegistry.sidToRc(SESSION_ID)).thenReturn(roomCode);
        when(roomRegistry.getRoom(roomCode)).thenReturn(room);
        when(roomRegistry.uidToSid(targetUser)).thenReturn(targetSession);
        when(roomRegistry.sidToUid(SESSION_ID)).thenReturn(USER_ID);

        // Simulate target peer being remote
        when(roomRegistry.isLocalConnection(targetSession)).thenReturn(false);
        when(roomRegistry.sidToRc(targetSession)).thenReturn(roomCode);

        signalingService.handleMessage(session, req);

        // Verify published to Redis channel
        verify(redisTemplate).convertAndSend(eq("redis:ROOM1"), anyString());

        // Verify RedisMessage payload structure
        ArgumentCaptor<RedisMessage> redisCaptor = ArgumentCaptor.forClass(RedisMessage.class);
        verify(mapper, atLeastOnce()).writeValueAsString(redisCaptor.capture());

        RedisMessage capturedMessage = redisCaptor.getAllValues().stream()
                .filter(m -> m instanceof RedisMessage)
                .findFirst()
                .orElseThrow();

        assertEquals(ResponseType.ANSWER, capturedMessage.getType());
        assertEquals("sdp-answer-data", capturedMessage.getMessage());
        assertEquals(targetSession, capturedMessage.getTo());
    }

    @Test
    void handleMessage_ShouldSendError_IfInvalidResponseType() {
        // Fix: Mock WebRequest to return null for the type.
        // When handleMessage calls req.getType().toString(), it will trigger a NullPointerException
        // effectively falling into the catch block without violating Enum constraints.
        WebRequest req = mock(WebRequest.class);
        when(req.getType()).thenReturn(null);

        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        signalingService.handleMessage(session, req);

        ArgumentCaptor<WsResponse> responseCaptor = ArgumentCaptor.forClass(WsResponse.class);
        verify(mapper).writeValueAsString(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getResponseType());
        assertEquals("Invalid response type", responseCaptor.getValue().getMessage());
    }

    // --- Disconnect Tests ---

    @Test
    void disconnect_ShouldNotifyPeersAndCloseSession_IfNotForced() throws Exception {
        String roomCode = "ROOM1";
        String peerUser = "peer-1";
        String peerSession = "peer-session-1";

        when(roomRegistry.sidToRc(SESSION_ID)).thenReturn(roomCode);
        when(roomRegistry.sidToUid(SESSION_ID)).thenReturn(USER_ID);
        when(roomRegistry.getPeers(roomCode)).thenReturn(List.of(USER_ID, peerUser));
        when(roomRegistry.uidToSid(peerUser)).thenReturn(peerSession);

        // Peer is local, current session is local
        when(roomRegistry.isLocalConnection(peerSession)).thenReturn(true);
        when(roomRegistry.isLocalConnection(SESSION_ID)).thenReturn(true);

        WebSocketSession mockPeerSession = mock(WebSocketSession.class);
        when(mockPeerSession.isOpen()).thenReturn(true);
        when(roomRegistry.getSessionFromSid(peerSession)).thenReturn(mockPeerSession);
        when(roomRegistry.getSessionFromSid(SESSION_ID)).thenReturn(session);

        signalingService.disconnect(session, false); // Not forced

        // 1. Notifies other peer
        verify(mockPeerSession).sendMessage(any(TextMessage.class));

        // 2. Disconnects from registry
        verify(roomRegistry).disconnect(SESSION_ID, USER_ID, roomCode);

        // 3. Sends success to self
        verify(session).sendMessage(any(TextMessage.class));

        // 4. Closes self
        verify(session).close(CloseStatus.NORMAL);

        // 5. Unsubscribes
        verify(subscriptionManager).unsubscribeFromRoom(roomCode);
    }

    @Test
    void disconnect_ShouldSkipSelfMessagesAndClose_IfForced() throws Exception {
        String roomCode = "ROOM1";

        when(roomRegistry.sidToRc(SESSION_ID)).thenReturn(roomCode);
        when(roomRegistry.sidToUid(SESSION_ID)).thenReturn(USER_ID);
        when(roomRegistry.getPeers(roomCode)).thenReturn(List.of(USER_ID)); // Only self in room

        signalingService.disconnect(session, true); // Forced disconnect

        // Verifies registry disconnect happens
        verify(roomRegistry).disconnect(SESSION_ID, USER_ID, roomCode);

        // Verifies we DO NOT send "SuccessFully Disconnected" to self or attempt normal close
        verify(session, never()).sendMessage(any(TextMessage.class));
        verify(session, never()).close(any(CloseStatus.class));

        // Unsubscribes
        verify(subscriptionManager).unsubscribeFromRoom(roomCode);
    }
}