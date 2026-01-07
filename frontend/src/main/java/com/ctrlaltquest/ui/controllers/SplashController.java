package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.ui.utils.SoundManager;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {
    @FXML private ProgressBar loadingBar;
    @FXML private Label loadingText;
    @FXML private StackPane root;
    @FXML private MediaView introVideo;
    @FXML private ImageView splashLogo;

    private MediaPlayer mediaPlayer;
    private double progress = 0.0;

    // Ajustado a 9 segundos: 9000ms / 100ms = 90 ciclos
    private final int TOTAL_CYCLES = 70;

    private final String[] rpgTips = {
        "Invocando el reino de los datos...",
        "Encantando la interfaz de usuario...",
        "Afilando las hojas de cálculo...",
        "Preparando herramientas de Office...",
        "Reclutando aventureros en la base de datos...",
        "Forjando videos de Youtube...",
        "Leyendo antiguos pergaminos del historial de navegación..."
    };
    private final Random random = new Random();

    @FXML
    public void initialize() {
        // 1. Cargar Logo con suavizado
        URL logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null) {
            splashLogo.setImage(new Image(logoUrl.toExternalForm()));
            splashLogo.setSmooth(true);
            aplicarEfectoRespiracion();
        }

        // 2. Cargar Video con Vía Segura
        URL videoUrl = getClass().getResource("/assets/videos/introVideo.mp4");
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                introVideo.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

                mediaPlayer.setOnReady(() -> {
                    mediaPlayer.play();
                });

                mediaPlayer.setOnError(() -> System.err.println("Error de video detectado."));

            } catch (Exception e) {
                System.err.println("No se pudo inicializar el video: " + e.getMessage());
            }
        }

        // 3. Simulación de Carga (9 segundos)
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> updateProgress()));
        timeline.setCycleCount(TOTAL_CYCLES); 
        timeline.setOnFinished(event -> playFadeOut());
        timeline.play();
    }

    private void aplicarEfectoRespiracion() {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(3), splashLogo);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.08); pulse.setToY(1.08);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private void updateProgress() {
        progress = Math.min(1.0, progress + (1.0 / TOTAL_CYCLES));
        loadingBar.setProgress(progress);
        
        if (Math.round(progress * TOTAL_CYCLES) % 15 == 0) {
            loadingText.setText(rpgTips[random.nextInt(rpgTips.length)]);
        }
    }

    private void playFadeOut() {
        loadingText.setText("¡Aventura lista!");
        FadeTransition fadeOut = new FadeTransition(Duration.millis(1200), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> loadLoginScene());
        fadeOut.play();
    }

    private void loadLoginScene() {
        try {
            Parent nextRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Stage stage = (Stage) root.getScene().getWindow();
            if (stage != null) {
                // Creamos la nueva escena para el Login
                Scene loginScene = new Scene(nextRoot, 1280, 720);
                
                // --- RE-APLICAR SONIDO DE TECLADO ---
                // Al ser una escena nueva, debemos inyectar el filtro de nuevo
                loginScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    SoundManager.playKeyClick();
                });

                nextRoot.setOpacity(0.0);
                stage.setScene(loginScene);
                stage.centerOnScreen();
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), nextRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}