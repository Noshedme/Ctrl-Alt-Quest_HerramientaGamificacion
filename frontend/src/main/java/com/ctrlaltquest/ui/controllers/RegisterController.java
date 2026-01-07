package com.ctrlaltquest.ui.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
        // Configuración visual: Desenfoque y Video
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }

        // Sonido de teclado (si está activado en ajustes)
        setupTypingSounds();
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
                mediaPlayer.setRate(0.5); 
                
                // Respetar estado de pausa global
                if (SettingsController.isVideoPaused) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.play();
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar video en Registro: " + e.getMessage());
        }
    }

    // Método para que SettingsController controle el video
    public void setVideoPlaying(boolean play) {
        if (mediaPlayer != null) {
            if (play) mediaPlayer.play();
            else mediaPlayer.pause();
        }
    }

    private void setupTypingSounds() {
        // Aplica el efecto de sonido a todos los campos de texto
        TextField[] fields = {usernameField, emailField, passwordField, confirmPasswordField};
        for (TextField field : fields) {
            field.setOnKeyTyped(e -> {
                if (SettingsController.isTypingSoundEnabled) {
                    System.out.println("♪ Click!"); // Reemplazar con clip de audio real si lo deseas
                }
            });
        }
    }

    @FXML
    public void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            
            // Conectar este controlador con el de ajustes
            SettingsController settingsCtrl = loader.getController();
            settingsCtrl.setRegisterController(this);

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(usernameField.getScene().getWindow());
            settingsStage.initStyle(StageStyle.TRANSPARENT); 
            
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT); 
            settingsStage.setScene(scene);
            settingsStage.show();
            
            System.out.println("Consultando el oráculo de ajustes...");
        } catch (IOException e) {
            System.err.println("Error al abrir ajustes: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToLogin() {
        Parent currentRoot = usernameField.getScene().getRoot();
        Stage stage = (Stage) usernameField.getScene().getWindow();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent loginRoot = loader.load();
                
                loginRoot.setOpacity(0);
                stage.getScene().setRoot(loginRoot);
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }
                
                fadeIn.play();
            } catch (IOException ex) {
                System.err.println("Error al regresar al Login: " + ex.getMessage());
            }
        });
        fadeOut.play();
    }

    @FXML
    public void handleRegister() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (user.isEmpty() || pass.isEmpty() || emailField.getText().isEmpty()) {
            System.out.println("⚠️ El pergamino tiene campos vacíos.");
            return;
        }

        if (!pass.equals(confirm)) {
            System.out.println("❌ Las llaves mágicas no coinciden.");
            return;
        }

        System.out.println("✨ Forjando cuenta para: " + user);
    }
}