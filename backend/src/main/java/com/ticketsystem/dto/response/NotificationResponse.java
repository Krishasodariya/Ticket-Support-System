package com.ticketsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private UUID ticketId;
}
