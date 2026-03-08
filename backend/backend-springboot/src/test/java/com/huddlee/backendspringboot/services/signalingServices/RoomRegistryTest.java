package com.huddlee.backendspringboot.services.signalingServices;

import com.huddlee.backendspringboot.models.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomRegistryTest {

    @Mock
    private StringRedisTemplate uidToSid;
    @Mock
    private StringRedisTemplate sidToUid;
    @Mock
    private StringRedisTemplate uidToRc;
    @Mock
    private RedisTemplate<String, Room> RcToRoom;

    @Mock
    private ValueOperations<String, String> stringValueOperations;
    @Mock
    private ValueOperations<String, Room> roomValueOperations;

    @Mock
    private WebSocketSession webSocketSession;

    // Notice: We removed @InjectMocks here
    private RoomRegistry roomRegistry;

    @BeforeEach
    void setUp() {
        // We manually construct it to ensure Mockito doesn't scramble the 4 RedisTemplates
        roomRegistry = new RoomRegistry(uidToSid, sidToUid, uidToRc, RcToRoom);

        // Inject @Value properties
        ReflectionTestUtils.setField(roomRegistry, "MAX_PEERS", 4);
        ReflectionTestUtils.setField(roomRegistry, "codeLen", 6);
        ReflectionTestUtils.setField(roomRegistry, "charSet", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

        // Globally mock opsForValue() to prevent NPEs across all tests
        lenient().when(uidToSid.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(sidToUid.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(uidToRc.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(RcToRoom.opsForValue()).thenReturn(roomValueOperations);
    }

    // --- generateRoomCode Tests ---

    @Test
    void generateRoomCode_ShouldGenerateAndSaveRoom() {
        when(RcToRoom.hasKey(anyString())).thenReturn(false);

        String roomCode = roomRegistry.generateRoomCode(1);

        assertNotNull(roomCode);
        assertEquals(6, roomCode.length());
        verify(roomValueOperations, times(1)).set(eq("rc2r:" + roomCode), any(Room.class));
    }

    @Test
    void generateRoomCode_ShouldRegenerateOnCollision() {
        // First call returns true (collision), second call returns false (success)
        when(RcToRoom.hasKey(anyString())).thenReturn(true).thenReturn(false);

        String roomCode = roomRegistry.generateRoomCode(1);

        assertNotNull(roomCode);
        assertEquals(6, roomCode.length());
        verify(RcToRoom, times(2)).hasKey(anyString());
        verify(roomValueOperations, times(1)).set(eq("rc2r:" + roomCode), any(Room.class));
    }

    // --- canJoin Tests ---

    @Test
    void canJoin_ShouldReturnTrue_WhenRoomExistsAndNotFull() {
        String roomCode = "ROOM12";
        Room room = new Room(roomCode, new ArrayList<>(List.of("user1", "user2")));

        when(RcToRoom.hasKey("rc2r:" + roomCode)).thenReturn(true);
        when(roomValueOperations.get("rc2r:" + roomCode)).thenReturn(room);

        assertTrue(roomRegistry.canJoin(roomCode));
    }

    @Test
    void canJoin_ShouldReturnFalse_WhenRoomIsFull() {
        String roomCode = "ROOM12";
        List<String> users = new ArrayList<>(Arrays.asList("u1", "u2", "u3", "u4", "u5")); // MAX_PEERS is 5
        Room room = new Room(roomCode, users);

        when(RcToRoom.hasKey("rc2r:" + roomCode)).thenReturn(true);
        when(roomValueOperations.get("rc2r:" + roomCode)).thenReturn(room);

        assertFalse(roomRegistry.canJoin(roomCode));
    }

    @Test
    void canJoin_ShouldReturnFalse_WhenRoomDoesNotExist() {
        String roomCode = "ROOM12";
        when(RcToRoom.hasKey("rc2r:" + roomCode)).thenReturn(false);

        assertFalse(roomRegistry.canJoin(roomCode));
    }

    // --- Registration & Check Tests ---

    @Test
    void registerConnection_ShouldSaveToRedisAndLocalMap() {
        String userId = "user123";
        String sessionId = "session456";
        when(webSocketSession.getId()).thenReturn(sessionId);

        roomRegistry.registerConnection(userId, webSocketSession);

        verify(stringValueOperations).set("u2s:" + userId, sessionId);
        verify(stringValueOperations).set("s2u:" + sessionId, userId);

        assertTrue(roomRegistry.isLocalConnection(sessionId));
        assertEquals(webSocketSession, roomRegistry.getSessionFromSid(sessionId));
    }

    @Test
    void roomExists_ShouldReturnTrueIfKeyExists() {
        when(RcToRoom.hasKey("rc2r:ROOM12")).thenReturn(true);
        assertTrue(roomRegistry.roomExists("ROOM12"));
    }

    @Test
    void uidInSession_ShouldReturnTrueIfUidMapped() {
        when(stringValueOperations.get("u2s:user123")).thenReturn("session123");
        assertTrue(roomRegistry.uidInSession("user123"));
    }

    @Test
    void sessionInRoom_ShouldReturnTrueIfSidMappedToRc() {
        when(stringValueOperations.get("s2u:session123")).thenReturn("user123");
        when(stringValueOperations.get("u2rc:user123")).thenReturn("ROOM12");

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session123");

        assertTrue(roomRegistry.sessionInRoom(session));
    }

    // --- Getter & Setter Tests ---

    @Test
    void getPeers_ShouldReturnUserList() {
        String roomCode = "ROOM12";
        List<String> users = Arrays.asList("user1", "user2");
        Room room = new Room(roomCode, users);

        when(roomValueOperations.get("rc2r:" + roomCode)).thenReturn(room);

        List<String> peers = roomRegistry.getPeers(roomCode);
        assertEquals(2, peers.size());
        assertTrue(peers.containsAll(users));
    }

    @Test
    void getRoom_ShouldReturnRoomObject() {
        Room room = new Room("ROOM12", new ArrayList<>());
        when(roomValueOperations.get("rc2r:ROOM12")).thenReturn(room);

        assertEquals(room, roomRegistry.getRoom("ROOM12"));
    }

    @Test
    void saveRoom_ShouldCallRedisSet() {
        Room room = new Room("ROOM12", new ArrayList<>());

        roomRegistry.saveRoom(room);
        verify(roomValueOperations).set("rc2r:ROOM12", room);
    }

    @Test
    void saveUidToRc_ShouldCallRedisSet() {
        roomRegistry.saveUidToRc("user123", "ROOM12");
        verify(stringValueOperations).set("u2rc:user123", "ROOM12");
    }

    // --- Local Connection Tests ---

    @Test
    void isLocalConnection_ShouldHandleNullAndEmptyCorrectly() {
        assertTrue(roomRegistry.isLocalConnection(null));
        assertTrue(roomRegistry.isLocalConnection(""));
        assertFalse(roomRegistry.isLocalConnection("unknownSession"));
    }

    // --- Disconnect Tests ---

    @Test
    void disconnect_ShouldRemoveUserFromRoomAndClearRedis() {
        String sid = "session123";
        String uid = "user123";
        String rc = "ROOM12";

        Room room = new Room(rc, new ArrayList<>(List.of(uid, "otherUser")));

        when(roomValueOperations.get("rc2r:" + rc)).thenReturn(room);

        roomRegistry.disconnect(sid, uid, rc);

        // Verify user was removed from room and room was saved
        assertFalse(room.getUsers().contains(uid));
        verify(roomValueOperations).set("rc2r:" + rc, room);

        // Verify redis deletions
        verify(uidToSid).delete("u2s:" + uid);
        verify(uidToRc).delete("u2rc:" + uid);
        verify(sidToUid).delete("s2u:" + sid);
    }

    @Test
    void disconnect_ShouldHandleNullRoomCodeSafely() {
        String sid = "session123";
        String uid = "user123";
        // User connects but never joins a room, so rc is null
        String rc = null;

        when(roomValueOperations.get("rc2r:null")).thenReturn(null);

        roomRegistry.disconnect(sid, uid, rc);

        // Verify we never attempted to save a null room
        verify(roomValueOperations, never()).set(anyString(), any(Room.class));

        // Verify redis deletions still happen
        verify(uidToSid).delete("u2s:" + uid);
        verify(uidToRc).delete("u2rc:" + uid);
        verify(sidToUid).delete("s2u:" + sid);
    }
}