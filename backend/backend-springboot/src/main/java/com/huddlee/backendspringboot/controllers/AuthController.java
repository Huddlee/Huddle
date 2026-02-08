package com.huddlee.backendspringboot.controllers;

import com.huddlee.backendspringboot.dtos.LoginRequest;
import com.huddlee.backendspringboot.dtos.RegisterRequest;
import com.huddlee.backendspringboot.models.User;
import com.huddlee.backendspringboot.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Value("${guest.user.password}")
    private String guestPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setDisplayName(user.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setRole("USER");

        user = userService.register(user);
        if (user == null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        return ResponseEntity.ok("User registered");
    }
    
    @PostMapping("/guest/register")
    public ResponseEntity<?> guestRegister() {
        String guestId = "guest_" + UUID.randomUUID();
        User user = new User();

        user.setUsername(guestId);
        user.setPassword(guestPassword);
        user.setDisplayName(user.getUsername());
        user.setRole("GUEST");

        userService.register(user);
        return ResponseEntity.ok(userService.guestLogin(guestId));
    }
}
