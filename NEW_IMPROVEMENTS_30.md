# 30 neue Verbesserungen im Ticket Support System

Diese Version enthält die 30 gewünschten Erweiterungen als funktionale Demo-Features oder als Backend-/Frontend-Grundlage.

1. Ticket-Kommentare für Kunden und Agenten.
2. Ticket-Timeline über Audit-Log/Historie.
3. SLA/Deadline pro Ticket abhängig von Priorität.
4. Überfällige Tickets werden erkannt und angezeigt.
5. Ticket-Anhänge als Dateiname/Pfad-Simulation.
6. Erweiterte Suchfunktion für Tickets.
7. Knowledge Base CRUD für Admins.
8. Antwort-Vorlagen für Agenten aus der Knowledge Base.
9. Admin-Dashboard mit zusätzlichen Kennzahlen.
10. Durchschnittliche Bearbeitungszeit im Dashboard.
11. Agent-Leistung über gelöste Tickets pro Agent im Backend.
12. Ticket-Eskalation für überfällige Tickets.
13. Simulierte E-Mail-Benachrichtigung als Notification.
14. UI-Grundlage für helle/dunkle Erweiterbarkeit bleibt im CSS vorbereitet.
15. Demo-Daten-Button für Präsentation.
16. Stärkere Passwort-Regeln mit Sonderzeichen.
17. Benutzerkonto aktivieren/deaktivieren.
18. Ticket schließen mit Lösungsgrund.
19. Kunden-Feedback mit Sternebewertung.
20. PDF/CSV Export mit Filteroptionen.
21. Suche nach Kunde, Agent, Kategorie und Ticketnummer.
22. Admin kann Prioritäts-Typen verwalten.
23. Admin kann Status-Typen verwalten.
24. Kommentare können intern/extern markiert werden.
25. Agent kann Tickets selbst übernehmen.
26. Kunde sieht backendseitig nur eigene Tickets.
27. Admin sieht backendseitig alle Tickets.
28. Automatische Ticketnummer wie TSS-2026-0001.
29. Dashboard zeigt heute erstellt und heute gelöst.
30. Dokumentationsdatei mit allen Verbesserungen für Abgabe/Präsentation.

## Neue Demo-Buttons

- Admin Dashboard: Demo-Daten erstellen
- Admin Dashboard: Überfällige Tickets eskalieren
- Admin Berichte: CSV/PDF Export mit Filter
- Admin Berichte: Knowledge Base Artikel erstellen/deaktivieren
- Admin Berichte: Status-/Prioritätsoptionen verwalten
- Ticket Detail: Ticket übernehmen, Anhang speichern, mit Lösungsgrund schließen, Feedback senden

## Wichtig

Da Flyway in dieser Demo-Version deaktiviert ist und `spring.jpa.hibernate.ddl-auto=update` gesetzt ist, erstellt/erweitert Hibernate die neuen Tabellen und Spalten automatisch beim Start.
