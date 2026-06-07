package com.ticketsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Feature 32 – System-Aktivitätsprotokoll Response DTO */
@Data
public class SystemAuditLogResponse {
    private UUID id;
    private String actor;
    private String eventType;
    private String detail;
    private UUID targetUserId;
    private String ipAddress;
    private LocalDateTime timestamp;
}