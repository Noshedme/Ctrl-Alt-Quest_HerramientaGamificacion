package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class VerifyController {

    @FXML private TextField  codeField;
    @FXML private MediaView  backgroundVideo;  // ← añadir fx:id en el FXML

    private String      userEmail;
    private MediaPlayer videoPlayer;
    private final AuthDAO authDAO = new AuthDAO();

    public void setEmail(String email) {
        this.userEmail = email;
    }

    // ════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Video de fondo con slow + blur (igual que Register)
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }

        // Sonidos de teclado
        if (codeField != null) codeField.setOnKeyTyped(e -> SoundManager.playKeyClick());

        // Toast container
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
            // La escena puede no estar lista en initialize() si es modal — se inicializa después
            codeField.sceneProperty().addListener((obs, old, newScene) -> {
                if (newScene != null) initToastDelayed();
            });
        }

        SoundManager.getInstance().synchronizeMusic();
    }

    private void initToastDelayed() {
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
        } catch (Exception ignored) {}
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
                videoPlayer.setRate(0.5); // slow motion igual que Register

                if (SettingsController.isVideoPaused) videoPlayer.pause();
                else videoPlayer.play();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error video verify: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LÓGICA DE VERIFICACIÓN
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            Toast.warning("Campo Vacío", "Debes ingresar el código de verificación.");
            return;
        }

        try {
            boolean isCorrect = authDAO.verifyUserCode(userEmail, code);

            if (isCorrect) {
                AuditService.log(null, "CUENTA_ACTIVADA", "Correo verificado: " + userEmail);
                Toast.success("¡Verificación Completada!", "Tu cuenta ha sido activada. Volviendo al inicio...");
                stopVideo();
                closeWindow();
                regresarAlLoginGlobal();
            } else {
                AuditService.log(null, "VERIFICACION_FALLIDA", "Código incorrecto para: " + userEmail);
                Toast.error("Código Inválido", "El código no coincide con el enviado a tu correo.");
            }
        } catch (Exception e) {
            Toast.error("Error en la Validación", "Ocurrió un error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        stopVideo();
        closeWindow();
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════════════════

    private void regresarAlLoginGlobal() {
        try {
            Stage mainStage = (Stage) Stage.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing() &&
                             !((Stage) w).getStyle().equals(StageStyle.TRANSPARENT))
                .findFirst().orElse(null);

            if (mainStage != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent loginRoot = loader.load();
                loginRoot.setOpacity(0);
                mainStage.getScene().setRoot(loginRoot);
                FadeTransition ft = new FadeTransition(Duration.millis(600), loginRoot);
                ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
            }
        } catch (IOException e) {
            System.err.println("Error al redirigir al Login: " + e.getMessage());
        }
    }

    private void stopVideo() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
            videoPlayer = null;
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        stage.close();
    }
}