package com.huddlee.backendspringboot.services.userServices;

import com.huddlee.backendspringboot.dtos.LoginRequest;
import com.huddlee.backendspringboot.models.User;
import com.huddlee.backendspringboot.repos.UserRepo;
import com.huddlee.backendspringboot.jwt.JwtAuthResponse;
import com.huddlee.backendspringboot.jwt.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepo userRepo;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;
    @Mock
    private UserDetailImpl userDetail;

    private UserService userService;

    private final String GUEST_PASSWORD = "guestSecretPassword";
    private final String MOCK_JWT_TOKEN = "mock.jwt.token";

    @BeforeEach
    void setUp() {
        // Manual construction to prevent @InjectMocks confusion
        userService = new UserService(passwordEncoder, userRepo, authManager, jwtUtils);

        // Inject the @Value property for the guest password
        ReflectionTestUtils.setField(userService, "guestPassword", GUEST_PASSWORD);

        // Ensure clean security context before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clean up security context after tests
        SecurityContextHolder.clearContext();
    }

    // --- Login Tests ---

    @Test
    void login_ShouldAuthenticateAndReturnJwt() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetail);
        when(jwtUtils.generateToken(userDetail)).thenReturn(MOCK_JWT_TOKEN);

        // Act
        JwtAuthResponse response = userService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(MOCK_JWT_TOKEN, response.getToken());

        // Verify AuthenticationManager was called with correct credentials
        verify(authManager).authenticate(argThat(auth ->
                auth.getPrincipal().equals("testuser") && auth.getCredentials().equals("password123")
        ));

        // Verify SecurityContext was set
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    // --- Guest Login Tests ---

    @Test
    void guestLogin_ShouldAuthenticateWithGuestPasswordAndReturnJwt() {
        // Arrange
        String guestUsername = "guest_12345";

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetail);
        when(jwtUtils.generateToken(userDetail)).thenReturn(MOCK_JWT_TOKEN);

        // Act
        JwtAuthResponse response = userService.guestLogin(guestUsername);

        // Assert
        assertNotNull(response);
        assertEquals(MOCK_JWT_TOKEN, response.getToken());

        // Verify AuthenticationManager was called with the guest username and the injected @Value guestPassword
        verify(authManager).authenticate(argThat(auth ->
                auth.getPrincipal().equals(guestUsername) && auth.getCredentials().equals(GUEST_PASSWORD)
        ));

        // Verify SecurityContext was set
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    // --- Register Tests ---

    @Test
    void register_ShouldReturnNull_WhenUsernameAlreadyExists() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("existingUser");

        when(userRepo.existsUserByUsername("existingUser")).thenReturn(true);

        // Act
        User result = userService.register(newUser);

        // Assert
        assertNull(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void register_ShouldEncodePasswordAndSaveUser_WhenUsernameIsUnique() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setPassword("rawPassword");

        User savedUser = new User();
        savedUser.setUsername("newUser");
        savedUser.setPassword("encodedPassword");

        when(userRepo.existsUserByUsername("newUser")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepo.save(newUser)).thenReturn(savedUser);

        // Act
        User result = userService.register(newUser);

        // Assert
        assertNotNull(result);
        assertEquals("encodedPassword", result.getPassword());
        assertEquals("newUser", result.getUsername());

        verify(passwordEncoder).encode("rawPassword");
        verify(userRepo).save(newUser);
    }
}