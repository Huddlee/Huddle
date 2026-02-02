package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.services.RoomRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class RoomController {

    private final RoomRegistry roomRegistry;

    @GetMapping("/create")
    public String createRoom() {
        // Validate the user JWT, before the req ig, and then create the room
        return roomRegistry.generateRoomCode();
    }

}
