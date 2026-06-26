package com.ticketsystem.frontend.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Data
public class NotificationFX {
    private String id;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private String ticketId;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // [Nzchupa | 2026-06-26] KAT-87: Lesbarer Zeitstempel — relativ für aktuelle, Datum für ältere Einträge
    // Human-readable timestamp — relative wording for recent notifications, absolute date for older ones
    public String getFormattedTime() {
        if (createdAt == null) return "-";
        long minutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        if (minutes < 1) return "Gerade jetzt";
        if (minutes < 60) return "vor " + minutes + " Min.";
        long hours = minutes / 60;
        if (hours < 24) return "vor " + hours + " Std.";
        long days = hours / 24;
        if (days < 7) return "vor " + days + " Tag" + (days == 1 ? "" : "en");
        return createdAt.format(DATE_FORMAT);
    }

    // [Nzchupa | 2026-06-26] KAT-86: Gruppen-Schlüssel für Section-Header (Heute/Gestern/Älter)
    // Bucket key used to group notifications into Heute / Gestern / Älter sections
    public String getDateBucket() {
        if (createdAt == null) return "Älter";
        LocalDateTime now = LocalDateTime.now();
        if (createdAt.toLocalDate().isEqual(now.toLocalDate())) return "Heute";
        if (createdAt.toLocalDate().isEqual(now.toLocalDate().minusDays(1))) return "Gestern";
        return "Älter";
    }

    // [Nzchupa | 2026-06-26] KAT-90: Icon je Benachrichtigungsart, abgeleitet vom Titel
    // Es gibt im Backend kein eigenes NotificationType-Enum — daher Ableitung über Titel-Schlüsselwörter,
    // analog zur bereits bestehenden Logik in NotificationPopup/NotificationsController.
    // Icon per notification kind, derived from the title keyword since there is no NotificationType enum
    public String getIcon() {
        String t = title == null ? "" : title.toLowerCase();
        if (t.contains("eskaliert") || t.contains("eskalation")) return "⚠";
        if (t.contains("zugewiesen"))                            return "🔔";
        if (t.contains("kommentar"))                             return "💬";
        if (t.contains("email"))                                 return "📧";
        if (t.contains("ticket"))                                return "🎫";
        return "ℹ";
    }

    // Begleitfarbe zum Icon — für den linken Akzentstreifen der Karte
    // Accent color matching the icon — used for the card's left border
    public String getColor() {
        String t = title == null ? "" : title.toLowerCase();
        if (t.contains("eskaliert") || t.contains("eskalation")) return "#EF4444";
        if (t.contains("zugewiesen"))                            return "#0EA5E9";
        if (t.contains("kommentar"))                             return "#22C55E";
        if (t.contains("email"))                                 return "#A855F7";
        if (t.contains("ticket"))                                return "#0EA5E9";
        return "#64748B";
    }
}
