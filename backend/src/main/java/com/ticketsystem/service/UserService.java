package com.ticketsystem.service;

import com.ticketsystem.dto.request.PasswordChangeRequest;
import com.ticketsystem.dto.request.ProfileUpdateRequest;
import com.ticketsystem.dto.response.UserResponse;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.mapper.UserMapper;
import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.UserRepository;
import com.ticketsystem.service.SystemAuditLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    // Feature 32 – System-Aktivitätsprotokoll
    private final SystemAuditLogService systemAuditLogService;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       SystemAuditLogService systemAuditLogService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.systemAuditLogService = systemAuditLogService;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toResponse).collect(Collectors.toList());
    }

    public List<UserResponse> getActiveAgents() {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.AGENT).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(findUserEntityById(id));
    }

    public UserResponse getCurrentUser(String username) {
        return userMapper.toResponse(findUserEntityByUsername(username));
    }

    public User findUserEntityById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User findUserEntityByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateUserActiveStatus(UUID id, boolean isActive) {
        User user = findUserEntityById(id);
        user.setActive(isActive);
        // Feature 32 – Benutzeraktivierung/-deaktivierung protokollieren
        systemAuditLogService.log("admin", isActive ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "Benutzer " + user.getUsername() + " wurde " + (isActive ? "aktiviert" : "deaktiviert"),
                user.getId(), null);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(UUID id, UserRole newRole) {
        User user = findUserEntityById(id);
        String oldRole = user.getRole().name();
        user.setRole(newRole);
        // Feature 32 – Rollenänderung protokollieren
        systemAuditLogService.log("admin", "ROLE_CHANGED",
                "Rolle von " + user.getUsername() + " geändert: " + oldRole + " → " + newRole.name(),
                user.getId(), null);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = findUserEntityByUsername(username);
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email is already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = findUserEntityByUsername(username);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
