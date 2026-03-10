package com.huddlee.backendspringboot.services.signalingServices;

import com.huddlee.backendspringboot.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserHandshakeInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ServletServerHttpRequest servletRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServerHttpRequest plainRequest;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private UserHandshakeInterceptor interceptor;

    private final String SECRET_SALT = "test-salt-123";
    private final String USERNAME = "testuser";
    private final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        interceptor = new UserHandshakeInterceptor(jwtUtils);
        ReflectionTestUtils.setField(interceptor, "secretSalt", SECRET_SALT);
    }

    @Test
    void beforeHandshake_ShouldExtractTokenFromQueryParam_AndReturnTrue_WhenValid() {
        Map<String, Object> attributes = new HashMap<>();

        // Mocking behavior for extracting token from URL (ServletServerHttpRequest)
        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter("token")).thenReturn(VALID_TOKEN);

        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        assertTrue(result);

        // Ensure accurate UUID calculation
        UUID expectedUuid = UUID.nameUUIDFromBytes((USERNAME + SECRET_SALT).getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedUuid, attributes.get("userId"));
    }

    @Test
    void beforeHandshake_ShouldExtractTokenFromHeader_AndReturnTrue_WhenValid() {
        Map<String, Object> attributes = new HashMap<>();

        // Token is NOT in query param, so mock a standard ServerHttpRequest with Headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + VALID_TOKEN);
        when(plainRequest.getHeaders()).thenReturn(headers);

        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        boolean result = interceptor.beforeHandshake(plainRequest, response, wsHandler, attributes);

        assertTrue(result);

        UUID expectedUuid = UUID.nameUUIDFromBytes((USERNAME + SECRET_SALT).getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedUuid, attributes.get("userId"));
    }

    @Test
    void beforeHandshake_ShouldReturnFalse_WhenTokenIsInvalid() {
        Map<String, Object> attributes = new HashMap<>();

        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter("token")).thenReturn("invalid.token");

        when(jwtUtils.validateToken("invalid.token")).thenReturn(false);

        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty()); // Should not add anything to attributes
    }

    @Test
    void beforeHandshake_ShouldReturnFalse_WhenNoTokenIsFound() {
        Map<String, Object> attributes = new HashMap<>();

        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter("token")).thenReturn(null); // No query param

        HttpHeaders headers = new HttpHeaders(); // No headers
        when(servletRequest.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());
    }
}