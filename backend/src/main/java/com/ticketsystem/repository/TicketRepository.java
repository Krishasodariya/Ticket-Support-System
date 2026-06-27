package com.ticketsystem.repository;

import com.ticketsystem.model.Category;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByCreatedByOrderByCreatedAtDesc(User user);
    List<Ticket> findByAssignedToOrderByCreatedAtDesc(User user);
    List<Ticket> findAllByOrderByCreatedAtDesc();
    long countByStatus(TicketStatus status);
    long countByPriority(TicketPriority priority);
    long countByResolvedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByAssignedToIsNull();
    // KAT-121: Anzahl Tickets pro Kategorie
    long countByCategory(Category category);
}
