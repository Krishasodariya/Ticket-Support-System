package com.ticketsystem.service;

import com.ticketsystem.model.*;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class DemoDataService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final AuditLogRepository auditLogRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataService(UserRepository userRepository, CategoryRepository categoryRepository,
                           TicketRepository ticketRepository, CommentRepository commentRepository,
                           AuditLogRepository auditLogRepository, KnowledgeBaseRepository knowledgeBaseRepository,
                           NotificationRepository notificationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.auditLogRepository = auditLogRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generate() {
        User admin = ensureUser("admin", "admin@test.com", UserRole.ADMIN, "admin123");
        User agent = ensureUser("agent", "agent@test.com", UserRole.AGENT, "agent123");
        User agent2 = ensureUser("agent2", "agent2@test.com", UserRole.AGENT, "agent123");
        User customer = ensureUser("customer", "customer@test.com", UserRole.CUSTOMER, "customer123");

        Category software = ensureCategory("Software", "Software, Login und App-Probleme");
        Category hardware = ensureCategory("Hardware", "Laptop, Drucker und Geräteprobleme");
        Category network = ensureCategory("Netzwerk", "WLAN, VPN und Internet");

        ensureKnowledgeBase("Passwort zurücksetzen", "Software", "Passwort-Reset-Link schicken und Benutzer auffordern, ein starkes Passwort zu setzen.", "password, login, reset", "Bitte nutzen Sie die Passwort-zurücksetzen-Funktion und wählen Sie ein Passwort mit mindestens 8 Zeichen, Zahl und Sonderzeichen.");
        ensureKnowledgeBase("Drucker offline", "Hardware", "Drucker neu starten, Kabel prüfen, Treiber aktualisieren.", "printer, offline, hardware", "Bitte starten Sie den Drucker neu und prüfen Sie, ob er im gleichen Netzwerk verbunden ist.");
        ensureKnowledgeBase("VPN verbindet nicht", "Netzwerk", "VPN-Profil prüfen und Netzwerkverbindung testen.", "vpn, network", "Bitte prüfen Sie Ihre Internetverbindung und melden Sie sich erneut im VPN-Client an.");

        if (ticketRepository.count() < 5) {
            createDemoTicket("Laptop startet nicht", "Der Laptop bleibt beim Startbildschirm hängen.", TicketPriority.HIGH, hardware, customer, agent, TicketStatus.IN_PROGRESS, false);
            createDemoTicket("VPN Problem", "VPN trennt sich alle 5 Minuten.", TicketPriority.CRITICAL, network, customer, agent2, TicketStatus.OPEN, true);
            createDemoTicket("Software Installation", "Benötige Installation von Visual Studio Code.", TicketPriority.MEDIUM, software, customer, null, TicketStatus.OPEN, false);
            createDemoTicket("Passwort vergessen", "Ich kann mich nicht mehr einloggen.", TicketPriority.LOW, software, customer, agent, TicketStatus.RESOLVED, false);
        }
        return "Demo-Daten wurden erstellt: Benutzer, Kategorien, Tickets, Kommentare, Audit-Logs, Knowledge Base und Benachrichtigungen.";
    }

    private User ensureUser(String username, String email, UserRole role, String password) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .build()));
    }

    private Category ensureCategory(String name, String description) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).description(description).build()));
    }

    private void ensureKnowledgeBase(String title, String category, String solution, String keywords, String template) {
        boolean exists = knowledgeBaseRepository.findByActiveTrueOrderByUpdatedAtDesc().stream()
                .anyMatch(a -> a.getTitle().equalsIgnoreCase(title));
        if (!exists) {
            knowledgeBaseRepository.save(KnowledgeBaseArticle.builder()
                    .title(title)
                    .category(category)
                    .solution(solution)
                    .keywords(keywords)
                    .answerTemplate(template)
                    .active(true)
                    .build());
        }
    }

    private void createDemoTicket(String title, String description, TicketPriority priority, Category category,
                                  User customer, User agent, TicketStatus status, boolean overdue) {
        Ticket ticket = Ticket.builder()
                .ticketNumber("TSS-" + Year.now().getValue() + "-D" + String.format("%03d", ticketRepository.count() + 1))
                .title(title)
                .description(description)
                .priority(priority)
                .category(category)
                .createdBy(customer)
                .assignedTo(agent)
                .status(status)
                .dueAt(overdue ? LocalDateTime.now().minusHours(5) : LocalDateTime.now().plusDays(priority == TicketPriority.HIGH ? 1 : 3))
                .escalated(overdue)
                .attachmentName(title.contains("Laptop") ? "screenshot.png" : null)
                .solutionReason(status == TicketStatus.RESOLVED ? "Problem wurde mit Standardlösung gelöst." : null)
                .resolvedAt(status == TicketStatus.RESOLVED ? LocalDateTime.now() : null)
                .build();
        Ticket saved = ticketRepository.save(ticket);
        auditLogRepository.save(AuditLog.builder().ticket(saved).changedBy(customer).changeType("DEMO_TICKET_CREATED").oldValue(null).newValue(saved.getStatus().name()).build());
        if (agent != null) {
            auditLogRepository.save(AuditLog.builder().ticket(saved).changedBy(adminFallback()).changeType("ASSIGNED").oldValue("Unassigned").newValue(agent.getUsername()).build());
        }
        commentRepository.save(Comment.builder().ticket(saved).author(customer).content("Demo-Kommentar vom Kunden: Bitte prüfen.").isInternal(false).build());
        if (agent != null) {
            commentRepository.save(Comment.builder().ticket(saved).author(agent).content("Interne Notiz: Ticket wurde geprüft.").isInternal(true).build());
            notificationRepository.save(Notification.builder().recipient(agent).ticket(saved).title("Demo-Zuweisung").message("Dir wurde ein Demo-Ticket zugewiesen.").read(false).build());
        }
    }

    private User adminFallback() {
        return userRepository.findByUsername("admin").orElseThrow();
    }
}
