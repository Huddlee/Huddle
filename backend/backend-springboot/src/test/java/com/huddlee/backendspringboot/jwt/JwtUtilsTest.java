package com.huddlee.backendspringboot.jwt;

import com.huddlee.backendspringboot.services.userServices.UserDetailImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    // A valid 256-bit Base64 encoded secret key required by HMAC-SHA algorithms
    private final String TEST_SECRET = "VGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIEpXVA==";
    private final long TEST_EXPIRATION = 3600000; // 1 hour

    private UserDetailImpl mockUserDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", TEST_EXPIRATION);

        mockUserDetails = new UserDetailImpl(
                "1",
                "testuser",
                "Test User",
                "test@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getJWTFromHeader_ShouldReturnToken_WhenBearerHeaderExists() {
        when(request.getHeader("Authorization")).thenReturn("Bearer my.fake.token");

        String token = jwtUtils.getJWTFromHeader(request);

        assertEquals("my.fake.token", token);
    }

    @Test
    void getJWTFromHeader_ShouldReturnNull_WhenHeaderIsMissingOrInvalid() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertNull(jwtUtils.getJWTFromHeader(request));

        when(request.getHeader("Authorization")).thenReturn("Basic user:pass");
        assertNull(jwtUtils.getJWTFromHeader(request));
    }

    @Test
    void generateToken_ShouldReturnValidJwtString() {
        String token = jwtUtils.generateToken(mockUserDetails);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // A valid JWT has 3 parts separated by dots
    }

    @Test
    void getUsernameFromToken_ShouldExtractCorrectUsername() {
        String token = jwtUtils.generateToken(mockUserDetails);

        String username = jwtUtils.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtils.generateToken(mockUserDetails);

        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {
        String invalidToken = "this.is.not.a.real.token";

        assertFalse(jwtUtils.validateToken(invalidToken));
    }
}