package com.ticketsystem.model;

import com.ticketsystem.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "profile_picture", columnDefinition = "TEXT")
    private String profilePicture;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * Aufgabe 15 – Agent-Spezialisierung.
     * Komma-separierte Kategorie-Namen (z. B. "Software,Netzwerk").
     * Null / leer = kein Fachbereich, Agent kann alle Tickets empfangen.
     */
    @Column(name = "specialization")
    private String specialization;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}