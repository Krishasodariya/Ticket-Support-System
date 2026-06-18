package com.ticketsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long waitingTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long resolvedToday;
    private long createdToday;
    private long overdueTickets;
    private long escalatedTickets;
    private long unassignedTickets;
    private double averageResolutionHours;
    private Map<String, Long> ticketsByPriority;
    private Map<String, Long> ticketsByStatus;
    private Map<String, Long> ticketsByAgent;
    private Map<String, Long> ticketsByCategory;
    private Map<String, Long> resolvedByAgent;
    // Aufgabe 3 – Top-Kunden nach Ticket-Anzahl
    private Map<String, Long> topCustomersByTickets;
}

