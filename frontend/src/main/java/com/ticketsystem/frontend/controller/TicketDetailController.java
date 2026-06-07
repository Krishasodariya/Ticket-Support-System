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
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
    @FXML private Label dueDateLabel, slaLabel, attachmentLabel, solutionLabel, ratingLabel;
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
    @FXML private ListView<AuditLogFX> historyList;
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
        if (attachmentNameField != null) attachmentNameField.setVisible(!customer);
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
        historyList.setCellFactory(param -> new ListCell<>() {
            @Override protected void updateItem(AuditLogFX item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTimestamp() + " | " + item.getChangedBy() + " | " + item.getChangeType()
                            + " | " + safe(item.getOldValue()) + " → " + safe(item.getNewValue()));
                }
            }
        });
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

    private String safe(String text) {
        return text == null ? "-" : text;
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
                    statusLabel.setText(currentTicket.getStatus());
                    priorityLabel.setText(currentTicket.getPriority());
                    createdByLabel.setText(currentTicket.getCreatedByUser() != null ? currentTicket.getCreatedByUser().getUsername() : currentTicket.getCreatedBy());
                    assignedToLabel.setText(currentTicket.getAssignedTo() != null ? currentTicket.getAssignedTo() : "Noch nicht zugewiesen");
                    if (dueDateLabel != null) dueDateLabel.setText(currentTicket.getDueAt() != null ? currentTicket.getDueAt().toString() : "Keine SLA");
                    if (slaLabel != null) slaLabel.setText(currentTicket.getSlaLabel() + (currentTicket.isEscalated() ? " / Eskaliert" : ""));
                    if (attachmentLabel != null) attachmentLabel.setText(currentTicket.getAttachmentName() != null ? currentTicket.getAttachmentName() : "Kein Anhang");
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
                    historyList.setItems(FXCollections.observableArrayList(logs));
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

    @FXML
    public void handleAddAttachment() {
        String fileName = attachmentNameField == null || attachmentNameField.getText() == null ? "" : attachmentNameField.getText().trim();
        if (fileName.isBlank()) {
            AlertHelper.showError("Fehler", "Bitte Dateiname eingeben, z.B. screenshot.png");
            return;
        }
        new Thread(() -> {
            try {
                Map<String, Object> req = Map.of(
                        "attachmentName", fileName,
                        "attachmentPath", "demo-attachments/" + fileName
                );
                ticketService.updateTicket(currentTicketId, req);
                Platform.runLater(() -> { attachmentNameField.clear(); AlertHelper.showInfo("Anhang", "Anhang wurde simuliert gespeichert."); loadTicket(); });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Fehler", "Anhang konnte nicht gespeichert werden.\n" + e.getMessage()));
            }
        }).start();
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
