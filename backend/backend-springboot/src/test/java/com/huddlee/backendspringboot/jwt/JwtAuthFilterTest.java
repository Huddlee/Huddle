package com.huddlee.backendspringboot.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private final String VALID_TOKEN = "valid.jwt.token";
    private final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Ensure the security context is empty before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clean up the context after each test to prevent cross-test pollution
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenValidTokenInHeader() throws ServletException, IOException {
        // Arrange
        when(jwtUtils.getJWTFromHeader(request)).thenReturn(VALID_TOKEN);
        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenValidTokenInQueryForWebSocket() throws ServletException, IOException {
        // Arrange
        when(jwtUtils.getJWTFromHeader(request)).thenReturn(null); // No header
        when(request.getRequestURI()).thenReturn("/ws/signaling"); // It's a WS connection
        when(request.getParameter("token")).thenReturn(VALID_TOKEN); // Token is in query

        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenNoTokenProvided() throws ServletException, IOException {
        // Arrange
        when(jwtUtils.getJWTFromHeader(request)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/regular-endpoint");

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtUtils, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenTokenIsInvalid() throws ServletException, IOException {
        // Arrange
        when(jwtUtils.getJWTFromHeader(request)).thenReturn("invalid.token");
        when(jwtUtils.validateToken("invalid.token")).thenReturn(false);

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenUserNotFound() throws ServletException, IOException {
        // Arrange
        when(jwtUtils.getJWTFromHeader(request)).thenReturn(VALID_TOKEN);
        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);

        when(userDetailsService.loadUserByUsername(USERNAME)).thenThrow(new UsernameNotFoundException("User not found"));

        // Act
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication()); // Context remains empty
        verify(filterChain, times(1)).doFilter(request, response); // But the chain continues
    }
}