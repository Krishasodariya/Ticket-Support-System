package com.ticketsystem.service;

import com.ticketsystem.dto.request.AssignTicketRequest;
import com.ticketsystem.dto.request.CreateTicketRequest;
import com.ticketsystem.dto.request.FeedbackRequest;
import com.ticketsystem.dto.request.UpdateTicketRequest;
import com.ticketsystem.dto.response.TicketDetailResponse;
import com.ticketsystem.dto.response.TicketResponse;
import com.ticketsystem.exception.InvalidStatusTransitionException;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.exception.TicketNotFoundException;
import com.ticketsystem.mapper.TicketMapper;
import com.ticketsystem.model.AuditLog;
import com.ticketsystem.model.Category;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.AuditLogRepository;
import com.ticketsystem.repository.CategoryRepository;
import com.ticketsystem.repository.TicketRepository;
import com.ticketsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final TicketMapper ticketMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, CategoryRepository categoryRepository,
                         AuditLogRepository auditLogRepository, TicketMapper ticketMapper,
                         UserService userService, NotificationService notificationService,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketMapper = ticketMapper;
        this.userService = userService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public List<TicketResponse> getAllTickets(String username) {
        User user = userService.findUserEntityByUsername(username);
        List<Ticket> tickets;
        if (user.getRole() == UserRole.CUSTOMER) {
            tickets = ticketRepository.findByCreatedByOrderByCreatedAtDesc(user);
        } else {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        }
        return tickets.stream().map(ticketMapper::toResponse).collect(Collectors.toList());
    }

    public List<TicketResponse> searchTickets(String username, String query, TicketStatus status,
                                              TicketPriority priority, Long categoryId) {
        User user = userService.findUserEntityByUsername(username);
        String q = query == null ? "" : query.trim().toLowerCase();
        return visibleTicketsFor(user).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> categoryId == null || (t.getCategory() != null && categoryId.equals(t.getCategory().getId())))
                .filter(t -> q.isBlank()
                        || contains(t.getTitle(), q)
                        || contains(t.getDescription(), q)
                        || contains(t.getTicketNumber(), q)
                        || contains(t.getCategory() != null ? t.getCategory().getName() : null, q)
                        || contains(t.getCreatedBy() != null ? t.getCreatedBy().getUsername() : null, q)
                        || contains(t.getAssignedTo() != null ? t.getAssignedTo().getUsername() : null, q))
                .sorted(Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getMyAssignedTickets(String username) {
        User user = userService.findUserEntityByUsername(username);
        return ticketRepository.findByAssignedToOrderByCreatedAtDesc(user).stream()
                .map(ticketMapper::toResponse).collect(Collectors.toList());
    }

    public TicketDetailResponse getTicketById(UUID id, String username) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);
        ensureCanView(ticket, user);
        return ticketMapper.toDetailResponse(ticket);
    }

    @Transactional
    public TicketDetailResponse createTicket(CreateTicketRequest request, String username) {
        User user = userService.findUserEntityByUsername(username);
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        // Aufgabe 16: Priorität automatisch vorschlagen, falls nicht gesetzt
        TicketPriority priority = request.getPriority() != null
                ? request.getPriority()
                : suggestPriority(request.getTitle(), request.getDescription(), category);

        Ticket ticket = Ticket.builder()
                .ticketNumber(nextTicketNumber())
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .priority(priority)
                .category(category)
                .status(TicketStatus.OPEN)
                .createdBy(user)
                .dueAt(calculateDueAt(priority))
                .escalated(false)
                .critical(false)
                .attachmentName(clean(request.getAttachmentName()))
                .attachmentPath(clean(request.getAttachmentPath()))
                .build();

        ticket = ticketRepository.save(ticket);
        createAuditLog(ticket, user, "TICKET_CREATED", null, TicketStatus.OPEN.name());
        createAuditLog(ticket, user, "SLA_SET", null,
                ticket.getDueAt() != null ? ticket.getDueAt().toString() : "Keine SLA");
        if (StringUtils.hasText(ticket.getAttachmentName())) {
            createAuditLog(ticket, user, "ATTACHMENT_ADDED", null, ticket.getAttachmentName());
        }
        notificationService.notifyUser(user, ticket, "Ticket erstellt",
                "Dein Ticket '" + ticket.getTitle() + "' wurde erstellt. Nummer: " + ticket.getTicketNumber());
        notificationService.notifySimulatedEmail(user, ticket, "Ticket-Bestätigung",
                "Dein Ticket wurde im System registriert.");

        // Aufgabe 14 + 15 + 25: Automatisches Routing nach Kategorie & Workload-Balancing
        autoRouteTicket(ticket, category, user);

        return ticketMapper.toDetailResponse(ticket);
    }

    @Transactional
    public TicketDetailResponse updateTicket(UUID id, UpdateTicketRequest request, String username) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);
        ensureCanUpdate(user);

        if (StringUtils.hasText(request.getTitle()) && !request.getTitle().equals(ticket.getTitle())) {
            String oldTitle = ticket.getTitle();
            ticket.setTitle(request.getTitle().trim());
            createAuditLog(ticket, user, "TITLE_CHANGED", oldTitle, ticket.getTitle());
        }

        if (StringUtils.hasText(request.getDescription()) && !request.getDescription().equals(ticket.getDescription())) {
            ticket.setDescription(request.getDescription().trim());
            createAuditLog(ticket, user, "DESCRIPTION_CHANGED", "Beschreibung geändert", "Beschreibung aktualisiert");
        }

        if (request.getPriority() != null && ticket.getPriority() != request.getPriority()) {
            String oldPriority = ticket.getPriority().name();
            ticket.setPriority(request.getPriority());
            ticket.setDueAt(calculateDueAt(request.getPriority()));
            ticket.setEscalated(false);
            createAuditLog(ticket, user, "PRIORITY_CHANGED", oldPriority, request.getPriority().name());
            createAuditLog(ticket, user, "SLA_RECALCULATED", oldPriority,
                    ticket.getDueAt() != null ? ticket.getDueAt().toString() : "Keine SLA");
            notifyTicketPeople(ticket, user, "Ticket-Priorität geändert",
                    "Priorität von '" + ticket.getTitle() + "' wurde von " + oldPriority
                            + " zu " + request.getPriority().name() + " geändert.");
        }

        if (StringUtils.hasText(request.getCustomPriorityLabel())) {
            String old = ticket.getCustomPriorityLabel();
            ticket.setCustomPriorityLabel(request.getCustomPriorityLabel().trim());
            createAuditLog(ticket, user, "CUSTOM_PRIORITY_LABEL_CHANGED", old, ticket.getCustomPriorityLabel());
        }
        if (StringUtils.hasText(request.getCustomStatusLabel())) {
            String old = ticket.getCustomStatusLabel();
            ticket.setCustomStatusLabel(request.getCustomStatusLabel().trim());
            createAuditLog(ticket, user, "CUSTOM_STATUS_LABEL_CHANGED", old, ticket.getCustomStatusLabel());
        }

        if (request.getCategoryId() != null
                && (ticket.getCategory() == null || !request.getCategoryId().equals(ticket.getCategory().getId()))) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            String oldCategory = ticket.getCategory() != null ? ticket.getCategory().getName() : "Keine Kategorie";
            ticket.setCategory(category);
            createAuditLog(ticket, user, "CATEGORY_CHANGED", oldCategory, category.getName());
        }

        if (StringUtils.hasText(request.getSolutionReason())) {
            String oldReason = ticket.getSolutionReason();
            ticket.setSolutionReason(request.getSolutionReason().trim());
            createAuditLog(ticket, user, "SOLUTION_REASON_SET", oldReason, ticket.getSolutionReason());
        }

        if (request.getStatus() != null && ticket.getStatus() != request.getStatus()) {
            validateStatusTransition(ticket.getStatus(), request.getStatus());
            String oldStatus = ticket.getStatus().name();
            ticket.setStatus(request.getStatus());
            if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.CLOSED) {
                ticket.setResolvedAt(LocalDateTime.now());
                ticket.setEscalated(false);
                if (!StringUtils.hasText(ticket.getSolutionReason())) {
                    ticket.setSolutionReason(StringUtils.hasText(request.getSolutionReason())
                            ? request.getSolutionReason().trim()
                            : "Gelöst ohne ausführlichen Lösungsgrund");
                }
            } else if (oldStatus.equals(TicketStatus.RESOLVED.name()) || oldStatus.equals(TicketStatus.CLOSED.name())) {
                ticket.setResolvedAt(null);
            }
            createAuditLog(ticket, user, "STATUS_CHANGED", oldStatus, request.getStatus().name());
            notifyTicketPeople(ticket, user, "Ticket-Status geändert",
                    "Status von '" + ticket.getTitle() + "' wurde von " + oldStatus
                            + " zu " + request.getStatus().name() + " geändert.");
            notificationService.notifySimulatedEmail(ticket.getCreatedBy(), ticket,
                    "Ticket-Status geändert", "Der neue Status ist " + request.getStatus().name());
        }

        if (StringUtils.hasText(request.getAttachmentName())) {
            String oldAttachment = ticket.getAttachmentName();
            ticket.setAttachmentName(request.getAttachmentName().trim());
            ticket.setAttachmentPath(clean(request.getAttachmentPath()));
            createAuditLog(ticket, user, "ATTACHMENT_ADDED", oldAttachment, ticket.getAttachmentName());
        }

        if (request.getAssignedTo() != null) {
            assignTicketInternal(ticket, request.getAssignedTo(), user);
        }

        return ticketMapper.toDetailResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketDetailResponse assignTicket(UUID id, AssignTicketRequest request, String username) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);
        ensureCanUpdate(user);
        assignTicketInternal(ticket, request.getAgentId(), user);
        return ticketMapper.toDetailResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketDetailResponse takeTicket(UUID id, String username) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);
        if (user.getRole() != UserRole.AGENT && user.getRole() != UserRole.ADMIN) {
            throw new ResourceNotFoundException("Only agents or admins can take tickets");
        }
        assignTicketInternal(ticket, user.getId(), user);
        return ticketMapper.toDetailResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketDetailResponse addFeedback(UUID id, FeedbackRequest request, String username) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);
        if (!ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Only the ticket owner can leave feedback");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalArgumentException("Feedback can only be added after the ticket is resolved or closed");
        }
        ticket.setCustomerRating(request.getRating());
        ticket.setCustomerFeedback(clean(request.getFeedback()));
        createAuditLog(ticket, user, "CUSTOMER_FEEDBACK", null, request.getRating() + " Sterne");
        return ticketMapper.toDetailResponse(ticketRepository.save(ticket));
    }

    /**
     * Aufgabe 13 – Automatische Eskalation.
     * Überfällige Tickets werden eskaliert und Admin sowie zugewiesener Agent
     * werden benachrichtigt.
     */
    @Transactional
    public int escalateOverdueTickets(String username) {
        User requestingUser = userService.findUserEntityByUsername(username);
        if (requestingUser.getRole() != UserRole.ADMIN) {
            throw new ResourceNotFoundException("Only admins can escalate overdue tickets");
        }
        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        // Alle Admins und Senior-Agenten ermitteln (Admin-Rolle = Eskalations-Empfänger)
        List<User> admins = userRepository.findByRoleAndIsActiveTrue(UserRole.ADMIN);

        for (Ticket ticket : ticketRepository.findAll()) {
            if (!ticket.isEscalated()
                    && ticket.getDueAt() != null
                    && now.isAfter(ticket.getDueAt())
                    && ticket.getStatus() != TicketStatus.RESOLVED
                    && ticket.getStatus() != TicketStatus.CLOSED) {

                ticket.setEscalated(true);
                createAuditLog(ticket, requestingUser, "TICKET_ESCALATED", "Normal", "Überfällig / eskaliert");

                // Ersteller informieren
                notificationService.notifyUser(ticket.getCreatedBy(), ticket, "Ticket eskaliert",
                        "Dein Ticket ist überfällig und wurde eskaliert.");

                // Zugewiesenen Agenten informieren
                if (ticket.getAssignedTo() != null) {
                    notificationService.notifyUser(ticket.getAssignedTo(), ticket, "Ticket eskaliert",
                            "Ein zugewiesenes Ticket ist überfällig.");
                }

                // Alle Admins als Eskalations-Empfänger benachrichtigen
                for (User admin : admins) {
                    notificationService.notifyUser(admin, ticket, "Eskalation: Überfälliges Ticket",
                            "Ticket " + ticket.getTicketNumber() + " – '" + ticket.getTitle()
                                    + "' ist überfällig und wurde automatisch eskaliert.");
                }

                ticketRepository.save(ticket);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public void deleteTicket(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException("Ticket not found");
        }
        ticketRepository.deleteById(id);
    }

    // ── Aufgabe 39: Ticket als kritisch markieren ─────────────────────────────

    /**
     * Nur Admins können ein Ticket unabhängig von der normalen Priorität als kritisch kennzeichnen.
     */
    @Transactional
    public TicketDetailResponse markAsCritical(UUID id, boolean critical, String username) {
        User user = userService.findUserEntityByUsername(username);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResourceNotFoundException("Nur Admins können Tickets als kritisch markieren.");
        }
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        boolean old = ticket.isCritical();
        ticket.setCritical(critical);
        createAuditLog(ticket, user, "CRITICAL_FLAG_CHANGED",
                String.valueOf(old), String.valueOf(critical));
        if (critical) {
            notifyTicketPeople(ticket, user, "Ticket als kritisch markiert",
                    "Ticket '" + ticket.getTitle() + "' wurde vom Admin als kritisch eingestuft.");
        }
        return ticketMapper.toDetailResponse(ticketRepository.save(ticket));
    }

    // ── Interne Hilfsmethoden ─────────────────────────────────────────────────

    private void assignTicketInternal(Ticket ticket, UUID agentId, User changedBy) {
        User agent = userService.findUserEntityById(agentId);
        if ((agent.getRole() != UserRole.AGENT && agent.getRole() != UserRole.ADMIN) || !agent.isActive()) {
            throw new IllegalArgumentException("Ticket can only be assigned to an active agent/admin");
        }
        String oldAgent = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : "Unassigned";
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(agent.getId())) {
            return;
        }
        ticket.setAssignedTo(agent);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            String oldStatus = ticket.getStatus().name();
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            createAuditLog(ticket, changedBy, "STATUS_CHANGED", oldStatus, TicketStatus.IN_PROGRESS.name());
        }
        createAuditLog(ticket, changedBy, "ASSIGNED", oldAgent, agent.getUsername());
        notificationService.notifyUser(agent, ticket, "Ticket zugewiesen",
                "Dir wurde das Ticket '" + ticket.getTitle() + "' zugewiesen.");
        notificationService.notifyUser(ticket.getCreatedBy(), ticket, "Ticket zugewiesen",
                "Dein Ticket wurde an " + agent.getUsername() + " zugewiesen.");
        notificationService.notifySimulatedEmail(agent, ticket, "Neue Ticket-Zuweisung",
                "Bitte bearbeite " + ticket.getTicketNumber() + ".");
    }

    /**
     * Aufgabe 14 + 15 + 25: Automatisches Routing mit Kategorie-Filter,
     * Spezialisierungs-Abgleich und Workload-Balancing.
     *
     * Ablauf:
     * 1. Alle aktiven Agenten mit passender Spezialisierung (oder Generalisten) ermitteln.
     * 2. Aus dieser Kandidatenliste denjenigen wählen, der die wenigsten offenen Tickets hat.
     * 3. Ticket automatisch zuweisen und Routing im Audit-Log festhalten.
     */
    private void autoRouteTicket(Ticket ticket, Category category, User createdBy) {
        String categoryName = category != null ? category.getName() : null;

        // Kandidaten: spezialisierte Agenten (oder alle, falls keine Kategorie)
        List<User> candidates;
        if (StringUtils.hasText(categoryName)) {
            candidates = userService.findActiveAgentsMatchingCategory(categoryName);
        } else {
            candidates = userRepository.findByRoleAndIsActiveTrue(UserRole.AGENT);
        }

        if (candidates.isEmpty()) return;

        // Offene Tickets laden (für Workload-Berechnung)
        List<Ticket> openTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED)
                .collect(Collectors.toList());

        // Aufgabe 25: Agent mit wenigsten offenen Tickets bevorzugen
        User bestAgent = candidates.stream()
                .min(Comparator.comparingLong(a -> userService.countOpenTicketsForUser(a.getId(), openTickets)))
                .orElse(null);

        if (bestAgent == null) return;

        // Audit vor der Zuweisung
        createAuditLog(ticket, createdBy, "AUTO_ROUTED",
                "Kein Agent",
                bestAgent.getUsername() + (StringUtils.hasText(categoryName) ? " (Kategorie: " + categoryName + ")" : ""));

        assignTicketInternal(ticket, bestAgent.getId(), createdBy);
        ticketRepository.save(ticket);
    }

    /**
     * Aufgabe 16: Priorität automatisch vorschlagen.
     * Heuristik: Schlüsselwörter in Titel/Beschreibung und Kategorie-Name.
     */
    /**
     * Aufgabe 16: Priorität automatisch vorschlagen.
     * Erkennt Verneinungen ("nicht", "kein", "keine") und wertet diese
     * als Indikator für höhere Dringlichkeit.
     */
    private TicketPriority suggestPriority(String title, String description, Category category) {
        String combined = ((title == null ? "" : title) + " "
                + (description == null ? "" : description) + " "
                + (category != null ? category.getName() : "")).toLowerCase();

        // Verneinungs-Muster: "nicht X" oder "kein/keine X" erhöhen die Dringlichkeit
        boolean hasNegation = combined.matches(".*(\\bnicht\\b|\\bkein\\b|\\bkeine\\b|\\bkann nicht\\b|\\bgeht nicht\\b|\\bfunktioniert nicht\\b|\\bnicht mehr\\b|\\bnicht erreichbar\\b|\\bnicht verfügbar\\b|\\bnicht möglich\\b).*");

        // ── CRITICAL ────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "kritisch", "critical", "notfall", "emergency",
                "totalausfall", "komplett ausgefallen", "komplett down",
                "nicht erreichbar", "nicht verfügbar", "kein zugriff",
                "system down", "server down", "crash", "absturz",
                "datenverlust", "sicherheitslücke", "hack", "gehackt",
                "passwort vergessen", "gesperrt", "konto gesperrt",
                "produktion steht", "produktionsausfall", "ausfall")) {
            return TicketPriority.CRITICAL;
        }

        // Verneinung + Schlüsselwort → CRITICAL hochstufen
        if (hasNegation && containsAny(combined,
                "internet", "netzwerk", "vpn", "login", "einloggen",
                "starten", "öffnen", "laden", "verbinden", "zugreifen",
                "arbeiten", "speichern", "senden", "empfangen", "drucken")) {
            return TicketPriority.CRITICAL;
        }

        // ── HIGH ────────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "dringend", "urgent", "sofort", "immediately", "asap",
                "wichtig", "blockiert", "blockierung", "fehlermeldung",
                "fehler", "error", "exception", "bug", "absturz",
                "kaputt", "defekt", "beschädigt", "verloren",
                "funktioniert nicht", "geht nicht", "startet nicht",
                "lädt nicht", "öffnet nicht", "reagiert nicht",
                "kein internet", "kein netzwerk", "kein zugang",
                "kein ton", "kein bild", "kein signal",
                "zugriff verweigert", "permission denied",
                "deadline", "frist", "zeitkritisch")) {
            return TicketPriority.HIGH;
        }

        // Verneinung allgemein → HIGH
        if (hasNegation) {
            return TicketPriority.HIGH;
        }

        // ── MEDIUM ──────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "normal", "mittel", "medium",
                "langsam", "verzögert", "hängt", "friert ein", "laggt",
                "drucker", "scanner", "kamera", "mikrofon", "headset",
                "installation", "installieren", "update", "aktualisierung",
                "einrichten", "konfigurieren", "einstellen", "anpassen",
                "netzwerk", "wlan", "wifi", "vpn", "verbindung",
                "passwort ändern", "email einrichten", "konto einrichten",
                "software", "programm", "anwendung", "app",
                "zugriff", "berechtigung", "freigabe",
                "backup", "wiederherstellung", "restore",
                "performance", "geschwindigkeit", "speicher")) {
            return TicketPriority.MEDIUM;
        }

        // ── LOW (Fallback) ──────────────────────────────────────────────────────
        // Explizite Low-Indikatoren
        if (containsAny(combined,
                "frage", "anfrage", "info", "information", "auskunft",
                "wunsch", "verbesserung", "vorschlag", "idee", "feedback",
                "allgemein", "wann", "wie", "hilfe", "erklärung",
                "dokumentation", "anleitung", "schulung", "training",
                "bestellung", "lieferung", "rechnung", "abrechnung")) {
            return TicketPriority.LOW;
        }

        return TicketPriority.LOW;
    }

    /**
     * Hilfsmethode: prüft ob der Text eines der Schlüsselwörter enthält.
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private List<Ticket> visibleTicketsFor(User user) {
        if (user.getRole() == UserRole.CUSTOMER) {
            return ticketRepository.findByCreatedByOrderByCreatedAtDesc(user);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private String nextTicketNumber() {
        long next = ticketRepository.count() + 1;
        return "TSS-" + Year.now().getValue() + "-" + String.format("%04d", next);
    }

    private LocalDateTime calculateDueAt(TicketPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        if (priority == null) return now.plusDays(7);
        return switch (priority) {
            case CRITICAL -> now.plusHours(8);
            case HIGH     -> now.plusHours(24);
            case MEDIUM   -> now.plusDays(3);
            case LOW      -> now.plusDays(7);
        };
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private void ensureCanView(Ticket ticket, User user) {
        if (user.getRole() == UserRole.CUSTOMER && !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Not authorized to view this ticket");
        }
    }

    private void ensureCanUpdate(User user) {
        if (user.getRole() == UserRole.CUSTOMER) {
            throw new ResourceNotFoundException("Customers are not allowed to update ticket status, priority or assignment");
        }
    }

    private void notifyTicketPeople(Ticket ticket, User changedBy, String title, String message) {
        if (!ticket.getCreatedBy().getId().equals(changedBy.getId())) {
            notificationService.notifyUser(ticket.getCreatedBy(), ticket, title, message);
        }
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().getId().equals(changedBy.getId())) {
            notificationService.notifyUser(ticket.getAssignedTo(), ticket, title, message);
        }
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus next) {
        if (current == null || next == null) {
            throw new InvalidStatusTransitionException("Invalid ticket status");
        }
    }

    private void createAuditLog(Ticket ticket, User user, String type, String oldVal, String newVal) {
        AuditLog log = AuditLog.builder()
                .ticket(ticket)
                .changedBy(user)
                .changeType(type)
                .oldValue(oldVal)
                .newValue(newVal)
                .build();
        auditLogRepository.save(log);
    }
}