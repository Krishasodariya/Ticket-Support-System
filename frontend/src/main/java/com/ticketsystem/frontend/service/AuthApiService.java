package com.ticketsystem.frontend.service;

import com.ticketsystem.dto.request.LoginRequest;
import com.ticketsystem.dto.request.RegisterRequest;
import com.ticketsystem.dto.response.AuthResponse;
import com.ticketsystem.frontend.util.ApiClient;

public class AuthApiService {
    public AuthResponse login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return ApiClient.post("/auth/login", req, AuthResponse.class);
    }

    public void register(String username, String email, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        ApiClient.post("/auth/register", req, Void.class);
    }
}
