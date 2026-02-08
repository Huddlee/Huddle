package com.huddlee.backendspringboot.services;

import com.huddlee.backendspringboot.models.Room;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomRegistry {

    // UserId to SessionId
    private final Map<String, String> userToSession = new HashMap<>();
    // SessionId to UserId
    private final Map<String, String> sessionToUser = new HashMap<>();
    // UserId to RoomCode
    private final Map<String, String> userToRoomCode = new HashMap<>();
    // RoomCode to Room
    private final Map<String, Room> roomCodeToRoom = new ConcurrentHashMap<>();
    // SessionId to Session (this cannot be stored in redis)
    private final Map<String, WebSocketSession> sidToSession = new ConcurrentHashMap<>();


    @Value("${room.code.length}")
    private int codeLen;
    @Value("${room.code.charset}")
    private String charSet;
    private final String rc = "rc:";

    public String generateRoomCode(){
        StringBuilder roomCode = new StringBuilder();
        Random rand = new Random();

        while (roomCode.length() < codeLen)
            roomCode.append(charSet.charAt(rand.nextInt(charSet.length())));

        // regenerate if the rc is a duplicate that is already present
        if(roomCodeToRoom.containsKey(rc + roomCode))
            return generateRoomCode();

        Room room = new Room(roomCode.toString(), new ArrayList<>());
        roomCodeToRoom.put(rc + roomCode, room);
        return roomCode.toString();
    }

    public void registerConnection(String userId, WebSocketSession session) {
        userToSession.put(userId, session.getId());
        sessionToUser.put(session.getId(), userId);
        sidToSession.put(session.getId(), session);
    }

    public boolean roomExists(String roomCode) {
        return roomCodeToRoom.containsKey(rc + roomCode);
    }

    public List<String> getPeers(String roomCode) {
        Room room = roomCodeToRoom.get(rc + roomCode);
        return room.getUsers();
    }

    public boolean peerJoin(WebSocketSession session, String roomCode) {
        Room room = roomCodeToRoom.get(rc + roomCode);
        // Make sure that the size of the room is not full
        if(room.getUsers().size() == 4) return false;
        room.getUsers().add(sessionToUser.get(session.getId()));

        userToRoomCode.put(sessionToUser.get(session.getId()), roomCode);
        return true;
    }

    public String sidToRc(String sid) {
        return userToRoomCode.get(sessionToUser.get(sid));
    }

    public String sidToUid(String sid) {
        return sessionToUser.get(sid);
    }
    
    public boolean peersInSameRoom(String p1, String p2) {
        String rc1 = userToRoomCode.get(p1);
        String rc2 = userToRoomCode.get(p2);
        if (rc1 == null || rc2 == null) return false;
        return rc1.equals(rc2);
    }

    public WebSocketSession uidToSession(String uid) {
        String sid = userToSession.get(uid);
        if(sid == null) return null;

        return sidToSession.get(sid);
    }

    public void disconnect(String sid) {
        String uid = sessionToUser.get(sid);
        Room room = roomCodeToRoom.get(rc + userToRoomCode.get(uid));

        if (room != null)
            room.getUsers().remove(uid);

        userToSession.remove(uid);
        userToRoomCode.remove(uid);
        sessionToUser.remove(sid);
        sidToSession.remove(sid);
    }
}
