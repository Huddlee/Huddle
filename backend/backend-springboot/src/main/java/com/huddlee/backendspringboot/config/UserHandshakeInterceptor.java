package com.huddlee.backendspringboot.config;

import com.huddlee.backendspringboot.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;
    @Value("${signaling.secret.salt}")
    private String secretSalt;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        HttpHeaders headers = request.getHeaders();
        List<String> auth = headers.get("Authorization");
        if (auth == null || auth.isEmpty()) {
            return false;
        }
        String token = auth.getFirst().replace("Bearer ", "");

        if (jwtUtils.validateToken(token)) {
            // Generate the UUID for signaling
            attributes.put("userId",
                    UUID.nameUUIDFromBytes(
                            (jwtUtils.getUsernameFromToken(token) + secretSalt)
                                    .getBytes(StandardCharsets.UTF_8)));
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {
    }
}
