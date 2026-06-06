package com.ticketsystem.frontend.util;

import javafx.scene.Parent;

public class ThemeManager {
    private static boolean darkMode = true;

    public static void toggle() {
        darkMode = !darkMode;
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void apply(Parent root) {
        if (root == null) return;
        root.getStyleClass().remove("theme-light");
        root.getStyleClass().remove("theme-dark");
        root.getStyleClass().add(darkMode ? "theme-dark" : "theme-light");
    }
}
