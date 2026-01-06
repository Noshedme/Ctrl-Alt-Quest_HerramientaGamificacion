package com.ctrlaltquest.ui.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {
    @FXML
    private ProgressBar loadingBar;

    @FXML
    private Label loadingText;

    @FXML
    private StackPane root;

    private double progress = 0.0;

    public void initialize() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(150), event -> updateProgress()));
        timeline.setCycleCount(60);
        timeline.setOnFinished(event -> {
            loadingText.setText("Preparando la aventura...");
            playFadeOut();
        });
        timeline.play();
    }

    private void updateProgress() {
        progress = Math.min(1.0, progress + 0.02);
        loadingBar.setProgress(progress);
        int percent = (int) (progress * 100);
        loadingText.setText("Cargando mundo RPG... " + percent + "%");
    }

    private void playFadeOut() {
        loadingText.setText("Preparando la aventura...");
        FadeTransition fadeOut = new FadeTransition(Duration.millis(900), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> loadLoginScene());
        fadeOut.play();
    }

    private void loadLoginScene() {
        try {
            Parent nextRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) root.getScene().getWindow();
            stage.getScene().setRoot(nextRoot);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), nextRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } catch (Exception exception) {
            loadingText.setText("Error cargando la interfaz.");
        }
    }
}
