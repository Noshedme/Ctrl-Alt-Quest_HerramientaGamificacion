package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.prefs.Preferences;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;
import com.ctrlaltquest.ui.utils.WindowManager;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
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

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordHidden;
    @FXML private TextField passwordShown;
    @FXML private Button btnTogglePassword;
    @FXML private ImageView sidebarLogo;
    @FXML private MediaView backgroundVideo;
    @FXML private StackPane newsSlider; 
    @FXML private CheckBox rememberMeCheck;
    @FXML private VBox loadingLayer; 
    
    private MediaPlayer videoPlayer;
    private boolean isPasswordVisible = false;
    private int currentNewsIndex = 0;

    private Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    public void initialize() {
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }
        
        SoundManager.getInstance().synchronizeMusic(); 
        setupTypingSounds(); 
        
        cargarLogo();
        setupCarousel(); 
        verificarRecordatorios();
        
        // Sincronizar texto inicial por si acaso
        passwordShown.textProperty().bindBidirectional(passwordHidden.textProperty());
        
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
        URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4");
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                videoPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(videoPlayer);
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                videoPlayer.setRate(0.5); 
                
                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.setOnReady(() -> videoPlayer.play());
                }
            } catch (Exception e) {
                System.err.println("❌ Error carga video: " + e.getMessage());
            }
        }
    }

    private void setupTypingSounds() {
        TextField[] fields = {usernameField, passwordHidden, passwordShown};
        for (TextField field : fields) {
            field.setOnKeyTyped(e -> SoundManager.playKeyClick());
        }
    }

    private void verificarRecordatorios() {
        if (prefs.getBoolean("remember_active", false)) {
            String user = prefs.get("saved_user", "");
            String pass = prefs.get("saved_pass", "");
            usernameField.setText(user);
            passwordHidden.setText(pass);
            // El bind en initialize se encarga de passwordShown
            rememberMeCheck.setSelected(true);
        }
    }

    private void procesarGuardadoCredenciales(String user, String pass) {
        if (rememberMeCheck.isSelected()) {
            prefs.putBoolean("remember_active", true);
            prefs.put("saved_user", user);
            prefs.put("saved_pass", pass);
        } else {
            prefs.putBoolean("remember_active", false);
            prefs.remove("saved_user");
            prefs.remove("saved_pass");
        }
    }

    private void setupCarousel() {
        if (newsSlider == null) return;
        int totalNews = newsSlider.getChildren().size();
        if (totalNews <= 1) return;

        for (int i = 0; i < totalNews; i++) {
            newsSlider.getChildren().get(i).setOpacity(i == 0 ? 1.0 : 0.0);
            newsSlider.getChildren().get(i).setManaged(i == 0);
        }

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(6), event -> {
            Node outNode = newsSlider.getChildren().get(currentNewsIndex);
            currentNewsIndex = (currentNewsIndex + 1) % totalNews;
            Node inNode = newsSlider.getChildren().get(currentNewsIndex);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(800), outNode);
            fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                outNode.setManaged(false);
                inNode.setManaged(true);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(800), inNode);
                fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void togglePassword() {
        isPasswordVisible = !isPasswordVisible;
        
        // Guardamos la posición del cursor para que no salte al inicio
        int caretPos = isPasswordVisible ? passwordHidden.getCaretPosition() : passwordShown.getCaretPosition();

        if (isPasswordVisible) {
            passwordShown.setVisible(true);
            passwordHidden.setVisible(false);
            btnTogglePassword.setText("🙈"); // Icono de ocultar
            
            passwordShown.requestFocus();
            passwordShown.positionCaret(caretPos);
        } else {
            passwordHidden.setVisible(true);
            passwordShown.setVisible(false);
            btnTogglePassword.setText("👁"); // Icono de ver
            
            passwordHidden.requestFocus();
            passwordHidden.positionCaret(caretPos);
        }
    }

    @FXML 
    public void handleLogin() {
        String user = usernameField.getText().trim();
        // Al estar vinculados (bind), basta con obtener el texto de uno de los dos
        String pass = passwordHidden.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            SoundManager.playErrorSound();
            Toast.warning("Campos Incompletos", "Debes ingresar tu identidad para cruzar el umbral.");
            return;
        }

        if (loadingLayer != null) loadingLayer.setVisible(true);
        Toast.info("Iniciando Sesión", "Verificando tus credenciales...");

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() {
                return AuthDAO.loginCompleto(user, pass);
            }
        };

        loginTask.setOnSucceeded(e -> {
            if (loginTask.getValue()) {
                procesarGuardadoCredenciales(user, pass);
                int userId = SessionManager.getInstance().getUserId();
                String finalUsername = SessionManager.getInstance().getUsername();
                Map<Integer, Character> personajes = CharacterDAO.getCharactersByUser(userId);

                Toast.success("¡Bienvenido!", "Acceso concedido, " + finalUsername + "!");
                
                if (personajes.isEmpty()) {
                    navigateToEditor(userId, finalUsername);
                } else {
                    navigateToSelection(userId, finalUsername);
                }
                AuditService.log(null, "LOGIN_SUCCESS", "Acceso concedido para: " + finalUsername);
            } else {
                if (loadingLayer != null) loadingLayer.setVisible(false);
                SoundManager.playErrorSound();
                Toast.error("Credenciales Inválidas", "El usuario o contraseña no coinciden con nuestros registros.");
            }
        });

        loginTask.setOnFailed(e -> {
            if (loadingLayer != null) loadingLayer.setVisible(false);
            SoundManager.playErrorSound();
            Toast.error("Error de Conexión", "No se pudo establecer contacto con la base de datos.");
        });

        new Thread(loginTask).start();
    }

    @FXML 
    public void handleForgotPassword() { 
        try {
            SoundManager.playClickSound();
            Toast.info("Recuperación", "Abriendo formulario de recuperación...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forgot_password.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                limpiarRecursos();
                
                // Usar WindowManager para cambiar escena y mantener maximizado
                WindowManager.getInstance().changeScene(root);
                
                // Inyectar sonidos globales después de cambiar la escena
                Stage newStage = WindowManager.getInstance().getPrimaryStage();
                if (newStage != null && newStage.getScene() != null) {
                    newStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> SoundManager.playKeyClick());
                }
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.error("Error", "No se pudo cargar la interfaz de recuperación.");
        }
    }

    private void navigateToSelection(int userId, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/character_selection.fxml"));
            Parent root = loader.load();
            CharacterSelectionController ctrl = loader.getController();
            ctrl.initData(userId, username);
            ejecutarCambioEscena(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToEditor(int userId, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/character_editor.fxml"));
            Parent root = loader.load();
            CharacterEditorController ctrl = loader.getController();
            ctrl.setInitData(userId, 1); 
            ejecutarCambioEscena(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ejecutarCambioEscena(Parent nextRoot) {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(event -> {
            limpiarRecursos();
            
            // Usar WindowManager para cambiar escena y mantener maximizado
            WindowManager.getInstance().changeScene(nextRoot);
            
            // Inyectar sonidos globales después de cambiar la escena
            Stage newStage = WindowManager.getInstance().getPrimaryStage();
            if (newStage != null && newStage.getScene() != null) {
                newStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> SoundManager.playKeyClick());
            }
            
            nextRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), nextRoot);
            fadeIn.setToValue(1.0); fadeIn.play();
        });
        fadeOut.play();
    }

    private void limpiarRecursos() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
            videoPlayer = null;
        }
    }

    @FXML 
    public void handleOpenSettings() {
        try {
            SoundManager.playClickSound();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController settingsCtrl = loader.getController();
            settingsCtrl.setLoginController(this);
            
            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(usernameField.getScene().getWindow());
            settingsStage.initStyle(StageStyle.TRANSPARENT); 
            
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT); 
            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (IOException e) {
            System.err.println("❌ Error al abrir ajustes: " + e.getMessage());
        }
    }

    @FXML 
    public void handleGoToRegister() { 
        try {
            SoundManager.playClickSound();
            Toast.info("Registro", "Abriendo formulario de registro...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent registerRoot = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                limpiarRecursos();
                
                // Usar WindowManager para cambiar escena y mantener maximizado
                WindowManager.getInstance().changeScene(registerRoot);
                
                // Inyectar sonidos globales después de cambiar la escena
                Stage newStage = WindowManager.getInstance().getPrimaryStage();
                if (newStage != null && newStage.getScene() != null) {
                    newStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> SoundManager.playKeyClick());
                }
            });
            fadeOut.play();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.error("Error", "No se pudo cargar el formulario de registro.");
        }
    }

    private void cargarLogo() {
        URL logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null && sidebarLogo != null) {
            sidebarLogo.setImage(new Image(logoUrl.toExternalForm()));
        }
    }



    public void setVideoPlaying(boolean play) {
        if (videoPlayer != null) {
            if (play) videoPlayer.play();
            else videoPlayer.pause();
        }
    }
}