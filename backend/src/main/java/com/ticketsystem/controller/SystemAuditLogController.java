package com.ticketsystem.controller;

import com.ticketsystem.dto.response.SystemAuditLogResponse;
import com.ticketsystem.service.SystemAuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feature 32 – System-Aktivitätsprotokoll für Admin.
 * GET /api/system-audit-logs          → alle Einträge
 * GET /api/system-audit-logs?type=X   → nach Event-Typ filtern
 */
@RestController
@RequestMapping("/api/system-audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class SystemAuditLogController {

    private final SystemAuditLogService service;

    public SystemAuditLogController(SystemAuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SystemAuditLogResponse>> getLogs(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(service.getByEventType(type.toUpperCase()));
        }
        return ResponseEntity.ok(service.getAll());
    }
}
