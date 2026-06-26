package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.service.NotificationApiService;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// [Nzchupa | 2026-06-13] Neuer Controller für das Alle-Benachrichtigungen-Fenster
// New controller for the "All Notifications" modal window
public class NotificationsController {

    @FXML private BorderPane rootPane;
    @FXML private VBox notificationsContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private Label unreadCountLabel;
    @FXML private Button filterButton;

    private final NotificationApiService notificationService = new NotificationApiService();
    private List<NotificationFX> notifications;

    // [Nzchupa | 2026-06-26] KAT-88: Filterzustand — nur ungelesene anzeigen
    // Filter state used by the "Nur ungelesen" toggle button
    private boolean showOnlyUnread = false;

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
    // [Nzchupa | 2026-06-26] KAT-86/KAT-88/KAT-92: Filter, Gruppierung nach Datum und Empty State ergänzt
    // Render notifications grouped by date bucket (Heute/Gestern/Älter), respecting the unread filter,
    // and showing a proper empty state when there is nothing to display
    private void renderNotifications() {
        notificationsContainer.getChildren().clear();

        List<NotificationFX> visible = (notifications == null) ? List.of() :
                notifications.stream()
                        .filter(n -> !showOnlyUnread || !n.isRead())
                        .collect(Collectors.toList());

        if (visible.isEmpty()) {
            notificationsContainer.getChildren().add(buildEmptyState());
            updateUnreadLabel();
            return;
        }

        // [Nzchupa | 2026-06-26] KAT-86: Section-Header je Zeitraum, Reihenfolge bleibt Heute -> Gestern -> Älter
        Map<String, List<NotificationFX>> grouped = new LinkedHashMap<>();
        for (String bucket : List.of("Heute", "Gestern", "Älter")) {
            List<NotificationFX> inBucket = visible.stream()
                    .filter(n -> bucket.equals(n.getDateBucket()))
                    .collect(Collectors.toList());
            if (!inBucket.isEmpty()) grouped.put(bucket, inBucket);
        }

        for (Map.Entry<String, List<NotificationFX>> entry : grouped.entrySet()) {
            Label sectionHeader = new Label(entry.getKey());
            sectionHeader.getStyleClass().add("text-muted");
            sectionHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 0 0 2;");
            notificationsContainer.getChildren().add(sectionHeader);

            for (NotificationFX n : entry.getValue()) {
                notificationsContainer.getChildren().add(buildCard(n));
            }
        }
        updateUnreadLabel();
    }

    // [Nzchupa | 2026-06-26] KAT-92: Empty State analog zum bestehenden Muster (z.B. CustomerController)
    private VBox buildEmptyState() {
        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.getStyleClass().add("empty-state");
        Label icon = new Label("📭");
        icon.getStyleClass().add("empty-state-icon");
        Label title = new Label(showOnlyUnread ? "Keine ungelesenen Benachrichtigungen" : "Keine Benachrichtigungen vorhanden");
        title.getStyleClass().add("empty-state-title");
        Label text = new Label(showOnlyUnread ? "Du hast bereits alles gelesen." : "Hier erscheinen neue Benachrichtigungen.");
        text.getStyleClass().add("empty-state-text");
        emptyBox.getChildren().addAll(icon, title, text);
        return emptyBox;
    }

    private VBox buildCard(NotificationFX notification) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");

        // [Nzchupa | 2026-06-26] KAT-90: Akzentfarbe je Benachrichtigungsart (immer sichtbar),
        // ungelesene Karten erhalten zusätzlich einen dickeren Rand
        String borderWidth = notification.isRead() ? "0 0 0 2" : "0 0 0 3";
        card.setStyle("-fx-border-color: " + notification.getColor() + "; -fx-border-width: " + borderWidth + "; -fx-cursor: hand;");

        // Titelzeile: Icon + Titel + Löschen-Button + Ungelesen-Status
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(notification.getIcon());
        iconLabel.setStyle("-fx-font-size: 20px;");

        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(safeText(notification.getTitle()));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label message = new Label(safeText(notification.getMessage()));
        message.getStyleClass().add("text-muted");
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 12px;");

        // [Nzchupa | 2026-06-26] KAT-87: lesbarer Zeitstempel statt nur "Gelesen/Ungelesen"
        Label timeLabel = new Label(notification.getFormattedTime());
        timeLabel.getStyleClass().add("text-muted");
        timeLabel.setStyle("-fx-font-size: 11px;");

        textBox.getChildren().addAll(title, message, timeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox statusBox = new VBox(6);
        statusBox.setAlignment(Pos.TOP_RIGHT);

        // [Nzchupa | 2026-06-26] KAT-91: Löschen-Button pro Zeile
        Button deleteButton = new Button("×");
        deleteButton.getStyleClass().add("notification-close-button");
        // Klick auf den Löschen-Button darf nicht die Klick-Logik der Karte (gelesen markieren/Navigation) auslösen
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
        deleteButton.setOnAction(e -> handleDelete(notification));

        Label statusLabel = new Label(notification.isRead() ? "Gelesen" : "● Ungelesen");
        statusLabel.setStyle(notification.isRead()
                ? "-fx-font-size: 11px; -fx-text-fill: #64748B;"
                : "-fx-font-size: 11px; -fx-text-fill: #0EA5E9; -fx-font-weight: bold;");

        statusBox.getChildren().addAll(deleteButton, statusLabel);

        titleRow.getChildren().addAll(iconLabel, textBox, spacer, statusBox);
        card.getChildren().add(titleRow);

        // [Nzchupa | 2026-06-13] Klick auf Karte → als gelesen markieren
        // [Nzchupa | 2026-06-26] KAT-89: Klick navigiert zusätzlich zum verknüpften Ticket, falls vorhanden
        // Click on card marks the notification as read and, when a ticketId is present, opens the ticket
        card.setOnMouseClicked(e -> handleCardClick(notification, statusLabel, card));

        return card;
    }

    private void handleCardClick(NotificationFX notification, Label statusLabel, VBox card) {
        if (!notification.isRead()) {
            notification.setRead(true);
            statusLabel.setText("Gelesen");
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            card.setStyle("-fx-border-color: " + notification.getColor() + "; -fx-border-width: 0 0 0 2; -fx-cursor: hand;");
            updateUnreadLabel();
            new Thread(() -> {
                try { notificationService.markAsRead(notification.getId()); }
                catch (Exception ignored) {}
            }, "mark-notification-read").start();
        }

        // [Nzchupa | 2026-06-26] KAT-89: ticketId aus dem Modell nutzen, um zur TicketDetailView zu navigieren
        if (notification.getTicketId() != null && !notification.getTicketId().isBlank()) {
            TicketDetailController.setCurrentTicketId(notification.getTicketId());
            Stage stage = (Stage) notificationsContainer.getScene().getWindow();
            stage.close();
            Navigator.navigateTo("TicketDetailView.fxml");
        }
    }

    // [Nzchupa | 2026-06-26] KAT-91: Löschen einer einzelnen Benachrichtigung — Karte sofort entfernen, API im Hintergrund
    private void handleDelete(NotificationFX notification) {
        notifications.remove(notification);
        renderNotifications();
        new Thread(() -> {
            try { notificationService.deleteNotification(notification.getId()); }
            catch (Exception ignored) { }
        }, "delete-notification").start();
    }

    // [Nzchupa | 2026-06-26] KAT-88: Toggle zwischen "Nur ungelesen" und "Alle anzeigen"
    @FXML
    public void handleToggleFilter() {
        showOnlyUnread = !showOnlyUnread;
        if (filterButton != null) {
            filterButton.setText(showOnlyUnread ? "Alle anzeigen" : "Nur ungelesen");
        }
        renderNotifications();
    }

    private void updateUnreadLabel() {
        if (notifications == null) return;
        long unread = notifications.stream().filter(n -> !n.isRead()).count();
        if (unreadCountLabel != null) {
            unreadCountLabel.setText(unread > 0 ? unread + " ungelesen" : "Alle gelesen ✓");
        }
    }

    // [Nzchupa | 2026-06-26] KAT-93: Zähler/Liste werden nach dem Backend-Call sofort lokal aktualisiert,
    // statt auf einen vollständigen Reload der Liste zu warten (war bereits durch TS-001 behoben, hier beibehalten)
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

    private String safeText(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
