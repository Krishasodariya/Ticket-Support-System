package com.ticketsystem.frontend.model;

import lombok.Data;

import java.time.LocalDateTime;

// KAT-107: Spiegelt backend TopCustomerStatResponse
@Data
public class TopCustomerStatFX {
    private String username;
    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private LocalDateTime lastActivity;
}
