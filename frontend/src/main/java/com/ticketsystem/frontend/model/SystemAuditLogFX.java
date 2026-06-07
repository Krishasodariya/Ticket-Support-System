package com.ticketsystem.frontend.model;

import lombok.Data;

import java.time.LocalDateTime;

/** Feature 32 – System-Aktivitätsprotokoll */
@Data
public class SystemAuditLogFX {
    private String id;
    private String actor;
    private String eventType;
    private String detail;
    private String targetUserId;
    private String ipAddress;
    private LocalDateTime timestamp;
}

