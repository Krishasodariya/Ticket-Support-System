package com.ticketsystem.service;

import com.ticketsystem.dto.response.DashboardStatsResponse;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final TicketRepository ticketRepository;

    public DashboardService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public DashboardStatsResponse getStats() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> allTickets = ticketRepository.findAll();

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (TicketPriority priority : TicketPriority.values()) {
            byPriority.put(priority.name(), ticketRepository.countByPriority(priority));
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status.name(), ticketRepository.countByStatus(status));
        }

        Map<String, Long> byAgent = allTickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : "Unassigned",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> byCategory = allTickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getCategory() != null ? ticket.getCategory().getName() : "Keine Kategorie",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> resolvedByAgent = allTickets.stream()
                .filter(ticket -> ticket.getResolvedAt() != null)
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : "Unassigned",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        long overdue = allTickets.stream()
                .filter(ticket -> ticket.getDueAt() != null
                        && now.isAfter(ticket.getDueAt())
                        && ticket.getStatus() != TicketStatus.RESOLVED
                        && ticket.getStatus() != TicketStatus.CLOSED)
                .count();

        double averageResolutionHours = allTickets.stream()
                .filter(ticket -> ticket.getResolvedAt() != null && ticket.getCreatedAt() != null)
                .mapToLong(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt()).toHours())
                .average()
                .orElse(0.0);

        long createdToday = allTickets.stream()
                .filter(ticket -> ticket.getCreatedAt() != null && !ticket.getCreatedAt().isBefore(start) && ticket.getCreatedAt().isBefore(end))
                .count();

        return DashboardStatsResponse.builder()
                .totalTickets(ticketRepository.count())
                .openTickets(byStatus.getOrDefault(TicketStatus.OPEN.name(), 0L))
                .inProgressTickets(byStatus.getOrDefault(TicketStatus.IN_PROGRESS.name(), 0L))
                .waitingTickets(byStatus.getOrDefault(TicketStatus.WAITING.name(), 0L))
                .resolvedTickets(byStatus.getOrDefault(TicketStatus.RESOLVED.name(), 0L))
                .closedTickets(byStatus.getOrDefault(TicketStatus.CLOSED.name(), 0L))
                .resolvedToday(ticketRepository.countByResolvedAtBetween(start, end))
                .createdToday(createdToday)
                .overdueTickets(overdue)
                .escalatedTickets(allTickets.stream().filter(Ticket::isEscalated).count())
                .unassignedTickets(ticketRepository.countByAssignedToIsNull())
                .averageResolutionHours(Math.round(averageResolutionHours * 10.0) / 10.0)
                .ticketsByPriority(byPriority)
                .ticketsByStatus(byStatus)
                .ticketsByAgent(byAgent)
                .ticketsByCategory(byCategory)
                .resolvedByAgent(resolvedByAgent)
                .build();
    }
}
