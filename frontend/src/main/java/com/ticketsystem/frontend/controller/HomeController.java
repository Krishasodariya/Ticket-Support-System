package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.util.AlertHelper;
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
    // [Nzchupa | 2026-06-26] TS-208 (KAT-108): aboutSection ist jetzt eine VBox (Stats-Box + Demo-Hinweis-Label),
    // vorher war es eine HBox — Typ angepasst, da sich sonst eine ClassCastException beim Laden ergibt.
    // aboutSection is now a VBox (stats box + demo-data hint label); previously an HBox — type fixed
    // to match the new FXML structure, otherwise FXMLLoader throws a ClassCastException.
    @FXML private VBox aboutSection;
    @FXML private VBox contactSection;

    // [Nzchupa | 2026-06-26] TS-201 (KAT-02): Referenz auf den "Meine Tickets"-Button
    // Reference to the "Meine Tickets" button so it can be hidden for guests
    @FXML private Button myTicketsBtn;

    // [Nzchupa | 2026-06-26] TS-202 (KAT-38): Referenzen auf alle Nav-Buttons für die Active-State-Logik
    // References to all nav buttons, used to toggle the active style class
    @FXML private Button navHomeBtn;
    @FXML private Button navFeaturesBtn;
    @FXML private Button navRolesBtn;
    @FXML private Button navAboutBtn;
    @FXML private Button navContactBtn;

    // [Nzchupa | 2026-06-26] TS-201 (KAT-02): "Meine Tickets" ist ein rollenspezifischer Shortcut
    // und darf für nicht angemeldete Nutzer nicht sichtbar sein. Vorher war der Button immer
    // sichtbar und hat erst beim Klick zum Login weitergeleitet — reines Logikproblem, kein UI-Bug.
    // "Meine Tickets" is a role-specific shortcut and must not be visible for guests. Previously
    // the button was always visible and only redirected to login on click — a logic bug, not a UI one.
    @FXML
    private void initialize() {
        boolean loggedIn = SessionManager.isLoggedIn();
        if (myTicketsBtn != null) {
            myTicketsBtn.setVisible(loggedIn);
            myTicketsBtn.setManaged(loggedIn);
        }
        setActiveNav(navHomeBtn);
    }

    // [Nzchupa | 2026-06-26] TS-202 (KAT-38): Setzt die "active" Style-Klasse auf den übergebenen
    // Nav-Button und entfernt sie von allen anderen — visuelles Feedback, welcher Abschnitt aktiv ist.
    // Sets the "active" style class on the given nav button and removes it from all others.
    private void setActiveNav(Button active) {
        for (Button b : new Button[]{navHomeBtn, navFeaturesBtn, navRolesBtn, navAboutBtn, navContactBtn}) {
            if (b == null) continue;
            b.getStyleClass().remove("start-nav-button-active");
            if (b == active) {
                b.getStyleClass().add("start-nav-button-active");
            }
        }
    }

    // [Nzchupa | 2026-06-26] TS-216/TS-238 (KAT-03): Platzhalter-Handler für rechtliche Footer-Links —
    // reicht für ein Studentenprojekt ohne echten Betrieb, kein vollständiges Rechtskonzept nötig.
    // Placeholder handlers for the legal footer links — sufficient for a student project without
    // a real-world deployment; no full legal framework needed.
    @FXML
    private void handleImpressum() {
        AlertHelper.showInfo("Impressum", "Ticket Support System ist ein studentisches Teamprojekt ohne kommerziellen Betrieb. Ein vollständiges Impressum ist daher nicht erforderlich.");
    }

    @FXML
    private void handleDatenschutz() {
        AlertHelper.showInfo("Datenschutz", "Im Rahmen dieses studentischen Projekts werden keine echten Nutzerdaten kommerziell verarbeitet. Diese Seite dient als Platzhalter für eine spätere Datenschutzerklärung.");
    }

    @FXML
    private void handleNutzungsbedingungen() {
        AlertHelper.showInfo("Nutzungsbedingungen", "Diese Anwendung ist ein Demo-/Lernprojekt im Rahmen eines Hochschulkurses. Es gelten keine offiziellen Nutzungsbedingungen.");
    }

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
        setActiveNav(navHomeBtn);
    }

    @FXML
    private void scrollToFeatures() {
        scrollTo(0.30);
        setActiveNav(navFeaturesBtn);
    }

    // [Nzchupa | 2026-06-12] TS-008: scrollToRoles-Methode hinzugefügt — scrollt zur Rollen-Sektion
    // Added scrollToRoles method — scrolls to the new roles section
    @FXML
    private void scrollToRoles() {
        scrollTo(0.52);
        setActiveNav(navRolesBtn);
    }

    @FXML
    private void scrollToAbout() {
        scrollTo(0.72);
        setActiveNav(navAboutBtn);
    }

    @FXML
    private void scrollToContact() {
        scrollTo(1.0);
        setActiveNav(navContactBtn);
    }

    private void scrollTo(double value) {
        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(value);
        }
    }
    }