package com.ctrlaltquest.ui.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {
    @FXML
    private ProgressBar loadingBar;

    @FXML
    private Label loadingText;

    @FXML
    private StackPane root;

    @FXML
    private MediaView introVideo;

    @FXML
    private ImageView splashLogo;

    private MediaPlayer mediaPlayer;
    private double progress = 0.0;

    public void initialize() {
        var logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null) {
            splashLogo.setImage(new Image(logoUrl.toExternalForm()));
        }

        var videoUrl = getClass().getResource("/assets/videos/introVideo.mp4");
        if (videoUrl != null) {
            Media media = new Media(videoUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            introVideo.setMediaPlayer(mediaPlayer);
            mediaPlayer.setAutoPlay(true);
        }

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
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
        } catch (Exception exception) {
            loadingText.setText("Error cargando la interfaz.");
        }
    }
}
