package com.ticketsystem.frontend.controller;

import com.ticketsystem.dto.request.CreateTicketRequest;
import com.ticketsystem.frontend.model.CategoryFX;
import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.model.TicketFX;
import com.ticketsystem.frontend.service.CategoryApiService;
import com.ticketsystem.frontend.service.NotificationApiService;
import com.ticketsystem.frontend.service.TicketApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.AvatarHelper;
import com.ticketsystem.frontend.util.LabelHelper;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.NotificationPopup;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.frontend.util.ThemeManager;
import com.ticketsystem.model.enums.TicketPriority;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomerController {

    @FXML private BorderPane rootPane;

    @FXML private ScrollPane paneOverview;
    @FXML private VBox paneMyTickets;
    @FXML private ScrollPane paneNewTicket;

    @FXML private HBox navOverview;
    @FXML private HBox navMyTickets;
    @FXML private HBox navNewTicket;

    @FXML private Circle dotOverview;
    @FXML private Circle dotMyTickets;
    @FXML private Circle dotNewTicket;

    @FXML private Label labelOverview;
    @FXML private Label labelMyTickets;
    @FXML private Label labelNewTicket;
    @FXML private Label breadcrumb;

    @FXML private Button themeToggleButton;
    @FXML private Label ticketCounterLabel;

    @FXML private Label sidebarInitials;
    @FXML private Label sidebarName;
    @FXML private Label topbarInitials;
    @FXML private Label greetingLabel;
    @FXML private Label notificationCountLabel;

    @FXML private ImageView sidebarProfileImage;
    @FXML private ImageView topbarProfileImage;

    @FXML private Circle sidebarAvatarBackground;
    @FXML private Circle topbarAvatarBackground;

    @FXML private Label statTotal;
    @FXML private Label statOpen;
    @FXML private Label statProgress;
    @FXML private Label statResolved;

    @FXML private VBox activeTicketsContainer;
    @FXML private VBox activityContainer;

    @FXML private TextField quickTitleField;
    @FXML private ComboBox<TicketPriority> quickPriorityCombo;

    @FXML private TableView<TicketFX> ticketTable;

    @FXML private TableColumn<TicketFX, String> colId;
    @FXML private TableColumn<TicketFX, String> colTitle;
    @FXML private TableColumn<TicketFX, String> colPriority;
    @FXML private TableColumn<TicketFX, String> colStatus;
    @FXML private TableColumn<TicketFX, String> colAgent;
    @FXML private TableColumn<TicketFX, String> colCreatedAt;
    @FXML private TableColumn<TicketFX, String> colUpdatedAt;

    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private TextField searchField;

    @FXML private TextField newTitleField;
    @FXML private TextArea newDescField;
    @FXML private ComboBox<TicketPriority> newPriorityCombo;
    @FXML private ComboBox<CategoryFX> newCategoryCombo;

    @FXML private TextField newFirstName;
    @FXML private TextField newLastName;
    @FXML private TextField newEmail;
    @FXML private TextField newAttachmentNameField;

    @FXML private Label newErrorLabel;

    private final TicketApiService ticketService = new TicketApiService();
    private final CategoryApiService categoryService = new CategoryApiService();
    private final NotificationApiService notificationService = new NotificationApiService();

    // [Nzchupa | 2026-06-13] TSS-007: Pfad der ausgewählten Anhangsdatei — wird beim Ticket-Erstellen mitgeschickt
    // Path of the selected attachment file — sent along when creating the ticket
    private String selectedAttachmentPath = null;

    private final ObservableList<TicketFX> ticketData = FXCollections.observableArrayList();

    // [Nzchupa | 2026-06-13] Lokales darkMode-Flag entfernt — ThemeManager wird jetzt verwendet
    // Removed local darkMode flag — ThemeManager is the single source of truth
    private List<TicketFX> latestTickets = List.of();

    @FXML
    public void initialize() {
        String username = SessionManager.getUsername();
        if (username == null || username.isBlank()) username = "customer";

        String initial = username.substring(0, 1).toUpperCase();
        sidebarInitials.setText(initial);
        topbarInitials.setText(initial);
        sidebarName.setText(username);
        greetingLabel.setText("Hallo, " + username + "! 👋");

        updateAvatarDisplay(SessionManager.getProfilePicture());

        if (newFirstName != null) newFirstName.setText(username);
        if (newEmail != null) newEmail.setText("");

        quickPriorityCombo.getItems().setAll(TicketPriority.values());

        // Aufgabe 16: "Automatisch ermitteln" als erste Option in der Priority-ComboBox
        newPriorityCombo.getItems().clear();
        newPriorityCombo.getItems().add(null); // null = automatisch ermitteln
        newPriorityCombo.getItems().addAll(TicketPriority.values());
        // [Nzchupa | 2026-06-13] TSS-004: Deutsche Prioritätsbezeichnungen in der ComboBox anzeigen
        // Show German priority labels instead of raw enum names (CRITICAL → Kritisch, etc.)
        newPriorityCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TicketPriority item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "Automatisch ermitteln" : LabelHelper.priorityToGerman(item.name()));
            }
        });
        newPriorityCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TicketPriority item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "Automatisch ermitteln" : LabelHelper.priorityToGerman(item.name()));
            }
        });
        newPriorityCombo.setValue(null); // Standard: automatisch

        initTheme();
        initFilters();
        initTable();

        loadCategories();
        loadUnreadNotifications();
        showOverview();
    }

    private void updateAvatarDisplay(String profilePictureUrl) {
        AvatarHelper.showAvatar(profilePictureUrl, sidebarProfileImage, sidebarAvatarBackground, sidebarInitials, 32);
        AvatarHelper.showAvatar(profilePictureUrl, topbarProfileImage, topbarAvatarBackground, topbarInitials, 28);
    }

    // [Nzchupa | 2026-06-13] ThemeManager statt lokalem darkMode — Konsistenz mit AlertHelper + anderen Views
    // Using ThemeManager so theme state is shared across Profile, Alerts and all views
    private void initTheme() {
        if (rootPane == null) return;
        ThemeManager.apply(rootPane);
        updateThemeButton();
    }

    @FXML
    public void handleToggleTheme() {
        if (rootPane == null) return;
        ThemeManager.toggle();
        ThemeManager.apply(rootPane);
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (themeToggleButton != null) themeToggleButton.setText(ThemeManager.isDarkMode() ? "☀" : "🌙");
    }

    private void initTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        colAgent.setCellValueFactory(cellData -> {
            String agent = cellData.getValue().getAssignedTo();
            return new SimpleStringProperty(agent != null ? agent : "–");
        });

        colStatus.setCellFactory(column -> badgeCell());
        colPriority.setCellFactory(column -> badgeCell());
        ticketTable.setItems(ticketData);

        ticketTable.setOnMouseClicked(e -> {
            TicketFX selected = ticketTable.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                TicketDetailController.setCurrentTicketId(selected.getId());
                Navigator.navigateTo("TicketDetailView.fxml");
            }
        });
    }

    private void initFilters() {
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().setAll("Alle", "OPEN", "IN_PROGRESS", "WAITING", "RESOLVED", "CLOSED");
            filterStatusCombo.setValue("Alle");
            // [Nzchupa | 2026-06-13] TSS-003: Deutsche Labels in Status-Filter anzeigen
            // Show German labels in filter combo; keep enum names as values so filtering still works
            javafx.util.Callback<javafx.scene.control.ListView<String>, ListCell<String>> statusCF =
                lv -> new ListCell<>() { @Override protected void updateItem(String s, boolean e) { super.updateItem(s, e); setText(e || s == null ? null : "Alle".equals(s) ? "Alle" : LabelHelper.statusToGerman(s)); } };
            filterStatusCombo.setCellFactory(statusCF);
            filterStatusCombo.setButtonCell(statusCF.call(null));
            // [Nzchupa | 2026-06-13] Echtzeit-Filter und Suche — sofortige Reaktion ohne Button
            // Real-time filter and search listeners
            filterStatusCombo.valueProperty().addListener((obs, old, val) -> applyFilter());
        }
        if (filterPriorityCombo != null) {
            filterPriorityCombo.getItems().setAll("Alle", "CRITICAL", "HIGH", "MEDIUM", "LOW");
            filterPriorityCombo.setValue("Alle");
            // [Nzchupa | 2026-06-13] TSS-004: Deutsche Labels in Priorität-Filter anzeigen
            // Show German labels in priority filter combo
            javafx.util.Callback<javafx.scene.control.ListView<String>, ListCell<String>> priorityCF =
                lv -> new ListCell<>() { @Override protected void updateItem(String s, boolean e) { super.updateItem(s, e); setText(e || s == null ? null : "Alle".equals(s) ? "Alle" : LabelHelper.priorityToGerman(s)); } };
            filterPriorityCombo.setCellFactory(priorityCF);
            filterPriorityCombo.setButtonCell(priorityCF.call(null));
            filterPriorityCombo.valueProperty().addListener((obs, old, val) -> applyFilter());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilter());
        }
    }

    private TableCell<TicketFX, String> badgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                if (!empty && item != null && !item.isBlank()) setGraphic(createBadge(item));
            }
        };
    }

    private Label createBadge(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        Label badge = new Label();
        badge.getStyleClass().add("badge");
        // [Nzchupa | 2026-06-13] Badge-Text vereinheitlicht — alle Controller verwenden jetzt "● " Präfix
        // Unified badge text — all controllers now use "● " prefix for consistency
        switch (value) {
            case "OPEN"        -> { badge.getStyleClass().add("badge-open");      badge.setText("● Offen"); }
            case "IN_PROGRESS" -> { badge.getStyleClass().add("badge-progress");  badge.setText("● In Bearbeitung"); }
            case "WAITING"     -> { badge.getStyleClass().add("badge-waiting");   badge.setText("● Wartend"); }
            case "RESOLVED"    -> { badge.getStyleClass().add("badge-resolved");  badge.setText("● Gelöst"); }
            case "CLOSED"      -> { badge.getStyleClass().add("badge-closed");    badge.setText("● Geschlossen"); }
            case "CRITICAL"    -> { badge.getStyleClass().add("badge-critical");  badge.setText("● Kritisch"); }
            case "HIGH"        -> { badge.getStyleClass().add("badge-high");      badge.setText("● Hoch"); }
            case "MEDIUM"      -> { badge.getStyleClass().add("badge-medium");    badge.setText("● Mittel"); }
            case "LOW"         -> { badge.getStyleClass().add("badge-low");       badge.setText("● Niedrig"); }
            default            -> { badge.getStyleClass().add("badge-customer");  badge.setText(type != null ? type : "–"); }
        }
        return badge;
    }

    private void loadCategories() {
        Task<List<CategoryFX>> task = new Task<>() {
            @Override protected List<CategoryFX> call() throws Exception { return categoryService.getAllCategories(); }
        };
        task.setOnSucceeded(e -> { if (newCategoryCombo != null) newCategoryCombo.getItems().setAll(task.getValue()); });
        new Thread(task, "customer-load-categories").start();
    }

    // [Nzchupa | 2026-06-13] Loading-Spinner während Datenladen — bessere UX
    private void loadTickets() {
        ticketTable.setPlaceholder(buildLoadingNode());
        Task<List<TicketFX>> task = new Task<>() {
            @Override protected List<TicketFX> call() throws Exception { return ticketService.getAllTickets(); }
        };
        task.setOnSucceeded(e -> {
            latestTickets = task.getValue() == null ? List.of() : task.getValue();
            ticketTable.setPlaceholder(buildEmptyNode("Keine Tickets gefunden."));
            updateDashboardStats(latestTickets);
            updateTicketCounter(latestTickets);
            updateActiveTickets(latestTickets);
            updateActivities(latestTickets);
            applyFilter();
        });
        task.setOnFailed(e -> {
            latestTickets = List.of();
            ticketTable.setPlaceholder(buildEmptyNode("Fehler beim Laden."));
            updateDashboardStats(latestTickets);
            updateTicketCounter(latestTickets);
            updateActiveTickets(latestTickets);
            updateActivities(latestTickets);
        });
        new Thread(task, "customer-load-tickets").start();
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

    private void updateDashboardStats(List<TicketFX> tickets) {
        long total = tickets.size();
        long open = tickets.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long progress = tickets.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long resolved = tickets.stream().filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())).count();
        statTotal.setText(String.valueOf(total));
        statOpen.setText(String.valueOf(open));
        statProgress.setText(String.valueOf(progress));
        statResolved.setText(String.valueOf(resolved));
    }

    private void updateTicketCounter(List<TicketFX> tickets) {
        if (ticketCounterLabel == null) return;
        ticketCounterLabel.setText(String.valueOf(tickets.size()));
        ticketCounterLabel.setVisible(!tickets.isEmpty());
        ticketCounterLabel.setManaged(!tickets.isEmpty());
    }

    private void updateActiveTickets(List<TicketFX> tickets) {
        if (activeTicketsContainer == null) return;
        activeTicketsContainer.getChildren().clear();
        List<TicketFX> activeTickets = tickets.stream()
                .filter(t -> !"CLOSED".equalsIgnoreCase(t.getStatus()))
                .limit(5).toList();
        if (activeTickets.isEmpty()) { activeTicketsContainer.getChildren().add(createEmptyState()); return; }
        for (TicketFX ticket : activeTickets) activeTicketsContainer.getChildren().add(createTicketCard(ticket));
    }

    private Node createEmptyState() {
        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.getStyleClass().add("empty-state");
        Label icon = new Label("📭"); icon.getStyleClass().add("empty-state-icon");
        Label title = new Label("Noch keine aktiven Tickets"); title.getStyleClass().add("empty-state-title");
        Label text = new Label("Erstellen Sie ein neues Ticket, wenn Sie Hilfe brauchen."); text.getStyleClass().add("empty-state-text");
        Button button = new Button("Ticket erstellen"); button.getStyleClass().add("btn-primary"); button.setOnAction(e -> showNewTicket());
        emptyBox.getChildren().addAll(icon, title, text, button);
        return emptyBox;
    }

    private Node createTicketCard(TicketFX ticket) {
        VBox card = new VBox(6);
        card.getStyleClass().add("customer-ticket-card");
        HBox titleRow = new HBox(8); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(nullSafe(ticket.getTitle())); title.getStyleClass().add("customer-ticket-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label idLabel = new Label(shortId(ticket.getId())); idLabel.getStyleClass().add("text-muted");
        titleRow.getChildren().addAll(title, spacer, idLabel);
        Label description = new Label(shortText(ticket.getDescription(), 90));
        description.getStyleClass().add("customer-ticket-description"); description.setWrapText(true);
        HBox badgeRow = new HBox(8);
        badgeRow.getChildren().addAll(createBadge(ticket.getStatus()), createBadge(ticket.getPriority()));
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().isBlank()) {
            Label agent = new Label("Agent: " + ticket.getAssignedTo()); agent.getStyleClass().add("text-muted");
            badgeRow.getChildren().add(agent);
        }
        card.getChildren().addAll(titleRow, description, badgeRow);
        card.setOnMouseClicked(e -> { TicketDetailController.setCurrentTicketId(ticket.getId()); Navigator.navigateTo("TicketDetailView.fxml"); });
        return card;
    }

    private void updateActivities(List<TicketFX> tickets) {
        if (activityContainer == null) return;
        activityContainer.getChildren().clear();
        if (tickets.isEmpty()) {
            Label empty = new Label("Keine Aktivitäten verfügbar"); empty.getStyleClass().add("text-muted");
            activityContainer.getChildren().add(empty); return;
        }
        tickets.stream().limit(4).forEach(ticket -> {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("activity-row");
            Label icon = new Label(activityIcon(ticket.getStatus())); icon.getStyleClass().add("activity-icon");
            VBox texts = new VBox(2);
            Label title = new Label(activityText(ticket)); title.getStyleClass().add("activity-title");
            Label time = new Label(nullSafe(ticket.getUpdatedAt())); time.getStyleClass().add("activity-time");
            texts.getChildren().addAll(title, time);
            row.getChildren().addAll(icon, texts);
            activityContainer.getChildren().add(row);
        });
    }

    private String activityIcon(String status) {
        if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) return "✓";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "↻";
        if ("WAITING".equalsIgnoreCase(status)) return "…";
        return "+";
    }

    private String activityText(TicketFX ticket) {
        String status = ticket.getStatus();
        if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) return "Ticket wurde gelöst";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "Ticket ist in Bearbeitung";
        if ("WAITING".equalsIgnoreCase(status)) return "Ticket wartet auf Rückmeldung";
        return "Ticket wurde erstellt";
    }

    @FXML public void handleApplyFilter() { applyFilter(); }

    @FXML
    public void handleClearFilter() {
        filterStatusCombo.setValue("Alle");
        filterPriorityCombo.setValue("Alle");
        searchField.clear();
        applyFilter();
    }

    private void applyFilter() {
        if (ticketTable == null) return;
        String status = filterStatusCombo == null ? "Alle" : filterStatusCombo.getValue();
        String priority = filterPriorityCombo == null ? "Alle" : filterPriorityCombo.getValue();
        String search = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<TicketFX> filtered = latestTickets.stream()
                .filter(t -> status == null || "Alle".equals(status) || status.equals(t.getStatus()))
                .filter(t -> priority == null || "Alle".equals(priority) || priority.equals(t.getPriority()))
                .filter(t -> search.isBlank()
                        || containsIgnoreCase(t.getTitle(), search)
                        || containsIgnoreCase(t.getTicketNumber(), search)
                        || containsIgnoreCase(t.getAssignedTo(), search)
                        || containsIgnoreCase(t.getCategoryName(), search))
                .collect(Collectors.toList());
        ticketData.setAll(filtered);
    }

    private boolean containsIgnoreCase(String text, String search) {
        return text != null && text.toLowerCase().contains(search);
    }

    @FXML
    public void handleQuickCreate() {
        if (quickTitleField.getText() == null || quickTitleField.getText().trim().isEmpty()
                || quickPriorityCombo.getValue() == null) return;
        String title = quickTitleField.getText().trim();
        String desc = "Schnell-Ticket: " + title;
        checkDuplicatesAndCreate(title, desc, () -> doQuickSubmit(title));
    }

    private void doQuickSubmit(String title) {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setTitle(title);
        req.setDescription("Schnell-Ticket: " + title);
        req.setPriority(quickPriorityCombo.getValue());
        doCreateTicket(req);
    }

    @FXML
    public void handleCreateFullTicket() {
        if (text(newTitleField).isBlank() || text(newDescField).isBlank()) {
            newErrorLabel.setVisible(true);
            return;
        }
        newErrorLabel.setVisible(false);
        String title = text(newTitleField);
        String desc = text(newDescField);
        checkDuplicatesAndCreate(title, desc, () -> submitFullTicket(title, desc));
    }

    private void checkDuplicatesAndCreate(String title, String desc, Runnable createAction) {
        new Thread(() -> {
            try {
                List<TicketFX> duplicates = ticketService.findDuplicates(title, desc);
                List<TicketFX> similar = ticketService.findSimilar(title, desc);
                Platform.runLater(() -> {
                    if (!duplicates.isEmpty()) showDuplicateDialog("Mögliche Duplikate gefunden", duplicates, createAction);
                    else if (!similar.isEmpty()) showDuplicateDialog("Ähnliche Tickets gefunden", similar, createAction);
                    else createAction.run();
                });
            } catch (Exception e) {
                Platform.runLater(createAction);
            }
        }, "customer-duplicate-check").start();
    }

    // [Nzchupa | 2026-06-13] Nativer Alert.CONFIRMATION ersetzt durch AlertHelper.showConfirm
    // Replaced native CONFIRMATION Alert with styled AlertHelper confirm dialog
    private void showDuplicateDialog(String title, List<TicketFX> tickets, Runnable createAction) {
        String names = tickets.stream().map(t -> "• " + t.getTitle()).limit(3).collect(Collectors.joining("\n"));
        boolean confirmed = AlertHelper.showConfirm(title, names + "\n\nTrotzdem ein neues Ticket erstellen?", "Erstellen");
        if (confirmed) createAction.run();
    }

    private void submitFullTicket(String title, String desc) {
        Map<String, Object> req = new HashMap<>();
        req.put("title", title);
        req.put("description", desc);

        // Aufgabe 16: Priority nur senden wenn explizit ausgewählt, sonst null = automatisch
        TicketPriority selectedPriority = newPriorityCombo.getValue();
        if (selectedPriority != null) {
            req.put("priority", selectedPriority);
        }

        if (newCategoryCombo.getValue() != null) req.put("categoryId", newCategoryCombo.getValue().getId());
        // [Nzchupa | 2026-06-13] TSS-007: Echten Datei-Pfad verwenden wenn per FileChooser ausgewählt
        // Use real file path from FileChooser if available, otherwise fall back to manual entry
        if (newAttachmentNameField != null && !text(newAttachmentNameField).isBlank()) {
            String attachmentName = text(newAttachmentNameField);
            req.put("attachmentName", attachmentName);
            req.put("attachmentPath", selectedAttachmentPath != null ? selectedAttachmentPath : "demo-attachments/" + attachmentName);
        }
        doCreateTicket(req);
    }

    private void doCreateTicket(Object req) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception { ticketService.createTicket(req); return null; }
        };
        task.setOnSucceeded(e -> {
            AlertHelper.showInfo("Erfolg", "Ticket erfolgreich erstellt.");
            resetNewTicketForm();
            showMyTickets();
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Ticket erstellen fehlgeschlagen.\n" + task.getException().getMessage()));
        new Thread(task, "customer-create-ticket").start();
    }

    @FXML public void showOverview() {
        if (!confirmDiscardNewTicketIfNeeded()) return;
        switchTab(paneOverview, navOverview, dotOverview, labelOverview, "Übersicht");
        loadTickets();
    }

    @FXML public void showMyTickets() {
        if (!confirmDiscardNewTicketIfNeeded()) return;
        switchTab(paneMyTickets, navMyTickets, dotMyTickets, labelMyTickets, "Meine Tickets");
        loadTickets();
    }

    @FXML public void showNewTicket() {
        switchTab(paneNewTicket, navNewTicket, dotNewTicket, labelNewTicket, "Neues Ticket");
    }

    private void switchTab(Node paneToShow, HBox navActive, Circle dotActive, Label labelActive, String crumbTitle) {
        paneOverview.setVisible(false); paneOverview.setManaged(false);
        paneMyTickets.setVisible(false); paneMyTickets.setManaged(false);
        paneNewTicket.setVisible(false); paneNewTicket.setManaged(false);
        paneToShow.setVisible(true); paneToShow.setManaged(true);
        navOverview.getStyleClass().remove("nav-item-active");
        navMyTickets.getStyleClass().remove("nav-item-active");
        navNewTicket.getStyleClass().remove("nav-item-active");
        dotOverview.setFill(javafx.scene.paint.Color.web("#475569"));
        dotMyTickets.setFill(javafx.scene.paint.Color.web("#475569"));
        dotNewTicket.setFill(javafx.scene.paint.Color.web("#475569"));
        resetNavLabel(labelOverview);
        resetNavLabel(labelMyTickets);
        resetNavLabel(labelNewTicket);
        navActive.getStyleClass().add("nav-item-active");
        dotActive.setFill(javafx.scene.paint.Color.web("#0EA5E9"));
        labelActive.getStyleClass().remove("text-secondary");
        if (!labelActive.getStyleClass().contains("text-primary")) labelActive.getStyleClass().add("text-primary");
        breadcrumb.setText("Customer  /  " + crumbTitle);
    }

    private void resetNavLabel(Label label) {
        label.getStyleClass().remove("text-primary");
        if (!label.getStyleClass().contains("text-secondary")) label.getStyleClass().add("text-secondary");
    }

    private boolean confirmDiscardNewTicketIfNeeded() {
        if (paneNewTicket != null && paneNewTicket.isVisible() && hasUnsavedNewTicketInput()) {
            return AlertHelper.confirmDiscardUnsavedChanges();
        }
        return true;
    }

    private boolean hasUnsavedNewTicketInput() {
        return !text(newTitleField).isBlank()
                || !text(newDescField).isBlank()
                || newPriorityCombo.getValue() != null
                || newCategoryCombo.getValue() != null
                || !text(newAttachmentNameField).isBlank()
                || !text(newLastName).isBlank()
                || !text(newFirstName).equals(SessionManager.getUsername())
                || !text(newEmail).isBlank();
    }

    private void resetNewTicketForm() {
        if (quickTitleField != null) quickTitleField.clear();
        if (newTitleField != null) newTitleField.clear();
        if (newDescField != null) newDescField.clear();
        if (newPriorityCombo != null) newPriorityCombo.setValue(null); // zurück auf "Automatisch ermitteln"
        if (newCategoryCombo != null) newCategoryCombo.setValue(null);
        if (newAttachmentNameField != null) newAttachmentNameField.clear();
        selectedAttachmentPath = null; // [Nzchupa | 2026-06-13] TSS-007: Datei-Pfad zurücksetzen
        if (newLastName != null) newLastName.clear();
        if (newFirstName != null) newFirstName.setText(SessionManager.getUsername());
        if (newEmail != null) newEmail.clear();
        if (newErrorLabel != null) newErrorLabel.setVisible(false);
    }

    private void loadUnreadNotifications() {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception { return notificationService.getUnreadCount(); }
        };
        task.setOnSucceeded(e -> notificationCountLabel.setText(String.valueOf(task.getValue())));
        task.setOnFailed(e -> notificationCountLabel.setText("0"));
        new Thread(task, "customer-notification-count").start();
    }

    // [Nzchupa | 2026-06-13] Alte handleShowNotifications / showNotificationDialog entfernt — war toter Code
    // Removed old native-Alert notification dialog — NotificationPopup (handleNotifications) is used instead

    @FXML
    private void handleNotifications(MouseEvent event) {
        new Thread(() -> {
            try {
                List<NotificationFX> notifications = notificationService.getMyNotifications();
                // [Nzchupa | 2026-06-12] TS-001: reloadCallback übergeben — Badge wird nach "Alle gelesen" aktualisiert
                Platform.runLater(() -> NotificationPopup.show((Node) event.getSource(), notifications, this::loadUnreadNotifications));
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Benachrichtigungen konnten nicht geladen werden."));
            }
        }, "customer-load-notifications-popup").start();
    }

    // [Nzchupa | 2026-06-13] TSS-007: FileChooser für Ticket-Anhang — Datei direkt auswählen
    // Opens a FileChooser so the customer can select a file to attach to the new ticket
    @FXML public void handleBrowseAttachment() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Anhang auswählen");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        Stage stage = (Stage) newAttachmentNameField.getScene().getWindow();
        java.io.File file = fc.showOpenDialog(stage);
        if (file != null) {
            selectedAttachmentPath = file.getAbsolutePath();
            newAttachmentNameField.setText(file.getName());
        }
    }

    // [Nzchupa | 2026-06-12] TS-007: Profil als Modal öffnen — Customer-Dashboard bleibt im Hintergrund
    // [Nzchupa | 2026-06-13] TSS-005: Avatar nach Schließen aktualisieren
    // Refresh topbar/sidebar avatar after profile modal closes
    @FXML public void handleProfile() {
        if (!confirmDiscardNewTicketIfNeeded()) return;
        Navigator.openModal("ProfileView.fxml", "Profil & Sicherheit",
            () -> updateAvatarDisplay(SessionManager.getProfilePicture()));
    }

    // [Nzchupa | 2026-06-13] Logout-Bestätigung — verhindert versehentliches Ausloggen
    // Logout confirmation: first check unsaved ticket, then ask to confirm logout
    @FXML public void handleLogout() {
        if (!confirmDiscardNewTicketIfNeeded()) return;
        if (AlertHelper.showConfirm("Abmelden", "Möchten Sie sich wirklich abmelden?", "Abmelden")) {
            Navigator.logout();
        }
    }

    private String text(TextInputControl control) {
        return control == null || control.getText() == null ? "" : control.getText().trim();
    }

    private String nullSafe(Object value) { return value == null ? "" : value.toString(); }

    private String shortId(String id) {
        if (id == null || id.isBlank()) return "#";
        return "#" + (id.length() > 6 ? id.substring(0, 6) : id);
    }

    private String shortText(String text, int maxLength) {
        if (text == null || text.isBlank()) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}