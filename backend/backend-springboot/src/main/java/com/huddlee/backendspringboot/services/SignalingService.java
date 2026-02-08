package com.huddlee.backendspringboot.services;

import com.huddlee.backendspringboot.dtos.WsResponse;
import com.huddlee.backendspringboot.models.ResponseType;
import com.huddlee.backendspringboot.dtos.WebRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignalingService {
    private final RoomRegistry roomRegistry;
    private final ObjectMapper mapper;

    public void onConnection(WebSocketSession session) {
        // Already a session with the userId
        String userId = session.getAttributes().get("userId").toString();

        if(roomRegistry.uidToSession(userId) != null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
            catch (Exception e) {
                log.error("Error closing session: {}, sessionId {}", e.getMessage(), session.getId());
            }
            return;
        }

        roomRegistry.registerConnection(userId, session);
    }

    public void handleJoin(WebSocketSession session, WebRequest req) {
        // Validate the room code, if the room exists, send the user the list of all the users in the room currently
        // else send an error that the room does not exist

        // Also check if the current peer is already in a room
        if (roomRegistry.sidToRc(session.getId()) != null) {
            sendMessage(session, ResponseType.ERROR, "Already in a room");
            return;
        }

        if (roomRegistry.roomExists(req.getRoomCode())){
            // add the current peer
            if (roomRegistry.peerJoin(session, req.getRoomCode())) {
                List<String> peerNames = roomRegistry.getPeers(req.getRoomCode());
                sendMessage(session, ResponseType.PEER_LIST, peerNames.toString());
            }
            else {
                sendMessage(session, ResponseType.ERROR, "Room is full");
            }
        }
        else {
            sendMessage(session, ResponseType.ERROR, "Room does not exist");
        }
    }

    public void handleMessage(WebSocketSession session, WebRequest req) {
        // get the room code by the session id, if the room exists, get the username for whom this message is sent
        // make sure that both the users are in the same room

        String roomCode = roomRegistry.sidToRc(session.getId());
        String userId = roomRegistry.sidToUid(session.getId());
        if (roomCode != null) {
            if (roomRegistry.peersInSameRoom(userId, req.getTo())) {
                // Get the other peers session and send them the response
                WebSocketSession otherPeer = roomRegistry.uidToSession(req.getTo());
                sendMessage(otherPeer, ResponseType.valueOf(req.getType().toString()), req.getMessage());
            }
            else {
                sendMessage(session, ResponseType.ERROR, "Invalid request");
            }
        }
        else {
            sendMessage(session, ResponseType.ERROR, "Not in a room");
        }
    }

    public void unknownMessageType(WebSocketSession session) {
        sendMessage(session, ResponseType.ERROR, "Unknown message type");
    }

    private void sendMessage(WebSocketSession session, ResponseType type, String message) {
        WsResponse response = new WsResponse(type, message);
        try {
            if (session != null && session.isOpen())
                session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
        }
        catch (Exception e) {
            log.warn("Error sending message: {}, sessionId {}", e.getMessage(), session.getId());
        }
    }

    public void disconnect(WebSocketSession session, boolean forced) {
        // Get hold of sessions of other peers and then disconnect curr peer
        String roomCode = roomRegistry.sidToRc(session.getId());
        String uid = roomRegistry.sidToUid(session.getId());
        if (roomCode != null) {
            List<String> peers = roomRegistry.getPeers(roomCode);
            // Notify all the other peers so that they can disconnect from the peer
            for (String peer : peers) {
                if (!peer.equals(uid))
                    sendMessage(roomRegistry.uidToSession(peer), ResponseType.PEER_DC, uid + " disconnected");
            }
        }
        roomRegistry.disconnect(session.getId());

        if (!forced)
            sendMessage(session, ResponseType.PEER_DC, "SuccessFully Disconnected");
    }

}