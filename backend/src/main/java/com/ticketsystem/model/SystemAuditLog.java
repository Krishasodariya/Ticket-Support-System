package com.ticketsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feature 32 – System-Aktivitätsprotokoll für Admin.
 * Speichert systemweite Ereignisse (Login, Rollenänderung, Benutzeränderung, Export)
 * unabhängig von einzelnen Tickets.
 */
@Entity
@Table(name = "system_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Wer hat die Aktion ausgelöst (Username, z. B. "admin" oder "anonymous") */
    @Column(name = "actor", nullable = false)
    private String actor;

    /**
     * Art des Ereignisses:
     *  LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT,
     *  ROLE_CHANGED, USER_ACTIVATED, USER_DEACTIVATED,
     *  USER_CREATED, USER_DELETED,
     *  EXPORT_CSV, EXPORT_PDF
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Freitext-Detail (z. B. "Rolle geändert von AGENT zu ADMIN") */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** Optional: betroffene Benutzer-ID */
    @Column(name = "target_user_id")
    private UUID targetUserId;

    /** IP-Adresse des Clients (optional) */
    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
