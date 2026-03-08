package com.huddlee.backendspringboot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huddlee.backendspringboot.dtos.LoginRequest;
import com.huddlee.backendspringboot.dtos.RegisterRequest;
import com.huddlee.backendspringboot.models.User;
import com.huddlee.backendspringboot.jwt.JwtAuthResponse;
import com.huddlee.backendspringboot.services.userServices.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final String GUEST_PASSWORD = "guest-secret-password";

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(userService);
        ReflectionTestUtils.setField(authController, "guestPassword", GUEST_PASSWORD);

        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_ShouldReturnOkWithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("pass123");

        JwtAuthResponse mockResponse = new JwtAuthResponse("mock-jwt-token");
        when(userService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void register_ShouldReturnOk_WhenUserIsRegistered() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("pass123");
        req.setEmail("test@test.com");

        User savedUser = new User();
        savedUser.setUsername("newuser");

        when(userService.register(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered"));

        // Verify mapping logic
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).register(userCaptor.capture());
        assertEquals("newuser", userCaptor.getValue().getUsername());
        assertEquals("newuser", userCaptor.getValue().getDisplayName()); // Display name is set to username
        assertEquals("USER", userCaptor.getValue().getRole());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUsernameExists() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existinguser");
        req.setPassword("pass123");

        // Service returns null when user exists
        when(userService.register(any(User.class))).thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username already exists"));
    }

    @Test
    void guestRegister_ShouldCreateGuestAndReturnToken() throws Exception {
        JwtAuthResponse mockResponse = new JwtAuthResponse("guest-jwt-token");

        // Mock the guest login to return our token
        when(userService.guestLogin(anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/guest/register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("guest-jwt-token"));

        // Verify the user details passed to the register method
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).register(userCaptor.capture());

        User registeredGuest = userCaptor.getValue();
        assertTrue(registeredGuest.getUsername().startsWith("guest_"));
        assertEquals("GUEST", registeredGuest.getRole());
        assertEquals(GUEST_PASSWORD, registeredGuest.getPassword());
    }
}