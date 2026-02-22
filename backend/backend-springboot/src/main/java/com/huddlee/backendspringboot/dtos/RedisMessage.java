package com.huddlee.backendspringboot.dtos;

import com.huddlee.backendspringboot.models.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedisMessage {
    private ResponseType type;
    private String message;
    private String from;
    private String to;
}
