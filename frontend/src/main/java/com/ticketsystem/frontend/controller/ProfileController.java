package com.ticketsystem.frontend.controller;

import com.ticketsystem.dto.request.ProfileUpdateRequest;
import com.ticketsystem.frontend.model.UserFX;
import com.ticketsystem.frontend.service.UserApiService;
import com.ticketsystem.frontend.util.AlertHelper;
import com.ticketsystem.frontend.util.AvatarHelper;
import com.ticketsystem.frontend.util.Navigator;
import com.ticketsystem.frontend.util.SessionManager;
import com.ticketsystem.frontend.util.ThemeManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.util.Objects;

public class ProfileController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField profilePicField;
    @FXML private DatePicker birthDatePicker;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Circle avatarBackground;
    @FXML private ImageView profileImageView;
    @FXML private Label avatarInitials;
    @FXML private Label nameLabel;
    @FXML private Label roleBadge;

    private final UserApiService userService = new UserApiService();

    private String savedEmail = "";
    private String savedProfilePicture = "";
    private LocalDate savedBirthDate;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            Navigator.navigateToLogin();
            return;
        }

        usernameField.setText(SessionManager.getUsername());
        String initial = SessionManager.getUsername().length() > 0 ? SessionManager.getUsername().substring(0, 1).toUpperCase() : "U";
        avatarInitials.setText(initial);
        nameLabel.setText(SessionManager.getUsername());
        roleBadge.setText(SessionManager.getRole() != null ? SessionManager.getRole().name() : "USER");

        setupProfilePicturePreview();
        updateProfilePicturePreview(SessionManager.getProfilePicture());
        loadProfile();
    }

    private void loadProfile() {
        Task<UserFX> task = new Task<>() {
            @Override protected UserFX call() throws Exception { return userService.getCurrentUser(); }
        };
        task.setOnSucceeded(e -> {
            UserFX user = task.getValue();
            emailField.setText(user.getEmail());
            if (user.getBirthDate() != null) birthDatePicker.setValue(user.getBirthDate());
            profilePicField.setText(user.getProfilePicture() != null ? user.getProfilePicture() : "");
            SessionManager.setProfilePicture(profilePicField.getText());
            updateProfilePicturePreview(profilePicField.getText());
            rememberSavedState();
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Profil konnte nicht geladen werden."));
        new Thread(task).start();
    }

    @FXML
    public void handleSave() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail(emailField.getText());
        if (birthDatePicker.getValue() != null) request.setBirthDate(birthDatePicker.getValue());
        request.setProfilePicture(profilePicField.getText());

        Task<UserFX> task = new Task<>() {
            @Override protected UserFX call() throws Exception {
                return userService.updateProfile(request);
            }
        };
        task.setOnSucceeded(e -> {
            UserFX updatedUser = task.getValue();
            if (updatedUser != null) {
                emailField.setText(updatedUser.getEmail());
                birthDatePicker.setValue(updatedUser.getBirthDate());
                profilePicField.setText(updatedUser.getProfilePicture() != null ? updatedUser.getProfilePicture() : "");
            }
            SessionManager.setProfilePicture(profilePicField.getText());
            updateProfilePicturePreview(profilePicField.getText());
            rememberSavedState();
            AlertHelper.showInfo("Erfolg", "Profil erfolgreich aktualisiert.");
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Profil-Update fehlgeschlagen.\n" + task.getException().getMessage()));
        new Thread(task).start();
    }

    @FXML
    public void handleChangePassword() {
        String current = currentPasswordField.getText();
        String next = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isBlank() || next.isBlank() || confirm.isBlank()) {
            AlertHelper.showError("Fehler", "Bitte alle Passwortfelder ausfüllen.");
            return;
        }
        if (!next.equals(confirm)) {
            AlertHelper.showError("Fehler", "Neue Passwörter stimmen nicht überein.");
            return;
        }
        if (next.length() < 8 || !next.matches(".*[A-Za-z].*") || !next.matches(".*\\d.*") || !next.matches(".*[^A-Za-z0-9].*")) {
            AlertHelper.showError("Fehler", "Neues Passwort braucht mindestens 8 Zeichen, Buchstaben, Zahl und Sonderzeichen.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                userService.changePassword(current, next);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            AlertHelper.showInfo("Erfolg", "Passwort wurde geändert.");
        });
        task.setOnFailed(e -> AlertHelper.showError("Fehler", "Passwort konnte nicht geändert werden.\n" + task.getException().getMessage()));
        new Thread(task).start();
    }

    @FXML
    public void handleToggleTheme() {
        ThemeManager.toggle();
        Navigator.navigateTo("ProfileView.fxml");
    }

    @FXML
    public void handleBack() {
        if (hasUnsavedChanges() && !AlertHelper.confirmDiscardUnsavedChanges()) {
            return;
        }
        Navigator.navigateAfterLogin(SessionManager.getRole());
    }

    private void setupProfilePicturePreview() {
        if (profilePicField != null) {
            profilePicField.setOnAction(e -> updateProfilePicturePreview(profilePicField.getText()));
            profilePicField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    updateProfilePicturePreview(profilePicField.getText());
                }
            });
        }
    }

    private void updateProfilePicturePreview(String imageUrl) {
        AvatarHelper.showAvatar(imageUrl, profileImageView, avatarBackground, avatarInitials, 88);
    }

    private void rememberSavedState() {
        savedEmail = text(emailField);
        savedProfilePicture = text(profilePicField);
        savedBirthDate = birthDatePicker.getValue();
    }

    private boolean hasUnsavedChanges() {
        boolean profileChanged = !Objects.equals(text(emailField), savedEmail)
                || !Objects.equals(text(profilePicField), savedProfilePicture)
                || !Objects.equals(birthDatePicker.getValue(), savedBirthDate);

        boolean passwordFieldsFilled = !text(currentPasswordField).isBlank()
                || !text(newPasswordField).isBlank()
                || !text(confirmPasswordField).isBlank();

        return profileChanged || passwordFieldsFilled;
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }
}
