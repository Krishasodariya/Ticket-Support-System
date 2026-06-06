package com.ticketsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditLogResponse {
    private UUID id;
    private UUID ticketId;
    private String ticketTitle;
    private String changedBy;
    private String changeType;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
}
