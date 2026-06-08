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
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.NotificationPopup;

import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
    @FXML private Label breadcrumb;

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

    @FXML private TableView<TicketFX> ticketTable;
    @FXML private TableColumn<TicketFX, String> colId, colTitle, colPriority, colStatus, colCategory, colAgent, colCreatedAt;
    @FXML private ComboBox<String> filterStatusCombo, filterPriorityCombo;
    @FXML private TextField searchField;

    @FXML private TableView<UserFX> userTable;
    @FXML private TableColumn<UserFX, String> colUsername, colEmail, colRole;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label selectedUserLabel;

    @FXML private ListView<CategoryFX> categoryList;
    @FXML private TextField categoryNameField;
    @FXML private TextArea categoryDescField;

    @FXML private TableView<AuditLogFX> auditLogTable;
    @FXML private TableColumn<AuditLogFX, String> auditColTicket, auditColUser, auditColType, auditColOld, auditColNew, auditColTime;

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

        initTable();
        initFiltersAndUserActions();
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

        auditColTicket.setCellValueFactory(new PropertyValueFactory<>("ticketTitle"));
        auditColUser.setCellValueFactory(new PropertyValueFactory<>("changedBy"));
        auditColType.setCellValueFactory(new PropertyValueFactory<>("changeType"));
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
    }

    private void initFiltersAndUserActions() {
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().setAll("Alle", TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING.name(), TicketStatus.RESOLVED.name(), TicketStatus.CLOSED.name());
            filterStatusCombo.setValue("Alle");
        }
        if (filterPriorityCombo != null) {
            filterPriorityCombo.getItems().setAll("Alle", TicketPriority.CRITICAL.name(), TicketPriority.HIGH.name(), TicketPriority.MEDIUM.name(), TicketPriority.LOW.name());
            filterPriorityCombo.setValue("Alle");
        }
        if (roleCombo != null) {
            roleCombo.getItems().setAll(UserRole.CUSTOMER.name(), UserRole.AGENT.name(), UserRole.ADMIN.name());
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
            });
        }
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
        if (dashboardLoading) return;
        dashboardLoading = true;

        Task<List<TicketFX>> ticketsTask = new Task<>() {
            @Override protected List<TicketFX> call() throws Exception { return ticketService.getAllTickets(); }
        };
        ticketsTask.setOnSucceeded(e -> dashTicketTable.setItems(FXCollections.observableArrayList(ticketsTask.getValue().stream().limit(8).toList())));
        ticketsTask.setOnFailed(e -> AlertHelper.showError("Fehler", "Tickets konnten nicht geladen werden. Läuft das Backend auf Port 8080?"));
        new Thread(ticketsTask, "admin-dashboard-tickets").start();

        Task<DashboardStatsFX> statsTask = new Task<>() {
            @Override protected DashboardStatsFX call() throws Exception { return dashboardService.getStats(); }
        };
        statsTask.setOnCancelled(e -> dashboardLoading = false);
        statsTask.setOnSucceeded(e -> { applyStats(statsTask.getValue()); dashboardLoading = false; });
        statsTask.setOnFailed(e -> {
            dashboardLoading = false;
            AlertHelper.showError("Fehler", "Dashboard-Statistik konnte nicht geladen werden. Bitte als Admin anmelden und Backend prüfen.");
        });
        new Thread(statsTask, "admin-dashboard-stats").start();
    }

    private void applyStats(DashboardStatsFX stats) {
        statTotal.setText(String.valueOf(stats.getTotalTickets()));
        statOpen.setText(String.valueOf(stats.getOpenTickets()));
        statResolvedToday.setText(String.valueOf(stats.getResolvedToday()));
        if (statCreatedToday != null) statCreatedToday.setText(String.valueOf(stats.getCreatedToday()));
        if (statOverdue != null) statOverdue.setText(String.valueOf(stats.getOverdueTickets()));
        if (statEscalated != null) statEscalated.setText(String.valueOf(stats.getEscalatedTickets()));
        if (statAvgResolution != null) statAvgResolution.setText(stats.getAverageResolutionHours() + " h");
        long critical = value(stats.getTicketsByPriority(), "CRITICAL");
        statCritical.setText(String.valueOf(critical));
        setPriorityBar(stats.getTicketsByPriority(), "CRITICAL", lblCriticalCount, progressCritical, stats.getTotalTickets());
        setPriorityBar(stats.getTicketsByPriority(), "HIGH", lblHighCount, progressHigh, stats.getTotalTickets());
        setPriorityBar(stats.getTicketsByPriority(), "MEDIUM", lblMediumCount, progressMedium, stats.getTotalTickets());
        setPriorityBar(stats.getTicketsByPriority(), "LOW", lblLowCount, progressLow, stats.getTotalTickets());
    }

    private void setPriorityBar(Map<String, Long> map, String key, Label label, ProgressBar progressBar, long total) {
        long count = value(map, key);
        label.setText(String.valueOf(count));
        progressBar.setProgress(total == 0 ? 0 : (double) count / total);
    }

    private long value(Map<String, Long> map, String key) {
        return map == null ? 0 : map.getOrDefault(key, 0L);
    }

    private void loadTickets() {
        Task<List<TicketFX>> task = new Task<>() {
            @Override
            protected List<TicketFX> call() throws Exception {
                return ticketService.getAllTickets();
            }
        };

        task.setOnSucceeded(e -> {
            allTickets.setAll(task.getValue());
            updateAdminTicketStatistics(allTickets);
            applyTicketFilter();
        });

        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Tickets konnten nicht geladen werden."));

        new Thread(task, "admin-load-tickets").start();
    }

    @FXML public void handleApplyTicketFilter() { applyTicketFilter(); }
    @FXML public void handleClearTicketFilter() {
        filterStatusCombo.setValue("Alle");
        filterPriorityCombo.setValue("Alle");
        searchField.clear();
        applyTicketFilter();
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
        task.setOnSucceeded(e -> userTable.setItems(FXCollections.observableArrayList(task.getValue())));
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
        task.setOnSucceeded(e -> auditLogTable.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Audit-Log konnte nicht geladen werden."));
        new Thread(task, "admin-load-audit").start();

        // Feature 32 – System-Audit-Log parallel laden
        loadSystemAuditLogs();
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

    @FXML public void handleShowNotifications() {
        new Thread(() -> {
            try {
                List<NotificationFX> notifications = notificationService.getMyNotifications();
                Platform.runLater(() -> showNotificationDialog(notifications));
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Benachrichtigungen konnten nicht geladen werden."));
            }
        }, "admin-load-notifications").start();
    }

    private void showNotificationDialog(List<NotificationFX> notifications) {
        ListView<NotificationFX> list = new ListView<>(FXCollections.observableArrayList(notifications));
        list.setPrefSize(520, 320);
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(NotificationFX item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.isRead() ? "✓ " : "● ") + item.getTitle() + "\n" + item.getMessage());
            }
        });
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Benachrichtigungen");
        alert.setHeaderText(notifications.isEmpty() ? "Keine Benachrichtigungen" : "Meine Benachrichtigungen");
        alert.getDialogPane().setContent(list);
        alert.showAndWait();
        markShownNotificationsAsRead(notifications);
    }

    private void markShownNotificationsAsRead(List<NotificationFX> notifications) {
        new Thread(() -> {
            for (NotificationFX notification : notifications) {
                try {
                    if (!notification.isRead()) notificationService.markAsRead(notification.getId());
                } catch (Exception ignored) { }
            }
            Platform.runLater(this::loadUnreadNotifications);
        }, "mark-notifications-read").start();
    }

    @FXML public void showDashboard() { switchTab(paneDashboard, navDashboard, dotDashboard, labelDashboard, "Dashboard"); loadDashboardData(); }
    @FXML public void showTickets() { switchTab(paneTickets, navTickets, dotTickets, labelTickets, "Alle Tickets"); loadTickets(); }
    @FXML public void showUsers() { switchTab(paneUsers, navUsers, dotUsers, labelUsers, "Benutzer"); loadUsers(); }
    @FXML public void showCategories() { switchTab(paneCategories, navCategories, dotCategories, labelCategories, "Kategorien"); loadCategories(); }
    @FXML 
    public void showReports() {
        switchTab(paneReports, navReports, dotReports, labelReports, "Berichte");

        if (paneReports != null) {
            paneReports.setVvalue(0.0);
        }

        loadKnowledgeBaseAdmin();
        loadWorkflowOptionsAdmin();
    }
    @FXML public void showAuditLog() { switchTab(paneAuditLog, navAuditLog, dotAuditLog, labelAuditLog, "Audit-Log"); loadAuditLogs(); }
    // Feature 32 – System-Aktivitätsprotokoll
    @FXML public void showSystemAuditLog() { switchTab(paneSystemAuditLog, navSystemAuditLog, dotSystemAuditLog, labelSystemAuditLog, "Aktivitätsprotokoll"); loadSystemAuditLogs(); }
    @FXML public void handleRefreshDashboard() { loadDashboardData(); loadUnreadNotifications(); }

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

    @FXML public void handleExportCsv() { exportFile("tickets.csv", () -> dashboardService.exportTicketsCsv(exportStatus(), exportPriority(), exportSearch())); }
    @FXML public void handleExportPdf() { exportFile("tickets.pdf", () -> dashboardService.exportTicketsPdf(exportStatus(), exportPriority(), exportSearch())); }

    private String exportStatus() { return exportStatusCombo == null ? null : exportStatusCombo.getValue(); }
    private String exportPriority() { return exportPriorityCombo == null ? null : exportPriorityCombo.getValue(); }
    private String exportSearch() { return exportSearchField == null ? null : exportSearchField.getText(); }

    private void exportFile(String defaultName, ExportSupplier supplier) {
        Task<byte[]> task = new Task<>() {
            @Override protected byte[] call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> {
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
        task.setOnFailed(e -> AlertHelper.showError("Export fehlgeschlagen", task.getException().getMessage()));
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

    private interface ExportSupplier { byte[] get() throws Exception; }

    @FXML public void handleProfile() { Navigator.navigateTo("ProfileView.fxml"); }
    @FXML public void handleLogout() { Navigator.logout(); }
    
    @FXML
    private void handleNotifications(MouseEvent event) {
        new Thread(() -> {
            try {
                List<NotificationFX> notifications = notificationService.getMyNotifications();
                Platform.runLater(() ->
                        NotificationPopup.show((Node) event.getSource(), notifications)
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
}
