package com.ticketsystem.frontend.util;

// [Nzchupa | 2026-06-13] ToastHelper — kleines Auto-Hide-Popup am unteren Bildschirmrand
// Toast notification that auto-dismisses after 2.5s — for non-blocking feedback like "Kopiert"

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ToastHelper {

    public enum ToastType { SUCCESS, INFO, WARNING }

    // ── Show from any Node in the scene ───────────────────────────────────────
    public static void show(Node ownerNode, String message) {
        show(ownerNode, message, ToastType.INFO);
    }

    public static void show(Node ownerNode, String message, ToastType type) {
        if (ownerNode == null || ownerNode.getScene() == null) return;
        show((Stage) ownerNode.getScene().getWindow(), message, type);
    }

    // ── Core ──────────────────────────────────────────────────────────────────
    public static void show(Stage stage, String message, ToastType type) {
        if (stage == null) return;

        String accent = switch (type) {
            case SUCCESS -> "#22C55E";
            case WARNING -> "#F59E0B";
            case INFO    -> "#0EA5E9";
        };

        String icon = switch (type) {
            case SUCCESS -> "✓  ";
            case WARNING -> "⚠  ";
            case INFO    -> "ℹ  ";
        };

        Label label = new Label(icon + message);
        label.setStyle(
                "-fx-background-color: #0F172A;" +
                "-fx-text-fill: #F1F5F9;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 13px;" +
                "-fx-border-color: " + accent + ";" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 1;"
        );

        Popup popup = new Popup();
        popup.setAutoHide(false);
        popup.getContent().add(label);

        // Show offscreen first to force layout, then reposition
        popup.show(stage, -9999, -9999);

        Platform.runLater(() -> {
            double popupW = popup.getWidth() > 0 ? popup.getWidth() : 220;
            double x = stage.getX() + (stage.getWidth()  - popupW) / 2.0;
            double y = stage.getY() +  stage.getHeight() - 72;
            popup.setX(x);
            popup.setY(y);

            // Pause then fade out / Pause dann ausblenden
            PauseTransition pause = new PauseTransition(Duration.seconds(2.0));
            pause.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.seconds(0.4), label);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(ev -> popup.hide());
                fade.play();
            });
            pause.play();
        });
    }
}
