package com.ticketsystem.frontend.util;

import com.ticketsystem.frontend.FrontendApplication;
import com.ticketsystem.model.enums.UserRole;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.ticketsystem.frontend.util.ThemeManager;

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
        RealtimeWebSocketClient.getInstance().clearViewListeners();
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

    // [Nzchupa | 2026-06-12] TS-007: Modales Fenster öffnen — Profile und TicketDetail als Modal
    // Opens a view as an APPLICATION_MODAL window (blocks the owner) — used for Profile and TicketDetail
    public static void openModal(String fxmlFile, String title) {
        openModal(fxmlFile, title, null);
    }

    // [Nzchupa | 2026-06-13] TSS-005: onClose-Callback — wird nach dem Schließen des Modals ausgeführt
    // onClose callback runs after modal closes — used to refresh avatar after profile save
    public static void openModal(String fxmlFile, String title, Runnable onClose) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    FrontendApplication.class.getResource("/fxml/" + fxmlFile)
            );
            Parent root = loader.load();
            ThemeManager.apply(root);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    FrontendApplication.class.getResource("/css/styles.css").toExternalForm()
            );

            Stage modal = new Stage();
            modal.setTitle(title);
            modal.setScene(scene);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(primaryStage);
            modal.setMinWidth(900);
            modal.setMinHeight(650);
            modal.showAndWait();

            // Nach dem Schließen Callback ausführen (z.B. Avatar aktualisieren)
            // Run callback after modal is closed (e.g. refresh avatar in parent view)
            if (onClose != null) onClose.run();

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