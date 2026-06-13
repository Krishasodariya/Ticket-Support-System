package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.service.NotificationApiService;
import com.ticketsystem.frontend.util.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

// [Nzchupa | 2026-06-13] Neuer Controller für das Alle-Benachrichtigungen-Fenster
// New controller for the "All Notifications" modal window
public class NotificationsController {

    @FXML private BorderPane rootPane;
    @FXML private VBox notificationsContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private Label unreadCountLabel;

    private final NotificationApiService notificationService = new NotificationApiService();
    private List<NotificationFX> notifications;

    @FXML
    public void initialize() {
        // ThemeManager після рендеру на кореневий BorderPane
        javafx.application.Platform.runLater(() -> {
            if (rootPane != null) ThemeManager.apply(rootPane);
        });
        loadNotifications();
    }

    private void loadNotifications() {
        notificationsContainer.getChildren().clear();
        Label loading = new Label("Wird geladen…");
        loading.getStyleClass().add("text-muted");
        notificationsContainer.getChildren().add(loading);

        Task<List<NotificationFX>> task = new Task<>() {
            @Override protected List<NotificationFX> call() throws Exception {
                return notificationService.getMyNotifications();
            }
        };
        task.setOnSucceeded(e -> {
            notifications = task.getValue();
            renderNotifications();
        });
        task.setOnFailed(e -> {
            notificationsContainer.getChildren().clear();
            Label err = new Label("Benachrichtigungen konnten nicht geladen werden.");
            err.getStyleClass().add("text-danger");
            notificationsContainer.getChildren().add(err);
        });
        new Thread(task, "notifications-modal-load").start();
    }

    // [Nzchupa | 2026-06-13] Alle Benachrichtigungen als Karten rendern
    // Render all notifications as clickable cards; unread ones are highlighted
    private void renderNotifications() {
        notificationsContainer.getChildren().clear();

        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("Keine Benachrichtigungen vorhanden. ✅");
            empty.getStyleClass().add("text-muted");
            notificationsContainer.getChildren().add(empty);
            updateUnreadLabel();
            return;
        }

        for (NotificationFX n : notifications) {
            notificationsContainer.getChildren().add(buildCard(n));
        }
        updateUnreadLabel();
    }

    private VBox buildCard(NotificationFX notification) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        if (!notification.isRead()) {
            card.setStyle("-fx-border-color: #0EA5E9; -fx-border-width: 0 0 0 3;");
        }
        card.setStyle(card.getStyle() + " -fx-cursor: hand;");

        // Titelzeile: Icon + Titel + Ungelesen-Punkt
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(getIcon(notification));
        iconLabel.setStyle("-fx-font-size: 20px;");

        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(safeText(notification.getTitle()));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label message = new Label(safeText(notification.getMessage()));
        message.getStyleClass().add("text-muted");
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 12px;");

        textBox.getChildren().addAll(title, message);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(notification.isRead() ? "Gelesen" : "● Ungelesen");
        statusLabel.setStyle(notification.isRead()
                ? "-fx-font-size: 11px; -fx-text-fill: #64748B;"
                : "-fx-font-size: 11px; -fx-text-fill: #0EA5E9; -fx-font-weight: bold;");

        titleRow.getChildren().addAll(iconLabel, textBox, spacer, statusLabel);
        card.getChildren().add(titleRow);

        // [Nzchupa | 2026-06-13] Klick auf Karte → als gelesen markieren
        // Click on card marks the notification as read and updates UI instantly
        card.setOnMouseClicked(e -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                // UI sofort aktualisieren
                card.setStyle("-fx-cursor: hand;"); // blauer Rand weg
                statusLabel.setText("Gelesen");
                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                updateUnreadLabel();
                // API-Aufruf im Hintergrund
                new Thread(() -> {
                    try { notificationService.markAsRead(notification.getId()); }
                    catch (Exception ignored) {}
                }, "mark-notification-read").start();
            }
        });

        return card;
    }

    private void updateUnreadLabel() {
        if (notifications == null) return;
        long unread = notifications.stream().filter(n -> !n.isRead()).count();
        if (unreadCountLabel != null) {
            unreadCountLabel.setText(unread > 0 ? unread + " ungelesen" : "Alle gelesen ✓");
        }
    }

    @FXML
    public void handleMarkAllRead() {
        if (notifications == null || notifications.isEmpty()) return;
        new Thread(() -> {
            notificationService.markAllAsRead(notifications);
            Platform.runLater(() -> {
                notifications.forEach(n -> n.setRead(true));
                renderNotifications();
            });
        }, "mark-all-notifications-read-modal").start();
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) notificationsContainer.getScene().getWindow();
        stage.close();
    }

    private String getIcon(NotificationFX n) {
        String t = safeText(n.getTitle()).toLowerCase();
        if (t.contains("ticket"))      return "🎫";
        if (t.contains("kommentar"))   return "💬";
        if (t.contains("zugewiesen"))  return "🔔";
        if (t.contains("email"))       return "📧";
        return "ℹ";
    }

    private String safeText(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
