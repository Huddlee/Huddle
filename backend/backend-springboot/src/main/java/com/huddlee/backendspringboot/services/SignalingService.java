package com.huddlee.backendspringboot.services;

import com.huddlee.backendspringboot.dtos.RedisMessage;
import com.huddlee.backendspringboot.dtos.WebRequest;
import com.huddlee.backendspringboot.dtos.WsResponse;
import com.huddlee.backendspringboot.models.ResponseType;
import com.huddlee.backendspringboot.models.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Value("${redis.channel.name}")
    private String channelName;
    @Value("${max.room.size}")
    private int MAX_PEERS;

    private final RoomRegistry roomRegistry;
    private final ObjectMapper mapper;
    private final RedisSubscriptionManager subscriptionManager;
    private final StringRedisTemplate redisTemplate;


    public void onConnection(WebSocketSession session) {
        // Already a session with the uid
        String uid = session.getAttributes().get("userId").toString();

        // check if this UID is already associated with a sid
        if(roomRegistry.uidToSid(uid) != null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
            catch (Exception e) {
                log.error("Error closing session: {}, sessionId {}", e.getMessage(), session.getId());
            }
            return;
        }

        roomRegistry.registerConnection(uid, session);

        sendMessage(session.getId(), ResponseType.USER_ID, uid, null);
    }

    // Notify other peers too, even if they don't send OFFER first
    public void handleJoin(WebSocketSession session, WebRequest req) {
        // Validate the room code, if the room exists, send the user the list of all the users in the room currently
        // else send an error that the room does not exist

        // Check if the current peer is already in a room
        if (roomRegistry.sessionInRoom(session)) {
            sendMessage(session.getId(), ResponseType.ERROR, "Already in a room", null);
            return;
        }

        Room room = roomRegistry.getRoom(req.getRoomCode());
        // Does the room exist?
        if (room != null){
            String roomCode = req.getRoomCode();

            // Is there any space in the room?
            if(room.getUsers().size() < MAX_PEERS){
                String uid = roomRegistry.sidToUid(session.getId());

                // Add the user into the room
                room.getUsers().add(uid);

                // Save the room and uid to rc
                roomRegistry.saveRoom(room);
                roomRegistry.saveUidToRc(uid, roomCode);

                // Subscribe to the redis channel for this room to get updates
                subscriptionManager.subscribeToRoom(roomCode);

                // Send the user the list of peers in the room
                List<String> peerNames = room.getUsers();
                sendMessage(session.getId(), ResponseType.PEER_LIST, peerNames.toString(), null);

                // Send the existing peers update for peer join
                List<String> peers = roomRegistry.getPeers(roomCode);
                for(String peer : peers) {
                    if(!peer.equals(uid))
                        sendMessage(roomRegistry.uidToSid(peer), ResponseType.PEER_JOIN, uid + "Joined the room", null);
                }
            }
            else {
                sendMessage(session.getId(), ResponseType.ERROR, "Room is full", null);
            }
        }
        else {
            sendMessage(session.getId(), ResponseType.ERROR, "Room does not exist", null);
        }
    }

    public void handleMessage(WebSocketSession session, WebRequest req) {
        // get the room code by the session id, if the room exists, get the username for whom this message is sent
        // make sure that both the users are in the same room

        try {
            // Is this a valid response type?
            ResponseType responseType = ResponseType.valueOf(req.getType().toString());

            String roomCode = roomRegistry.sidToRc(session.getId());
            // Does the room even exist or the sender is in a room?
            if (roomCode != null) {
                Room room = roomRegistry.getRoom(roomCode);

                // Check if the receiver is in the sender's room
                if(room.getUsers().contains(req.getTo())){
                    // Get the other peers sid and send them the response
                    String otherPeer = roomRegistry.uidToSid(req.getTo());

                    String userId = roomRegistry.sidToUid(session.getId());
                    sendMessage(
                            otherPeer, // To
                            responseType, // Message type
                            req.getMessage(), // Actual Message
                            userId); // From
                }
                else {
                    sendMessage(session.getId(), ResponseType.ERROR, "Invalid request", null);
                }
            }
            else {
                sendMessage(session.getId(), ResponseType.ERROR, "Not in a room", null);
            }
        }
        catch (Exception e) {
            sendMessage(session.getId(), ResponseType.ERROR, "Invalid response type", null);
        }
    }

    public void unknownMessageType(WebSocketSession session) {
        sendMessage(session.getId(), ResponseType.ERROR, "Unknown message type", null);
    }

    private void sendMessage(String sid, ResponseType type, String message, String from) {
        if(from == null) from = "SERVER";

        // If this is a local session, then get the session from sid and send the message/response
        if (roomRegistry.isLocalConnection(sid)) {
            WebSocketSession session = roomRegistry.getSessionFromSid(sid);
            WsResponse response = new WsResponse(type, message, from);
            try {
                if (session != null && session.isOpen())
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
            }
            catch (Exception e) {
                log.warn("Error sending message: {}, sessionId {}", e.getMessage(), session.getId());
            }
        }
        // If this is not a local session, then publish the message to the redis channel
        else {
            publishToRedis(sid, type, message, from);
        }
    }

    private void publishToRedis(String to, ResponseType type, String message, String from) {
        RedisMessage redisMessage = new RedisMessage(type, message, from, to);
        try {
            redisTemplate.convertAndSend(channelName + roomRegistry.sidToRc(to), mapper.writeValueAsString(redisMessage));
        }
        catch (Exception e) {
            log.warn("Error publishing message: {}, sessionId {}", e.getMessage(), to);
        }
    }

    public void disconnect(WebSocketSession session, boolean forced) {
        // If a peer is not in a room, we can check that and send not in a room message, but it won't matter anyway

        // Get hold of sessions of other peers and then disconnect curr peer
        String roomCode = roomRegistry.sidToRc(session.getId());
        String uid = roomRegistry.sidToUid(session.getId());

        if (roomCode != null) {
            List<String> peers = roomRegistry.getPeers(roomCode);
            // Notify all the other peers so that they can disconnect from the peer
            for (String peer : peers) {
                if (!peer.equals(uid))
                    sendMessage(roomRegistry.uidToSid(uid), ResponseType.PEER_DC, uid + " disconnected", null);
            }
        }

        roomRegistry.disconnect(session.getId(), uid, roomCode);

        if (!forced){
            sendMessage(session.getId(), ResponseType.PEER_DC, "SuccessFully Disconnected", null);
            try{
                session.close(CloseStatus.NORMAL);
            }catch (Exception e){
                log.warn("Error closing session: {}, sessionId {}", e.getMessage(), session.getId());
            }
        }

        // Unsubscribe to the redis channel for this room
        subscriptionManager.unsubscribeFromRoom(roomCode);
    }

}