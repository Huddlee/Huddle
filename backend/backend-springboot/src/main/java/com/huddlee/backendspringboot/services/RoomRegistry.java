package com.huddlee.backendspringboot.services;

import com.huddlee.backendspringboot.models.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RoomRegistry {

    private final static String u2s = "u2s:";
    private final static String s2u = "s2u:";
    private final static String u2rc = "u2rc:";
    private final static String rc2r = "rc2r:";

    // UserId to SessionId
    private final StringRedisTemplate uidToSid;
    // SessionId to UserId
    private final StringRedisTemplate sidToUid;
    // UserId to RoomCode
    private final StringRedisTemplate uidToRc;
    // RoomCode to Room
    private final RedisTemplate<String, Room> RcToRoom;

    // SessionId to Session (this cannot be stored in redis)
    private final Map<String, WebSocketSession> sidToSession = new ConcurrentHashMap<>();

    private final Random rand = new Random();

    @Value("${max.room.size}")
    private int MAX_PEERS;
    @Value("${room.code.length}")
    private int codeLen;
    @Value("${room.code.charset}")
    private String charSet;

    public String generateRoomCode(){
        StringBuilder roomCode = new StringBuilder();

        while (roomCode.length() < codeLen)
            roomCode.append(charSet.charAt(rand.nextInt(charSet.length())));

        // regenerate if the rc is a duplicate that is already present
        if(RcToRoom.hasKey(rc2r + roomCode))
            return generateRoomCode();

        Room room = new Room(roomCode.toString(), new ArrayList<>());
        RcToRoom.opsForValue().set(rc2r + roomCode, room);
        return roomCode.toString();
    }

    public boolean canJoin(String roomCode){
        if (RcToRoom.hasKey(rc2r + roomCode)){
            Room room = RcToRoom.opsForValue().get(rc2r + roomCode);
            return room.getUsers().size() < MAX_PEERS;
        }
        return false;
    }

    public void registerConnection(String userId, WebSocketSession session) {
        uidToSid.opsForValue().set(u2s + userId, session.getId());
        sidToUid.opsForValue().set(s2u + session.getId(), userId);
        sidToSession.put(session.getId(), session);
    }

    public boolean roomExists(String roomCode) {
        return RcToRoom.hasKey(rc2r + roomCode);
    }

    public boolean sessionInRoom(WebSocketSession session) {
        return sidToRc(session.getId()) != null;
    }

    public List<String> getPeers(String roomCode) {
        // Gets the room from room code and returns the list of users
        return RcToRoom.opsForValue().get(rc2r + roomCode).getUsers();
    }

    public String sidToRc(String sid) {
        return uidToRc.opsForValue().get(u2rc + sidToUid(sid));
    }

    public String sidToUid(String sid) {
        return sidToUid.opsForValue().get(s2u + sid);
    }

    public String uidToSid(String uid) {
        return uidToSid.opsForValue().get(u2s + uid);
    }

    public Room getRoom(String roomCode) {
        return RcToRoom.opsForValue().get(rc2r + roomCode);
    }

    public void saveRoom(Room room) {
        RcToRoom.opsForValue().set(rc2r + room.getRoomCode(), room);
    }

    public void saveUidToRc(String uid, String rc){
        uidToRc.opsForValue().set(u2rc + uid, rc);
    }


    public boolean isLocalConnection(String sid) {
        if (sid == null || sid.isEmpty()) return true;
        return sidToSession.containsKey(sid);
    }

    public WebSocketSession getSessionFromSid(String sid) {
        return sidToSession.get(sid);
    }

    public void disconnect(String sid, String uid, String rc) {
        Room room = RcToRoom.opsForValue().get(rc2r + rc);

        if (room != null){
            room.getUsers().remove(uid);
            RcToRoom.opsForValue().set(rc2r + uidToRc.opsForValue().get(u2rc + uid), room);
        }

        uidToSid.delete(u2s + uid);
        uidToRc.delete(u2rc + uid);
        sidToUid.delete(s2u + sid);
        sidToSession.remove(sid);
    }
}
