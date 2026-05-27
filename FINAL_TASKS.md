# Finale Aufgaben und umgesetzte Projektbereiche

| Person | Finale Aufgabe | Umgesetzte Dateien / Bereiche |
|---|---|---|
| Person 1 | Audit Log + Ticket-Historie + Ticket-Zuweisung an Agenten | Backend: `AuditLogController`, `AuditLogService`, `AssignTicketRequest`, erweiterter `TicketService`; Frontend: Ticket-Detailansicht mit Historie und Agent-Zuweisung |
| Person 2 | Benachrichtigungen | Backend: `Notification`, `NotificationRepository`, `NotificationService`, `NotificationController`, Migration `V5__create_notifications.sql`; Frontend: Notification-Zähler im Admin-Topbar |
| Person 3 | Sicherheit + Rollenrechte + Login/Register-Validierung | Backend: stärkere `RegisterRequest`-Validierung, Rollenrechte mit `@PreAuthorize`, Passwortwechsel-Endpunkt; Frontend: Register-Validierung mit Fehlermarkierung |
| Person 4 | Profilseite + Passwort ändern | Backend: `PasswordChangeRequest`, `/api/users/me/password`; Frontend: Profilseite mit persönlichen Daten und Passwort-Änderung |
| Person 5 | Admin-Statistik/Dashboard + PDF/CSV Export | Backend: `DashboardController`, `DashboardService`, `ExportService`; Frontend: Dashboard-Livewerte, Prioritätsbalken und Export-Buttons |
| Person 6 | UI-Design + Hover Effects + Register-Seite mit Fonts/Farben | Frontend: `styles.css`, neue `RegisterView.fxml`, Hover-Effekte für Buttons, Karten und Notification-Pill |
