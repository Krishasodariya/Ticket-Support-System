package com.ticketsystem.service;

import com.ticketsystem.model.Category;
import com.ticketsystem.model.KnowledgeBaseArticle;
import com.ticketsystem.model.User;
import com.ticketsystem.model.WorkflowOption;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.CategoryRepository;
import com.ticketsystem.repository.KnowledgeBaseRepository;
import com.ticketsystem.repository.UserRepository;
import com.ticketsystem.repository.WorkflowOptionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final WorkflowOptionRepository workflowOptionRepository;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      PasswordEncoder passwordEncoder, KnowledgeBaseRepository knowledgeBaseRepository,
                      WorkflowOptionRepository workflowOptionRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.workflowOptionRepository = workflowOptionRepository;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@test.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .build());
        }
        if (!userRepository.existsByUsername("agent")) {
            userRepository.save(User.builder()
                    .username("agent")
                    .email("agent@test.com")
                    .passwordHash(passwordEncoder.encode("agent123"))
                    .role(UserRole.AGENT)
                    .isActive(true)
                    .build());
        }
        if (!userRepository.existsByUsername("customer")) {
            userRepository.save(User.builder()
                    .username("customer")
                    .email("customer@test.com")
                    .passwordHash(passwordEncoder.encode("customer123"))
                    .role(UserRole.CUSTOMER)
                    .isActive(true)
                    .build());
        }

        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().name("Software").description("Software related issues").build());
            categoryRepository.save(Category.builder().name("Hardware").description("Hardware related issues").build());
            categoryRepository.save(Category.builder().name("Netzwerk").description("WLAN, VPN and internet issues").build());
            categoryRepository.save(Category.builder().name("Management").description("Management or account issues").build());
            categoryRepository.save(Category.builder().name("Sonstiges").description("Other issues").build());
        }

        seedWorkflowOption("STATUS", "OPEN", "Offen", 1);
        seedWorkflowOption("STATUS", "IN_PROGRESS", "In Bearbeitung", 2);
        seedWorkflowOption("STATUS", "WAITING", "Wartend", 3);
        seedWorkflowOption("STATUS", "RESOLVED", "Gelöst", 4);
        seedWorkflowOption("STATUS", "CLOSED", "Geschlossen", 5);
        seedWorkflowOption("PRIORITY", "CRITICAL", "Kritisch", 1);
        seedWorkflowOption("PRIORITY", "HIGH", "Hoch", 2);
        seedWorkflowOption("PRIORITY", "MEDIUM", "Mittel", 3);
        seedWorkflowOption("PRIORITY", "LOW", "Niedrig", 4);

        if (knowledgeBaseRepository.count() == 0) {
            knowledgeBaseRepository.save(KnowledgeBaseArticle.builder()
                    .title("Passwort zurücksetzen")
                    .category("Software")
                    .solution("Benutzer über Reset-Funktion zurücksetzen lassen und Passwortregeln erklären.")
                    .keywords("password, login, reset")
                    .answerTemplate("Bitte nutzen Sie die Passwort-zurücksetzen-Funktion und wählen Sie ein sicheres Passwort.")
                    .active(true)
                    .build());
            knowledgeBaseRepository.save(KnowledgeBaseArticle.builder()
                    .title("Drucker offline")
                    .category("Hardware")
                    .solution("Drucker neu starten, Kabel prüfen und Treiber aktualisieren.")
                    .keywords("printer, offline, hardware")
                    .answerTemplate("Bitte starten Sie den Drucker neu und prüfen Sie die Kabel-/Netzwerkverbindung.")
                    .active(true)
                    .build());
        }
    }

    private void seedWorkflowOption(String type, String name, String label, int order) {
        if (!workflowOptionRepository.existsByTypeIgnoreCaseAndNameIgnoreCase(type, name)) {
            workflowOptionRepository.save(WorkflowOption.builder()
                    .type(type)
                    .name(name)
                    .label(label)
                    .sortOrder(order)
                    .active(true)
                    .build());
        }
    }
}
