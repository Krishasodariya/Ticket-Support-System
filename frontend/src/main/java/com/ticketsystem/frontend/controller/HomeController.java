package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.frontend.util.ThemeManager;
import com.ticketsystem.model.enums.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class HomeController {

    // [Nzchupa | 2026-06-13] TSS-001: Button-Referenz für Icon-Update nach Theme-Toggle
    // Button reference so we can update the icon text after toggling
    @FXML private Button themeToggleBtn;

    // [Nzchupa | 2026-06-13] TSS-001: ThemeManager.apply() fehlte — Theme wurde nur intern umgeschaltet
    // Bug-Fix: ThemeManager.apply() was missing — theme toggled internally but UI never updated
    @FXML
    private void handleToggle() {
        ThemeManager.toggle();
        if (mainScrollPane != null && mainScrollPane.getScene() != null) {
            ThemeManager.apply(mainScrollPane.getScene().getRoot());
        }
        if (themeToggleBtn != null) themeToggleBtn.setText(ThemeManager.isDarkMode() ? "☀" : "🌙");
    }

    @FXML private ScrollPane mainScrollPane;
    @FXML private HBox homeSection;
    @FXML private VBox featuresSection;
    // [Nzchupa | 2026-06-12] TS-008: Neues Feld für die Rollen-Sektion
    // New field for the roles section
    @FXML private VBox rolesSection;
    @FXML private HBox aboutSection;
    @FXML private VBox contactSection;

    @FXML
    private void handleLogin() {
        Navigator.navigateTo("LoginView.fxml");
    }

    @FXML
    private void handleRegister() {
        Navigator.navigateTo("RegisterView.fxml");
    }

    @FXML
    private void handleCreateTicket() {
        if (!SessionManager.isLoggedIn()) {
            Navigator.navigateTo("LoginView.fxml");
            return;
        }

        if (SessionManager.hasRole(UserRole.CUSTOMER)) {
            Navigator.navigateTo("CustomerView.fxml");
        } else {
            Navigator.navigateAfterLogin(SessionManager.getRole());
        }
    }

    @FXML
    private void handleMyTickets() {
        if (!SessionManager.isLoggedIn()) {
            Navigator.navigateTo("LoginView.fxml");
            return;
        }

        Navigator.navigateAfterLogin(SessionManager.getRole());
    }

    @FXML
    private void scrollToHome() {
        scrollTo(0.0);
    }

    @FXML
    private void scrollToFeatures() {
        scrollTo(0.30);
    }

    // [Nzchupa | 2026-06-12] TS-008: scrollToRoles-Methode hinzugefügt — scrollt zur Rollen-Sektion
    // Added scrollToRoles method — scrolls to the new roles section
    @FXML
    private void scrollToRoles() {
        scrollTo(0.52);
    }

    @FXML
    private void scrollToAbout() {
        scrollTo(0.72);
    }

    @FXML
    private void scrollToContact() {
        scrollTo(1.0);
    }

    private void scrollTo(double value) {
        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(value);
        }
    }
    }