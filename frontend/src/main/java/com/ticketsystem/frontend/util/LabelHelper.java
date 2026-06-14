package com.ticketsystem.frontend.util;

// [Nzchupa | 2026-06-13] TSS-003/004: Hilfsmethoden für deutsche Status- und Prioritätsbezeichnungen
// Helper methods for translating ticket status and priority enum values to German labels
public class LabelHelper {

    public static String statusToGerman(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "OPEN"        -> "Offen";
            case "IN_PROGRESS" -> "In Bearbeitung";
            case "WAITING"     -> "Wartend";
            case "RESOLVED"    -> "Gelöst";
            case "CLOSED"      -> "Geschlossen";
            default            -> status;
        };
    }

    public static String priorityToGerman(String priority) {
        if (priority == null) return "-";
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> "Kritisch";
            case "HIGH"     -> "Hoch";
            case "MEDIUM"   -> "Mittel";
            case "LOW"      -> "Niedrig";
            default         -> priority;
        };
    }
}
