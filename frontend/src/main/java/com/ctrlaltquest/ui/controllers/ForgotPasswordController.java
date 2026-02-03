package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.services.EmailService;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Button btnRecover;
    @FXML private VBox loadingLayer;
    @FXML private MediaView backgroundVideo;

    private MediaPlayer videoPlayer;
    private final AuthDAO authDAO = new AuthDAO();
    private final EmailService emailService = new EmailService();

    @FXML
    public void initialize() {
        // --- EFECTO VISUAL ---
        // Aplicamos el mismo blur (15) que usas en RegisterController
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new GaussianBlur(15));
            configurarVideo();
        }

        // --- SONIDOS ---
        emailField.setOnKeyTyped(e -> SoundManager.playKeyClick());
        btnRecover.setOnMouseEntered(e -> SoundManager.playHoverSound());
        
        // Sincronizar música si es necesario
        SoundManager.getInstance().synchronizeMusic();
    }

    private void configurarVideo() {
        try {
            URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                videoPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(videoPlayer);
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                
                // Mantenemos la consistencia con la velocidad lenta de Register (0.5)
                videoPlayer.setRate(0.5);

                // Sincronización con los ajustes globales del juego
                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.play();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al cargar video de fondo: " + e.getMessage());
        }
    }

    @FXML
    public void handleRecover() {
        String email = emailField.getText().trim();

        if (email.isEmpty() || !isValidEmail(email)) {
            SoundManager.playErrorSound();
            showAlert("Error de Pergamino", "La dirección de correo no es válida en este reino.");
            return;
        }

        btnRecover.setDisable(true);
        loadingLayer.setVisible(true);
        SoundManager.playClickSound();

        AuditService.log(null, "FORGOT_PASSWORD_REQ", "Email: " + email);

        Task<Boolean> recoverTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                String code = authDAO.generateResetCode(email);
                if (code != null) {
                    emailService.sendPasswordResetCode(email, code);
                    return true;
                }
                return false;
            }
        };

        recoverTask.setOnSucceeded(e -> {
            loadingLayer.setVisible(false);
            if (recoverTask.getValue()) {
                showAlert("Mensajero Enviado", "Un cuervo con tu código de recuperación va en camino.");
                abrirVentanaReset(email);
            } else {
                btnRecover.setDisable(false);
                SoundManager.playErrorSound();
                showAlert("Desconocido", "Este correo no pertenece a ningún habitante del reino.");
            }
        });

        recoverTask.setOnFailed(e -> {
            btnRecover.setDisable(false);
            loadingLayer.setVisible(false);
            SoundManager.playErrorSound();
            Throwable error = recoverTask.getException();
            showAlert("Error Arcano", "No pudimos contactar con el oráculo: " + error.getMessage());
        });

        new Thread(recoverTask).start();
    }

    private void abrirVentanaReset(String email) {
        try {
            stopVideo();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reset_password.fxml"));
            Parent root = loader.load();

            ResetPasswordController resetCtrl = loader.getController();
            resetCtrl.setEmail(email);

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            
            stage.setScene(scene);
            
            // Animación de entrada
            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(600), root);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

        } catch (IOException e) {
            System.err.println("❌ ERROR: No se encontró /fxml/reset_password.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToLogin() {
        Parent currentRoot = emailField.getScene().getRoot();
        Stage stage = (Stage) emailField.getScene().getWindow();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            try {
                stopVideo();
                SoundManager.playClickSound();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent loginRoot = loader.load();
                loginRoot.setOpacity(0);
                stage.getScene().setRoot(loginRoot);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            } catch (IOException ex) {
                System.err.println("Error al regresar al Login: " + ex.getMessage());
            }
        });
        fadeOut.play();
    }

    private void stopVideo() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sistema de Seguridad");
        alert.setHeaderText(title);
        alert.setContentText(content);

        try {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/styles/alerts.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
            
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.initStyle(StageStyle.TRANSPARENT);
            dialogPane.getScene().setFill(Color.TRANSPARENT);
        } catch (Exception e) {
            // Fallback
        }
        
        if (emailField.getScene() != null) {
            alert.initOwner(emailField.getScene().getWindow());
        }
        alert.showAndWait();
    }
}