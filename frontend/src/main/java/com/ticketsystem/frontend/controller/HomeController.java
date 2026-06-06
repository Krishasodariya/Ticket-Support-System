package com.ticketsystem.frontend.controller;

import com.ticketsystem.frontend.util.Navigator;
import javafx.fxml.FXML;

public class HomeController {
    @FXML
    public void handleLogin() {
        Navigator.navigateToLogin();
    }

    @FXML
    public void handleRegister() {
        Navigator.navigateTo("RegisterView.fxml");
    }
}
