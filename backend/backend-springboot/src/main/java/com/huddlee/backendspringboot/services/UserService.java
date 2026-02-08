package com.huddlee.backendspringboot.services;

import com.huddlee.backendspringboot.dtos.LoginRequest;
import com.huddlee.backendspringboot.models.User;
import com.huddlee.backendspringboot.repos.UserRepo;
import com.huddlee.backendspringboot.security.jwt.JwtAuthResponse;
import com.huddlee.backendspringboot.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    @Value("${guest.user.password}")
    private String guestPassword;
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;
    private final AuthenticationManager authManager;
    private final JwtUtils  jwtUtils;


    public JwtAuthResponse login(LoginRequest loginRequest) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDetailImpl userDetail = (UserDetailImpl) auth.getPrincipal();
        String jwtToken = jwtUtils.generateToken(userDetail);

        return new JwtAuthResponse(jwtToken);
    }

    public JwtAuthResponse guestLogin(String username) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username, guestPassword
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        UserDetailImpl userDetail = (UserDetailImpl) auth.getPrincipal();
        String jwtToken = jwtUtils.generateToken(userDetail);
        return new JwtAuthResponse(jwtToken);
    }


    public User register(User user) {
        // Check for unique username
        if (userRepo.existsUserByUsername(user.getUsername())){
            return null;
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public User registerGuest(User user) {
        user.setPassword(passwordEncoder.encode(guestPassword));
        return userRepo.save(user);
    }
}
