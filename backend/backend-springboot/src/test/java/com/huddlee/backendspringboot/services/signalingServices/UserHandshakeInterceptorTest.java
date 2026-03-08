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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserHandshakeInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;

    // Used for query parameter tests
    @Mock
    private ServletServerHttpRequest servletRequest;
    @Mock
    private HttpServletRequest httpServletRequest;

    // Used for header fallback tests
    @Mock
    private ServerHttpRequest standardRequest;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private UserHandshakeInterceptor interceptor;

    private Map<String, Object> attributes;

    private final String SECRET_SALT = "test-salt-123";
    private final String VALID_TOKEN = "valid.jwt.token";
    private final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        interceptor = new UserHandshakeInterceptor(jwtUtils);
        ReflectionTestUtils.setField(interceptor, "secretSalt", SECRET_SALT);
        attributes = new HashMap<>();
    }

    private UUID calculateExpectedUuid(String username) {
        return UUID.nameUUIDFromBytes((username + SECRET_SALT).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void beforeHandshake_ShouldReturnTrueAndSetAttribute_WhenTokenInQuery() {
        // Arrange
        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter("token")).thenReturn(VALID_TOKEN);

        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        // Act
        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertEquals(calculateExpectedUuid(USERNAME), attributes.get("userId"));
    }

    @Test
    void beforeHandshake_ShouldReturnTrueAndSetAttribute_WhenTokenInHeader() {
        // Arrange
        // Simulate a standard request where it's not a ServletServerHttpRequest,
        // forcing it to fall back to the header extraction logic
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", List.of("Bearer " + VALID_TOKEN));
        when(standardRequest.getHeaders()).thenReturn(headers);

        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        // Act
        boolean result = interceptor.beforeHandshake(standardRequest, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertEquals(calculateExpectedUuid(USERNAME), attributes.get("userId"));
    }

    @Test
    void beforeHandshake_ShouldReturnFalse_WhenNoTokenProvided() {
        // Arrange
        // 1. No token in query
        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter("token")).thenReturn(null);

        // 2. No token in headers
        HttpHeaders headers = new HttpHeaders();
        when(servletRequest.getHeaders()).thenReturn(headers);

        // Act
        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        // Assert
        assertFalse(result);
        assertTrue(attributes.isEmpty());
        verify(jwtUtils, never()).validateToken(anyString()); // Verify we bailed out early
    }

    @Test
    void beforeHandshake_ShouldReturnFalse_WhenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid.token";
        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter("token")).thenReturn(invalidToken);

        when(jwtUtils.validateToken(invalidToken)).thenReturn(false);

        // Act
        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        // Assert
        assertFalse(result);
        assertTrue(attributes.isEmpty());
        // Verify we never attempted to extract the username since the token failed validation
        verify(jwtUtils, never()).getUsernameFromToken(anyString());
    }

    @Test
    void afterHandshake_ShouldExecuteWithoutErrors() {
        // Act & Assert
        // Ensures the empty void method doesn't throw unexpected exceptions
        assertDoesNotThrow(() ->
                interceptor.afterHandshake(standardRequest, response, wsHandler, null)
        );
    }
}