package com.chat.backend.service;

import com.chat.backend.dto.AuthResponse;
import com.chat.backend.dto.LoginRequest;
import com.chat.backend.dto.RegisterRequest;
import com.chat.backend.model.User;
import com.chat.backend.model.UserSession;
import com.chat.backend.repository.UserRepository;
import com.chat.backend.repository.UserSessionRepository;
import com.chat.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserSessionRepository userSessionRepository;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenHash(token);
        userSessionRepository.save(session);

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenHash(token);
        userSessionRepository.save(session);

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }
}