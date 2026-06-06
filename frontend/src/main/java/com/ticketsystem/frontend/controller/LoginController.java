package com.ticketsystem.frontend.controller;

import com.ticketsystem.dto.response.AuthResponse;
import com.ticketsystem.frontend.service.AuthApiService;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthApiService authService = new AuthApiService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Bitte füllen Sie alle Felder aus.");
            return;
        }

        new Thread(() -> {
            try {
                AuthResponse response = authService.login(username, password);
                Platform.runLater(() -> {
                    SessionManager.login(response.getToken(), response.getUsername(), response.getRole());
                    Navigator.navigateAfterLogin(SessionManager.getRole());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Anmeldung fehlgeschlagen. Überprüfen Sie Ihre Daten."));
            }
        }).start();
    }

    @FXML
    public void handleRegisterLink() {
        Navigator.navigateTo("RegisterView.fxml");
    }

    @FXML
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
