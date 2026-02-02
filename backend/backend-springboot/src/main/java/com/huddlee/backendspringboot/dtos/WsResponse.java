package com.huddlee.backendspringboot.dtos;

import com.huddlee.backendspringboot.models.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WsResponse {
    ResponseType responseType;
    String message;
}
