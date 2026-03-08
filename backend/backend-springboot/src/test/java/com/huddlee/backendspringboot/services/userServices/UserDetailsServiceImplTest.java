package com.huddlee.backendspringboot.services.userServices;

import com.huddlee.backendspringboot.models.User;
import com.huddlee.backendspringboot.repos.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId("1");
        mockUser.setUsername("testuser");
        mockUser.setPassword("password123");
        mockUser.setRole("USER");
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Arrange
        when(userRepo.findByUsername("testuser")).thenReturn(mockUser);

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        verify(userRepo, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserDoesNotExist() {
        // Arrange
        when(userRepo.findByUsername("unknownuser")).thenReturn(null);

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknownuser")
        );

        assertEquals("User not found with username: unknownuser", exception.getMessage());
        verify(userRepo, times(1)).findByUsername("unknownuser");
    }
}