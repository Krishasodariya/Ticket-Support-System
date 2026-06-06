package com.ticketsystem.frontend.util;

import com.ticketsystem.frontend.FrontendApplication;
import com.ticketsystem.model.enums.UserRole;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Navigator {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(FrontendApplication.class.getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            ThemeManager.apply(root);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(FrontendApplication.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Navigation Error", "Could not load view: " + fxmlFile);
        }
    }

    public static void navigateAfterLogin(UserRole role) {
        switch (role) {
            case ADMIN -> navigateTo("AdminView.fxml");
            case AGENT -> navigateTo("AgentView.fxml");
            case CUSTOMER -> navigateTo("CustomerView.fxml");
        }
    }

    public static void navigateToHome() {
        navigateTo("HomeView.fxml");
    }

    public static void navigateToLogin() {
        navigateTo("LoginView.fxml");
    }

    public static void logout() {
        SessionManager.clear();
        navigateToHome();
    }
}
