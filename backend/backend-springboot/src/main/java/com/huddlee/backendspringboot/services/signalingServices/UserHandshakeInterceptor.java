package com.huddlee.backendspringboot.services.signalingServices;

import com.huddlee.backendspringboot.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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

        String token = null;

        // 1. Try to get the token from query parameters (For Browsers)
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }

        // 2. Fallback to header (For Postman/Testing tools)
        if (token == null) {
            HttpHeaders headers = request.getHeaders();
            List<String> auth = headers.get("Authorization");
            if (auth != null && !auth.isEmpty()) {
                token = auth.getFirst().replace("Bearer ", "");
            }
        }

        // 3. Reject if no token is found
        if (token == null) {
            return false;
        }

        // Validate and set attributes
        if (jwtUtils.validateToken(token)) {
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
