package com.ticketsystem.frontend.service;

import com.ticketsystem.dto.request.ProfileUpdateRequest;
import com.ticketsystem.frontend.model.UserFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserApiService {
    public List<UserFX> getAllUsers() throws Exception {
        UserFX[] arr = ApiClient.get("/users", UserFX[].class);
        return Arrays.asList(arr);
    }

    public UserFX getCurrentUser() throws Exception {
        return ApiClient.get("/users/me", UserFX.class);
    }

    public List<UserFX> getActiveAgents() throws Exception {
        UserFX[] arr = ApiClient.get("/users/agents", UserFX[].class);
        return Arrays.asList(arr);
    }

    public UserFX toggleActive(String id, boolean isActive) throws Exception {
        return ApiClient.patch("/users/" + id + "/active", Map.of("isActive", isActive), UserFX.class);
    }

    public UserFX updateRole(String id, String role) throws Exception {
        return ApiClient.patch("/users/" + id + "/role", Map.of("role", role), UserFX.class);
    }

    /** Aufgabe 15 - Spezialisierung eines Agenten setzen */
    public UserFX updateSpecialization(String id, String specialization) throws Exception {
        return ApiClient.patch("/users/" + id + "/specialization",
                Map.of("specialization", specialization == null ? "" : specialization), UserFX.class);
    }

    public UserFX updateProfile(ProfileUpdateRequest request) throws Exception {
        return ApiClient.put("/users/me/profile", request, UserFX.class);
    }

    public void changePassword(String currentPassword, String newPassword) throws Exception {
        ApiClient.put("/users/me/password", Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword
        ), Void.class);
    }
}