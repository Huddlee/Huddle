package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.services.signalingServices.RoomRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final RoomRegistry roomRegistry;
    @Value("${signaling.secret.salt}")
    private String secretSalt;

    @GetMapping("/create")
    public ResponseEntity<?> createRoom(Principal principal) {

        if (canNotQuery(principal)) {
            return ResponseEntity.status(409).body("User already in a room");
        }
        return ResponseEntity.ok(roomRegistry.generateRoomCode());
    }

    // Check if the user is already in a room?
    @GetMapping("/join/{roomCode}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomCode, Principal principal) {

        if (canNotQuery(principal)) {
            return ResponseEntity.status(409).body("User already in a room");
        }

        // If there is any place in the room, send a confirmation. After that the user can upgrade to webSocket connection
        if (!roomRegistry.roomExists(roomCode)) {
            return ResponseEntity.notFound().build();
        }
        if (roomRegistry.canJoin(roomCode)) {
            return ResponseEntity.ok("Room can be joined");
        }
        return ResponseEntity.status(409).body("Room is full");
    }

    private boolean canNotQuery(Principal principal) {
        String username = principal.getName();

        String uid = String.valueOf(
                UUID.nameUUIDFromBytes(
                        (username + secretSalt)
                                .getBytes(StandardCharsets.UTF_8)));

        // Check if the user is already in a room (session)
        return roomRegistry.uidInSession(uid);
    }
}
