package com.ticketsystem.frontend.util;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public final class AvatarHelper {
    private AvatarHelper() {}

    public static void showAvatar(String imageUrl,
                                  ImageView imageView,
                                  Circle backgroundCircle,
                                  Label initialsLabel,
                                  double size) {
        if (imageView == null || initialsLabel == null || backgroundCircle == null) {
            return;
        }

        setupCircularClip(imageView, size);

        String url = imageUrl == null ? "" : imageUrl.trim();
        if (url.isBlank()) {
            showInitials(imageView, backgroundCircle, initialsLabel);
            return;
        }

        try {
            Image image = new Image(url, size, size, false, true, true);

            image.errorProperty().addListener((obs, oldValue, hasError) -> {
                if (hasError) {
                    runOnFxThread(() -> showInitials(imageView, backgroundCircle, initialsLabel));
                }
            });

            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0) {
                    if (image.isError()) {
                        runOnFxThread(() -> showInitials(imageView, backgroundCircle, initialsLabel));
                    } else {
                        runOnFxThread(() -> showImage(imageView, backgroundCircle, initialsLabel, image));
                    }
                }
            });

            // [Nzchupa | 2026-06-12] TS-004: Race condition behoben — fehlender Fehler-Zweig ergänzt
            // Bug-Fix: if image was already loaded before listeners were registered,
            // handle both success and error cases (previously only success was handled)
            if (image.getProgress() >= 1.0) {
                if (image.isError()) {
                    showInitials(imageView, backgroundCircle, initialsLabel);
                } else {
                    showImage(imageView, backgroundCircle, initialsLabel, image);
                }
            }
        } catch (IllegalArgumentException ex) {
            showInitials(imageView, backgroundCircle, initialsLabel);
        }
    }

    public static void setupCircularClip(ImageView imageView, double size) {
        if (imageView == null) {
            return;
        }
        double radius = size / 2.0;
        imageView.setClip(new Circle(radius, radius, radius));
    }

    public static void showInitials(ImageView imageView, Circle backgroundCircle, Label initialsLabel) {
        if (imageView != null) {
            imageView.setImage(null);
            imageView.setVisible(false);
            imageView.setManaged(false);
        }
        if (backgroundCircle != null) {
            backgroundCircle.setVisible(true);
        }
        if (initialsLabel != null) {
            initialsLabel.setVisible(true);
        }
    }

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static void showImage(ImageView imageView, Circle backgroundCircle, Label initialsLabel, Image image) {
        imageView.setImage(image);
        imageView.setVisible(true);
        imageView.setManaged(true);
        backgroundCircle.setVisible(false);
        initialsLabel.setVisible(false);
    }
}
