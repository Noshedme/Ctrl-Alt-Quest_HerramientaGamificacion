package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ResetPasswordController {

    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button btnReset;
    @FXML private VBox loadingLayer;
    @FXML private MediaView backgroundVideo;

    private String userEmail;
    private MediaPlayer videoPlayer;
    private final AuthDAO authDAO = new AuthDAO();

    @FXML
    public void initialize() {
        // --- EFECTO VISUAL DE FONDO ---
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new GaussianBlur(15));
            configurarVideo();
        }

        // inicializar Toast container
        try {
            StackPane root = (StackPane) codeField.getScene().getRoot();
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            Toast.initialize(toastContainer);
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar Toast: " + e.getMessage());
        }

        // --- SONIDOS Y EVENTOS ---
        setupFieldSounds(codeField, newPasswordField, confirmPasswordField);
        if (btnReset != null) {
            btnReset.setOnMouseEntered(e -> SoundManager.playHoverSound());
        }
        
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
                videoPlayer.setRate(0.5); // Velocidad épica lenta

                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.play();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error video reset: " + e.getMessage());
        }
    }

    public void setEmail(String email) {
        this.userEmail = email;
    }

    @FXML
    private void handleConfirmReset() {
        String code = codeField.getText().trim();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (code.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            SoundManager.playErrorSound();
            showToast("Campos Vacíos", "Por favor, completa todos los campos del ritual.", Toast.ToastType.WARNING);
            return;
        }

        if (newPass.length() < 6) {
            SoundManager.playErrorSound();
            showToast("Contraseña Débil", "La nueva credencial debe tener al menos 6 caracteres.", Toast.ToastType.WARNING);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            SoundManager.playErrorSound();
            showToast("Error de Coincidencia", "Las llaves no coinciden en el altar.", Toast.ToastType.ERROR);
            return;
        }

        loadingLayer.setVisible(true);
        btnReset.setDisable(true);
        SoundManager.playClickSound();

        new Thread(() -> {
            try {
                if (authDAO.verifyResetCode(userEmail, code)) {
                    if (authDAO.resetPassword(userEmail, newPass)) {
                        AuditService.log(null, "PASSWORD_RESET_SUCCESS", "Usuario: " + userEmail);
                        
                        Platform.runLater(() -> {
                            loadingLayer.setVisible(false);
                            showToast("¡Éxito!", "Tu nueva llave ha sido forjada correctamente.", Toast.ToastType.SUCCESS);
                            regresarAlLoginGlobal();
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        loadingLayer.setVisible(false);
                        btnReset.setDisable(false);
                        SoundManager.playErrorSound();
                        showToast("Código Inválido", "El código es incorrecto o se ha desvanecido.", Toast.ToastType.ERROR);
                    });
                }
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loadingLayer.setVisible(false);
                    btnReset.setDisable(false);
                    showToast("Error de BD", "El servidor no responde: " + e.getMessage(), Toast.ToastType.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        SoundManager.playClickSound();
        regresarAlLoginGlobal();
    }

    private void regresarAlLoginGlobal() {
        try {
            stopVideo();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();

            root.setOpacity(0);
            stage.getScene().setRoot(root);

            FadeTransition ft = new FadeTransition(Duration.millis(600), root);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } catch (IOException e) {
            System.err.println("Error al redirigir: " + e.getMessage());
        }
    }

    private void stopVideo() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
        }
    }

    private void setupFieldSounds(TextField... fields) {
        for (TextField f : fields) {
            if (f != null) f.setOnKeyTyped(e -> SoundManager.playKeyClick());
        }
    }

    private void showToast(String title, String message, Toast.ToastType type) {
        switch (type) {
            case SUCCESS:
                Toast.success(title, message);
                break;
            case ERROR:
                Toast.error(title, message);
                break;
            case WARNING:
                Toast.warning(title, message);
                break;
            default:
                Toast.info(title, message);
                break;
        }
    }
}