package com.ticketsystem.controller;

import com.ticketsystem.dto.response.DashboardStatsResponse;
import com.ticketsystem.service.DashboardService;
import com.ticketsystem.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {
    private final DashboardService dashboardService;
    private final ExportService exportService;

    public DashboardController(DashboardService dashboardService, ExportService exportService) {
        this.dashboardService = dashboardService;
        this.exportService = exportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping(value = "/export/tickets.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String priority,
                                            @RequestParam(required = false) String q) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.csv")
                .body(exportService.exportTicketsCsv(status, priority, q));
    }

    @GetMapping(value = "/export/tickets.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String priority,
                                            @RequestParam(required = false) String q) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.pdf")
                .body(exportService.exportTicketsPdf(status, priority, q));
    }

    // KAT-131: Audit-Log CSV/PDF-Export
    @GetMapping(value = "/export/audit-log.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportAuditLogCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.csv")
                .body(exportService.exportAuditLogCsv());
    }

    @GetMapping(value = "/export/audit-log.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportAuditLogPdf() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.pdf")
                .body(exportService.exportAuditLogPdf());
    }
}
