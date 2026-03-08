package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.services.signalingServices.RoomRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomRegistry roomRegistry;

    private final String SECRET_SALT = "test-salt-123";
    private final String USERNAME = "testuser";
    private String expectedUid;

    @BeforeEach
    void setUp() {
        RoomController roomController = new RoomController(roomRegistry);
        ReflectionTestUtils.setField(roomController, "secretSalt", SECRET_SALT);

        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();

        // Calculate the exact UUID the controller will generate
        expectedUid = String.valueOf(
                UUID.nameUUIDFromBytes((USERNAME + SECRET_SALT).getBytes(StandardCharsets.UTF_8))
        );
    }

    // --- Mock Principal Setup ---
    private Principal getMockPrincipal() {
        return () -> USERNAME;
    }

    // --- Create Room Tests ---

    @Test
    void createRoom_ShouldReturnRoomCode_WhenUserNotInSession() throws Exception {
        when(roomRegistry.uidInSession(expectedUid)).thenReturn(false);
        when(roomRegistry.generateRoomCode(1)).thenReturn("ROOM12");

        mockMvc.perform(get("/api/room/create").principal(getMockPrincipal()))
                .andExpect(status().isOk())
                .andExpect(content().string("ROOM12"));
    }

    @Test
    void createRoom_ShouldReturn409_WhenUserAlreadyInSession() throws Exception {
        when(roomRegistry.uidInSession(expectedUid)).thenReturn(true);

        mockMvc.perform(get("/api/room/create").principal(getMockPrincipal()))
                .andExpect(status().isConflict())
                .andExpect(content().string("User already in a room"));
    }

    // --- Join Room Tests ---

    @Test
    void joinRoom_ShouldReturnOk_WhenRoomCanBeJoined() throws Exception {
        String roomCode = "ROOM12";
        when(roomRegistry.uidInSession(expectedUid)).thenReturn(false);
        when(roomRegistry.roomExists(roomCode)).thenReturn(true);
        when(roomRegistry.canJoin(roomCode)).thenReturn(true);

        mockMvc.perform(get("/api/room/join/{roomCode}", roomCode).principal(getMockPrincipal()))
                .andExpect(status().isOk())
                .andExpect(content().string("Room can be joined"));
    }

    @Test
    void joinRoom_ShouldReturn409_WhenUserAlreadyInSession() throws Exception {
        when(roomRegistry.uidInSession(expectedUid)).thenReturn(true);

        mockMvc.perform(get("/api/room/join/ROOM12").principal(getMockPrincipal()))
                .andExpect(status().isConflict())
                .andExpect(content().string("User already in a room"));
    }

    @Test
    void joinRoom_ShouldReturn404_WhenRoomDoesNotExist() throws Exception {
        String roomCode = "ROOM12";
        when(roomRegistry.uidInSession(expectedUid)).thenReturn(false);
        when(roomRegistry.roomExists(roomCode)).thenReturn(false);

        mockMvc.perform(get("/api/room/join/{roomCode}", roomCode).principal(getMockPrincipal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinRoom_ShouldReturn409_WhenRoomIsFull() throws Exception {
        String roomCode = "ROOM12";
        when(roomRegistry.uidInSession(expectedUid)).thenReturn(false);
        when(roomRegistry.roomExists(roomCode)).thenReturn(true);
        when(roomRegistry.canJoin(roomCode)).thenReturn(false); // Room is full

        mockMvc.perform(get("/api/room/join/{roomCode}", roomCode).principal(getMockPrincipal()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Room is full"));
    }
}