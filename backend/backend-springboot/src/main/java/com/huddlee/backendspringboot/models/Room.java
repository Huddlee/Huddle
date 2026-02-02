package com.huddlee.backendspringboot.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Room {
    String roomCode;
    List<String> users;
}
