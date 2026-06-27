package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.model.AuditLogFX;
import com.ticketsystem.frontend.model.CategoryFX;
import com.ticketsystem.frontend.model.DashboardStatsFX;
import com.ticketsystem.frontend.model.KnowledgeBaseFX;
import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.model.SystemAuditLogFX;
import com.ticketsystem.frontend.model.TicketFX;
import com.ticketsystem.frontend.model.UserFX;
import com.ticketsystem.frontend.model.WorkflowOptionFX;
import com.ticketsystem.frontend.service.AuditLogApiService;
import com.ticketsystem.frontend.service.CategoryApiService;
import com.ticketsystem.frontend.service.DashboardApiService;
import com.ticketsystem.frontend.service.DemoDataApiService;
import com.ticketsystem.frontend.service.KnowledgeBaseApiService;
import com.ticketsystem.frontend.service.NotificationApiService;
import com.ticketsystem.frontend.service.SystemAuditLogApiService;
import com.ticketsystem.frontend.service.TicketApiService;
import com.ticketsystem.frontend.service.WorkflowOptionApiService;
import com.ticketsystem.frontend.service.UserApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.AvatarHelper;
import com.ticketsystem.frontend.util.LabelHelper;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.NotificationPopup;
import com.ticketsystem.frontend.util.RealtimeWebSocketClient;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.frontend.util.ThemeManager;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.input.MouseEvent;
import javafx.scene.image.ImageView;

import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private ScrollPane paneDashboard;
    @FXML private ScrollPane paneReports;
    @FXML private VBox paneTickets, paneUsers, paneCategories, paneAuditLog;
    // Feature 32 – System-Aktivitätsprotokoll
    @FXML private VBox paneSystemAuditLog;

    @FXML private HBox navDashboard, navTickets, navUsers, navCategories, navReports, navAuditLog;
    @FXML private HBox navSystemAuditLog; // Feature 32
    @FXML private Circle dotDashboard, dotTickets, dotUsers, dotCategories, dotReports, dotAuditLog;
    @FXML private Circle dotSystemAuditLog; // Feature 32
    @FXML private Label labelDashboard, labelTickets, labelUsers, labelCategories, labelReports, labelAuditLog;
    @FXML private Label labelSystemAuditLog; // Feature 32
    // KAT-48: Breadcrumb ist jetzt klickbar (Hyperlink statt Label) -> springt zum Dashboard
    @FXML private Hyperlink breadcrumb;

    @FXML private Label sidebarInitials, sidebarName, topbarInitials, greetingLabel, notificationCountLabel;
    @FXML private ImageView sidebarProfileImage, topbarProfileImage;
    @FXML private Circle sidebarAvatarBackground, topbarAvatarBackground;

    @FXML private Label statTotal, statOpen, statResolvedToday, statCritical;
    @FXML private Label adminTotalTicketsLabel;
    @FXML private Label adminOpenTicketsLabel;
    @FXML private Label adminProgressTicketsLabel;
    @FXML private Label adminWaitingTicketsLabel;
    @FXML private Label adminResolvedTicketsLabel;
    @FXML private Label statCreatedToday, statOverdue, statEscalated, statAvgResolution;
    @FXML private Label lblCriticalCount, lblHighCount, lblMediumCount, lblLowCount;
    @FXML private ProgressBar progressCritical, progressHigh, progressMedium, progressLow;
    @FXML private TableView<TicketFX> dashTicketTable;
    @FXML private TableColumn<TicketFX, String> dashColId, dashColTitle, dashColStatus;
    // Aufgabe 3 – Top-Kunden Tabelle
    @FXML private TableView<com.ticketsystem.frontend.model.TopCustomerStatFX> topCustomersTable;
    @FXML private TableColumn<com.ticketsystem.frontend.model.TopCustomerStatFX, String> topColRank, topColUsername, topColCount;
    @FXML private TableColumn<com.ticketsystem.frontend.model.TopCustomerStatFX, String> topColOpen, topColResolved, topColLastActivity;
    // KAT-103: Bewertungsverteilung
    @FXML private Label statRatingDistribution;
    @FXML private ComboBox<TicketFX> dashboardTicketCombo;
    @FXML private ComboBox<UserFX> dashboardAgentCombo;
    @FXML private Button dashboardAssignButton;
    @FXML private Label dashboardAssignmentHint;

    @FXML private TableView<TicketFX> ticketTable;
    @FXML private TableColumn<TicketFX, String> colId, colTitle, colPriority, colStatus, colCategory, colAgent, colCreatedAt;
    @FXML private ComboBox<String> filterStatusCombo, filterPriorityCombo;
    @FXML private TextField searchField;
    @FXML private Label doubleClickHintLabel;
    // [Nzchupa | 2026-06-13] TSS-015: Topbar-Suchfeld — delegiert an aktiven Pane-Filter
    // Topbar search field reference — delegates to whichever pane is currently active
    @FXML private TextField topbarSearchField;

    @FXML private TableView<UserFX> userTable;
    @FXML private TableColumn<UserFX, String> colUsername, colEmail, colRole;
    // KAT-116: letzter Login pro Benutzer
    @FXML private TableColumn<UserFX, String> colLastLogin;
    @FXML private ComboBox<String> roleCombo;
    // [Nzchupa | 2026-06-13] TSS-015: Alle Benutzer speichern für clientseitige Topbar-Suche
    // Store all users so topbar search can filter without re-fetching
    private List<UserFX> allUsers = new java.util.ArrayList<>();
    @FXML private Label selectedUserLabel;
    // KAT-117: Agentenstatistik bei Auswahl eines Agenten in der Benutzerliste
    @FXML private Label agentStatsLabel;
    @FXML private TextField specializationField;

    @FXML private ListView<CategoryFX> categoryList;
    @FXML private TextField categoryNameField;
    @FXML private TextArea categoryDescField;

    @FXML private TableView<AuditLogFX> auditLogTable;
    @FXML private TableColumn<AuditLogFX, String> auditColTicket, auditColUser, auditColType, auditColOld, auditColNew, auditColTime;
    // [Nzchupa | 2026-06-13] TSS-014: Such- und Filterfelder für Audit-Log
    // Search and filter controls for Audit-Log pane
    @FXML private TextField auditSearchField;
    @FXML private ComboBox<String> auditTypeFilter;
    private List<AuditLogFX> allAuditLogs = new java.util.ArrayList<>();

    // Feature 32 – System-Aktivitätsprotokoll
    @FXML private TableView<SystemAuditLogFX> sysAuditTable;
    @FXML private TableColumn<SystemAuditLogFX, String> sysColActor, sysColEventType, sysColDetail, sysColTarget, sysColTime;
    @FXML private ComboBox<String> sysAuditTypeFilter;

    @FXML private ListView<KnowledgeBaseFX> kbAdminList;
    @FXML private TextField kbTitleField, kbCategoryField, kbKeywordsField;
    @FXML private TextArea kbSolutionArea, kbTemplateArea;

    @FXML private ListView<WorkflowOptionFX> statusOptionList, priorityOptionList;
    @FXML private TextField statusNameField, statusLabelField, priorityNameField, priorityLabelField;
    @FXML private ComboBox<String> exportStatusCombo, exportPriorityCombo;
    @FXML private TextField exportSearchField;

    private final TicketApiService ticketService = new TicketApiService();
    private final UserApiService userService = new UserApiService();
    private final CategoryApiService categoryService = new CategoryApiService();
    private final DashboardApiService dashboardService = new DashboardApiService();
    private final AuditLogApiService auditLogService = new AuditLogApiService();
    // Feature 32 – System-Aktivitätsprotokoll
    private final SystemAuditLogApiService systemAuditLogService = new SystemAuditLogApiService();
    private final NotificationApiService notificationService = new NotificationApiService();
    private final KnowledgeBaseApiService knowledgeBaseService = new KnowledgeBaseApiService();
    private final WorkflowOptionApiService workflowOptionService = new WorkflowOptionApiService();
    private final DemoDataApiService demoDataService = new DemoDataApiService();

    private final ObservableList<TicketFX> allTickets = FXCollections.observableArrayList();
    private boolean dashboardLoading = false;
    private boolean dashboardRefreshPending = false;
    private boolean ticketLoading = false;
    private boolean ticketRefreshPending = false;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !SessionManager.hasRole(UserRole.ADMIN)) {
            Navigator.navigateToLogin();
            return;
        }

        String username = SessionManager.getUsername();
        String initial = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "A";
        sidebarInitials.setText(initial);
        topbarInitials.setText(initial);
        sidebarName.setText(username);
        greetingLabel.setText("Willkommen zurück, " + username + "!");
        updateAvatarDisplay(SessionManager.getProfilePicture());

        // KAT-64: StackPane besitzt keine tooltip-Property -> Tooltip per Code installieren
        if (notificationButton != null) {
            Tooltip.install(notificationButton, new Tooltip("Benachrichtigungen anzeigen"));
        }

        initTable();
        initFiltersAndUserActions();
        // [Nzchupa | 2026-06-13] TSS-015: Topbar-Suche — Echtzeit-Listener, delegiert an aktiven Pane
        // Real-time topbar search: copy typed text to the active pane's search field so filter applies
        if (topbarSearchField != null) {
            topbarSearchField.textProperty().addListener((obs, old, val) -> handleTopbarSearch(val));
        }
        RealtimeWebSocketClient.getInstance().setViewListener("admin-dashboard", event -> {
            if (!event.isTicketOrCommentEvent()) {
                return;
            }
            if (paneDashboard != null && paneDashboard.isVisible()) {
                loadDashboardData();
            } else if (paneTickets != null && paneTickets.isVisible()) {
                loadTickets();
            }
            loadUnreadNotifications();
        });

        loadUnreadNotifications();
        showDashboard();
    }

    private void updateAvatarDisplay(String profilePictureUrl) {
        AvatarHelper.showAvatar(profilePictureUrl, sidebarProfileImage, sidebarAvatarBackground, sidebarInitials, 32);
        AvatarHelper.showAvatar(profilePictureUrl, topbarProfileImage, topbarAvatarBackground, topbarInitials, 28);
    }

    private void initTable() {
        dashColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        dashColTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        dashColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        dashColStatus.setCellFactory(c -> badgeCell());

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colAgent.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellFactory(c -> badgeCell());
        colPriority.setCellFactory(c -> badgeCell());
        ticketTable.setItems(allTickets);

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(c -> badgeCell());
        // KAT-116: letzter Login formatiert anzeigen (oder "-" wenn noch nie eingeloggt)
        if (colLastLogin != null) {
            java.time.format.DateTimeFormatter loginFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            colLastLogin.setCellValueFactory(data -> {
                java.time.LocalDateTime lastLogin = data.getValue().getLastLogin();
                return new javafx.beans.property.SimpleStringProperty(lastLogin != null ? lastLogin.format(loginFmt) : "-");
            });
        }

        auditColTicket.setCellValueFactory(new PropertyValueFactory<>("ticketTitle"));
        auditColUser.setCellValueFactory(new PropertyValueFactory<>("changedBy"));
        auditColType.setCellValueFactory(new PropertyValueFactory<>("changeType"));
        // KAT-132: Aktionstyp farblich kennzeichnen (CREATE/UPDATE/DELETE/ASSIGNED)
        auditColType.setCellFactory(c -> auditActionBadgeCell());
        auditColOld.setCellValueFactory(new PropertyValueFactory<>("oldValue"));
        auditColNew.setCellValueFactory(new PropertyValueFactory<>("newValue"));
        auditColTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Feature 32 – System-Aktivitätsprotokoll
        if (sysAuditTable != null) {
            if (sysColActor != null)     sysColActor.setCellValueFactory(new PropertyValueFactory<>("actor"));
            if (sysColEventType != null) sysColEventType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
            if (sysColDetail != null)    sysColDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));
            if (sysColTarget != null)    sysColTarget.setCellValueFactory(new PropertyValueFactory<>("targetUserId"));
            if (sysColTime != null)      sysColTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        }
        if (sysAuditTypeFilter != null) {
            sysAuditTypeFilter.getItems().setAll("Alle", "LOGIN_SUCCESS", "LOGIN_FAILURE",
                    "USER_CREATED", "USER_ACTIVATED", "USER_DEACTIVATED",
                    "ROLE_CHANGED", "EXPORT_CSV", "EXPORT_PDF");
            sysAuditTypeFilter.setValue("Alle");
        }

        ticketTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && ticketTable.getSelectionModel().getSelectedItem() != null) {
                TicketDetailController.setCurrentTicketId(ticketTable.getSelectionModel().getSelectedItem().getId());
                Navigator.navigateTo("TicketDetailView.fxml");
            }
        });

        // KAT-130: Doppelklick im Audit-Log öffnet das zugehörige Ticket (analog zu KAT-89)
        if (auditLogTable != null) {
            auditLogTable.setOnMouseClicked(e -> {
                AuditLogFX selected = auditLogTable.getSelectionModel().getSelectedItem();
                if (e.getClickCount() == 2 && selected != null
                        && selected.getTicketId() != null && !selected.getTicketId().isBlank()) {
                    TicketDetailController.setCurrentTicketId(selected.getTicketId());
                    Navigator.navigateTo("TicketDetailView.fxml");
                }
            });
        }

        if (dashTicketTable != null) {
            dashTicketTable.getSelectionModel().selectedItemProperty().addListener((obs, oldTicket, ticket) -> {
                if (ticket != null && dashboardTicketCombo != null) {
                    dashboardTicketCombo.getItems().stream()
                            .filter(item -> item.getId().equals(ticket.getId()))
                            .findFirst()
                            .ifPresent(dashboardTicketCombo::setValue);
                }
            });
        }
    }

    private void initFiltersAndUserActions() {
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().setAll("Alle", TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING.name(), TicketStatus.RESOLVED.name(), TicketStatus.CLOSED.name());
            filterStatusCombo.setValue("Alle");
            // [Nzchupa | 2026-06-13] TSS-003: Deutsche Labels in Status-Filter anzeigen, interne Enum-Namen für Filter-Logik behalten
            // Show German labels in filter combo; keep enum names as values so filtering still works
            javafx.util.Callback<javafx.scene.control.ListView<String>, ListCell<String>> statusCF =
                    lv -> new ListCell<>() { @Override protected void updateItem(String s, boolean e) { super.updateItem(s, e); setText(e || s == null ? null : "Alle".equals(s) ? "Alle" : LabelHelper.statusToGerman(s)); } };
            filterStatusCombo.setCellFactory(statusCF);
            filterStatusCombo.setButtonCell(statusCF.call(null));
            // [Nzchupa | 2026-06-13] Echtzeit-Filter — Tabelle aktualisiert sich sofort ohne Button-Klick
            // Real-time filter: update table immediately on combo change
            filterStatusCombo.valueProperty().addListener((obs, old, val) -> applyTicketFilter());
        }
        if (filterPriorityCombo != null) {
            filterPriorityCombo.getItems().setAll("Alle", TicketPriority.CRITICAL.name(), TicketPriority.HIGH.name(), TicketPriority.MEDIUM.name(), TicketPriority.LOW.name());
            filterPriorityCombo.setValue("Alle");
            // [Nzchupa | 2026-06-13] TSS-004: Deutsche Labels in Priorität-Filter anzeigen
            // Show German labels in priority filter combo
            javafx.util.Callback<javafx.scene.control.ListView<String>, ListCell<String>> priorityCF =
                    lv -> new ListCell<>() { @Override protected void updateItem(String s, boolean e) { super.updateItem(s, e); setText(e || s == null ? null : "Alle".equals(s) ? "Alle" : LabelHelper.priorityToGerman(s)); } };
            filterPriorityCombo.setCellFactory(priorityCF);
            filterPriorityCombo.setButtonCell(priorityCF.call(null));
            filterPriorityCombo.valueProperty().addListener((obs, old, val) -> applyTicketFilter());
        }
        if (searchField != null) {
            // [Nzchupa | 2026-06-13] Echtzeit-Suche — Ergebnisse erscheinen beim Tippen
            // Real-time search: filter on every keystroke
            searchField.textProperty().addListener((obs, old, val) -> applyTicketFilter());
        }
        if (roleCombo != null) {
            roleCombo.getItems().setAll(UserRole.CUSTOMER.name(), UserRole.AGENT.name(), UserRole.ADMIN.name());
        }
        if (dashboardTicketCombo != null) {
            dashboardTicketCombo.setCellFactory(param -> dashboardTicketCell());
            dashboardTicketCombo.setButtonCell(dashboardTicketCell());
        }
        if (dashboardAgentCombo != null) {
            dashboardAgentCombo.setCellFactory(param -> dashboardAgentCell());
            dashboardAgentCombo.setButtonCell(dashboardAgentCell());
        }
        if (exportStatusCombo != null) {
            exportStatusCombo.getItems().setAll("Alle", TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING.name(), TicketStatus.RESOLVED.name(), TicketStatus.CLOSED.name());
            exportStatusCombo.setValue("Alle");
        }
        if (exportPriorityCombo != null) {
            exportPriorityCombo.getItems().setAll("Alle", TicketPriority.CRITICAL.name(), TicketPriority.HIGH.name(), TicketPriority.MEDIUM.name(), TicketPriority.LOW.name());
            exportPriorityCombo.setValue("Alle");
        }
        if (userTable != null) {
            userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, user) -> {
                if (user == null) {
                    selectedUserLabel.setText("Kein Benutzer ausgewählt");
                    roleCombo.setValue(null);
                } else {
                    selectedUserLabel.setText("Ausgewählt: " + user.getUsername());
                    roleCombo.setValue(user.getRole());
                }
                updateAgentStatsLabel(user);
            });
        }
    }

    // KAT-117: Ticketanzahl (gesamt/offen/gelöst) für den ausgewählten Agenten anzeigen
    private void updateAgentStatsLabel(UserFX user) {
        if (agentStatsLabel == null) return;
        if (user == null || !"AGENT".equalsIgnoreCase(user.getRole())) {
            agentStatsLabel.setText("");
            return;
        }
        long total = 0, open = 0, resolved = 0;
        for (TicketFX ticket : allTickets) {
            if (!user.getUsername().equals(ticket.getAssignedTo())) continue;
            total++;
            String status = ticket.getStatus();
            if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
                resolved++;
            } else {
                open++;
            }
        }
        agentStatsLabel.setText("Tickets gesamt: " + total + "   Offen: " + open + "   Gelöst: " + resolved);
    }

    private <T> TableCell<T, String> badgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(null);
                setGraphic(null);

                if (empty || item == null || item.isBlank()) {
                    return;
                }

                setGraphic(createBadge(item));
            }
        };
    }

    // KAT-132: Audit-Log Aktionstyp farblich kennzeichnen (CREATE/UPDATE/DELETE/ASSIGNED)
    private <T> TableCell<T, String> auditActionBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                if (empty || item == null || item.isBlank()) return;
                setGraphic(createAuditActionBadge(item));
            }
        };
    }

    private Label createAuditActionBadge(String changeType) {
        String value = changeType == null ? "" : changeType.toUpperCase();
        Label badge = new Label(changeType);
        badge.getStyleClass().add("badge");

        if (value.contains("DELETE") || value.contains("REMOVED")) {
            badge.getStyleClass().add("badge-audit-delete");
        } else if (value.equals("ASSIGNED")) {
            badge.getStyleClass().add("badge-audit-assigned");
        } else if (value.contains("CREATED")) {
            badge.getStyleClass().add("badge-audit-create");
        } else {
            // alle übrigen Aktionen (z.B. STATUS_CHANGED, TITLE_CHANGED) gelten als UPDATE
            badge.getStyleClass().add("badge-audit-update");
        }
        return badge;
    }

    private ListCell<TicketFX> dashboardTicketCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(TicketFX ticket, boolean empty) {
                super.updateItem(ticket, empty);
                if (empty || ticket == null) {
                    setText(null);
                    return;
                }

                String number = ticket.getTicketNumber() == null || ticket.getTicketNumber().isBlank()
                        ? ticket.getId()
                        : ticket.getTicketNumber();
                String agent = ticket.getAssignedTo() == null || ticket.getAssignedTo().isBlank()
                        ? "nicht zugewiesen"
                        : ticket.getAssignedTo();
                setText(number + " – " + ticket.getTitle() + " (" + agent + ")");
            }
        };
    }

    private ListCell<UserFX> dashboardAgentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(UserFX agent, boolean empty) {
                super.updateItem(agent, empty);
                setText(empty || agent == null ? null : agent.getUsername() + " (" + agent.getEmail() + ")");
            }
        };
    }

    private Label createBadge(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();

        Label badge = new Label();
        badge.getStyleClass().add("badge");

        switch (value) {
            case "OPEN" -> {
                badge.getStyleClass().add("badge-open");
                badge.setText("● Offen");
            }
            case "IN_PROGRESS" -> {
                badge.getStyleClass().add("badge-progress");
                badge.setText("● In Bearbeitung");
            }
            case "WAITING" -> {
                badge.getStyleClass().add("badge-waiting");
                badge.setText("● Wartend");
            }
            case "RESOLVED" -> {
                badge.getStyleClass().add("badge-resolved");
                badge.setText("● Gelöst");
            }
            case "CLOSED" -> {
                badge.getStyleClass().add("badge-closed");
                badge.setText("● Geschlossen");
            }
            case "CRITICAL" -> {
                badge.getStyleClass().add("badge-critical");
                badge.setText("● Kritisch");
            }
            case "HIGH" -> {
                badge.getStyleClass().add("badge-high");
                badge.setText("● Hoch");
            }
            case "MEDIUM" -> {
                badge.getStyleClass().add("badge-medium");
                badge.setText("● Mittel");
            }
            case "LOW" -> {
                badge.getStyleClass().add("badge-low");
                badge.setText("● Niedrig");
            }
            case "ADMIN" -> {
                badge.getStyleClass().add("badge-admin");
                badge.setText("Admin");
            }
            case "AGENT" -> {
                badge.getStyleClass().add("badge-agent");
                badge.setText("Agent");
            }
            case "CUSTOMER" -> {
                badge.getStyleClass().add("badge-customer");
                badge.setText("Customer");
            }
            default -> {
                badge.getStyleClass().add("badge-customer");
                badge.setText(type);
            }
        }

        return badge;
    }

    private void loadDashboardData() {
        if (dashboardLoading) {
            dashboardRefreshPending = true;
            return;
        }
        dashboardLoading = true;

        Task<List<TicketFX>> ticketsTask = new Task<>() {
            @Override protected List<TicketFX> call() throws Exception { return ticketService.getAllTickets(); }
        };
        ticketsTask.setOnSucceeded(e -> {
            List<TicketFX> tickets = ticketsTask.getValue();
            dashTicketTable.setItems(FXCollections.observableArrayList(tickets.stream().limit(8).toList()));

            if (dashboardTicketCombo != null) {
                String selectedTicketId = dashboardTicketCombo.getValue() == null
                        ? null
                        : dashboardTicketCombo.getValue().getId();
                List<TicketFX> assignableTickets = tickets.stream()
                        .filter(ticket -> !"RESOLVED".equalsIgnoreCase(ticket.getStatus()))
                        .filter(ticket -> !"CLOSED".equalsIgnoreCase(ticket.getStatus()))
                        .toList();
                dashboardTicketCombo.setItems(FXCollections.observableArrayList(assignableTickets));
                if (selectedTicketId != null) {
                    assignableTickets.stream()
                            .filter(ticket -> selectedTicketId.equals(ticket.getId()))
                            .findFirst()
                            .ifPresent(dashboardTicketCombo::setValue);
                }
            }
        });
        ticketsTask.setOnFailed(e -> AlertHelper.showError("Fehler", "Tickets konnten nicht geladen werden. Läuft das Backend auf Port 8080?"));
        new Thread(ticketsTask, "admin-dashboard-tickets").start();

        Task<List<UserFX>> agentsTask = new Task<>() {
            @Override protected List<UserFX> call() throws Exception { return userService.getActiveAgents(); }
        };
        agentsTask.setOnSucceeded(e -> {
            if (dashboardAgentCombo == null) return;
            String selectedAgentId = dashboardAgentCombo.getValue() == null
                    ? null
                    : dashboardAgentCombo.getValue().getId();
            List<UserFX> agents = agentsTask.getValue();
            dashboardAgentCombo.setItems(FXCollections.observableArrayList(agents));
            if (selectedAgentId != null) {
                agents.stream()
                        .filter(agent -> selectedAgentId.equals(agent.getId()))
                        .findFirst()
                        .ifPresent(dashboardAgentCombo::setValue);
            }
            if (dashboardAssignmentHint != null && agents.isEmpty()) {
                dashboardAssignmentHint.setText("Keine aktiven Agenten vorhanden.");
            }
        });
        agentsTask.setOnFailed(e -> {
            if (dashboardAssignmentHint != null) {
                dashboardAssignmentHint.setText("Agenten konnten nicht geladen werden.");
            }
        });
        new Thread(agentsTask, "admin-dashboard-agents").start();

        Task<DashboardStatsFX> statsTask = new Task<>() {
            @Override protected DashboardStatsFX call() throws Exception { return dashboardService.getStats(); }
        };
        statsTask.setOnCancelled(e -> finishDashboardLoad());
        statsTask.setOnSucceeded(e -> {
            applyStats(statsTask.getValue());
            finishDashboardLoad();
        });
        statsTask.setOnFailed(e -> {
            AlertHelper.showError("Fehler", "Dashboard-Statistik konnte nicht geladen werden. Bitte als Admin anmelden und Backend prüfen.");
            finishDashboardLoad();
        });
        new Thread(statsTask, "admin-dashboard-stats").start();
    }

    private void finishDashboardLoad() {
        dashboardLoading = false;
        boolean refreshAgain = dashboardRefreshPending
                && paneDashboard != null
                && paneDashboard.isVisible();
        dashboardRefreshPending = false;
        if (refreshAgain) {
            loadDashboardData();
        }
    }

    private void applyStats(DashboardStatsFX stats) {
        long total    = stats.getTotalTickets();
        long overdue  = stats.getOverdueTickets();

        // [Nzchupa | 2026-06-13] TSS-013: Korrektur der Diagramm-Werte
        // Bug-Fix: resolvedToday war falsch (nur heute), open fehlte IN_PROGRESS + WAITING.
        // Gelöst  = alle abgeschlossenen Tickets (RESOLVED + CLOSED)
        // Überfällig = überfällige aktive Tickets
        // Offen   = Rest: aktiv aber nicht überfällig (disjoint für saubere Tortendarstellung)
        long resolved = stats.getResolvedTickets() + stats.getClosedTickets();
        long active   = stats.getOpenTickets() + stats.getInProgressTickets() + stats.getWaitingTickets();
        long openNormal = Math.max(0, active - overdue); // aktiv aber nicht überfällig

        long critical = value(stats.getTicketsByPriority(), "CRITICAL");
        long high     = value(stats.getTicketsByPriority(), "HIGH");
        long medium   = value(stats.getTicketsByPriority(), "MEDIUM");
        long low      = value(stats.getTicketsByPriority(), "LOW");

        statTotal.setText(String.valueOf(total));
        statOpen.setText(String.valueOf(active));                          // Stat-Karte: alle aktiven
        statResolvedToday.setText(String.valueOf(stats.getResolvedToday())); // Stat-Karte: heute gelöst

        if (statCritical != null)    statCritical.setText(String.valueOf(critical));
        if (statCreatedToday != null) statCreatedToday.setText(String.valueOf(stats.getCreatedToday()));
        if (statOverdue != null)     statOverdue.setText(String.valueOf(overdue));
        if (statEscalated != null)   statEscalated.setText(String.valueOf(stats.getEscalatedTickets()));
        if (statAvgResolution != null) statAvgResolution.setText(stats.getAverageResolutionHours() + " h");

        // KAT-103: Bewertungsverteilung anzeigen, z.B. "1★:0  2★:1  3★:4  4★:9  5★:12"
        if (statRatingDistribution != null && stats.getRatingDistribution() != null) {
            StringBuilder sb = new StringBuilder();
            for (int rating = 1; rating <= 5; rating++) {
                long count = stats.getRatingDistribution().getOrDefault(String.valueOf(rating), 0L);
                if (sb.length() > 0) sb.append("   ");
                sb.append(rating).append("★: ").append(count);
            }
            statRatingDistribution.setText(sb.toString());
        }

        updateStatusChart(total, resolved, openNormal, overdue);
        updatePriorityChart(total, critical, high, medium, low);

        // KAT-107 – Top-Kunden Tabelle mit Detailinfos befüllen
        if (topCustomersTable != null && stats.getTopCustomers() != null) {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            topColRank.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(
                            String.valueOf(topCustomersTable.getItems().indexOf(data.getValue()) + 1)));
            topColUsername.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
            topColCount.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getTotalTickets())));
            if (topColOpen != null) {
                topColOpen.setCellValueFactory(data ->
                        new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getOpenTickets())));
            }
            if (topColResolved != null) {
                topColResolved.setCellValueFactory(data ->
                        new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getResolvedTickets())));
            }
            if (topColLastActivity != null) {
                topColLastActivity.setCellValueFactory(data -> {
                    java.time.LocalDateTime last = data.getValue().getLastActivity();
                    return new javafx.beans.property.SimpleStringProperty(last != null ? last.format(fmt) : "-");
                });
            }

            javafx.collections.ObservableList<com.ticketsystem.frontend.model.TopCustomerStatFX> rows =
                    javafx.collections.FXCollections.observableArrayList(stats.getTopCustomers());
            topCustomersTable.setItems(rows);
        }
    }
    // [Nzchupa | 2026-06-13] TSS-013: Explizite Slice-Farben nach Platform.runLater — verhindert Farb-Reset
    // After getData().clear()+addAll() JavaFX re-assigns default-color CSS classes.
    // Platform.runLater waits for the scene to render nodes, then sets inline -fx-pie-color per slice.
    private void updateStatusChart(long total, long resolved, long open, long overdue) {
        if (statusPieChart == null) return;
        statusPieChart.getData().clear();
        if (!statusPieChart.getStyleClass().contains("status-chart")) statusPieChart.getStyleClass().add("status-chart");

        PieChart.Data dResolved  = new PieChart.Data("Gelöst",    resolved);
        PieChart.Data dOpen      = new PieChart.Data("Offen",      open);
        PieChart.Data dOverdue   = new PieChart.Data("Überfällig", overdue);
        statusPieChart.getData().addAll(dResolved, dOpen, dOverdue);

        // Farben nach Render setzen, damit getData().clear() sie nicht zurücksetzt
        Platform.runLater(() -> {
            applyPieColor(dResolved, "#22C55E");
            applyPieColor(dOpen,     "#3B82F6");
            applyPieColor(dOverdue,  "#EF4444");
            // [Nzchupa | 2026-06-13] TSS-013: Legendensymbole ebenfalls einfärben
            // Legend symbols use default-colorN CSS classes — must override them too
            applyLegendColors(statusPieChart, "#22C55E", "#3B82F6", "#EF4444");
        });

        if (statusChartTotalLabel != null) statusChartTotalLabel.setText(String.valueOf(total));
        if (legendResolved != null) legendResolved.setText(formatLegend(resolved, total));
        if (legendOpen != null)     legendOpen.setText(formatLegend(open, total));
        if (legendOverdue != null)  legendOverdue.setText(formatLegend(overdue, total));
        if (legendTotal != null)    legendTotal.setText(total + " (100%)");
    }

    private void updatePriorityChart(long total, long critical, long high, long medium, long low) {
        if (priorityPieChart == null) return;
        priorityPieChart.getData().clear();
        if (!priorityPieChart.getStyleClass().contains("priority-chart")) priorityPieChart.getStyleClass().add("priority-chart");

        PieChart.Data dCritical = new PieChart.Data("Kritisch", critical);
        PieChart.Data dHigh     = new PieChart.Data("Hoch",     high);
        PieChart.Data dMedium   = new PieChart.Data("Mittel",   medium);
        PieChart.Data dLow      = new PieChart.Data("Niedrig",  low);
        priorityPieChart.getData().addAll(dCritical, dHigh, dMedium, dLow);

        Platform.runLater(() -> {
            applyPieColor(dCritical, "#EF4444");
            applyPieColor(dHigh,     "#F97316");
            applyPieColor(dMedium,   "#EAB308");
            applyPieColor(dLow,      "#22C55E");
            // [Nzchupa | 2026-06-13] TSS-013: Legendensymbole ebenfalls einfärben
            applyLegendColors(priorityPieChart, "#EF4444", "#F97316", "#EAB308", "#22C55E");
        });

        if (priorityChartTotalLabel != null) priorityChartTotalLabel.setText(String.valueOf(total));
        if (legendCritical != null) legendCritical.setText(formatLegend(critical, total));
        if (legendHigh != null)     legendHigh.setText(formatLegend(high, total));
        if (legendMedium != null)   legendMedium.setText(formatLegend(medium, total));
        if (legendLow != null)      legendLow.setText(formatLegend(low, total));
    }

    // [Nzchupa | 2026-06-13] TSS-013: Hilfsmethode — setzt -fx-pie-color direkt am Slice-Node
    // Helper: sets inline -fx-pie-color on the pie slice node; noop if node not yet rendered
    private void applyPieColor(PieChart.Data data, String color) {
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
    }

    // [Nzchupa | 2026-06-13] TSS-013: Legendensymbole einfärben — default-colorN überschreiben
    // JavaFX legend symbols keep their default-colorN class; override -fx-background-color inline
    private void applyLegendColors(javafx.scene.chart.PieChart chart, String... colors) {
        for (int i = 0; i < colors.length; i++) {
            final String color = colors[i];
            chart.lookupAll(".default-color" + i + ".chart-legend-item-symbol")
                    .forEach(node -> node.setStyle("-fx-background-color: " + color + ";"));
        }
    }

    private String formatLegend(long value, long total) {
        if (total == 0) {
            return value + " (0%)";
        }

        long percent = Math.round((value * 100.0) / total);
        return value + " (" + percent + "%)";
    }
    private void setPriorityBar(Map<String, Long> map, String key, Label label, ProgressBar progressBar, long total) {
        long count = value(map, key);
        label.setText(String.valueOf(count));
        progressBar.setProgress(total == 0 ? 0 : (double) count / total);
    }

    private long value(Map<String, Long> map, String key) {
        return map == null ? 0 : map.getOrDefault(key, 0L);
    }

    // [Nzchupa | 2026-06-13] Loading-Spinner während Datenladen — bessere UX
    // Show spinner while loading, restore empty-state placeholder on finish
    private void loadTickets() {
        if (ticketLoading) {
            ticketRefreshPending = true;
            return;
        }
        ticketLoading = true;
        ticketTable.setPlaceholder(buildLoadingNode());
        Task<List<TicketFX>> task = new Task<>() {
            @Override
            protected List<TicketFX> call() throws Exception {
                return ticketService.getAllTickets();
            }
        };
        task.setOnSucceeded(e -> {
            allTickets.setAll(task.getValue());
            ticketTable.setPlaceholder(buildEmptyNode("Keine Tickets gefunden."));
            updateAdminTicketStatistics(allTickets);
            applyTicketFilter();
            updateTicketEmptyStateBindings();
            finishTicketLoad();
        });
        task.setOnFailed(e -> {
            ticketTable.setPlaceholder(buildEmptyNode("Fehler beim Laden."));
            AlertHelper.showError("Fehler", "Tickets konnten nicht geladen werden.");
            updateTicketEmptyStateBindings();
            finishTicketLoad();
        });
        new Thread(task, "admin-load-tickets").start();
    }

    // KAT-35/36: Filter und Doppelklick-Hinweis sind nur sinnvoll, wenn ueberhaupt Tickets vorhanden sind
    private void updateTicketEmptyStateBindings() {
        boolean empty = allTickets.isEmpty();
        if (doubleClickHintLabel != null) {
            doubleClickHintLabel.setVisible(!empty);
            doubleClickHintLabel.setManaged(!empty);
        }
        if (filterStatusCombo != null) filterStatusCombo.setDisable(empty);
        if (filterPriorityCombo != null) filterPriorityCombo.setDisable(empty);
        if (searchField != null) searchField.setDisable(empty);
    }

    private void finishTicketLoad() {
        ticketLoading = false;
        boolean refreshAgain = ticketRefreshPending
                && paneTickets != null
                && paneTickets.isVisible();
        ticketRefreshPending = false;
        if (refreshAgain) {
            loadTickets();
        }
    }

    private javafx.scene.Node buildLoadingNode() {
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(36, 36);
        javafx.scene.control.Label lbl = new javafx.scene.control.Label("Daten werden geladen…");
        lbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, spinner, lbl);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    private javafx.scene.Node buildEmptyNode(String msg) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(msg);
        lbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        return lbl;
    }

    @FXML public void handleApplyTicketFilter() { applyTicketFilter(); }
    @FXML public void handleClearTicketFilter() {
        filterStatusCombo.setValue("Alle");
        filterPriorityCombo.setValue("Alle");
        searchField.clear();
        applyTicketFilter();
    }

    // [Nzchupa | 2026-06-13] TSS-015: Topbar-Suche delegiert an aktiven Pane
    // Topbar search delegates to whichever pane is currently visible
    private void handleTopbarSearch(String val) {
        if (paneTickets != null && paneTickets.isVisible()) {
            // Tickets-Pane: Suchtext in pane-eigenes searchField setzen → löst applyTicketFilter() aus
            if (searchField != null) searchField.setText(val);
        } else if (paneUsers != null && paneUsers.isVisible()) {
            // Benutzer-Pane: direkt allUsers filtern nach Username oder E-Mail
            String search = val == null ? "" : val.trim().toLowerCase();
            List<UserFX> filtered = allUsers.stream()
                    .filter(u -> search.isBlank()
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(search))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(search)))
                    .collect(Collectors.toList());
            userTable.setItems(FXCollections.observableArrayList(filtered));
        } else if (paneAuditLog != null && paneAuditLog.isVisible()) {
            // Audit-Log-Pane: Text in auditSearchField setzen und Filter ausführen
            if (auditSearchField != null) auditSearchField.setText(val);
            applyAuditFilter();
        }
    }

    private void applyTicketFilter() {
        String status = filterStatusCombo == null ? "Alle" : filterStatusCombo.getValue();
        String priority = filterPriorityCombo == null ? "Alle" : filterPriorityCombo.getValue();
        String search = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<TicketFX> filtered = allTickets.stream()
                .filter(t -> status == null || "Alle".equals(status) || status.equals(t.getStatus()))
                .filter(t -> priority == null || "Alle".equals(priority) || priority.equals(t.getPriority()))
                .filter(t -> search.isBlank()
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(search))
                        || (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(search))
                        || (t.getCreatedBy() != null && t.getCreatedBy().toLowerCase().contains(search))
                        || (t.getAssignedTo() != null && t.getAssignedTo().toLowerCase().contains(search))
                        || (t.getCategoryName() != null && t.getCategoryName().toLowerCase().contains(search)))
                .collect(Collectors.toList());
        ticketTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void loadUsers() {
        Task<List<UserFX>> task = new Task<>() {
            @Override protected List<UserFX> call() throws Exception { return userService.getAllUsers(); }
        };
        task.setOnSucceeded(e -> {
            // [Nzchupa | 2026-06-13] TSS-015: allUsers speichern für Topbar-Suche
            allUsers = new java.util.ArrayList<>(task.getValue());
            userTable.setItems(FXCollections.observableArrayList(allUsers));
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Benutzer konnten nicht geladen werden."));
        new Thread(task, "admin-load-users").start();
    }

    @FXML public void handleUpdateSelectedUserRole() {
        UserFX selected = userTable.getSelectionModel().getSelectedItem();
        String role = roleCombo.getValue();
        if (selected == null || role == null) {
            AlertHelper.showError("Fehler", "Bitte Benutzer und Rolle auswählen.");
            return;
        }
        new Thread(() -> {
            try {
                userService.updateRole(selected.getId(), role);
                Platform.runLater(() -> { AlertHelper.showInfo("Erfolg", "Rolle wurde auf " + role + " gesetzt."); loadUsers(); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Rolle konnte nicht geändert werden.\n" + ex.getMessage()));
            }
        }, "admin-update-role").start();
    }

    @FXML public void handleToggleSelectedUserActive() {
        UserFX selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Fehler", "Bitte zuerst einen Benutzer auswählen.");
            return;
        }
        new Thread(() -> {
            try {
                userService.toggleActive(selected.getId(), !selected.isActive());
                Platform.runLater(() -> { AlertHelper.showInfo("Erfolg", "Benutzerstatus wurde geändert."); loadUsers(); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Status konnte nicht geändert werden.\n" + ex.getMessage()));
            }
        }, "admin-toggle-active").start();
    }

    @FXML public void handleUpdateSpecialization() {
        UserFX selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Fehler", "Bitte zuerst einen Agenten auswählen.");
            return;
        }
        String spec = specializationField != null ? specializationField.getText() : "";
        new Thread(() -> {
            try {
                userService.updateSpecialization(selected.getId(), spec);
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Erfolg", "Spezialisierung wurde gesetzt: " + (spec.isBlank() ? "Generalist" : spec));
                    loadUsers();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Spezialisierung konnte nicht gesetzt werden.\n" + ex.getMessage()));
            }
        }, "admin-update-specialization").start();
    }

    private void loadCategories() {
        Task<List<CategoryFX>> task = new Task<>() {
            @Override protected List<CategoryFX> call() throws Exception { return categoryService.getAllCategories(); }
        };
        task.setOnSucceeded(e -> categoryList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Kategorien konnten nicht geladen werden."));
        new Thread(task, "admin-load-categories").start();
    }

    @FXML public void handleAddCategory() {
        String name = categoryNameField.getText() == null ? "" : categoryNameField.getText().trim();
        String desc = categoryDescField.getText() == null ? "" : categoryDescField.getText().trim();
        if (name.isBlank()) {
            AlertHelper.showError("Fehler", "Bitte Kategoriename eingeben.");
            return;
        }
        new Thread(() -> {
            try {
                categoryService.createCategory(name, desc);
                Platform.runLater(() -> {
                    categoryNameField.clear();
                    categoryDescField.clear();
                    loadCategories();
                    AlertHelper.showInfo("Erfolg", "Kategorie wurde hinzugefügt.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Kategorie konnte nicht erstellt werden.\n" + ex.getMessage()));
            }
        }, "admin-add-category").start();
    }

    @FXML public void handleDeleteCategory() {
        CategoryFX selected = categoryList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Fehler", "Bitte Kategorie auswählen.");
            return;
        }
        // KAT-114: Löschen war bisher ohne Bestätigung möglich -> Klick auf falsche Zeile löscht sofort
        if (!AlertHelper.showConfirm("Kategorie löschen", "Möchten Sie die Kategorie \"" + selected.getName() + "\" wirklich löschen?", "Löschen")) {
            return;
        }
        new Thread(() -> {
            try {
                categoryService.deleteCategory(selected.getId());
                Platform.runLater(() -> { loadCategories(); AlertHelper.showInfo("Erfolg", "Kategorie wurde gelöscht."); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Kategorie konnte nicht gelöscht werden.\n" + ex.getMessage()));
            }
        }, "admin-delete-category").start();
    }

    private void loadAuditLogs() {
        Task<List<AuditLogFX>> task = new Task<>() {
            @Override protected List<AuditLogFX> call() throws Exception { return auditLogService.getAllLogs(); }
        };
        task.setOnSucceeded(e -> {
            // [Nzchupa | 2026-06-13] TSS-014: Alle Einträge speichern für clientseitige Filterung
            // Store all entries so client-side filter can work without re-fetching
            allAuditLogs = new java.util.ArrayList<>(task.getValue());
            // Aktionstypen für Filter-ComboBox befüllen
            if (auditTypeFilter != null && auditTypeFilter.getItems().isEmpty()) {
                java.util.Set<String> types = new java.util.TreeSet<>();
                allAuditLogs.forEach(l -> { if (l.getChangeType() != null) types.add(l.getChangeType()); });
                auditTypeFilter.getItems().add("Alle");
                auditTypeFilter.getItems().addAll(types);
                auditTypeFilter.setValue("Alle");
            }
            applyAuditFilter();
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Audit-Log konnte nicht geladen werden."));
        new Thread(task, "admin-load-audit").start();

        // Feature 32 – System-Audit-Log parallel laden
        loadSystemAuditLogs();
    }

    // [Nzchupa | 2026-06-13] TSS-014: Audit-Log-Filter — Suche nach Ticket/Benutzer + Aktionstyp
    // Filter audit log by ticket title / user name and action type
    private void applyAuditFilter() {
        String search = auditSearchField != null && auditSearchField.getText() != null
                ? auditSearchField.getText().trim().toLowerCase() : "";
        String type = auditTypeFilter != null ? auditTypeFilter.getValue() : "Alle";
        List<AuditLogFX> filtered = allAuditLogs.stream()
                .filter(l -> search.isEmpty()
                        || (l.getTicketTitle() != null && l.getTicketTitle().toLowerCase().contains(search))
                        || (l.getChangedBy() != null && l.getChangedBy().toLowerCase().contains(search)))
                .filter(l -> type == null || "Alle".equals(type)
                        || type.equals(l.getChangeType()))
                .collect(Collectors.toList());
        auditLogTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML public void handleFilterAuditLog() { applyAuditFilter(); }

    @FXML public void handleClearAuditFilter() {
        if (auditSearchField != null) auditSearchField.clear();
        if (auditTypeFilter  != null) auditTypeFilter.setValue("Alle");
        applyAuditFilter();
    }

    // Feature 32
    private void loadSystemAuditLogs() {
        if (sysAuditTable == null) return;
        String type = sysAuditTypeFilter != null ? sysAuditTypeFilter.getValue() : "Alle";
        Task<List<SystemAuditLogFX>> task = new Task<>() {
            @Override protected List<SystemAuditLogFX> call() throws Exception {
                if (type == null || "Alle".equals(type)) return systemAuditLogService.getAll();
                return systemAuditLogService.getByType(type);
            }
        };
        task.setOnSucceeded(e -> sysAuditTable.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "System-Protokoll konnte nicht geladen werden."));
        new Thread(task, "admin-load-sys-audit").start();
    }

    @FXML public void handleFilterSystemAuditLog() { loadSystemAuditLogs(); }

    private void loadUnreadNotifications() {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception { return notificationService.getUnreadCount(); }
        };
        task.setOnSucceeded(e -> notificationCountLabel.setText(String.valueOf(task.getValue())));
        task.setOnFailed(e -> notificationCountLabel.setText("0"));
        new Thread(task, "admin-notification-count").start();
    }

    // [Nzchupa | 2026-06-13] Alte handleShowNotifications / showNotificationDialog entfernt — war toter Code
    // Removed old native-Alert notification dialog — NotificationPopup (handleNotifications) is used instead

    @FXML public void showDashboard() { switchTab(paneDashboard, navDashboard, dotDashboard, labelDashboard, "Dashboard"); loadDashboardData(); }
    @FXML public void showTickets() { switchTab(paneTickets, navTickets, dotTickets, labelTickets, "Alle Tickets"); loadTickets(); }
    @FXML public void showUsers() { switchTab(paneUsers, navUsers, dotUsers, labelUsers, "Benutzer"); loadUsers(); }
    @FXML public void showCategories() {
        switchTab(paneCategories, navCategories, dotCategories, labelCategories, "Kategorien");
        loadCategories();
        loadWorkflowOptionsAdmin();
    }
    @FXML
    public void showReports() {
        switchTab(paneReports, navReports, dotReports, labelReports, "Berichte");

        if (paneReports != null) {
            paneReports.setVvalue(0.0);
        }

        loadKnowledgeBaseAdmin();
    }
    @FXML public void showAuditLog() { switchTab(paneAuditLog, navAuditLog, dotAuditLog, labelAuditLog, "Audit-Log"); loadAuditLogs(); }
    // Feature 32 – System-Aktivitätsprotokoll
    @FXML public void showSystemAuditLog() { switchTab(paneSystemAuditLog, navSystemAuditLog, dotSystemAuditLog, labelSystemAuditLog, "Aktivitätsprotokoll"); loadSystemAuditLogs(); }
    @FXML public void handleRefreshDashboard() { loadDashboardData(); loadUnreadNotifications(); }

    @FXML
    public void handleDashboardAssignAgent() {
        TicketFX ticket = dashboardTicketCombo == null ? null : dashboardTicketCombo.getValue();
        UserFX agent = dashboardAgentCombo == null ? null : dashboardAgentCombo.getValue();

        if (ticket == null || agent == null) {
            AlertHelper.showError("Fehlende Auswahl", "Bitte zuerst ein Ticket und einen Agenten auswählen.");
            return;
        }

        if (dashboardAssignButton != null) dashboardAssignButton.setDisable(true);
        if (dashboardAssignmentHint != null) dashboardAssignmentHint.setText("Zuweisung wird gespeichert …");

        Task<TicketFX> task = new Task<>() {
            @Override
            protected TicketFX call() throws Exception {
                return ticketService.assignTicket(ticket.getId(), agent.getId());
            }
        };
        task.setOnSucceeded(e -> {
            if (dashboardAssignButton != null) dashboardAssignButton.setDisable(false);
            if (dashboardAssignmentHint != null) {
                dashboardAssignmentHint.setText("Ticket wurde erfolgreich " + agent.getUsername() + " zugewiesen.");
            }
            AlertHelper.showInfo("Agent zugewiesen", "Das Ticket wurde " + agent.getUsername() + " zugewiesen.");
            loadDashboardData();
        });
        task.setOnFailed(e -> {
            if (dashboardAssignButton != null) dashboardAssignButton.setDisable(false);
            if (dashboardAssignmentHint != null) dashboardAssignmentHint.setText("Zuweisung fehlgeschlagen.");
            String message = task.getException() == null ? "Unbekannter Fehler" : task.getException().getMessage();
            AlertHelper.showError("Zuweisung fehlgeschlagen", message);
        });
        new Thread(task, "admin-dashboard-assign-agent").start();
    }

    @FXML public void handleGenerateDemoData() {
        new Thread(() -> {
            try {
                String message = demoDataService.generateDemoData();
                Platform.runLater(() -> { AlertHelper.showInfo("Demo-Daten", message); loadDashboardData(); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Demo-Daten konnten nicht erstellt werden.\n" + ex.getMessage()));
            }
        }, "admin-demo-data").start();
    }

    @FXML public void handleEscalateOverdue() {
        new Thread(() -> {
            try {
                int count = ticketService.escalateOverdueTickets();
                Platform.runLater(() -> { AlertHelper.showInfo("Eskalation", count + " überfällige Tickets wurden eskaliert."); loadDashboardData(); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Tickets konnten nicht eskaliert werden.\n" + ex.getMessage()));
            }
        }, "admin-escalate").start();
    }

    private void switchTab(javafx.scene.Node paneToShow, HBox navActive, Circle dotActive, Label labelActive, String crumbTitle) {
        List<javafx.scene.Node> panes = List.of(paneDashboard, paneTickets, paneUsers, paneCategories, paneReports, paneAuditLog, paneSystemAuditLog);
        panes.forEach(p -> { p.setVisible(false); p.setManaged(false); });
        paneToShow.setVisible(true);
        paneToShow.setManaged(true);

        List<HBox> navs = List.of(navDashboard, navTickets, navUsers, navCategories, navReports, navAuditLog, navSystemAuditLog);
        navs.forEach(n -> n.getStyleClass().remove("nav-item-active"));

        List<Circle> dots = List.of(dotDashboard, dotTickets, dotUsers, dotCategories, dotReports, dotAuditLog, dotSystemAuditLog);
        dots.forEach(d -> d.setFill(javafx.scene.paint.Color.web("#475569")));

        List<Label> labels = List.of(labelDashboard, labelTickets, labelUsers, labelCategories, labelReports, labelAuditLog, labelSystemAuditLog);
        labels.forEach(l -> { l.getStyleClass().remove("text-primary"); if (!l.getStyleClass().contains("text-secondary")) l.getStyleClass().add("text-secondary"); });

        navActive.getStyleClass().add("nav-item-active");
        dotActive.setFill(javafx.scene.paint.Color.web("#0EA5E9"));
        labelActive.getStyleClass().remove("text-secondary");
        labelActive.getStyleClass().add("text-primary");

        breadcrumb.setText("Admin  /  " + crumbTitle);
    }

    // KAT-115: fx:id-Felder für den Doppelklick-Schutz beim Export
    @FXML private Button exportCsvBtn;
    @FXML private Button exportPdfBtn;

    @FXML public void handleExportCsv() {
        if (exportCsvBtn != null) exportCsvBtn.setDisable(true);
        exportFile("tickets.csv", () -> dashboardService.exportTicketsCsv(exportStatus(), exportPriority(), exportSearch()), exportCsvBtn);
    }
    @FXML public void handleExportPdf() {
        if (exportPdfBtn != null) exportPdfBtn.setDisable(true);
        exportFile("tickets.pdf", () -> dashboardService.exportTicketsPdf(exportStatus(), exportPriority(), exportSearch()), exportPdfBtn);
    }

    // KAT-131: Audit-Log CSV/PDF-Export
    @FXML private Button auditExportCsvBtn;
    @FXML private Button auditExportPdfBtn;

    @FXML public void handleExportAuditLogCsv() {
        if (auditExportCsvBtn != null) auditExportCsvBtn.setDisable(true);
        exportFile("audit-log.csv", dashboardService::exportAuditLogCsv, auditExportCsvBtn);
    }
    @FXML public void handleExportAuditLogPdf() {
        if (auditExportPdfBtn != null) auditExportPdfBtn.setDisable(true);
        exportFile("audit-log.pdf", dashboardService::exportAuditLogPdf, auditExportPdfBtn);
    }

    private String exportStatus() { return exportStatusCombo == null ? null : exportStatusCombo.getValue(); }
    private String exportPriority() { return exportPriorityCombo == null ? null : exportPriorityCombo.getValue(); }
    private String exportSearch() { return exportSearchField == null ? null : exportSearchField.getText(); }

    private void exportFile(String defaultName, ExportSupplier supplier, Button triggerBtn) {
        Task<byte[]> task = new Task<>() {
            @Override protected byte[] call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> {
            // KAT-115: Button erst nach Abschluss des Hintergrund-Requests wieder freigeben
            if (triggerBtn != null) triggerBtn.setDisable(false);
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(defaultName);
            if (defaultName.endsWith(".csv")) chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Datei", "*.csv"));
            if (defaultName.endsWith(".pdf")) chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Datei", "*.pdf"));
            File file = chooser.showSaveDialog(paneReports.getScene().getWindow());
            if (file != null) {
                try {
                    Files.write(file.toPath(), task.getValue());
                    AlertHelper.showInfo("Export erfolgreich", "Datei wurde gespeichert: " + file.getName());
                } catch (Exception ex) {
                    AlertHelper.showError("Export fehlgeschlagen", ex.getMessage());
                }
            }
        });
        task.setOnFailed(e -> {
            if (triggerBtn != null) triggerBtn.setDisable(false);
            AlertHelper.showError("Export fehlgeschlagen", task.getException().getMessage());
        });
        new Thread(task, "admin-export").start();
    }


    private void loadKnowledgeBaseAdmin() {
        if (kbAdminList == null) return;
        Task<List<KnowledgeBaseFX>> task = new Task<>() {
            @Override protected List<KnowledgeBaseFX> call() throws Exception { return knowledgeBaseService.search(""); }
        };
        task.setOnSucceeded(e -> kbAdminList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Knowledge Base konnte nicht geladen werden."));
        new Thread(task, "admin-load-kb").start();
    }

    @FXML public void handleAddKnowledgeBase() {
        String title = text(kbTitleField);
        String category = text(kbCategoryField);
        String solution = area(kbSolutionArea);
        if (title.isBlank() || category.isBlank() || solution.isBlank()) {
            AlertHelper.showError("Fehler", "Titel, Kategorie und Lösung sind Pflichtfelder.");
            return;
        }
        new Thread(() -> {
            try {
                knowledgeBaseService.create(title, category, solution, text(kbKeywordsField), area(kbTemplateArea));
                Platform.runLater(() -> { clearKnowledgeBaseForm(); loadKnowledgeBaseAdmin(); AlertHelper.showInfo("Erfolg", "Knowledge-Base-Artikel wurde erstellt."); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Artikel konnte nicht erstellt werden.\n" + ex.getMessage()));
            }
        }, "admin-add-kb").start();
    }

    @FXML public void handleDeleteKnowledgeBase() {
        KnowledgeBaseFX selected = kbAdminList == null ? null : kbAdminList.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Fehler", "Bitte Artikel auswählen."); return; }
        // KAT-114: Löschen war bisher ohne Bestätigung möglich
        if (!AlertHelper.showConfirm("Artikel löschen", "Möchten Sie den Artikel \"" + selected.getTitle() + "\" wirklich löschen?", "Löschen")) {
            return;
        }
        new Thread(() -> {
            try {
                knowledgeBaseService.delete(selected.getId());
                Platform.runLater(() -> { loadKnowledgeBaseAdmin(); AlertHelper.showInfo("Erfolg", "Artikel wurde deaktiviert."); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Artikel konnte nicht gelöscht werden.\n" + ex.getMessage()));
            }
        }, "admin-delete-kb").start();
    }

    private void clearKnowledgeBaseForm() {
        if (kbTitleField != null) kbTitleField.clear();
        if (kbCategoryField != null) kbCategoryField.clear();
        if (kbKeywordsField != null) kbKeywordsField.clear();
        if (kbSolutionArea != null) kbSolutionArea.clear();
        if (kbTemplateArea != null) kbTemplateArea.clear();
    }

    private void loadWorkflowOptionsAdmin() {
        if (statusOptionList == null || priorityOptionList == null) return;
        Task<List<WorkflowOptionFX>> statusTask = new Task<>() {
            @Override protected List<WorkflowOptionFX> call() throws Exception { return workflowOptionService.get("STATUS"); }
        };
        statusTask.setOnSucceeded(e -> statusOptionList.setItems(FXCollections.observableArrayList(statusTask.getValue())));
        new Thread(statusTask, "admin-status-options").start();

        Task<List<WorkflowOptionFX>> priorityTask = new Task<>() {
            @Override protected List<WorkflowOptionFX> call() throws Exception { return workflowOptionService.get("PRIORITY"); }
        };
        priorityTask.setOnSucceeded(e -> priorityOptionList.setItems(FXCollections.observableArrayList(priorityTask.getValue())));
        new Thread(priorityTask, "admin-priority-options").start();
    }

    @FXML public void handleAddStatusOption() { addWorkflowOption("STATUS", statusNameField, statusLabelField); }
    @FXML public void handleAddPriorityOption() { addWorkflowOption("PRIORITY", priorityNameField, priorityLabelField); }
    @FXML public void handleDeleteStatusOption() { deleteWorkflowOption(statusOptionList); }
    @FXML public void handleDeletePriorityOption() { deleteWorkflowOption(priorityOptionList); }

    private void addWorkflowOption(String type, TextField nameField, TextField labelField) {
        String name = text(nameField);
        String label = text(labelField);
        if (name.isBlank()) { AlertHelper.showError("Fehler", "Name ist erforderlich."); return; }
        new Thread(() -> {
            try {
                workflowOptionService.create(type, name, label.isBlank() ? name : label, 99);
                Platform.runLater(() -> { if (nameField != null) nameField.clear(); if (labelField != null) labelField.clear(); loadWorkflowOptionsAdmin(); AlertHelper.showInfo("Erfolg", type + " wurde hinzugefügt."); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Workflow-Option konnte nicht erstellt werden.\n" + ex.getMessage()));
            }
        }, "admin-add-workflow-option").start();
    }

    private void deleteWorkflowOption(ListView<WorkflowOptionFX> list) {
        WorkflowOptionFX selected = list == null ? null : list.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Fehler", "Bitte Option auswählen."); return; }
        // KAT-114: Löschen war bisher ohne Bestätigung möglich (gilt für Status- und Prioritäts-Optionen)
        if (!AlertHelper.showConfirm("Option löschen", "Möchten Sie die Option \"" + selected + "\" wirklich deaktivieren?", "Deaktivieren")) {
            return;
        }
        new Thread(() -> {
            try {
                workflowOptionService.delete(selected.getId());
                Platform.runLater(() -> { loadWorkflowOptionsAdmin(); AlertHelper.showInfo("Erfolg", "Option wurde deaktiviert."); });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Workflow-Option konnte nicht deaktiviert werden.\n" + ex.getMessage()));
            }
        }, "admin-delete-workflow-option").start();
    }

    private String text(TextField field) { return field == null || field.getText() == null ? "" : field.getText().trim(); }
    private String area(TextArea field) { return field == null || field.getText() == null ? "" : field.getText().trim(); }
    @FXML private StackPane notificationButton;

    private interface ExportSupplier { byte[] get() throws Exception; }

    // [Nzchupa | 2026-06-12] TS-007: Profil als Modal öffnen — Dashboard bleibt im Hintergrund
    // Open Profile as modal window so the admin dashboard is not replaced
    // [Nzchupa | 2026-06-13] TSS-005: Avatar nach Profil-Modal-Schließen aktualisieren
    // Refresh topbar/sidebar avatar after profile modal closes (picks up new picture from SessionManager)
    @FXML public void handleProfile() {
        Navigator.openModal("ProfileView.fxml", "Profil & Sicherheit",
                () -> updateAvatarDisplay(SessionManager.getProfilePicture()));
    }
    // [Nzchupa | 2026-06-13] Logout-Bestätigung — verhindert versehentliches Ausloggen
    // Logout confirmation dialog to prevent accidental logouts
    @FXML public void handleLogout() {
        if (AlertHelper.showConfirm("Abmelden", "Möchten Sie sich wirklich abmelden?", "Abmelden")) {
            Navigator.logout();
        }
    }

    // [Nzchupa | 2026-06-13] TSS-002: themeToggleBtn für dynamisches Icon-Update
    // Theme toggle button reference for dynamic icon update
    @FXML private Button themeToggleBtn;

    // [Nzchupa | 2026-06-12] TS-002: Theme-Toggle im Topbar — Dark/Light Mode umschalten
    // Theme toggle in topbar — switches dark/light mode and applies it to the current view
    @FXML public void handleToggleTheme() {
        ThemeManager.toggle();
        // getRoot() gibt bereits Parent zurück — kein Cast nötig / getRoot() already returns Parent, no cast needed
        ThemeManager.apply(notificationButton.getScene().getRoot());
        // [Nzchupa | 2026-06-13] TSS-002: Icon nach Theme-Wechsel aktualisieren
        // Update icon after theme switch
        if (themeToggleBtn != null) themeToggleBtn.setText(ThemeManager.isDarkMode() ? "☀" : "🌙");
    }

    @FXML
    private void handleNotifications(MouseEvent event) {
        new Thread(() -> {
            try {
                List<NotificationFX> notifications = notificationService.getMyNotifications();
                // [Nzchupa | 2026-06-12] TS-001: reloadCallback übergeben — Badge-Counter wird nach "Alle gelesen" aktualisiert
                // Pass reloadCallback so the unread badge refreshes after "Alle gelesen" is clicked
                Platform.runLater(() ->
                        NotificationPopup.show((Node) event.getSource(), notifications, this::loadUnreadNotifications)
                );
            } catch (Exception ex) {
                Platform.runLater(() ->
                        AlertHelper.showError("Fehler", "Benachrichtigungen konnten nicht geladen werden.")
                );
            }
        }, "admin-load-notifications-popup").start();
    }
    private void updateAdminTicketStatistics(List<TicketFX> tickets) {
        if (tickets == null) {
            adminTotalTicketsLabel.setText("0");
            adminOpenTicketsLabel.setText("0");
            adminProgressTicketsLabel.setText("0");
            adminWaitingTicketsLabel.setText("0");
            adminResolvedTicketsLabel.setText("0");
            return;
        }

        long total = tickets.size();

        long open = tickets.stream()
                .filter(t -> "OPEN".equalsIgnoreCase(t.getStatus()))
                .count();

        long progress = tickets.stream()
                .filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                .count();

        long waiting = tickets.stream()
                .filter(t -> "WAITING".equalsIgnoreCase(t.getStatus()))
                .count();

        long resolved = tickets.stream()
                .filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()))
                .count();

        adminTotalTicketsLabel.setText(String.valueOf(total));
        adminOpenTicketsLabel.setText(String.valueOf(open));
        adminProgressTicketsLabel.setText(String.valueOf(progress));
        adminWaitingTicketsLabel.setText(String.valueOf(waiting));
        adminResolvedTicketsLabel.setText(String.valueOf(resolved));
    }
    @FXML private PieChart statusPieChart;
    @FXML private PieChart priorityPieChart;

    @FXML private Label statusChartTotalLabel;
    @FXML private Label priorityChartTotalLabel;

    @FXML private Label legendResolved;
    @FXML private Label legendOpen;
    @FXML private Label legendOverdue;
    @FXML private Label legendTotal;

    @FXML private Label legendCritical;
    @FXML private Label legendHigh;
    @FXML private Label legendMedium;
    @FXML private Label legendLow;
}