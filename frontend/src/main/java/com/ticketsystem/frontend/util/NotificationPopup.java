package com.ticketsystem.frontend.util;


import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.service.NotificationApiService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.net.URL;
import java.util.List;

public class NotificationPopup {

    private static Popup popup;
    private static Popup detailsPopup;

    // [Nzchupa | 2026-06-12] TS-001: reloadCallback hinzugefügt — wird nach "Alle gelesen" aufgerufen
    // Added reloadCallback — called after "Alle gelesen" so the badge counter updates
    public static void show(Node bellIcon, List<NotificationFX> notifications, Runnable reloadCallback) {
        if (popup != null && popup.isShowing()) {
            popup.hide();
            return;
        }

        closeDetailsPopup();

        popup = new Popup();
        popup.setAutoHide(true);

        VBox root = new VBox();
        root.setPrefWidth(390);
        root.setMaxWidth(390);
        root.getStyleClass().add("notification-popup");
        applyTheme(root);

        // [Nzchupa | 2026-06-12] TS-001: notifications + reloadCallback an createHeader übergeben
        HBox header = createHeader(notifications, reloadCallback);

        VBox listBox = new VBox();
        listBox.getStyleClass().add("notification-list");

        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("Keine Benachrichtigungen");
            empty.getStyleClass().add("notification-empty");
            listBox.getChildren().add(empty);
        } else {
            for (NotificationFX notification : notifications) {
                listBox.getChildren().add(createNotificationRow(notification, bellIcon));
            }
        }

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(280);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("notification-scroll");

        Button showAll = new Button("Alle Benachrichtigungen anzeigen");
        showAll.getStyleClass().add("notification-show-all");

        root.getChildren().addAll(header, scrollPane, showAll);
        addStylesheet(root);

        popup.getContent().add(root);

        double x = bellIcon.localToScreen(bellIcon.getBoundsInLocal()).getMinX() - 340;
        double y = bellIcon.localToScreen(bellIcon.getBoundsInLocal()).getMaxY() + 10;

        popup.show(bellIcon, x, y);
    }

    // [Nzchupa | 2026-06-12] TS-001: Überladene Methode ohne Callback für Abwärtskompatibilität
    // Overloaded method without callback for backward compatibility
    public static void show(Node bellIcon, List<NotificationFX> notifications) {
        show(bellIcon, notifications, null);
    }

    // [Nzchupa | 2026-06-12] TS-001: Bug-Fix — fehlende setOnAction-Logik hinzugefügt
    // Bug-Fix: createHeader now accepts notifications + callback and wires up the markAll button
    private static HBox createHeader(List<NotificationFX> notifications, Runnable reloadCallback) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("notification-header");

        Label title = new Label("Benachrichtigungen");
        title.getStyleClass().add("notification-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAll = new Button("Alle gelesen");
        markAll.getStyleClass().add("notification-link-button");

        // [Nzchupa | 2026-06-12] TS-001: Button war nie verdrahtet — jetzt API-Aufruf + UI-Reload
        // Button had no action before — now calls markAllAsRead in background, then reloads badge
        markAll.setOnAction(e -> {
            markAll.setDisable(true);
            new Thread(() -> {
                new NotificationApiService().markAllAsRead(notifications);
                Platform.runLater(() -> {
                    if (popup != null) popup.hide();
                    if (reloadCallback != null) reloadCallback.run();
                });
            }, "mark-all-notifications-read").start();
        });

        header.getChildren().addAll(title, spacer, markAll);
        return header;
    }

    private static Node createNotificationRow(NotificationFX notification, Node bellIcon) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("notification-row");

        Label icon = new Label(getNotificationIcon(notification));
        icon.getStyleClass().add("notification-row-icon");

        VBox textBox = new VBox(5);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safeText(notification.getTitle()));
        title.getStyleClass().add("notification-row-title");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Label unreadDot = new Label("●");
        unreadDot.getStyleClass().add("notification-unread-dot");
        unreadDot.setVisible(!notification.isRead());

        titleLine.getChildren().addAll(title, titleSpacer, unreadDot);

        Label message = new Label(safeText(notification.getMessage()));
        message.getStyleClass().add("notification-row-message");
        message.setWrapText(true);

        Label status = new Label(notification.isRead() ? "Gelesen" : "Ungelesen");
        status.getStyleClass().add("notification-row-time");

        textBox.getChildren().addAll(titleLine, message, status);
        row.getChildren().addAll(icon, textBox);

        row.setOnMouseClicked(event -> showDetails(bellIcon, notification));

        return row;
    }

    private static void showDetails(Node ownerNode, NotificationFX notification) {
        closeDetailsPopup();

        detailsPopup = new Popup();
        detailsPopup.setAutoHide(true);

        VBox root = new VBox(14);
        root.setPrefWidth(360);
        root.getStyleClass().add("notification-detail-content");
        applyTheme(root);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(getNotificationIcon(notification));
        icon.getStyleClass().add("notification-detail-icon");

        VBox titleBox = new VBox(3);

        Label title = new Label(safeText(notification.getTitle()));
        title.getStyleClass().add("notification-detail-title");

        Label status = new Label(notification.isRead() ? "Gelesen" : "Ungelesen");
        status.getStyleClass().add("notification-row-time");

        titleBox.getChildren().addAll(title, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButtonTop = new Button("×");
        closeButtonTop.getStyleClass().add("notification-close-button");
        closeButtonTop.setOnAction(event -> closeDetailsPopup());

        header.getChildren().addAll(icon, titleBox, spacer, closeButtonTop);

        Separator separator = new Separator();

        Label section = new Label("Beschreibung");
        section.getStyleClass().add("notification-detail-section");

        Label message = new Label(safeText(notification.getMessage()));
        message.getStyleClass().add("notification-detail-message");
        message.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Schließen");
        closeButton.getStyleClass().addAll("btn-ghost", "notification-detail-button");
        closeButton.setOnAction(event -> closeDetailsPopup());

        buttonBox.getChildren().add(closeButton);

        root.getChildren().addAll(
                header,
                separator,
                section,
                message,
                buttonBox
        );

        addStylesheet(root);

        detailsPopup.getContent().add(root);

        double x = ownerNode.localToScreen(ownerNode.getBoundsInLocal()).getMinX() - 340;
        double y = ownerNode.localToScreen(ownerNode.getBoundsInLocal()).getMaxY() + 10;

        if (popup != null && popup.isShowing()) {
            popup.hide();
        }

        detailsPopup.show(ownerNode, x, y);
    }

    private static void closeDetailsPopup() {
        if (detailsPopup != null && detailsPopup.isShowing()) {
            detailsPopup.hide();
        }
    }

    private static void applyTheme(Node node) {
        if (ThemeManager.isDarkMode()) {
            node.getStyleClass().add("theme-dark");
        } else {
            node.getStyleClass().add("theme-light");
        }
    }

    private static void addStylesheet(Parent node) {
        URL cssUrl = NotificationPopup.class.getResource("/css/styles.css");

        if (cssUrl != null) {
            node.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    private static String safeText(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private static String getNotificationIcon(NotificationFX notification) {
        String title = safeText(notification.getTitle()).toLowerCase();

        if (title.contains("ticket")) {
            return "🎫";
        }

        if (title.contains("kommentar")) {
            return "💬";
        }

        if (title.contains("zugewiesen")) {
            return "🔔";
        }

        return "ℹ";
    }
}