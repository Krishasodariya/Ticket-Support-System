package com.ticketsystem.frontend.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationFX {
    private String id;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private String ticketId;
}
