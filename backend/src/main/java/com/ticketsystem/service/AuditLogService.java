package com.ticketsystem.service;

import com.ticketsystem.dto.response.AuditLogResponse;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.model.AuditLog;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.repository.AuditLogRepository;
import com.ticketsystem.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final TicketRepository ticketRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, TicketRepository ticketRepository) {
        this.auditLogRepository = auditLogRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponse> getLogsForTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return auditLogRepository.findByTicketOrderByTimestampDesc(ticket).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setTicketId(log.getTicket().getId());
        response.setTicketTitle(log.getTicket().getTitle());
        response.setChangedBy(log.getChangedBy().getUsername());
        response.setChangeType(log.getChangeType());
        response.setOldValue(log.getOldValue());
        response.setNewValue(log.getNewValue());
        response.setTimestamp(log.getTimestamp());
        return response;
    }
}
