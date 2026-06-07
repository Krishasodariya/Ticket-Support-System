package com.ticketsystem.controller;

import com.ticketsystem.dto.request.AssignTicketRequest;
import com.ticketsystem.dto.request.CreateTicketRequest;
import com.ticketsystem.dto.request.FeedbackRequest;
import com.ticketsystem.dto.request.UpdateTicketRequest;
import com.ticketsystem.dto.response.TicketDetailResponse;
import com.ticketsystem.dto.response.TicketResponse;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getAllTickets(authentication.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponse>> searchTickets(@RequestParam(required = false) String q,
                                                              @RequestParam(required = false) TicketStatus status,
                                                              @RequestParam(required = false) TicketPriority priority,
                                                              @RequestParam(required = false) Long categoryId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(ticketService.searchTickets(authentication.getName(), q, status, priority, categoryId));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<List<TicketResponse>> getAssignedTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getMyAssignedTickets(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDetailResponse> getTicketById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicketById(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> createTicket(@Valid @RequestBody CreateTicketRequest request, Authentication authentication) {
        return ResponseEntity.ok(ticketService.createTicket(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<TicketDetailResponse> updateTicket(@PathVariable UUID id, @RequestBody UpdateTicketRequest request, Authentication authentication) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<TicketDetailResponse> assignTicket(@PathVariable UUID id,
                                                             @Valid @RequestBody AssignTicketRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(ticketService.assignTicket(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/take")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<TicketDetailResponse> takeTicket(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.takeTicket(id, authentication.getName()));
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketDetailResponse> addFeedback(@PathVariable UUID id,
                                                            @Valid @RequestBody FeedbackRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(ticketService.addFeedback(id, request, authentication.getName()));
    }

    @PostMapping("/escalate-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> escalateOverdue(Authentication authentication) {
        return ResponseEntity.ok(Map.of("escalated", ticketService.escalateOverdueTickets(authentication.getName())));
    }

    // ── Feature 17: Ähnliche Tickets ──────────────────────────────────────────
    @GetMapping("/search/similar")
    public ResponseEntity<List<TicketResponse>> findSimilar(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(ticketService.findSimilarTickets(title, description));
    }

    // ── Feature 18: Duplikat-Erkennung ────────────────────────────────────────
    @GetMapping("/search/duplicates")
    public ResponseEntity<List<TicketResponse>> findDuplicates(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(ticketService.findDuplicates(title, description));
    }

    // ── Feature 38: Ticket wiedereröffnen ─────────────────────────────────────
    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketDetailResponse> reopenTicket(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
