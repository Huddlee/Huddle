package com.huddlee.backendspringboot.config;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class UserHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        String query = request.getURI().getQuery();

        // later this can be used to get the JWT token and maybe generate guest id
        // rather than using this, maybe try to put a unique userId in the username and use that

        // generate the userId rather than extracting it from the req
        String userId = extractUserId(query);
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }

    private String extractUserId(String query) {
        return query.split("=")[1];
    }

}
