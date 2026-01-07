package com.ctrlaltquest.ui.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.net.URL;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private MediaView backgroundVideo;
    
    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        // Aseguramos que el video cargue antes que nada
        configurarVideo();
        
        // Desenfoque para el fondo
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
        }
    }

    private void configurarVideo() {
        try {
            URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                // Forzamos el play inmediato
                mediaPlayer.play();
            }
        } catch (Exception e) {
            System.err.println("Error al cargar video en Registro: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToLogin() {
        // Transición de Salida (Fade Out)
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), usernameField.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(e -> {
            try {
                // Cargar Login
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent loginRoot = loader.load();
                
                // Forzar opacidad 0 para la entrada
                loginRoot.setOpacity(0);
                
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.getScene().setRoot(loginRoot);
                
                // Transición de Entrada (Fade In)
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
                
                // Detener el video actual para liberar memoria
                if (mediaPlayer != null) mediaPlayer.stop();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        fadeOut.play();
    }

    @FXML
    public void handleRegister() {
        System.out.println("Forjando cuenta para: " + usernameField.getText());
    }
}