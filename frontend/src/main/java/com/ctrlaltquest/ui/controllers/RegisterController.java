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
import javafx.scene.layout.VBox;
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
import java.util.regex.Pattern;

public class RegisterController {

    // --- Campos FXML ---
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private MediaView backgroundVideo;
    @FXML private Button btnRegister; 
    @FXML private VBox loadingLayer;  

    // --- Variables Lógicas ---
    private MediaPlayer videoPlayer;
    private final AuthDAO authDAO = new AuthDAO();
    private final EmailService emailService = new EmailService();

    @FXML
    public void initialize() {
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }
        
        SoundManager.getInstance().synchronizeMusic(); 
        setupTypingSounds(); 
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
                videoPlayer.setRate(0.5);

                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.play();
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error video registro: " + e.getMessage());
        }
    }

    public void setVideoPlaying(boolean play) {
        if (videoPlayer != null) {
            if (play) videoPlayer.play();
            else videoPlayer.pause();
        }
    }

    private void setupTypingSounds() {
        TextField[] fields = {usernameField, emailField, passwordField, confirmPasswordField};
        for (TextField field : fields) {
            field.setOnKeyTyped(e -> {
                SoundManager.playKeyClick(); 
            });
        }
    }

    @FXML
    public void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();

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
                if (videoPlayer != null) { videoPlayer.stop(); videoPlayer.dispose(); }

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

    @FXML
    public void handleRegister() {
        String user = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
            showAlert("Campos Incompletos", "Debes llenar todos los datos del formulario.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Correo Inválido", "El correo no tiene un formato válido.");
            return;
        }

        if (!pass.equals(confirm)) {
            showAlert("Error de Contraseña", "Las contraseñas no coinciden.");
            return;
        }

        AuditService.log(null, "INTENTO_REGISTRO", "Usuario: " + user + " | Email: " + email);

        if(btnRegister != null) btnRegister.setDisable(true);
        if(loadingLayer != null) loadingLayer.setVisible(true);

        Task<String> registerTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                if (authDAO.userExists(user, email)) {
                    throw new Exception("El usuario o correo ya están registrados.");
                }
                String token = authDAO.registerUser(user, email, pass);
                emailService.sendVerificationCode(email, token);
                return email; 
            }
        };

        registerTask.setOnSucceeded(e -> {
            if(btnRegister != null) btnRegister.setDisable(false);
            if(loadingLayer != null) loadingLayer.setVisible(false);
            
            String registeredEmail = registerTask.getValue();
            AuditService.log(null, "REGISTRO_PENDIENTE", "Código enviado a: " + registeredEmail);

            abrirVentanaVerificacion(registeredEmail);
        });

        registerTask.setOnFailed(e -> {
            if(btnRegister != null) btnRegister.setDisable(false);
            if(loadingLayer != null) loadingLayer.setVisible(false);
            
            Throwable error = registerTask.getException();
            showAlert("Error en el Registro", error.getMessage());
            AuditService.log(null, "REGISTRO_FALLIDO", "Error: " + error.getMessage());
        });

        new Thread(registerTask).start();
    }

    private void abrirVentanaVerificacion(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verify_code.fxml"));
            Parent root = loader.load();

            VerifyController verifyCtrl = loader.getController();
            verifyCtrl.setEmail(email);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(usernameField.getScene().getWindow());
            stage.initStyle(StageStyle.TRANSPARENT); 
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT); 
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert("Error de Interfaz", "No se pudo abrir la ventana de verificación.");
            e.printStackTrace();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ctrl + Alt + Quest");
        alert.setHeaderText(title);
        alert.setContentText(content);

        // Vincular el CSS al DialogPane
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/alerts.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");

        // Configurar el Stage de la alerta para ser transparente y sin bordes de Windows
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        if (usernameField.getScene() != null) {
            alert.initOwner(usernameField.getScene().getWindow());
        }

        alert.showAndWait();
    }
}