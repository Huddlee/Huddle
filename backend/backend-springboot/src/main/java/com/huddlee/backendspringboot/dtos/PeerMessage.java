package com.huddlee.backendspringboot.dtos;

import com.huddlee.backendspringboot.models.PeerMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PeerMessage {
    PeerMessageType type;
    String message;
    String from;
}
