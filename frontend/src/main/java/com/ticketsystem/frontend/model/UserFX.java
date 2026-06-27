package com.ticketsystem.frontend.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserFX {
    private String id;
    private String username;
    private String email;
    private String role;
    private boolean active;
    private String profilePicture;
    private LocalDate birthDate;
    /** Aufgabe 15 - Spezialisierung des Agenten (z.B. "Software,Netzwerk") */
    private String specialization;
    // KAT-116: Zeitpunkt des letzten Logins
    private LocalDateTime lastLogin;
}