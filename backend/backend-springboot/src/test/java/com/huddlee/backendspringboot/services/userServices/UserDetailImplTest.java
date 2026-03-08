package com.huddlee.backendspringboot.services.userServices;

import com.huddlee.backendspringboot.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailImplTest {

    @Test
    void build_ShouldMapUserToUserDetailsCorrectly() {
        // Arrange
        User user = new User();
        user.setId("user123");
        user.setUsername("testuser");
        user.setDisplayName("Test User");
        user.setEmail("test@test.com");
        user.setPassword("securePassword");
        user.setRole("ROLE_USER");

        // Act
        UserDetailImpl userDetails = UserDetailImpl.build(user);

        // Assert
        assertEquals("user123", userDetails.getId());
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("Test User", userDetails.getDisplayName());
        assertEquals("test@test.com", userDetails.getEmail());
        assertEquals("securePassword", userDetails.getPassword());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    void securityFlags_ShouldAllBeTrue() {
        UserDetailImpl userDetails = new UserDetailImpl();

        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }
}