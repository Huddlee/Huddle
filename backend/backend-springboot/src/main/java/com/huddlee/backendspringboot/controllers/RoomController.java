package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.services.RoomRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final RoomRegistry roomRegistry;

    @GetMapping("/create")
    public String createRoom() {
        // Validate the user JWT, before the req ig, and then create the room
        return roomRegistry.generateRoomCode();
    }

    @PostMapping("/join/{roomCode}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomCode) {
        // If there is any place in the room, send a confirmation. After that the user can upgrade to webSocket connection
        if (!roomRegistry.roomExists(roomCode)){
            return ResponseEntity.notFound().build();
        }
        if (roomRegistry.canJoin(roomCode)){
            return ResponseEntity.ok("Room can be joined");
        }
        return ResponseEntity.status(409).body("Room is full");
    }
}
