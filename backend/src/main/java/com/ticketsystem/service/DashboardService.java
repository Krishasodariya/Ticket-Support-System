package com.ticketsystem.service;

import com.ticketsystem.dto.response.DashboardStatsResponse;
import com.ticketsystem.dto.response.TopCustomerStatResponse;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        // KAT-107 – Top-Kunden mit Detailinfos (offene/gelöste Tickets, letzte Aktivität), max 10
        Map<String, List<Ticket>> ticketsByCustomer = allTickets.stream()
                .filter(ticket -> ticket.getCreatedBy() != null)
                .collect(Collectors.groupingBy(ticket -> ticket.getCreatedBy().getUsername()));

        List<TopCustomerStatResponse> topCustomers = ticketsByCustomer.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    List<Ticket> customerTickets = entry.getValue();
                    long open = customerTickets.stream()
                            .filter(ticket -> ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED)
                            .count();
                    long resolved = customerTickets.stream()
                            .filter(ticket -> ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED)
                            .count();
                    LocalDateTime lastActivity = customerTickets.stream()
                            .map(ticket -> ticket.getUpdatedAt() != null ? ticket.getUpdatedAt() : ticket.getCreatedAt())
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    return TopCustomerStatResponse.builder()
                            .username(username)
                            .totalTickets(customerTickets.size())
                            .openTickets(open)
                            .resolvedTickets(resolved)
                            .lastActivity(lastActivity)
                            .build();
                })
                .sorted(Comparator.comparingLong(TopCustomerStatResponse::getTotalTickets).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // KAT-103 – Bewertungsverteilung (1-5 Sterne -> Anzahl Tickets)
        Map<String, Long> ratingDistribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            ratingDistribution.put(String.valueOf(rating), 0L);
        }
        allTickets.stream()
                .filter(ticket -> ticket.getCustomerRating() != null
                        && ticket.getCustomerRating() >= 1 && ticket.getCustomerRating() <= 5)
                .forEach(ticket -> ratingDistribution.merge(String.valueOf(ticket.getCustomerRating()), 1L, Long::sum));

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
                .topCustomers(topCustomers)
                .ratingDistribution(ratingDistribution)
                .build();
    }
}
