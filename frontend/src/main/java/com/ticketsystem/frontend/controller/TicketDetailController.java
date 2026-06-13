package com.ticketsystem.frontend.controller;

import com.ticketsystem.dto.request.UpdateTicketRequest;
import com.ticketsystem.frontend.model.AuditLogFX;
import com.ticketsystem.frontend.model.CommentFX;
import com.ticketsystem.frontend.model.TicketFX;
import com.ticketsystem.frontend.model.UserFX;
import com.ticketsystem.frontend.service.AuditLogApiService;
import com.ticketsystem.frontend.service.CommentApiService;
import com.ticketsystem.frontend.service.TicketApiService;
import com.ticketsystem.frontend.service.UserApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.LabelHelper;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.frontend.util.ThemeManager;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TicketDetailController {
    private static String currentTicketId;

    public static void setCurrentTicketId(String id) {
        currentTicketId = id;
    }

    @FXML private Label titleLabel;
    @FXML private Label ticketNumberLabel;
    @FXML private Label statusLabel;
    @FXML private Label priorityLabel;
    @FXML private Label createdByLabel;
    @FXML private Label assignedToLabel;
    @FXML private Label dueDateLabel, slaLabel, solutionLabel, ratingLabel;
    // [Nzchupa | 2026-06-13] TSS-016: Hyperlink statt Label — öffnet Datei per Desktop.open()
    // Hyperlink replaces plain Label so agents/admins can click to open the attachment
    @FXML private javafx.scene.control.Hyperlink attachmentLink;
    @FXML private TextArea descriptionArea;

    @FXML private ComboBox<TicketStatus> statusCombo;
    @FXML private Button updateStatusBtn;
    @FXML private ComboBox<TicketPriority> priorityCombo;
    @FXML private Button updatePriorityBtn;
    @FXML private ComboBox<UserFX> agentCombo;
    @FXML private Button assignAgentBtn;
    @FXML private Button takeTicketBtn, closeWithSolutionBtn, attachBtn, sendFeedbackBtn;
    // Feature 38 – Ticket wiedereröffnen (nur CUSTOMER, sichtbar wenn RESOLVED/CLOSED)
    @FXML private Button reopenTicketBtn;
    @FXML private TextArea solutionReasonArea, feedbackArea;
    @FXML private TextField attachmentNameField;

    // Feature 23 – Stern-Bewertung
    @FXML private Label star1, star2, star3, star4, star5, starValueLabel;
    private int selectedRating = 0;

    @FXML private ListView<CommentFX> commentsList;
    // [Nzchupa | 2026-06-13] timelineContainer ersetzt historyList — visuelle Timeline statt flacher Liste
    // timelineContainer replaces historyList — visual timeline instead of flat ListView
    @FXML private javafx.scene.layout.VBox timelineContainer;
    @FXML private TextArea newCommentArea;
    @FXML private CheckBox internalCheckBox;

    private final TicketApiService ticketService = new TicketApiService();
    private final CommentApiService commentService = new CommentApiService();
    private final UserApiService userService = new UserApiService();
    private final AuditLogApiService auditLogService = new AuditLogApiService();
    private TicketFX currentTicket;

    @FXML
    public void initialize() {
        if (currentTicketId == null || !SessionManager.isLoggedIn()) {
            Navigator.navigateToHome();
            return;
        }

        boolean customer = SessionManager.getRole() == UserRole.CUSTOMER;
        statusCombo.setVisible(!customer);
        updateStatusBtn.setVisible(!customer);
        priorityCombo.setVisible(!customer);
        updatePriorityBtn.setVisible(!customer);
        agentCombo.setVisible(!customer);
        assignAgentBtn.setVisible(!customer);
        internalCheckBox.setVisible(!customer);
        if (takeTicketBtn != null) takeTicketBtn.setVisible(!customer);
        if (closeWithSolutionBtn != null) closeWithSolutionBtn.setVisible(!customer);
        if (solutionReasonArea != null) solutionReasonArea.setVisible(!customer);
        if (attachBtn != null) attachBtn.setVisible(!customer);
        if (sendFeedbackBtn != null) sendFeedbackBtn.setVisible(customer);
        if (feedbackArea != null) feedbackArea.setVisible(customer);
        // Feature 38 – Reopen-Button: nur für Customer, Sichtbarkeit nach Ticket-Load gesetzt
        if (reopenTicketBtn != null) reopenTicketBtn.setVisible(false);
        // Feature 23 – Sterne nur für Customer sichtbar
        for (Label s : new Label[]{star1, star2, star3, star4, star5, starValueLabel}) {
            if (s != null) { s.setVisible(customer); s.setManaged(customer); }
        }

        if (!customer) {
            statusCombo.getItems().setAll(TicketStatus.values());
            priorityCombo.getItems().setAll(TicketPriority.values());
            // [Nzchupa | 2026-06-13] TSS-003/004: Deutsche Bezeichnungen in ComboBoxen anzeigen
            // Show German labels in status and priority ComboBoxes
            statusCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(TicketStatus s) { return s == null ? "" : LabelHelper.statusToGerman(s.name()); }
                @Override public TicketStatus fromString(String s) { return null; }
            });
            priorityCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(TicketPriority p) { return p == null ? "" : LabelHelper.priorityToGerman(p.name()); }
                @Override public TicketPriority fromString(String s) { return null; }
            });
            loadAgents();
        }

        initListCells();
        loadTicket();
    }

    private void initListCells() {
        commentsList.setCellFactory(param -> new ListCell<>() {
            @Override protected void updateItem(CommentFX item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String text = item.getAuthorUsername() + " [" + item.getCreatedAt() + "]:\n" + item.getContent();
                    if (item.isInternal()) text += "\n(INTERN)";
                    setText(text);
                }
            }
        });
        // [Nzchupa | 2026-06-13] historyList.setCellFactory entfernt — buildTimeline() wird jetzt verwendet
        // Removed old cell factory; timeline entries are built programmatically in buildTimeline()
        agentCombo.setCellFactory(param -> userCell());
        agentCombo.setButtonCell(userCell());
    }

    private ListCell<UserFX> userCell() {
        return new ListCell<>() {
            @Override protected void updateItem(UserFX item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername() + " (" + item.getEmail() + ")");
            }
        };
    }

    // [Nzchupa | 2026-06-13] TSS-003/004: Badge-Klasse + deutsches Label dynamisch setzen
    // Applies the correct colored badge CSS class and German display text to a label
    private void applyBadge(javafx.scene.control.Label label, String value) {
        if (label == null || value == null) return;
        label.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        String v = value.trim().toUpperCase();
        String cssClass = switch (v) {
            case "OPEN"        -> "badge-open";
            case "IN_PROGRESS" -> "badge-progress";
            case "WAITING"     -> "badge-waiting";
            case "RESOLVED"    -> "badge-resolved";
            case "CLOSED"      -> "badge-closed";
            case "CRITICAL"    -> "badge-critical";
            case "HIGH"        -> "badge-high";
            case "MEDIUM"      -> "badge-medium";
            case "LOW"         -> "badge-low";
            default            -> "";
        };
        if (!cssClass.isEmpty()) label.getStyleClass().add(cssClass);
        // Спочатку перевіряємо статус, потім пріоритет / Try status translation first, fall back to priority
        String german = LabelHelper.statusToGerman(v);
        if (german.equals(v)) german = LabelHelper.priorityToGerman(v);
        label.setText(german);
    }

    private String safe(String text) {
        return text == null ? "-" : text;
    }

    // [Nzchupa | 2026-06-13] buildTimeline — visuelle Timeline für Ticket-Verlauf
    // Builds visual timeline entries from AuditLog list; replaces the old flat ListView
    private void buildTimeline(List<AuditLogFX> logs) {
        if (timelineContainer == null) return;
        timelineContainer.getChildren().clear();

        if (logs == null || logs.isEmpty()) {
            javafx.scene.control.Label empty = new javafx.scene.control.Label("Keine Änderungen vorhanden.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-padding: 8 12;");
            timelineContainer.getChildren().add(empty);
            return;
        }

        boolean dark = ThemeManager.isDarkMode();
        String lineBg   = dark ? "#334155" : "#E2E8F0";
        String dotBg    = "#0EA5E9";
        String cardBg   = dark ? "#1E293B" : "#F8FAFC";
        String cardBord = dark ? "#334155" : "#E2E8F0";
        String textMain = dark ? "#F1F5F9" : "#0F172A";
        String textSub  = dark ? "#94A3B8" : "#64748B";

        for (int i = 0; i < logs.size(); i++) {
            AuditLogFX log = logs.get(i);
            boolean last = (i == logs.size() - 1);

            // Row: dot column + card column
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
            row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            javafx.geometry.Insets rowPadding = new javafx.geometry.Insets(0, 8, 0, 8);
            javafx.scene.layout.HBox.setMargin(row, rowPadding);

            // Dot + vertical line column
            javafx.scene.layout.VBox dotCol = new javafx.scene.layout.VBox();
            dotCol.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            dotCol.setMinWidth(16);
            dotCol.setMaxWidth(16);

            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
            dot.setStyle("-fx-fill: " + dotBg + ";");

            javafx.scene.layout.VBox dotColInner = new javafx.scene.layout.VBox(dot);
            dotColInner.setAlignment(javafx.geometry.Pos.CENTER);
            dotColInner.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

            if (!last) {
                javafx.scene.layout.Region line = new javafx.scene.layout.Region();
                line.setPrefWidth(2);
                line.setPrefHeight(30);
                line.setMaxWidth(2);
                line.setStyle("-fx-background-color: " + lineBg + ";");
                javafx.scene.layout.VBox.setVgrow(line, javafx.scene.layout.Priority.ALWAYS);
                dotColInner.getChildren().add(line);
            }
            dotCol.getChildren().add(dotColInner);

            // Card
            javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(3);
            card.setPadding(new javafx.geometry.Insets(6, 10, 8, 10));
            card.setStyle(
                "-fx-background-color: " + cardBg + ";" +
                "-fx-border-color: " + cardBord + ";" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
            );
            javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

            // Change type + user
            String changeType = safe(log.getChangeType());
            String icon = iconForChangeType(changeType);
            javafx.scene.control.Label headerLbl = new javafx.scene.control.Label(icon + " " + changeType + "  •  " + safe(log.getChangedBy()));
            headerLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + textMain + ";");

            // old → new value
            String detail = safe(log.getOldValue()) + " → " + safe(log.getNewValue());
            javafx.scene.control.Label detailLbl = new javafx.scene.control.Label(detail);
            detailLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textSub + ";");
            detailLbl.setWrapText(true);

            // timestamp
            String ts = log.getTimestamp() != null
                ? log.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "-";
            javafx.scene.control.Label tsLbl = new javafx.scene.control.Label(ts);
            tsLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + textSub + ";");

            card.getChildren().addAll(headerLbl, detailLbl, tsLbl);
            row.getChildren().addAll(dotCol, card);
            timelineContainer.getChildren().add(row);

            if (!last) {
                // spacing between entries
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                spacer.setPrefHeight(4);
                timelineContainer.getChildren().add(spacer);
            }
        }
    }

    // Icon auswählen anhand des ChangeType-Wertes / Pick icon based on change type
    private String iconForChangeType(String changeType) {
        if (changeType == null) return "•";
        return switch (changeType.toUpperCase()) {
            case "STATUS_CHANGED"   -> "↔";
            case "PRIORITY_CHANGED" -> "⚑";
            case "AGENT_ASSIGNED"   -> "👤";
            case "COMMENT_ADDED"    -> "💬";
            case "TICKET_CREATED"   -> "✚";
            case "TICKET_CLOSED"    -> "✔";
            case "TICKET_REOPENED"  -> "↺";
            case "ATTACHMENT_ADDED" -> "📎";
            default -> "•";
        };
    }

    private void loadAgents() {
        new Thread(() -> {
            try {
                List<UserFX> agents = userService.getActiveAgents();
                Platform.runLater(() -> {
                    agentCombo.setItems(FXCollections.observableArrayList(agents));
                    selectCurrentAgent();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Agenten konnten nicht geladen werden."));
            }
        }).start();
    }

    private void loadTicket() {
        new Thread(() -> {
            try {
                currentTicket = ticketService.getTicketDetails(currentTicketId);
                var comments = commentService.getComments(currentTicketId);
                List<AuditLogFX> logs = SessionManager.getRole() == UserRole.CUSTOMER
                        ? List.of()
                        : auditLogService.getLogsForTicket(currentTicketId);

                Platform.runLater(() -> {
                    titleLabel.setText(currentTicket.getTitle());
                    if (ticketNumberLabel != null) ticketNumberLabel.setText(currentTicket.getTicketNumber() != null ? currentTicket.getTicketNumber() : currentTicket.getId());
                    // [Nzchupa | 2026-06-13] TSS-003/004: Badge mit Farb-Klasse und deutschen Labels setzen
                    // Apply colored badge class + German label for status and priority
                    applyBadge(statusLabel,   currentTicket.getStatus());
                    applyBadge(priorityLabel, currentTicket.getPriority());
                    createdByLabel.setText(currentTicket.getCreatedByUser() != null ? currentTicket.getCreatedByUser().getUsername() : currentTicket.getCreatedBy());
                    assignedToLabel.setText(currentTicket.getAssignedTo() != null ? currentTicket.getAssignedTo() : "Noch nicht zugewiesen");
                    if (dueDateLabel != null) dueDateLabel.setText(currentTicket.getDueAt() != null ? currentTicket.getDueAt().toString() : "Keine SLA");
                    if (slaLabel != null) slaLabel.setText(currentTicket.getSlaLabel() + (currentTicket.isEscalated() ? " / Eskaliert" : ""));
                    // [Nzchupa | 2026-06-13] TSS-016: Anhang als Hyperlink — bei Klick wird Datei geöffnet
                    // If attachment exists show filename as blue link; otherwise show grey placeholder
                    if (attachmentLink != null) {
                        String attachName = currentTicket.getAttachmentName();
                        if (attachName != null && !attachName.isBlank()) {
                            attachmentLink.setText("📎 " + attachName);
                            attachmentLink.getStyleClass().remove("text-muted");
                            attachmentLink.setStyle("-fx-text-fill: #0EA5E9;");
                        } else {
                            attachmentLink.setText("Kein Anhang");
                            attachmentLink.setStyle("");
                            if (!attachmentLink.getStyleClass().contains("text-muted")) attachmentLink.getStyleClass().add("text-muted");
                        }
                    }
                    if (solutionLabel != null) solutionLabel.setText(currentTicket.getSolutionReason() != null ? currentTicket.getSolutionReason() : "Noch kein Lösungsgrund");
                    if (ratingLabel != null) ratingLabel.setText(currentTicket.getCustomerRating() != null ? currentTicket.getCustomerRating() + "/5 - " + safe(currentTicket.getCustomerFeedback()) : "Noch kein Feedback");
                    descriptionArea.setText(currentTicket.getDescription());

                    // Feature 38 – Reopen-Button nur wenn RESOLVED oder CLOSED und CUSTOMER
                    if (reopenTicketBtn != null) {
                        boolean canReopen = SessionManager.getRole() == UserRole.CUSTOMER
                                && ("RESOLVED".equals(currentTicket.getStatus()) || "CLOSED".equals(currentTicket.getStatus()));
                        reopenTicketBtn.setVisible(canReopen);
                        reopenTicketBtn.setManaged(canReopen);
                    }

                    if (SessionManager.getRole() != UserRole.CUSTOMER) {
                        statusCombo.setValue(TicketStatus.valueOf(currentTicket.getStatus()));
                        priorityCombo.setValue(TicketPriority.valueOf(currentTicket.getPriority()));
                        selectCurrentAgent();
                    }
                    commentsList.setItems(FXCollections.observableArrayList(comments));
                    buildTimeline(logs);
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Ticket konnte nicht geladen werden.\n" + e.getMessage()));
            }
        }).start();
    }

    private void selectCurrentAgent() {
        if (currentTicket == null || currentTicket.getAssignedToUser() == null || agentCombo.getItems() == null) return;
        for (UserFX agent : agentCombo.getItems()) {
            if (agent.getId().equals(currentTicket.getAssignedToUser().getId())) {
                agentCombo.setValue(agent);
                return;
            }
        }
    }

    @FXML
    public void handleUpdateStatus() {
        TicketStatus newStatus = statusCombo.getValue();
        if (newStatus == null) return;

        new Thread(() -> {
            try {
                UpdateTicketRequest req = new UpdateTicketRequest();
                req.setStatus(newStatus);
                ticketService.updateTicket(currentTicketId, req);
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Erfolg", "Status aktualisiert.");
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Status konnte nicht aktualisiert werden.\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleUpdatePriority() {
        TicketPriority newPriority = priorityCombo.getValue();
        if (newPriority == null) return;

        new Thread(() -> {
            try {
                UpdateTicketRequest req = new UpdateTicketRequest();
                req.setPriority(newPriority);
                ticketService.updateTicket(currentTicketId, req);
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Erfolg", "Priorität aktualisiert.");
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Priorität konnte nicht aktualisiert werden.\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleAssignAgent() {
        UserFX selectedAgent = agentCombo.getValue();
        if (selectedAgent == null) {
            AlertHelper.showError("Fehler", "Bitte zuerst einen Agenten auswählen.");
            return;
        }
        new Thread(() -> {
            try {
                ticketService.assignTicket(currentTicketId, selectedAgent.getId());
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Erfolg", "Ticket wurde an " + selectedAgent.getUsername() + " zugewiesen.");
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Zuweisung fehlgeschlagen.\n" + e.getMessage()));
            }
        }).start();
    }


    @FXML
    public void handleTakeTicket() {
        new Thread(() -> {
            try {
                ticketService.takeTicket(currentTicketId);
                Platform.runLater(() -> { AlertHelper.showInfo("Erfolg", "Ticket wurde übernommen."); loadTicket(); });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Ticket konnte nicht übernommen werden.\n" + e.getMessage()));
            }
        }).start();
    }

    // [Nzchupa | 2026-06-13] TSS-016: FileChooser öffnet Dateiauswahl-Dialog — speichert echten Dateipfad
    // FileChooser replaces text-field input; stores real absolute path so Desktop.open() works later
    @FXML
    public void handleAddAttachment() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Datei auswählen");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Alle Dateien", "*.*"),
                new javafx.stage.FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new javafx.stage.FileChooser.ExtensionFilter("Dokumente", "*.pdf", "*.doc", "*.docx", "*.txt", "*.xlsx")
        );
        java.io.File selected = chooser.showOpenDialog(
                attachBtn != null ? attachBtn.getScene().getWindow() : null
        );
        if (selected == null) return; // Benutzer hat abgebrochen / user cancelled

        String fileName = selected.getName();
        String filePath = selected.getAbsolutePath();

        new Thread(() -> {
            try {
                Map<String, Object> req = Map.of(
                        "attachmentName", fileName,
                        "attachmentPath", filePath
                );
                ticketService.updateTicket(currentTicketId, req);
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Anhang gespeichert", "Datei angehängt: " + fileName);
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Anhang konnte nicht gespeichert werden.\n" + e.getMessage()));
            }
        }).start();
    }

    // [Nzchupa | 2026-06-13] TSS-016: Anhang öffnen per java.awt.Desktop — plattformübergreifend
    // Opens the attachment file using the OS default application (Desktop.open)
    @FXML
    public void handleOpenAttachment() {
        if (currentTicket == null) return;
        String path = currentTicket.getAttachmentPath();
        String name = currentTicket.getAttachmentName();
        if (path == null || path.isBlank() || name == null || name.isBlank()) {
            AlertHelper.showInfo("Anhang", "Kein Anhang vorhanden.");
            return;
        }
        java.io.File file = new java.io.File(path);
        if (file.exists()) {
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                AlertHelper.showError("Fehler beim Öffnen", "Datei konnte nicht geöffnet werden:\n" + ex.getMessage());
            }
        } else {
            AlertHelper.showInfo("Datei nicht gefunden",
                    "Die Datei ist auf diesem Gerät nicht zugänglich.\n\nDateiname: " + name + "\nPfad: " + path);
        }
    }

    @FXML
    public void handleCloseWithSolution() {
        String solution = solutionReasonArea == null || solutionReasonArea.getText() == null ? "" : solutionReasonArea.getText().trim();
        if (solution.isBlank()) {
            AlertHelper.showError("Fehler", "Bitte einen Lösungsgrund eingeben.");
            return;
        }
        new Thread(() -> {
            try {
                Map<String, Object> req = Map.of(
                        "solutionReason", solution,
                        "status", TicketStatus.RESOLVED
                );
                ticketService.updateTicket(currentTicketId, req);
                Platform.runLater(() -> { solutionReasonArea.clear(); AlertHelper.showInfo("Gelöst", "Ticket wurde mit Lösungsgrund geschlossen."); loadTicket(); });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Ticket konnte nicht geschlossen werden.\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleSendFeedback() {
        if (selectedRating == 0) {
            AlertHelper.showError("Bewertung fehlt", "Bitte wähle zuerst eine Sternebewertung.");
            return;
        }
        String feedback = feedbackArea == null || feedbackArea.getText() == null ? "" : feedbackArea.getText().trim();
        new Thread(() -> {
            try {
                ticketService.sendFeedback(currentTicketId, selectedRating, feedback);
                Platform.runLater(() -> {
                    if (feedbackArea != null) feedbackArea.clear();
                    selectedRating = 0;
                    updateStarDisplay(0);
                    AlertHelper.showInfo("Danke", "Feedback wurde gespeichert.");
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Feedback konnte nicht gespeichert werden.\n" + e.getMessage()));
            }
        }).start();
    }

    // ── Feature 23: Stern-Handler ─────────────────────────────────────────────
    @FXML public void handleStar1() { selectedRating = 1; updateStarDisplay(1); }
    @FXML public void handleStar2() { selectedRating = 2; updateStarDisplay(2); }
    @FXML public void handleStar3() { selectedRating = 3; updateStarDisplay(3); }
    @FXML public void handleStar4() { selectedRating = 4; updateStarDisplay(4); }
    @FXML public void handleStar5() { selectedRating = 5; updateStarDisplay(5); }

    @FXML public void hoverStar1() { if (selectedRating == 0) updateStarDisplay(1); }
    @FXML public void hoverStar2() { if (selectedRating == 0) updateStarDisplay(2); }
    @FXML public void hoverStar3() { if (selectedRating == 0) updateStarDisplay(3); }
    @FXML public void hoverStar4() { if (selectedRating == 0) updateStarDisplay(4); }
    @FXML public void hoverStar5() { if (selectedRating == 0) updateStarDisplay(5); }
    @FXML public void hoverStarReset() { if (selectedRating == 0) updateStarDisplay(0); }

    private void updateStarDisplay(int count) {
        Label[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            if (i < count) {
                stars[i].setText("★");
                stars[i].setStyle("-fx-font-size: 28px; -fx-cursor: hand; -fx-text-fill: #F59E0B;");
            } else {
                stars[i].setText("☆");
                stars[i].setStyle("-fx-font-size: 28px; -fx-cursor: hand; -fx-text-fill: #94A3B8;");
            }
        }
        if (starValueLabel != null) {
            starValueLabel.setText(count > 0 ? count + " / 5" : "");
        }
    }

    @FXML
    public void handleAddComment() {
        String content = newCommentArea.getText();
        if (content == null || content.trim().isEmpty()) return;
        boolean internal = internalCheckBox.isVisible() && internalCheckBox.isSelected();

        new Thread(() -> {
            try {
                commentService.createComment(currentTicketId, content, internal);
                Platform.runLater(() -> {
                    newCommentArea.clear();
                    internalCheckBox.setSelected(false);
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Kommentar konnte nicht gesendet werden."));
            }
        }).start();
    }

    // Feature 38 – Ticket wiedereröffnen
    @FXML
    public void handleReopenTicket() {
        new Thread(() -> {
            try {
                ticketService.reopenTicket(currentTicketId);
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Wiedereröffnet", "Ticket wurde wieder geöffnet.");
                    loadTicket();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Ticket konnte nicht wiedereröffnet werden.\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleBack() {
        if (hasUnsavedChanges() && !AlertHelper.confirmDiscardUnsavedChanges()) {
            return;
        }
        Navigator.navigateAfterLogin(SessionManager.getRole());
    }

    private boolean hasUnsavedChanges() {
        if (!text(newCommentArea).isBlank()) return true;
        if (solutionReasonArea != null && solutionReasonArea.isVisible() && !text(solutionReasonArea).isBlank()) return true;
        if (attachmentNameField != null && attachmentNameField.isVisible() && !text(attachmentNameField).isBlank()) return true;
        if (feedbackArea != null && feedbackArea.isVisible() && !text(feedbackArea).isBlank()) return true;

        if (currentTicket == null || SessionManager.getRole() == UserRole.CUSTOMER) return false;

        if (statusCombo != null && statusCombo.isVisible() && statusCombo.getValue() != null
                && !statusCombo.getValue().name().equals(currentTicket.getStatus())) {
            return true;
        }
        if (priorityCombo != null && priorityCombo.isVisible() && priorityCombo.getValue() != null
                && !priorityCombo.getValue().name().equals(currentTicket.getPriority())) {
            return true;
        }
        if (agentCombo != null && agentCombo.isVisible() && agentCombo.getValue() != null) {
            String selectedAgentId = agentCombo.getValue().getId();
            String currentAgentId = currentTicket.getAssignedToUser() == null ? null : currentTicket.getAssignedToUser().getId();
            return !Objects.equals(selectedAgentId, currentAgentId);
        }
        return false;
    }

    private String text(TextInputControl control) {
        return control == null || control.getText() == null ? "" : control.getText().trim();
    }
}
