package com.ticketsystem.frontend.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class AlertHelper {
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    public static boolean confirmDiscardUnsavedChanges() {
        ButtonType discardButton = new ButtonType("Zurückgehen", ButtonBar.ButtonData.OK_DONE);
        ButtonType stayButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ungespeicherte Eingaben");
        alert.setHeaderText("Ungespeicherte Eingaben verwerfen?");
        alert.setContentText("Wenn Sie diese Seite verlassen, gehen Ihre nicht gespeicherten Eingaben verloren.");
        alert.getButtonTypes().setAll(discardButton, stayButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == discardButton;
    }
}