package com.ticketsystem.frontend.controller;

import com.ticketsystem.dto.request.CreateTicketRequest;
import com.ticketsystem.frontend.model.CategoryFX;
import com.ticketsystem.frontend.model.TicketFX;
import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.service.CategoryApiService;
import com.ticketsystem.frontend.service.TicketApiService;
import com.ticketsystem.frontend.service.NotificationApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.AvatarHelper;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomerController {

    @FXML private ScrollPane paneOverview;
    @FXML private VBox paneMyTickets;
    @FXML private ScrollPane paneNewTicket;

    @FXML private HBox navOverview, navMyTickets, navNewTicket;
    @FXML private Circle dotOverview, dotMyTickets, dotNewTicket;
    @FXML private Label labelOverview, labelMyTickets, labelNewTicket;
    @FXML private Label breadcrumb;

    @FXML private Label sidebarInitials, sidebarName, topbarInitials, greetingLabel, notificationCountLabel;
    @FXML private ImageView sidebarProfileImage, topbarProfileImage;
    @FXML private Circle sidebarAvatarBackground, topbarAvatarBackground;

    @FXML private Label statTotal, statOpen, statProgress, statResolved;
    @FXML private VBox activeTicketsContainer;

    @FXML private TableView<TicketFX> ticketTable;
    @FXML private TableColumn<TicketFX, String> colId, colTitle, colPriority, colStatus, colAgent, colCreatedAt, colUpdatedAt;

    @FXML private ComboBox<String> filterStatusCombo, filterPriorityCombo;
    @FXML private TextField searchField;

    @FXML private TextField quickTitleField;
    @FXML private ComboBox<TicketPriority> quickPriorityCombo;

    @FXML private TextField newTitleField, newFirstName, newLastName, newEmail, newAttachmentNameField;
    @FXML private TextArea newDescField;
    @FXML private ComboBox<TicketPriority> newPriorityCombo;
    @FXML private ComboBox<CategoryFX> newCategoryCombo;
    @FXML private Label newErrorLabel;

    private final TicketApiService ticketService = new TicketApiService();
    private final CategoryApiService categoryService = new CategoryApiService();
    private final NotificationApiService notificationService = new NotificationApiService();
    private ObservableList<TicketFX> ticketData = FXCollections.observableArrayList();
    private List<TicketFX> latestTickets = List.of();

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !SessionManager.hasRole(UserRole.CUSTOMER)) {
            Navigator.navigateToLogin();
            return;
        }

        String username = SessionManager.getUsername();
        String initial = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "U";
        sidebarInitials.setText(initial);
        topbarInitials.setText(initial);
        sidebarName.setText(username);
        greetingLabel.setText("Hallo, " + username + "!");
        updateAvatarDisplay(SessionManager.getProfilePicture());

        newFirstName.setText(username);
        newEmail.setText("email@test.com"); // Dummy for now

        quickPriorityCombo.getItems().setAll(TicketPriority.values());
        newPriorityCombo.getItems().setAll(TicketPriority.values());
        initFilters();

        initTable();
        showOverview();
        loadCategories();
        loadUnreadNotifications();
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
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        colAgent.setCellValueFactory(cellData -> {
            String agent = cellData.getValue().getAssignedTo();
            return new javafx.beans.property.SimpleStringProperty(agent != null ? agent : "–");
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else setGraphic(createBadge(item));
            }
        });

        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else setGraphic(createBadge(item));
            }
        });

        ticketTable.setItems(ticketData);
        ticketTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && ticketTable.getSelectionModel().getSelectedItem() != null) {
                TicketDetailController.setCurrentTicketId(ticketTable.getSelectionModel().getSelectedItem().getId());
                Navigator.navigateTo("TicketDetailView.fxml");
            }
        });
    }

    private void initFilters() {
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().setAll("Alle", "OPEN", "IN_PROGRESS", "WAITING", "RESOLVED", "CLOSED");
            filterStatusCombo.setValue("Alle");
        }
        if (filterPriorityCombo != null) {
            filterPriorityCombo.getItems().setAll("Alle", "CRITICAL", "HIGH", "MEDIUM", "LOW");
            filterPriorityCombo.setValue("Alle");
        }
    }

    private Label createBadge(String type) {
        Label badge = new Label(type);
        badge.getStyleClass().add("badge");
        switch (type.toUpperCase()) {
            case "OPEN": badge.getStyleClass().add("badge-open"); badge.setText("Offen"); break;
            case "IN_PROGRESS": badge.getStyleClass().add("badge-progress"); badge.setText("In Bearbeitung"); break;
            case "WAITING": badge.getStyleClass().add("badge-waiting"); badge.setText("Wartend"); break;
            case "RESOLVED": badge.getStyleClass().add("badge-resolved"); badge.setText("Gelöst"); break;
            case "CLOSED": badge.getStyleClass().add("badge-closed"); badge.setText("Geschlossen"); break;
            case "CRITICAL": badge.getStyleClass().add("badge-critical"); badge.setText("Kritisch"); break;
            case "HIGH": badge.getStyleClass().add("badge-high"); badge.setText("Hoch"); break;
            case "MEDIUM": badge.getStyleClass().add("badge-medium"); badge.setText("Mittel"); break;
            case "LOW": badge.getStyleClass().add("badge-low"); badge.setText("Niedrig"); break;
        }
        return badge;
    }

    private void loadCategories() {
        Task<List<CategoryFX>> task = new Task<>() {
            @Override
            protected List<CategoryFX> call() throws Exception {
                return categoryService.getAllCategories();
            }
        };
        task.setOnSucceeded(e -> newCategoryCombo.getItems().setAll(task.getValue()));
        new Thread(task).start();
    }

    private void loadTickets() {
        Task<List<TicketFX>> task = new Task<>() {
            @Override
            protected List<TicketFX> call() throws Exception {
                return ticketService.getAllTickets();
            }
        };
        task.setOnSucceeded(e -> {
            List<TicketFX> tickets = task.getValue();
            latestTickets = tickets;
            applyFilter();

            // Dummy Stats
            statTotal.setText(String.valueOf(tickets.size()));
            statOpen.setText(String.valueOf(tickets.stream().filter(t -> "OPEN".equals(t.getStatus())).count()));
            statProgress.setText(String.valueOf(tickets.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count()));
            statResolved.setText(String.valueOf(tickets.stream().filter(t -> "RESOLVED".equals(t.getStatus()) || "CLOSED".equals(t.getStatus())).count()));

            activeTicketsContainer.getChildren().clear();
            tickets.stream().filter(t -> !"CLOSED".equals(t.getStatus())).limit(5).forEach(t -> {
                VBox card = new VBox(5);
                card.getStyleClass().add("ticket-card");

                String borderColor = switch(t.getPriority()) {
                    case "CRITICAL" -> "#EF4444";
                    case "HIGH" -> "#F59E0B";
                    case "MEDIUM" -> "#38BDF8";
                    default -> "#22C55E";
                };
                card.setStyle("-fx-border-color: transparent transparent transparent " + borderColor + "; -fx-border-width: 0 0 0 3;");

                HBox row1 = new HBox(new Label(t.getTitle()));
                ((Label)row1.getChildren().get(0)).setStyle("-fx-font-size: 14px; -fx-text-fill: #F1F5F9; -fx-font-weight: bold;");
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                Label idLabel = new Label("#" + (t.getId().length() > 6 ? t.getId().substring(0,6) : t.getId()));
                idLabel.getStyleClass().add("text-muted");
                row1.getChildren().addAll(spacer, idLabel);

                Label descLabel = new Label(t.getDescription() != null && t.getDescription().length() > 60 ? t.getDescription().substring(0, 60) + "..." : t.getDescription());
                descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

                HBox row3 = new HBox(8);
                row3.getChildren().addAll(createBadge(t.getStatus()), createBadge(t.getPriority()));
                if (t.getAssignedTo() != null) {
                    Label agentLabel = new Label(" Agent: " + t.getAssignedTo());
                    agentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                    row3.getChildren().add(agentLabel);
                }

                card.getChildren().addAll(row1, descLabel, row3);
                card.setOnMouseClicked(ev -> {
                    TicketDetailController.setCurrentTicketId(t.getId());
                    Navigator.navigateTo("TicketDetailView.fxml");
                });
                activeTicketsContainer.getChildren().add(card);
            });
        });
        task.setOnFailed(e -> {
            // Handle softly
        });
        new Thread(task).start();
    }

    @FXML public void handleApplyFilter() { applyFilter(); }

    @FXML public void handleClearFilter() {
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
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(search))
                        || (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(search))
                        || (t.getAssignedTo() != null && t.getAssignedTo().toLowerCase().contains(search))
                        || (t.getCategoryName() != null && t.getCategoryName().toLowerCase().contains(search)))
                .collect(Collectors.toList());
        ticketData.setAll(filtered);
    }

    @FXML public void handleQuickCreate() {
        if (quickTitleField.getText().isEmpty() || quickPriorityCombo.getValue() == null) return;

        String title = quickTitleField.getText().trim();
        String desc  = "Schnell-Ticket: " + title;

        // Feature 17 & 18 – auch beim Schnellticket prüfen
        new Thread(() -> {
            try {
                List<TicketFX> duplicates = ticketService.findDuplicates(title, desc);
                List<TicketFX> similar    = ticketService.findSimilar(title, desc);
                Platform.runLater(() -> {
                    if (!duplicates.isEmpty()) {
                        String names = duplicates.stream().map(t -> "• " + t.getTitle()).limit(3).collect(Collectors.joining("\n"));
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Mögliche Duplikate gefunden:\n" + names + "\n\nTrotzdem erstellen?",
                                ButtonType.YES, ButtonType.NO);
                        alert.setTitle("Duplikat erkannt");
                        alert.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) doQuickSubmit(title); });
                    } else if (!similar.isEmpty()) {
                        String names = similar.stream().map(t -> "• " + t.getTitle()).limit(3).collect(Collectors.joining("\n"));
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Ähnliche Tickets gefunden:\n" + names + "\n\nTrotzdem ein neues Ticket erstellen?",
                                ButtonType.YES, ButtonType.NO);
                        alert.setTitle("Ähnliche Tickets");
                        alert.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) doQuickSubmit(title); });
                    } else {
                        doQuickSubmit(title);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Feature17/18 DEBUG] Fehler: " + e.getMessage());
                Platform.runLater(() -> doQuickSubmit(title));
            }
        }).start();
    }

    private void doQuickSubmit(String title) {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setTitle(title);
        req.setDescription("Schnell-Ticket: " + title);
        req.setPriority(quickPriorityCombo.getValue());
        doCreateTicket(req);
    }

    @FXML public void handleCreateFullTicket() {
        if (newTitleField.getText().isEmpty() || newDescField.getText().isEmpty() || newPriorityCombo.getValue() == null) {
            newErrorLabel.setVisible(true);
            return;
        }

        String title = newTitleField.getText().trim();
        String desc  = newDescField.getText().trim();

        // Feature 17 & 18 – Ähnliche Tickets + Duplikat-Check VOR dem Erstellen
        new Thread(() -> {
            try {
                List<TicketFX> duplicates = ticketService.findDuplicates(title, desc);
                List<TicketFX> similar    = ticketService.findSimilar(title, desc);
                Platform.runLater(() -> {
                    if (!duplicates.isEmpty()) {
                        String names = duplicates.stream().map(t -> "• " + t.getTitle()).limit(3).collect(Collectors.joining("\n"));
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Mögliche Duplikate gefunden:\n" + names + "\n\nTrotzdem erstellen?",
                                ButtonType.YES, ButtonType.NO);
                        alert.setTitle("Duplikat erkannt");
                        alert.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) submitFullTicket(title, desc); });
                    } else if (!similar.isEmpty()) {
                        String names = similar.stream().map(t -> "• " + t.getTitle()).limit(3).collect(Collectors.joining("\n"));
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Ähnliche Tickets gefunden:\n" + names + "\n\nTrotzdem ein neues Ticket erstellen?",
                                ButtonType.YES, ButtonType.NO);
                        alert.setTitle("Ähnliche Tickets");
                        alert.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) submitFullTicket(title, desc); });
                    } else {
                        submitFullTicket(title, desc);
                    }
                });
            } catch (Exception e) {
                // Bei Fehler einfach direkt erstellen
                Platform.runLater(() -> {
                    System.err.println("[Feature17/18 DEBUG] Fehler: " + e.getMessage());
                    e.printStackTrace();
                    submitFullTicket(title, desc);
                });
            }
        }).start();
    }

    private void submitFullTicket(String title, String desc) {
        Map<String, Object> req = new HashMap<>();
        req.put("title", title);
        req.put("description", desc);
        req.put("priority", newPriorityCombo.getValue());
        if (newCategoryCombo.getValue() != null) req.put("categoryId", newCategoryCombo.getValue().getId());
        if (newAttachmentNameField != null && !newAttachmentNameField.getText().trim().isBlank()) {
            String attachmentName = newAttachmentNameField.getText().trim();
            req.put("attachmentName", attachmentName);
            req.put("attachmentPath", "demo-attachments/" + attachmentName);
        }
        doCreateTicket(req);
    }

    private void doCreateTicket(Object req) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ticketService.createTicket(req);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            AlertHelper.showInfo("Erfolg", "Ticket erfolgreich erstellt.");
            quickTitleField.clear();
            resetNewTicketForm();
            newErrorLabel.setVisible(false);
            showMyTickets();
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Ticket erstellen fehlgeschlagen.\n" + task.getException().getMessage()));
        new Thread(task).start();
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
    @FXML public void showNewTicket() { switchTab(paneNewTicket, navNewTicket, dotNewTicket, labelNewTicket, "Neues Ticket"); }

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
                || !text(newEmail).equals("email@test.com");
    }

    private String text(TextInputControl control) {
        return control == null || control.getText() == null ? "" : control.getText().trim();
    }

    private void resetNewTicketForm() {
        if (newTitleField != null) newTitleField.clear();
        if (newDescField != null) newDescField.clear();
        if (newPriorityCombo != null) newPriorityCombo.setValue(null);
        if (newCategoryCombo != null) newCategoryCombo.setValue(null);
        if (newAttachmentNameField != null) newAttachmentNameField.clear();
        if (newLastName != null) newLastName.clear();
        if (newFirstName != null) newFirstName.setText(SessionManager.getUsername());
        if (newEmail != null) newEmail.setText("email@test.com");
    }

    private void switchTab(javafx.scene.Node paneToShow, HBox navActive, Circle dotActive, Label labelActive, String crumbTitle) {
        paneOverview.setVisible(false); paneMyTickets.setVisible(false); paneNewTicket.setVisible(false);
        paneToShow.setVisible(true);

        navOverview.getStyleClass().remove("nav-item-active"); navMyTickets.getStyleClass().remove("nav-item-active"); navNewTicket.getStyleClass().remove("nav-item-active");
        dotOverview.setFill(javafx.scene.paint.Color.web("#475569")); dotMyTickets.setFill(javafx.scene.paint.Color.web("#475569")); dotNewTicket.setFill(javafx.scene.paint.Color.web("#475569"));
        labelOverview.getStyleClass().remove("text-primary"); labelOverview.getStyleClass().add("text-secondary");
        labelMyTickets.getStyleClass().remove("text-primary"); labelMyTickets.getStyleClass().add("text-secondary");
        labelNewTicket.getStyleClass().remove("text-primary"); labelNewTicket.getStyleClass().add("text-secondary");

        navActive.getStyleClass().add("nav-item-active");
        dotActive.setFill(javafx.scene.paint.Color.web("#0EA5E9"));
        labelActive.getStyleClass().remove("text-secondary"); labelActive.getStyleClass().add("text-primary");

        breadcrumb.setText("Customer  /  " + crumbTitle);
    }

    private void loadUnreadNotifications() {
        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception { return notificationService.getUnreadCount(); }
        };
        task.setOnSucceeded(e -> notificationCountLabel.setText(String.valueOf(task.getValue())));
        task.setOnFailed(e -> notificationCountLabel.setText("0"));
        new Thread(task, "customer-notification-count").start();
    }

    @FXML public void handleShowNotifications() {
        new Thread(() -> {
            try {
                List<NotificationFX> notifications = notificationService.getMyNotifications();
                Platform.runLater(() -> showNotificationDialog(notifications));
            } catch (Exception ex) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Benachrichtigungen konnten nicht geladen werden."));
            }
        }, "customer-load-notifications").start();
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

    @FXML public void handleProfile() {
        if (!confirmDiscardNewTicketIfNeeded()) return;
        Navigator.navigateTo("ProfileView.fxml");
    }

    @FXML public void handleLogout() {
        if (!confirmDiscardNewTicketIfNeeded()) return;
        Navigator.logout();
    }
}
