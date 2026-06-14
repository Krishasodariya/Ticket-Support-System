package com.ticketsystem.frontend.util;

// [Nzchupa | 2026-06-13] Custom Alert — Variante B: Modaler Dialog mit farbiger Kopfleiste
// Variant B: centered modal with colored top accent bar + icon circle

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class AlertHelper {

    private enum AlertType { SUCCESS, ERROR, WARNING, CONFIRM }

    // ── Public API ───────────────────────────────────────────────────────────

    public static void showInfo(String title, String content) {
        buildAndShow(AlertType.SUCCESS, title, content, null, null);
    }

    public static void showError(String title, String content) {
        buildAndShow(AlertType.ERROR, title, content, null, null);
    }

    public static void showWarning(String title, String content) {
        buildAndShow(AlertType.WARNING, title, content, null, null);
    }

    public static boolean confirmDiscardUnsavedChanges() {
        boolean[] result = {false};
        buildAndShow(
                AlertType.CONFIRM,
                "Ungespeicherte Eingaben",
                "Wenn Sie diese Seite verlassen, gehen Ihre nicht gespeicherten Eingaben verloren.",
                "Verlassen",
                result
        );
        return result[0];
    }

    // [Nzchupa | 2026-06-13] Allgemeine Bestätigungsmethode hinzugefügt — für Duplikat-Dialog in CustomerController
    // Generic confirm method — replaces native Alert.CONFIRMATION dialogs
    public static boolean showConfirm(String title, String content, String confirmLabel) {
        boolean[] result = {false};
        buildAndShow(AlertType.CONFIRM, title, content, confirmLabel, result);
        return result[0];
    }

    // ── Core builder ─────────────────────────────────────────────────────────

    private static void buildAndShow(AlertType type, String title, String content,
                                     String confirmLabel, boolean[] resultHolder) {
        // [Nzchupa | 2026-06-13] TRANSPARENT statt UNDECORATED — sonst sieht man weiße Ecken
        // TRANSPARENT instead of UNDECORATED so rounded corners clip correctly
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        boolean dark = ThemeManager.isDarkMode();
        String bg    = dark ? "#1E293B" : "#FFFFFF";
        String accent = accent(type);

        // ── Outer shell: двошарова заливка — акцент зверху + фон знизу ──────
        // Two-layer background: accent strip on top (4px), card color below
        VBox root = new VBox(0);
        root.setPrefWidth(380);
        root.setMaxWidth(420);
        root.setStyle(
                "-fx-background-color: " + accent + ", " + bg + ";" +
                "-fx-background-insets: 0, 4 0 0 0;" +
                "-fx-background-radius: 12, 0 0 12 12;"
        );

        // Drag support (TRANSPARENT window) / Підтримка перетягування
        double[] delta = {0, 0};
        root.setOnMousePressed(e -> {
            delta[0] = stage.getX() - e.getScreenX();
            delta[1] = stage.getY() - e.getScreenY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + delta[0]);
            stage.setY(e.getScreenY() + delta[1]);
        });

        // ── Inner content ─────────────────────────────────────────────────────
        VBox content_box = new VBox(12);
        content_box.setPadding(new Insets(16, 20, 20, 20));

        // Header: icon-circle + title
        HBox header = buildHeader(type, title, dark);

        // Separator — Region statt Separator, weil .separator > .line die Farbe ignoriert
        // Using Region instead of Separator because JavaFX Separator ignores background-color on the container
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: " + (dark ? "#334155" : "#E2E8F0") + ";");
        VBox.setMargin(sep, new Insets(2, 0, 2, 0));

        // Message text
        Label msg = new Label(content);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (dark ? "#94A3B8" : "#475569") + "; -fx-line-spacing: 2;");

        // Buttons
        HBox btnRow = buildButtons(type, stage, confirmLabel, resultHolder, dark);

        content_box.getChildren().addAll(header, sep, msg, btnRow);
        root.getChildren().add(content_box);

        // ── Scene + CSS ───────────────────────────────────────────────────────
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        URL css = AlertHelper.class.getResource("/css/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
        stage.showAndWait();
    }

    // ── Header: іконка у кружку + заголовок ──────────────────────────────────
    private static HBox buildHeader(AlertType type, String title, boolean dark) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon circle / Іконка у кружку
        StackPane iconWrap = new StackPane();
        Circle circleBg = new Circle(18);
        circleBg.setStyle("-fx-fill: " + iconBg(type) + ";");

        Label iconLbl = new Label(icon(type));
        iconLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: " + accent(type) + "; -fx-font-weight: bold;");

        iconWrap.getChildren().addAll(circleBg, iconLbl);

        // Title
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (dark ? "#F1F5F9" : "#0F172A") + ";");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        header.getChildren().addAll(iconWrap, titleLbl);
        return header;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────
    private static HBox buildButtons(AlertType type, Stage stage, String confirmLabel,
                                     boolean[] resultHolder, boolean dark) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_RIGHT);

        if (type == AlertType.CONFIRM && confirmLabel != null && resultHolder != null) {
            // Confirm: Abbrechen + Verlassen(rot)
            Button cancel = styledButton("Abbrechen", false, dark);
            cancel.setOnAction(e -> stage.close());

            Button confirm = styledButton(confirmLabel, true, dark);
            confirm.setStyle(confirm.getStyle() + "-fx-background-color: #EF4444; -fx-text-fill: white;");
            confirm.setOnAction(e -> { resultHolder[0] = true; stage.close(); });

            row.getChildren().addAll(cancel, confirm);
        } else {
            // Simple alert: OK-Button
            Button ok = styledButton("OK", true, dark);
            ok.setOnAction(e -> stage.close());
            row.getChildren().add(ok);
        }
        return row;
    }

    private static Button styledButton(String label, boolean primary, boolean dark) {
        Button btn = new Button(label);
        btn.setPrefHeight(34);
        btn.setMinWidth(80);
        if (primary) {
            btn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; "
                    + "-fx-font-size: 13px; -fx-font-weight: bold; "
                    + "-fx-background-radius: 6; -fx-cursor: hand;");
        } else {
            String border = dark ? "#334155" : "#CBD5E1";
            String text   = dark ? "#94A3B8" : "#475569";
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + border + "; "
                    + "-fx-border-radius: 6; -fx-background-radius: 6; "
                    + "-fx-text-fill: " + text + "; -fx-font-size: 13px; -fx-cursor: hand;");
        }
        return btn;
    }

    // ── Кольори / Colors ──────────────────────────────────────────────────────

    private static String accent(AlertType type) {
        return switch (type) {
            case SUCCESS -> "#22C55E";
            case ERROR   -> "#EF4444";
            case WARNING -> "#F59E0B";
            case CONFIRM -> "#0EA5E9";
        };
    }

    private static String iconBg(AlertType type) {
        return switch (type) {
            case SUCCESS -> "rgba(34,197,94,0.15)";
            case ERROR   -> "rgba(239,68,68,0.15)";
            case WARNING -> "rgba(245,158,11,0.15)";
            case CONFIRM -> "rgba(14,165,233,0.15)";
        };
    }

    private static String icon(AlertType type) {
        return switch (type) {
            case SUCCESS -> "✓";
            case ERROR   -> "✕";
            case WARNING -> "⚠";
            case CONFIRM -> "?";
        };
    }
}
