package com.ticketsystem.frontend;

import com.ticketsystem.frontend.util.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class FrontendApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
       primaryStage.setTitle("Ticket Support System");
        Navigator.setPrimaryStage(primaryStage);
        Navigator.navigateToHome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
