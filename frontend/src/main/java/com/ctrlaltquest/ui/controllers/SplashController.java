package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

    @FXML
    public void initialize() {
        // Depuración rápida: muestra si los recursos están en el classpath
        System.out.println("logo resource: " + getClass().getResource("/assets/images/logo.png"));
        System.out.println("font resource: " + getClass().getResource("/assets/fonts/pixelcastle/Pixelcastle-Regular.otf"));
        System.out.println("video resource: " + getClass().getResource("/assets/videos/introVideo.mp4"));

        // Cargar logo de forma segura
        URL logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null) {
            try {
                splashLogo.setImage(new Image(logoUrl.toExternalForm()));
            } catch (Exception e) {
                System.err.println("ERROR cargando logo: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("ERROR: /assets/images/logo.png no encontrado en classpath");
        }

        // Cargar fuente de forma segura (si la necesitas aquí)
        try (InputStream fontIs = getClass().getResourceAsStream("/assets/fonts/pixelcastle/Pixelcastle-Regular.otf")) {
            if (fontIs != null) {
                javafx.scene.text.Font.loadFont(fontIs, 12);
            } else {
                System.err.println("WARNING: fuente Pixelcastle-Regular.otf no encontrada en classpath");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cargar video solo si existe
        URL videoUrl = getClass().getResource("/assets/videos/introVideo.mp4");
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                introVideo.setMediaPlayer(mediaPlayer);
                mediaPlayer.setAutoPlay(true);
            } catch (Exception e) {
                System.err.println("ERROR cargando video: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("INFO: /assets/videos/introVideo.mp4 no encontrado, se omitirá reproducción");
        }

        // Timeline / progreso
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(150), event -> updateProgress()));
        timeline.setCycleCount(60);
        timeline.setOnFinished(event -> {
            loadingText.setText("Preparando archivos...");
            playFadeOut();
        });
        timeline.play();
    }

    private void updateProgress() {
        progress = Math.min(1.0, progress + 0.02);
        loadingBar.setProgress(progress);
        int percent = (int) (progress * 100);
        loadingText.setText("Cargando herramientas de ofimática... " + percent + "%");
    }

    private void playFadeOut() {
        loadingText.setText("Preparando la aventura diigital...");
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
            exception.printStackTrace();
        }
    }
}
