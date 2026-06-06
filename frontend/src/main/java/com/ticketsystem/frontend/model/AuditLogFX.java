package com.ticketsystem.frontend.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogFX {
    private String id;
    private String ticketId;
    private String ticketTitle;
    private String changedBy;
    private String changeType;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
}
