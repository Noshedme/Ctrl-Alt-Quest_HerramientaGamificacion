package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.services.EmailService;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class RegisterController {

    // --- Campos FXML ---
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    
    // Contraseña Principal
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordShown;
    @FXML private Button btnTogglePass;

    // Confirmar Contraseña
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordShown;
    @FXML private Button btnToggleConfirm;

    @FXML private MediaView backgroundVideo;
    @FXML private Button btnRegister; 
    @FXML private VBox loadingLayer;  

    // --- Variables Lógicas ---
    private MediaPlayer videoPlayer;
    private final AuthDAO authDAO = new AuthDAO();
    private final EmailService emailService = new EmailService();
    
    private boolean isPassVisible = false;
    private boolean isConfirmVisible = false;

    @FXML
    public void initialize() {
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }
        
        SoundManager.getInstance().synchronizeMusic(); 
        setupTypingSounds(); 
        
        // Sincronización de campos ocultos/visibles
        passwordShown.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordShown.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        
        // Inicializar Toast cuando la escena esté lista
        usernameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                initializeToast();
            }
        });
    }
    
    private void initializeToast() {
        try {
            StackPane root = (StackPane) usernameField.getScene().getRoot();
            
            // Crear contenedor de Toast
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            
            // Inicializar el sistema de Toast
            Toast.initialize(toastContainer);
            
            // Añadir al root
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar Toast: " + e.getMessage());
        }
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
        TextField[] fields = {usernameField, emailField, passwordField, passwordShown, confirmPasswordField, confirmPasswordShown};
        for (TextField field : fields) {
            field.setOnKeyTyped(e -> {
                SoundManager.playKeyClick(); 
            });
        }
    }

    // --- Lógica de visibilidad de contraseñas ---

    @FXML
    private void togglePassword() {
        isPassVisible = !isPassVisible;
        int caret = isPassVisible ? passwordField.getCaretPosition() : passwordShown.getCaretPosition();

        if (isPassVisible) {
            passwordShown.setVisible(true);
            passwordField.setVisible(false);
            btnTogglePass.setText("🙈");
            passwordShown.requestFocus();
            passwordShown.positionCaret(caret);
        } else {
            passwordField.setVisible(true);
            passwordShown.setVisible(false);
            btnTogglePass.setText("👁");
            passwordField.requestFocus();
            passwordField.positionCaret(caret);
        }
    }

    @FXML
    private void toggleConfirmPassword() {
        isConfirmVisible = !isConfirmVisible;
        int caret = isConfirmVisible ? confirmPasswordField.getCaretPosition() : confirmPasswordShown.getCaretPosition();

        if (isConfirmVisible) {
            confirmPasswordShown.setVisible(true);
            confirmPasswordField.setVisible(false);
            btnToggleConfirm.setText("🙈");
            confirmPasswordShown.requestFocus();
            confirmPasswordShown.positionCaret(caret);
        } else {
            confirmPasswordField.setVisible(true);
            confirmPasswordShown.setVisible(false);
            btnToggleConfirm.setText("👁");
            confirmPasswordField.requestFocus();
            confirmPasswordField.positionCaret(caret);
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
        Toast.info("Regresando", "Volviendo a la pantalla de inicio...");

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
                Toast.error("Error", "No se pudo regresar a la pantalla de inicio.");
            }
        });
        fadeOut.play();
    }

    @FXML
    public void handleRegister() {
        String user = usernameField.getText().trim();
        String email = emailField.getText().trim();
        // Obtenemos el texto del campo oculto (que está sincronizado con el visible)
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        // 1. Validación de campos vacíos
        if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
            Toast.warning("Campos Incompletos", "Debes llenar todos los datos del formulario.");
            return;
        }

        // 2. Validación de formato de email
        if (!isValidEmail(email)) {
            Toast.error("Correo Inválido", "El correo no tiene un formato válido.");
            return;
        }

        // 3. SEGURIDAD: Validación de robustez de contraseña
        if (!isValidPassword(pass)) {
            Toast.warning("Contraseña Insegura", 
                "Por seguridad, tu contraseña debe tener:\n" +
                "• Entre 8 y 16 caracteres.\n" +
                "• Al menos una letra mayúscula.\n" +
                "• Al menos un número.\n" +
                "• Al menos un carácter especial (@, #, $, %, etc.).");
            return;
        }

        // 4. Validación de coincidencia de contraseñas
        if (!pass.equals(confirm)) {
            Toast.error("Error de Contraseña", "Las contraseñas no coinciden.");
            return;
        }

        AuditService.log(null, "INTENTO_REGISTRO", "Usuario: " + user + " | Email: " + email);
        Toast.info("Registrando", "Verificando disponibilidad del usuario...");

        if(btnRegister != null) btnRegister.setDisable(true);
        if(loadingLayer != null) loadingLayer.setVisible(true);

        Task<String> registerTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                if (authDAO.userExists(user, email)) {
                    throw new Exception("El usuario o correo ya están registrados.");
                }
                // Aquí se asume que authDAO encripta la contraseña antes de guardar
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
            Toast.success("¡Registro Exitoso!", "Se envió un código de verificación a tu correo.");

            abrirVentanaVerificacion(registeredEmail);
        });

        registerTask.setOnFailed(e -> {
            if(btnRegister != null) btnRegister.setDisable(false);
            if(loadingLayer != null) loadingLayer.setVisible(false);
            
            Throwable error = registerTask.getException();
            Toast.error("Error en el Registro", error.getMessage());
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
            Toast.error("Error de Interfaz", "No se pudo abrir la ventana de verificación.");
            e.printStackTrace();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!._-])(?=\\S+$).{8,16}$";
        return Pattern.compile(passwordRegex).matcher(password).matches();
    }

}