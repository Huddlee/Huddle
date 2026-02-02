package com.huddlee.backendspringboot.dtos;

import com.huddlee.backendspringboot.models.MessageType;
import lombok.Data;

@Data
public class WebRequest {
    MessageType type;
    String roomCode;
    String to;
    String message;
}
