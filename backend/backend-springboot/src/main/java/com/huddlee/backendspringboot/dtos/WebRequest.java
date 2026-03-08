package com.huddlee.backendspringboot.dtos;

import com.huddlee.backendspringboot.models.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WebRequest {
    @NotBlank(message = "Message type is required")
    MessageType type;
    String roomCode;
    String to;
    String message;
}
