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
import java.util.concurrent.TimeUnit;

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

    private RoomRegistry roomRegistry;

    private final long EXPIRATION_TIME = 3600L;
    private final TimeUnit EXPIRATION_UNIT = TimeUnit.SECONDS;

    @BeforeEach
    void setUp() {
        roomRegistry = new RoomRegistry(uidToSid, sidToUid, uidToRc, RcToRoom);

        ReflectionTestUtils.setField(roomRegistry, "MAX_PEERS", 4);
        ReflectionTestUtils.setField(roomRegistry, "codeLen", 6);
        ReflectionTestUtils.setField(roomRegistry, "charSet", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        ReflectionTestUtils.setField(roomRegistry, "attempts", 5);
        ReflectionTestUtils.setField(roomRegistry, "expirationTime", EXPIRATION_TIME);
        ReflectionTestUtils.setField(roomRegistry, "expirationTimeUnit", EXPIRATION_UNIT);

        lenient().when(uidToSid.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(sidToUid.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(uidToRc.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(RcToRoom.opsForValue()).thenReturn(roomValueOperations);
    }

    // --- generateRoomCode Tests ---

    @Test
    void generateRoomCode_ShouldGenerateAndSaveRoomWithExpiration() {
        when(RcToRoom.hasKey(anyString())).thenReturn(false);

        String roomCode = roomRegistry.generateRoomCode(1);

        assertNotNull(roomCode);
        assertEquals(6, roomCode.length());

        // Verifies the new overloaded set method with expiration params
        verify(roomValueOperations, times(1))
                .set(eq("rc2r:" + roomCode), any(Room.class), eq(EXPIRATION_TIME), eq(EXPIRATION_UNIT));
    }

    @Test
    void generateRoomCode_ShouldRegenerateOnCollision() {
        when(RcToRoom.hasKey(anyString())).thenReturn(true).thenReturn(false);

        String roomCode = roomRegistry.generateRoomCode(1);

        assertNotNull(roomCode);
        assertEquals(6, roomCode.length());
        verify(RcToRoom, times(2)).hasKey(anyString());
        verify(roomValueOperations, times(1))
                .set(eq("rc2r:" + roomCode), any(Room.class), eq(EXPIRATION_TIME), eq(EXPIRATION_UNIT));
    }

    @Test
    void generateRoomCode_ShouldReturnNullWhenMaxAttemptsReached() {
        // attempts is set to 5
        String roomCode = roomRegistry.generateRoomCode(6);
        assertNull(roomCode);
    }

    // --- Expiration & Persistence Tests ---

    @Test
    void persistRoom_ShouldCallRedisPersist() {
        String roomCode = "ROOM12";
        when(RcToRoom.persist("rc2r:" + roomCode)).thenReturn(true);

        boolean result = roomRegistry.persistRoom(roomCode);

        assertTrue(result);
        verify(RcToRoom, times(1)).persist("rc2r:" + roomCode);
    }

    @Test
    void setExpiration_ShouldCallRedisExpire() {
        String roomCode = "ROOM12";
        when(RcToRoom.expire("rc2r:" + roomCode, EXPIRATION_TIME, EXPIRATION_UNIT)).thenReturn(true);

        boolean result = roomRegistry.setExpiration(roomCode);

        assertTrue(result);
        verify(RcToRoom, times(1)).expire("rc2r:" + roomCode, EXPIRATION_TIME, EXPIRATION_UNIT);
    }

    // --- Registration & Check Tests (Omitted duplicates for brevity, standard checks remain intact) ---

    @Test
    void registerConnection_ShouldSaveToRedisAndLocalMap() {
        String userId = "user123";
        String sessionId = "session456";
        when(webSocketSession.getId()).thenReturn(sessionId);

        roomRegistry.registerConnection(userId, webSocketSession);

        verify(stringValueOperations).set("u2s:" + userId, sessionId);
        verify(stringValueOperations).set("s2u:" + sessionId, userId);
    }

    // --- Disconnect Tests ---

    @Test
    void disconnect_ShouldRemoveUserFromRoomButNotExpire_WhenRoomIsNotEmpty() {
        String sid = "session123";
        String uid = "user123";
        String rc = "ROOM12";

        // Room has two users
        Room room = new Room(rc, new ArrayList<>(List.of(uid, "otherUser")));

        when(roomValueOperations.get("rc2r:" + rc)).thenReturn(room);

        roomRegistry.disconnect(sid, uid, rc);

        assertFalse(room.getUsers().contains(uid));
        verify(roomValueOperations).set("rc2r:" + rc, room);

        // Ensure expiration is NOT set because there is still another user
        verify(RcToRoom, never()).expire(anyString(), anyLong(), any());

        verify(uidToSid).delete("u2s:" + uid);
    }

    @Test
    void disconnect_ShouldSetExpiration_WhenRoomBecomesEmpty() {
        String sid = "session123";
        String uid = "user123";
        String rc = "ROOM12";

        // Room only has the disconnecting user
        Room room = new Room(rc, new ArrayList<>(List.of(uid)));

        when(roomValueOperations.get("rc2r:" + rc)).thenReturn(room);

        roomRegistry.disconnect(sid, uid, rc);

        assertTrue(room.getUsers().isEmpty());
        verify(roomValueOperations).set("rc2r:" + rc, room);

        // Ensure setExpiration is called when the room empties out
        verify(RcToRoom, times(1)).expire("rc2r:" + rc, EXPIRATION_TIME, EXPIRATION_UNIT);

        verify(uidToSid).delete("u2s:" + uid);
    }
}