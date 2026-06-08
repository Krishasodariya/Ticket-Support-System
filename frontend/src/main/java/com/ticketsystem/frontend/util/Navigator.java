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

    private static final double DEFAULT_WIDTH = 1000;
    private static final double DEFAULT_HEIGHT = 700;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;

        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
    }

    public static void navigateTo(String fxmlFile) {
        try {
            if (primaryStage == null) {
                AlertHelper.showError("Navigation Error", "Primary stage is not initialized.");
                return;
            }

            // Поточний стан вікна перед переходом
            boolean wasMaximized = primaryStage.isMaximized();
            double width = primaryStage.getWidth() > 0 ? primaryStage.getWidth() : DEFAULT_WIDTH;
            double height = primaryStage.getHeight() > 0 ? primaryStage.getHeight() : DEFAULT_HEIGHT;
            double x = primaryStage.getX();
            double y = primaryStage.getY();

            FXMLLoader loader = new FXMLLoader(
                    FrontendApplication.class.getResource("/fxml/" + fxmlFile)
            );

            Parent root = loader.load();

            ThemeManager.apply(root);

            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(
                    FrontendApplication.class.getResource("/css/styles.css").toExternalForm()
            );

            primaryStage.setScene(scene);

            // Відновлення розміру після переходу
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.setX(x);
            primaryStage.setY(y);
            primaryStage.setMaximized(wasMaximized);

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