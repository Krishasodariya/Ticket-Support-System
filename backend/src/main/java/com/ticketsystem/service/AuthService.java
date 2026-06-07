package com.ticketsystem.service;

import com.ticketsystem.dto.request.LoginRequest;
import com.ticketsystem.dto.request.RegisterRequest;
import com.ticketsystem.dto.response.AuthResponse;
import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.UserRepository;
import com.ticketsystem.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    // Feature 32 – System-Aktivitätsprotokoll
    private final SystemAuditLogService systemAuditLogService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       SystemAuditLogService systemAuditLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.systemAuditLogService = systemAuditLogService;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);

            User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
            // Feature 32 – Login-Erfolg protokollieren
            systemAuditLogService.log(user.getUsername(), "LOGIN_SUCCESS",
                    "Erfolgreicher Login für Benutzer " + user.getUsername());

            return AuthResponse.builder()
                    .token(jwt)
                    .id(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .build();
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Feature 32 – fehlgeschlagenen Login protokollieren
            systemAuditLogService.log(request.getUsername(), "LOGIN_FAILURE",
                    "Fehlgeschlagener Login-Versuch für Benutzer " + request.getUsername());
            throw ex;
        }
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        // Feature 32 – neue Registrierung protokollieren
        systemAuditLogService.log(saved.getUsername(), "USER_CREATED",
                "Neuer Benutzer registriert: " + saved.getUsername(), saved.getId(), null);
        return saved;
    }
}
