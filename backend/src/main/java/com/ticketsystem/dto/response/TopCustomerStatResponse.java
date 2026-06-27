package com.ticketsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// KAT-107: Top-Kunden-Liste mit Detailinfos statt nur Ticket-Anzahl
@Data
@Builder
public class TopCustomerStatResponse {
    private String username;
    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private LocalDateTime lastActivity;
}
