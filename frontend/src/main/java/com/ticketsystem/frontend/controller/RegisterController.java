package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.service.AuthApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final AuthApiService authService = new AuthApiService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
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
        if (username.length() < 3 || !username.matches("^[A-Za-z0-9_.-]+$")) {
            usernameField.getStyleClass().add("text-field-error");
            showError("Benutzername: mindestens 3 Zeichen, nur Buchstaben, Zahlen, Punkt, Unterstrich oder Bindestrich.");
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
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
