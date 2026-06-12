package com.ticketsystem.controller;

import com.ticketsystem.dto.request.PasswordChangeRequest;
import com.ticketsystem.dto.request.ProfileUpdateRequest;
import com.ticketsystem.dto.response.UserResponse;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<UserResponse>> getActiveAgents() {
        return ResponseEntity.ok(userService.getActiveAgents());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateActiveStatus(@PathVariable UUID id,
                                                           @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(userService.updateUserActiveStatus(id, body.get("isActive")));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(@PathVariable UUID id,
                                                   @RequestBody Map<String, String> body) {
        UserRole newRole = UserRole.valueOf(body.get("role"));
        return ResponseEntity.ok(userService.updateUserRole(id, newRole));
    }

    /**
     * Aufgabe 15 - Spezialisierung eines Agenten setzen (nur Admin).
     * Body: { "specialization": "Software,Netzwerk" } oder leer fuer Generalist.
     */
    @PatchMapping("/{id}/specialization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateSpecialization(@PathVariable UUID id,
                                                             @RequestBody Map<String, String> body) {
        String spec = body.getOrDefault("specialization", "");
        return ResponseEntity.ok(userService.updateSpecialization(id, spec));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                               Authentication authentication) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}