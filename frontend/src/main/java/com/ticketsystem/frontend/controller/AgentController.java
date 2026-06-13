package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.model.KnowledgeBaseFX;
import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.model.TicketFX;
import com.ticketsystem.frontend.service.KnowledgeBaseApiService;
import com.ticketsystem.frontend.service.NotificationApiService;
import com.ticketsystem.frontend.service.TicketApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.AvatarHelper;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.NotificationPopup;
import com.ticketsystem.frontend.util.ThemeManager;
import com.ticketsystem.frontend.util.ToastHelper;

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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.stream.Collectors;

public class AgentController {

    @FXML private ScrollPane paneMyTickets;
    @FXML private VBox paneAllTickets, paneKnowledgeBase;

    @FXML private HBox navMyTickets, navAllTickets, navKnowledgeBase;
    @FXML private Circle dotMyTickets, dotAllTickets, dotKnowledgeBase;
    @FXML private Label labelMyTickets, labelAllTickets, labelKnowledgeBase;
    @FXML private Label breadcrumb;

    // [Nzchupa | 2026-06-12] TS-002: notificationButton für Theme-Toggle-Root benötigt
    @FXML private StackPane notificationButton;
    @FXML private Label sidebarInitials, sidebarName, topbarInitials, notificationCountLabel;
    @FXML private ImageView sidebarProfileImage, topbarProfileImage;
    @FXML private Circle sidebarAvatarBackground, topbarAvatarBackground;
    @FXML private Label statAssigned;
    @FXML private Label statResolvedToday;
    @FXML private Label statOpenTotal;
    @FXML private Label statAvgRating;
    @FXML private Label sidebarBadge;
    @FXML private ProgressBar perfResolvedBar;
    @FXML private ProgressBar perfOpenBar;
    @FXML private Label perfResolvedLabel;
    @FXML private Label perfOpenLabel;
    @FXML private VBox assignedTicketsContainer;

    @FXML private TableView<TicketFX> ticketTable;
    @FXML private TableColumn<TicketFX, String> colId, colTitle, colPriority, colStatus, colCreator, colAgent, colCreatedAt;
    @FXML private ComboBox<String> filterStatusCombo, filterPriorityCombo;
    @FXML private TextField searchField;
    @FXML private TextField kbSearchField;
    @FXML private ListView<KnowledgeBaseFX> kbList;

    private final TicketApiService ticketService = new TicketApiService();
    private final NotificationApiService notificationService = new NotificationApiService();
    private final KnowledgeBaseApiService knowledgeBaseService = new KnowledgeBaseApiService();
    private final ObservableList<TicketFX> allTicketData = FXCollections.observableArrayList();
    private List<TicketFX> latestTickets = List.of();

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !SessionManager.hasRole(UserRole.AGENT)) {
            Navigator.navigateToLogin();
            return;
        }

        String username = SessionManager.getUsername();
        String initial = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "A";
        sidebarInitials.setText(initial);
        topbarInitials.setText(initial);
        sidebarName.setText(username);
        updateAvatarDisplay(SessionManager.getProfilePicture());

        initTable();
        initFilters();
        loadUnreadNotifications();
        showMyTickets();
    }

    private void updateAvatarDisplay(String profilePictureUrl) {
        AvatarHelper.showAvatar(profilePictureUrl, sidebarProfileImage, sidebarAvatarBackground, sidebarInitials, 32);
        AvatarHelper.showAvatar(profilePictureUrl, topbarProfileImage, topbarAvatarBackground, topbarInitials, 28);
    }

    private void initTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCreator.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        colAgent.setCellValueFactory(cellData -> {
            String agent = cellData.getValue().getAssignedTo();
            return new javafx.beans.property.SimpleStringProperty(agent != null ? agent : "–");
        });

        colStatus.setCellFactory(column -> badgeTableCell());
        colPriority.setCellFactory(column -> badgeTableCell());

        ticketTable.setItems(allTicketData);
        ticketTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && ticketTable.getSelectionModel().getSelectedItem() != null) {
                TicketDetailController.setCurrentTicketId(ticketTable.getSelectionModel().getSelectedItem().getId());
                Navigator.navigateTo("TicketDetailView.fxml");
            }
        });
    }

    private TableCell<TicketFX, String> badgeTableCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : createBadge(item));
            }
        };
    }

    private void initFilters() {
        filterStatusCombo.getItems().setAll("Alle", TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING.name(), TicketStatus.RESOLVED.name(), TicketStatus.CLOSED.name());
        filterStatusCombo.setValue("Alle");
        // [Nzchupa | 2026-06-13] Echtzeit-Filter und Suche — sofortige Reaktion ohne Button
        // Real-time filter and search listeners — no need to press "Suchen"
        filterStatusCombo.valueProperty().addListener((obs, old, val) -> applyFilter());
        filterPriorityCombo.getItems().setAll("Alle", TicketPriority.CRITICAL.name(), TicketPriority.HIGH.name(), TicketPriority.MEDIUM.name(), TicketPriority.LOW.name());
        filterPriorityCombo.setValue("Alle");
        filterPriorityCombo.valueProperty().addListener((obs, old, val) -> applyFilter());
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilter());
        }
    }

    // [Nzchupa | 2026-06-13] Null-Check hinzugefügt — verhindert NPE wenn type == null
    // Added null-check to prevent NPE; unified badge text with Admin/Customer (● prefix)
    private Label createBadge(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        Label badge = new Label(value);
        badge.getStyleClass().add("badge");
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

    // [Nzchupa | 2026-06-13] Loading-Spinner während Datenladen — bessere UX
    private void loadTickets() {
        ticketTable.setPlaceholder(buildLoadingNode());
        Task<List<TicketFX>> task = new Task<>() {
            @Override protected List<TicketFX> call() throws Exception {
                return ticketService.getAllTickets();
            }
        };
        task.setOnSucceeded(e -> {
            latestTickets = task.getValue();
            ticketTable.setPlaceholder(buildEmptyNode("Keine Tickets gefunden."));
            applyFilter();
            updateAssignedCards(latestTickets);
        });
        task.setOnFailed(e -> {
            ticketTable.setPlaceholder(buildEmptyNode("Fehler beim Laden."));
            AlertHelper.showError("Fehler", "Tickets konnten nicht geladen werden. Backend prüfen.");
        });
        new Thread(task, "agent-load-tickets").start();
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

    private void updateAssignedCards(List<TicketFX> tickets) {
        String me = SessionManager.getUsername();
        String today = java.time.LocalDate.now().toString(); // "yyyy-MM-dd"

        // ── Stat 1: Mir zugewiesen (aktiv, nicht CLOSED/RESOLVED) ──────────────
        List<TicketFX> assigned = tickets.stream()
                .filter(t -> me.equals(t.getAssignedTo()))
                .filter(t -> !"CLOSED".equals(t.getStatus()) && !"RESOLVED".equals(t.getStatus()))
                .collect(Collectors.toList());
        statAssigned.setText(String.valueOf(assigned.size()));
        if (sidebarBadge != null) sidebarBadge.setText(String.valueOf(assigned.size()));

        // ── Stat 2: Heute von mir gelöst ───────────────────────────────────────
        long resolvedToday = tickets.stream()
                .filter(t -> me.equals(t.getAssignedTo()))
                .filter(t -> "RESOLVED".equals(t.getStatus()) || "CLOSED".equals(t.getStatus()))
                .filter(t -> t.getResolvedAt() != null && t.getResolvedAt().startsWith(today))
                .count();
        if (statResolvedToday != null) statResolvedToday.setText(String.valueOf(resolvedToday));

        // ── Stat 3: Alle offenen Tickets (systemweit, nicht nur meine) ─────────
        long openTotal = tickets.stream()
                .filter(t -> "OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()) || "WAITING".equals(t.getStatus()))
                .count();
        if (statOpenTotal != null) statOpenTotal.setText(String.valueOf(openTotal));

        // ── Stat 4: Ø Kundenzufriedenheit meiner gelösten Tickets ──────────────
        java.util.OptionalDouble avgRating = tickets.stream()
                .filter(t -> me.equals(t.getAssignedTo()))
                .filter(t -> t.getFeedbackRating() != null && t.getFeedbackRating() > 0)
                .mapToInt(TicketFX::getFeedbackRating)
                .average();
        if (statAvgRating != null) {
            statAvgRating.setText(avgRating.isPresent()
                    ? String.format("%.1f ★", avgRating.getAsDouble())
                    : "–");
        }

        // ── Performance-Bars: meine Tickets (alle, inkl. gelöste) ──────────────
        long myTotal = tickets.stream().filter(t -> me.equals(t.getAssignedTo())).count();
        if (myTotal > 0) {
            long myResolved = tickets.stream()
                    .filter(t -> me.equals(t.getAssignedTo()))
                    .filter(t -> "RESOLVED".equals(t.getStatus()) || "CLOSED".equals(t.getStatus()))
                    .count();
            long myOpen = myTotal - myResolved;
            double resolvedPct = (double) myResolved / myTotal;
            double openPct     = (double) myOpen / myTotal;
            if (perfResolvedBar   != null) perfResolvedBar.setProgress(resolvedPct);
            if (perfOpenBar       != null) perfOpenBar.setProgress(openPct);
            if (perfResolvedLabel != null) perfResolvedLabel.setText(String.format("%.0f%%", resolvedPct * 100));
            if (perfOpenLabel     != null) perfOpenLabel.setText(String.format("%.0f%%", openPct * 100));
        } else {
            if (perfResolvedBar   != null) perfResolvedBar.setProgress(0);
            if (perfOpenBar       != null) perfOpenBar.setProgress(0);
            if (perfResolvedLabel != null) perfResolvedLabel.setText("0%");
            if (perfOpenLabel     != null) perfOpenLabel.setText("0%");
        }

        // ── Ticket-Karten ──────────────────────────────────────────────────────
        assignedTicketsContainer.getChildren().clear();
        if (assigned.isEmpty()) {
            Label empty = new Label("Keine offenen Tickets — Gut gemacht! 🎉");
            empty.getStyleClass().add("text-muted");
            assignedTicketsContainer.getChildren().add(empty);
            return;
        }

        assigned.stream().limit(6).forEach(t -> {
            VBox card = new VBox(5);
            card.getStyleClass().add("ticket-card");
            String borderColor = switch (t.getPriority()) {
                case "CRITICAL" -> "#EF4444";
                case "HIGH"     -> "#F59E0B";
                case "MEDIUM"   -> "#38BDF8";
                default         -> "#22C55E";
            };
            card.setStyle("-fx-border-color: transparent transparent transparent " + borderColor + "; -fx-border-width: 0 0 0 3;");

            HBox row1 = new HBox(new Label(t.getTitle()));
            ((Label) row1.getChildren().get(0)).setStyle("-fx-font-size: 13px; -fx-text-fill: #F1F5F9; -fx-font-weight: bold;");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Label idLabel = new Label("#" + shortId(t.getId()));
            idLabel.getStyleClass().add("text-muted");
            row1.getChildren().addAll(spacer, idLabel);

            Label descLabel = new Label(t.getDescription() != null && t.getDescription().length() > 80
                    ? t.getDescription().substring(0, 80) + "..." : safe(t.getDescription()));
            descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

            HBox row3 = new HBox(8);
            row3.getChildren().addAll(createBadge(t.getStatus()), createBadge(t.getPriority()));
            Region r3s = new Region(); HBox.setHgrow(r3s, Priority.ALWAYS);
            Button actionBtn = new Button("Öffnen");
            actionBtn.getStyleClass().add("btn-ghost");
            actionBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 10px;");
            row3.getChildren().addAll(r3s, actionBtn);

            card.getChildren().addAll(row1, descLabel, row3);
            card.setOnMouseClicked(ev -> openTicket(t));
            actionBtn.setOnAction(ev -> openTicket(t));
            assignedTicketsContainer.getChildren().add(card);
        });
    }

    private void openTicket(TicketFX t) {
        TicketDetailController.setCurrentTicketId(t.getId());
        Navigator.navigateTo("TicketDetailView.fxml");
    }

    private String shortId(String id) {
        return id != null && id.length() > 6 ? id.substring(0, 6) : safe(id);
    }

    private String safe(String text) { return text == null ? "" : text; }

    @FXML public void handleApplyFilter() { applyFilter(); }

    @FXML public void handleClearFilter() {
        filterStatusCombo.setValue("Alle");
        filterPriorityCombo.setValue("Alle");
        searchField.clear();
        applyFilter();
    }

    private void applyFilter() {
        String status = filterStatusCombo.getValue();
        String priority = filterPriorityCombo.getValue();
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<TicketFX> filtered = latestTickets.stream()
                .filter(t -> status == null || "Alle".equals(status) || status.equals(t.getStatus()))
                .filter(t -> priority == null || "Alle".equals(priority) || priority.equals(t.getPriority()))
                .filter(t -> search.isBlank()
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(search))
                        || (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(search))
                        || (t.getCreatedBy() != null && t.getCreatedBy().toLowerCase().contains(search))
                        || (t.getAssignedTo() != null && t.getAssignedTo().toLowerCase().contains(search))
                        || (t.getCategoryName() != null && t.getCategoryName().toLowerCase().contains(search)))
                .collect(Collectors.toList());
        allTicketData.setAll(filtered);
    }

    @FXML public void showMyTickets() { switchTab(paneMyTickets, navMyTickets, dotMyTickets, labelMyTickets, "Meine Tickets"); loadTickets(); }
    @FXML public void showAllTickets() { switchTab(paneAllTickets, navAllTickets, dotAllTickets, labelAllTickets, "Alle Tickets"); loadTickets(); }
    @FXML public void showKnowledgeBase() { switchTab(paneKnowledgeBase, navKnowledgeBase, dotKnowledgeBase, labelKnowledgeBase, "Wissensdatenbank"); loadKnowledgeBase(); }

    private void switchTab(javafx.scene.Node paneToShow, HBox navActive, Circle dotActive, Label labelActive, String crumbTitle) {
        List<javafx.scene.Node> panes = List.of(paneMyTickets, paneAllTickets, paneKnowledgeBase);
        panes.forEach(p -> { p.setVisible(false); p.setManaged(false); });
        paneToShow.setVisible(true);
        paneToShow.setManaged(true);

        List<HBox> navs = List.of(navMyTickets, navAllTickets, navKnowledgeBase);
        navs.forEach(n -> n.getStyleClass().remove("nav-item-active"));
        List<Circle> dots = List.of(dotMyTickets, dotAllTickets, dotKnowledgeBase);
        dots.forEach(d -> d.setFill(javafx.scene.paint.Color.web("#475569")));
        List<Label> labels = List.of(labelMyTickets, labelAllTickets, labelKnowledgeBase);
        labels.forEach(l -> { l.getStyleClass().remove("text-primary"); if (!l.getStyleClass().contains("text-secondary")) l.getStyleClass().add("text-secondary"); });

        navActive.getStyleClass().add("nav-item-active");
        dotActive.setFill(javafx.scene.paint.Color.web("#0EA5E9"));
        labelActive.getStyleClass().remove("text-secondary");
        labelActive.getStyleClass().add("text-primary");
        breadcrumb.setText("Agent  /  " + crumbTitle);
    }


    @FXML public void handleSearchKnowledgeBase() { loadKnowledgeBase(); }

    private void loadKnowledgeBase() {
        if (kbList == null) return;
        String query = kbSearchField == null ? "" : kbSearchField.getText();
        Task<List<KnowledgeBaseFX>> task = new Task<>() {
            @Override protected List<KnowledgeBaseFX> call() throws Exception { return knowledgeBaseService.search(query); }
        };
        task.setOnSucceeded(e -> {
            kbList.setItems(FXCollections.observableArrayList(task.getValue()));
            kbList.setCellFactory(v -> new ListCell<>() {
                @Override protected void updateItem(KnowledgeBaseFX item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitle() + " [" + item.getCategory() + "]\n" + item.getSolution());
                }
            });
            kbList.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && kbList.getSelectionModel().getSelectedItem() != null) {
                    copyText(kbList.getSelectionModel().getSelectedItem().getAnswerTemplate());
                }
            });
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Wissensdatenbank konnte nicht geladen werden."));
        new Thread(task, "agent-kb-load").start();
    }

    @FXML public void copySelectedKbArticle() {
        if (kbList == null || kbList.getSelectionModel().getSelectedItem() == null) {
            AlertHelper.showError("Fehler", "Bitte zuerst einen Artikel auswählen.");
            return;
        }
        copyText(kbList.getSelectionModel().getSelectedItem().getAnswerTemplate());
    }

    // [Nzchupa | 2026-06-13] Toast statt AlertHelper — kein Modal für einfaches Clipboard-Feedback
    // Use Toast instead of AlertHelper modal for non-blocking clipboard feedback
    private void copyText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        ToastHelper.show(notificationButton, "Text in Zwischenablage kopiert", ToastHelper.ToastType.SUCCESS);
    }

    @FXML public void copyTemplate1() { copyText("Vielen Dank für Ihre Anfrage. Wir arbeiten aktiv an einer Lösung und melden uns mit einem Update."); }
    @FXML public void copyTemplate2() { copyText("Könnten Sie uns bitte weitere Details mitteilen, z.B. Screenshot, Fehlermeldung und Zeitpunkt des Problems?"); }
    @FXML public void copyTemplate3() { copyText("Ihr Ticket wurde erfolgreich gelöst. Bitte melden Sie sich, falls das Problem erneut auftritt."); }
    @FXML public void copyKbSoftware() { copyText("Software prüfen: Neustart durchführen, Version kontrollieren, Fehlermeldung dokumentieren und Ticket auf Software setzen."); }
    @FXML public void copyKbHardware() { copyText("Hardware prüfen: Kabel/Anschluss kontrollieren, Gerät neu starten, Seriennummer notieren und Ticket auf Hardware setzen."); }
    @FXML public void copyKbPassword() { copyText("Passwortproblem: Identität prüfen, Reset-Link senden und den Nutzer bitten, ein neues sicheres Passwort zu setzen."); }

    private void loadUnreadNotifications() {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception { return notificationService.getUnreadCount(); }
        };
        task.setOnSucceeded(e -> notificationCountLabel.setText(String.valueOf(task.getValue())));
        task.setOnFailed(e -> notificationCountLabel.setText("0"));
        new Thread(task, "agent-notification-count").start();
    }

    // [Nzchupa | 2026-06-13] Alte handleShowNotifications / showNotificationDialog entfernt — war toter Code
    // Removed old native-Alert notification dialog — NotificationPopup (handleNotifications) is used instead

    // [Nzchupa | 2026-06-12] TS-007: Profil als Modal öffnen — Dashboard bleibt im Hintergrund
    @FXML public void handleProfile() { Navigator.openModal("ProfileView.fxml", "Profil & Sicherheit"); }
    // [Nzchupa | 2026-06-13] Logout-Bestätigung — verhindert versehentliches Ausloggen
    // Logout confirmation dialog to prevent accidental logouts
    @FXML public void handleLogout() {
        if (AlertHelper.showConfirm("Abmelden", "Möchten Sie sich wirklich abmelden?", "Abmelden")) {
            Navigator.logout();
        }
    }

    // [Nzchupa | 2026-06-12] TS-002: Theme-Toggle im Topbar — Dark/Light Mode umschalten
    @FXML public void handleToggleTheme() {
        ThemeManager.toggle();
        // getRoot() gibt bereits Parent zurück — kein Cast nötig / getRoot() already returns Parent, no cast needed
        ThemeManager.apply(notificationButton.getScene().getRoot());
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
        }, "agent-load-notifications-popup").start();
    }
}
