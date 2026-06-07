package com.ticketsystem.frontend.util;

import com.ticketsystem.model.enums.UserRole;

public class SessionManager {
    private static String token;
    private static String username;
    private static UserRole role;

    public static void login(String jwt, String user, String userRole) {
        token = jwt;
        username = user;
        role = UserRole.valueOf(userRole);
    }

    public static void clear() {
        token = null;
        username = null;
        role = null;
    }

    public static boolean isLoggedIn() {
        return token != null;
    }

    public static String getToken() {
        return token;
    }

    public static String getUsername() {
        return username;
    }

    public static UserRole getRole() {
        return role;
    }

    public static boolean hasRole(UserRole requiredRole) {
        return role == requiredRole;
    }
}
