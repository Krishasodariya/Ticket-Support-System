package com.ticketsystem.service;

import com.ticketsystem.model.AuditLog;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.repository.AuditLogRepository;
import com.ticketsystem.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {
    private final TicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ExportService(TicketRepository ticketRepository, AuditLogRepository auditLogRepository) {
        this.ticketRepository = ticketRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // KAT-131: Audit-Log als CSV exportieren
    public byte[] exportAuditLogCsv() {
        StringBuilder sb = new StringBuilder("﻿Ticket;Geändert von;Aktion;Alter Wert;Neuer Wert;Zeitstempel\r\n");
        for (AuditLog log : auditLogRepository.findAllByOrderByTimestampDesc()) {
            sb.append(csv(log.getTicket() != null ? nvl(log.getTicket().getTitle()) : "")).append(';')
              .append(csv(log.getChangedBy() != null ? log.getChangedBy().getUsername() : "")).append(';')
              .append(csv(nvl(log.getChangeType()))).append(';')
              .append(csv(nvl(log.getOldValue()))).append(';')
              .append(csv(nvl(log.getNewValue()))).append(';')
              .append(csv(log.getTimestamp() != null ? log.getTimestamp().format(FMT) : ""))
              .append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // KAT-131: Audit-Log als PDF exportieren
    public byte[] exportAuditLogPdf() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        List<String> lines = new ArrayList<>();
        lines.add("Ticket Support System - Audit-Log Export");
        lines.add("Einträge: " + logs.size());
        lines.add("");
        if (logs.isEmpty()) {
            lines.add("Keine Audit-Log-Einträge vorhanden.");
        } else {
            int index = 1;
            for (AuditLog log : logs.stream().limit(35).toList()) {
                lines.add(index++ + ". " + (log.getTicket() != null ? safe(log.getTicket().getTitle()) : "-")
                        + " | " + log.getChangeType()
                        + " | " + (log.getChangedBy() != null ? log.getChangedBy().getUsername() : "-")
                        + " | " + (log.getTimestamp() != null ? log.getTimestamp().format(FMT) : "-"));
            }
        }
        return buildSimplePdf(lines);
    }

    public byte[] exportTicketsCsv() {
        return exportTicketsCsv(null, null, null);
    }

    public byte[] exportTicketsCsv(String status, String priority, String query) {
        StringBuilder sb = new StringBuilder("\uFEFFTicket Number;ID;Title;Status;Priority;Category;Created By;Assigned To;SLA Due;Overdue;Escalated;Rating;Created At;Solution Reason\r\n");
        for (Ticket t : filterTickets(status, priority, query)) {
            sb.append(csv(nvl(t.getTicketNumber()))).append(';')
              .append(csv(t.getId().toString())).append(';')
              .append(csv(t.getTitle())).append(';')
              .append(csv(t.getStatus() != null ? t.getStatus().name() : "")).append(';')
              .append(csv(t.getPriority() != null ? t.getPriority().name() : "")).append(';')
              .append(csv(t.getCategory() != null ? t.getCategory().getName() : "")).append(';')
              .append(csv(t.getCreatedBy() != null ? t.getCreatedBy().getUsername() : "")).append(';')
              .append(csv(t.getAssignedTo() != null ? t.getAssignedTo().getUsername() : "Unassigned")).append(';')
              .append(csv(t.getDueAt() != null ? t.getDueAt().format(FMT) : "")).append(';')
              .append(csv(isOverdue(t) ? "YES" : "NO")).append(';')
              .append(csv(t.isEscalated() ? "YES" : "NO")).append(';')
              .append(csv(t.getCustomerRating() != null ? String.valueOf(t.getCustomerRating()) : "")).append(';')
              .append(csv(t.getCreatedAt() != null ? t.getCreatedAt().format(FMT) : "")).append(';')
              .append(csv(nvl(t.getSolutionReason())))
              .append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportTicketsPdf() {
        return exportTicketsPdf(null, null, null);
    }

    public byte[] exportTicketsPdf(String status, String priority, String query) {
        List<Ticket> tickets = filterTickets(status, priority, query);
        List<String> lines = new ArrayList<>();
        lines.add("Ticket Support System - Ticket Export");
        lines.add("Total Tickets: " + tickets.size());
        lines.add("Filter: Status=" + nvl(status) + " Priority=" + nvl(priority) + " Suche=" + nvl(query));
        lines.add("");
        if (tickets.isEmpty()) {
            lines.add("Keine Tickets vorhanden.");
        } else {
            int index = 1;
            for (Ticket ticket : tickets.stream().limit(35).toList()) {
                lines.add(index++ + ". " + nvl(ticket.getTicketNumber()) + " | " + safe(ticket.getTitle())
                        + " | " + ticket.getStatus()
                        + " | " + ticket.getPriority()
                        + " | SLA: " + (ticket.getDueAt() != null ? ticket.getDueAt().format(FMT) : "-")
                        + " | Agent: " + (ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : "Unassigned"));
            }
        }
        return buildSimplePdf(lines);
    }

    private List<Ticket> filterTickets(String status, String priority, String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(ticket -> status == null || status.isBlank() || status.equalsIgnoreCase(ticket.getStatus().name()))
                .filter(ticket -> priority == null || priority.isBlank() || priority.equalsIgnoreCase(ticket.getPriority().name()))
                .filter(ticket -> q.isBlank()
                        || contains(ticket.getTitle(), q)
                        || contains(ticket.getDescription(), q)
                        || contains(ticket.getTicketNumber(), q)
                        || contains(ticket.getCategory() != null ? ticket.getCategory().getName() : null, q)
                        || contains(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getUsername() : null, q)
                        || contains(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : null, q))
                .toList();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private boolean isOverdue(Ticket t) {
        return t.getDueAt() != null
                && java.time.LocalDateTime.now().isAfter(t.getDueAt())
                && t.getStatus() != TicketStatus.RESOLVED
                && t.getStatus() != TicketStatus.CLOSED;
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private byte[] buildSimplePdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 11 Tf\n14 TL\n50 790 Td\n");
        for (String line : lines) {
            content.append("(").append(escapePdf(line)).append(") Tj\nT*\n");
        }
        content.append("ET\n");

        byte[] streamBytes = content.toString().getBytes(StandardCharsets.UTF_8);
        String[] objects = new String[] {
                "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
                "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
                "5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n" + content + "endstream\nendobj\n"
        };

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
            pdf.append(objects[i]);
        }
        int xref = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i <= objects.length; i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        pdf.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
           .append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapePdf(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
