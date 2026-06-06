package com.ticketsystem.service;

import com.ticketsystem.dto.response.SystemAuditLogResponse;
import com.ticketsystem.model.SystemAuditLog;
import com.ticketsystem.repository.SystemAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feature 32 – System-Aktivitätsprotokoll.
 * Wird von AuthService, UserService und ExportService aufgerufen,
 * um Login-Versuche, Rollenänderungen, Benutzeränderungen und Exporte zu erfassen.
 */
@Service
public class SystemAuditLogService {

    private final SystemAuditLogRepository repo;

    public SystemAuditLogService(SystemAuditLogRepository repo) {
        this.repo = repo;
    }

    /** Alle Einträge absteigend nach Zeitstempel – nur für ADMIN */
    public List<SystemAuditLogResponse> getAll() {
        return repo.findAllByOrderByTimestampDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Einträge nach Event-Typ filtern */
    public List<SystemAuditLogResponse> getByEventType(String eventType) {
        return repo.findByEventTypeOrderByTimestampDesc(eventType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Hilfsmethode zum Protokollieren – von anderen Services verwendet */
    public void log(String actor, String eventType, String detail,
                    UUID targetUserId, String ipAddress) {
        SystemAuditLog entry = SystemAuditLog.builder()
                .actor(actor)
                .eventType(eventType)
                .detail(detail)
                .targetUserId(targetUserId)
                .ipAddress(ipAddress)
                .build();
        repo.save(entry);
    }

    /** Kurzform ohne IP (intern) */
    public void log(String actor, String eventType, String detail) {
        log(actor, eventType, detail, null, null);
    }

    private SystemAuditLogResponse toResponse(SystemAuditLog e) {
        SystemAuditLogResponse r = new SystemAuditLogResponse();
        r.setId(e.getId());
        r.setActor(e.getActor());
        r.setEventType(e.getEventType());
        r.setDetail(e.getDetail());
        r.setTargetUserId(e.getTargetUserId());
        r.setIpAddress(e.getIpAddress());
        r.setTimestamp(e.getTimestamp());
        return r;
    }
}
