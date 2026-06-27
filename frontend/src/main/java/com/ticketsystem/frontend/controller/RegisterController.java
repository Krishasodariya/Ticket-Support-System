package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.service.AuthApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox termsCheckBox;
    @FXML private Label errorLabel;

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_.-]+$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final AuthApiService authService = new AuthApiService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        // KAT-12: Fokus automatisch auf das erste Feld setzen
        Platform.runLater(() -> usernameField.requestFocus());

        // KAT-16: Live-Validierung von Benutzername und E-Mail beim Verlassen des Feldes
        usernameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateUsernameLive();
        });
        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateEmailLive();
        });
    }

    private void validateUsernameLive() {
        String username = usernameField.getText().trim();
        boolean valid = username.isEmpty() || (username.length() >= 3 && username.matches(USERNAME_PATTERN));
        usernameField.getStyleClass().remove("text-field-error");
        if (!valid) {
            usernameField.getStyleClass().add("text-field-error");
        }
    }

    private void validateEmailLive() {
        String email = emailField.getText().trim();
        boolean valid = email.isEmpty() || email.matches(EMAIL_PATTERN);
        emailField.getStyleClass().remove("text-field-error");
        if (!valid) {
            emailField.getStyleClass().add("text-field-error");
        }
    }

    @FXML
    public void handleRegister() {
        clearInputErrors();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            markAllEmpty();
            showError("Alle Felder sind Pflichtfelder.");
            return;
        }
        if (username.length() < 3 || !username.matches(USERNAME_PATTERN)) {
            usernameField.getStyleClass().add("text-field-error");
            showError("Benutzername: mindestens 3 Zeichen, nur Buchstaben, Zahlen, Punkt, Unterstrich oder Bindestrich.");
            return;
        }
        if (!email.matches(EMAIL_PATTERN)) {
            emailField.getStyleClass().add("text-field-error");
            showError("Bitte eine gültige E-Mail-Adresse eingeben.");
            return;
        }
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
            passwordField.getStyleClass().add("text-field-error");
            showError("Passwort braucht mindestens 8 Zeichen, Buchstaben, Zahl und Sonderzeichen.");
            return;
        }
        if (!password.equals(confirm)) {
            passwordField.getStyleClass().add("text-field-error");
            confirmPasswordField.getStyleClass().add("text-field-error");
            showError("Passwörter stimmen nicht überein.");
            return;
        }
        // KAT-04: Zustimmung zu den Nutzungsbedingungen ist Pflicht
        if (!termsCheckBox.isSelected()) {
            showError("Bitte akzeptieren Sie die Nutzungsbedingungen.");
            return;
        }

        new Thread(() -> {
            try {
                authService.register(username, email, password);
                Platform.runLater(() -> {
                    AlertHelper.showInfo("Erfolg", "Registrierung erfolgreich! Sie können sich jetzt anmelden.");
                    Navigator.navigateToLogin();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Fehler bei der Registrierung: " + e.getMessage()));
            }
        }).start();
    }

    // [Nzchupa | 2026-06-13] Enter-Taste zum Absenden — bessere UX in Formularfeldern
    // Allow pressing Enter in any field to submit the form
    @FXML
    public void handleEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleRegister();
        }
    }

    @FXML
    public void handleBackToLogin() {
        if (hasUnsavedRegistrationInput() && !AlertHelper.confirmDiscardUnsavedChanges()) {
            return;
        }
        Navigator.navigateToLogin();
    }

    private boolean hasUnsavedRegistrationInput() {
        return !usernameField.getText().trim().isBlank()
                || !emailField.getText().trim().isBlank()
                || !passwordField.getText().isBlank()
                || !confirmPasswordField.getText().isBlank();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearInputErrors() {
        usernameField.getStyleClass().remove("text-field-error");
        emailField.getStyleClass().remove("text-field-error");
        passwordField.getStyleClass().remove("text-field-error");
        confirmPasswordField.getStyleClass().remove("text-field-error");
        errorLabel.setVisible(false);
    }

    private void markAllEmpty() {
        if (usernameField.getText().isBlank()) usernameField.getStyleClass().add("text-field-error");
        if (emailField.getText().isBlank()) emailField.getStyleClass().add("text-field-error");
        if (passwordField.getText().isBlank()) passwordField.getStyleClass().add("text-field-error");
        if (confirmPasswordField.getText().isBlank()) confirmPasswordField.getStyleClass().add("text-field-error");
    }
    @FXML
    private void handleBackToHome() {
        Navigator.navigateToHome();
    }
}
