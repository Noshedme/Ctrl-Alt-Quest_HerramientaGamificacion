package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.ui.utils.SoundManager;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
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
    
    private MediaPlayer mediaPlayer;
    private boolean isPasswordVisible = false;
    private int currentNewsIndex = 0;

    @FXML
    public void initialize() {
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }
        cargarLogo();
        setupCarousel(); 
    }

    private void configurarVideo() {
        URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4");
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                mediaPlayer.setRate(0.5); 
                
                // Verificamos si los ajustes globales indican que el video debe estar pausado
                if (SettingsController.isVideoPaused) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.setOnReady(() -> mediaPlayer.play());
                }
            } catch (Exception e) {
                System.err.println("❌ Error carga video: " + e.getMessage());
            }
        }
    }

    public void setVideoPlaying(boolean play) {
        if (mediaPlayer != null) {
            if (play) mediaPlayer.play();
            else mediaPlayer.pause();
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
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                outNode.setManaged(false);
                inNode.setManaged(true);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(800), inNode);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void togglePassword() {
        if (isPasswordVisible) {
            passwordHidden.setText(passwordShown.getText());
            passwordHidden.setVisible(true);
            passwordShown.setVisible(false);
            btnTogglePassword.setText("👁");
        } else {
            passwordShown.setText(passwordHidden.getText());
            passwordShown.setVisible(true);
            passwordHidden.setVisible(false);
            btnTogglePassword.setText("🙈");
        }
        isPasswordVisible = !isPasswordVisible;
    }

    @FXML 
    public void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = isPasswordVisible ? passwordShown.getText() : passwordHidden.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Portal Cerrado", "Debes ingresar tu identidad y tu llave para cruzar.");
            return;
        }

        // --- LLAMADA AL DAO ACTUALIZADO CON AUDITORÍA ---
        // Este método valida el usuario, registra el dispositivo, la IP y el log en la BBDD
        if (AuthDAO.loginCompleto(user, pass)) {
            
            int userId = AuthDAO.getUserIdByUsername(user); 
            
            // Consultar personajes existentes
            Map<Integer, Character> personajes = CharacterDAO.getCharactersByUser(userId);

            if (personajes.isEmpty()) {
                // Usuario sin personajes -> Editor de Personaje
                System.out.println("✨ Nuevo héroe detectado. Abriendo la forja.");
                navigateToEditor(userId, user);
            } else {
                // Usuario veterano -> Selección de Personaje
                System.out.println("📜 Héroes encontrados. Cargando selección.");
                navigateToSelection(userId, user);
            }
            
            AuditService.log(null, "LOGIN_SUCCESS", "Acceso concedido para: " + user);
        } else {
            // El DAO ya registró el log de fallo internamente
            showAlert("Fallo Astral", "La identidad o la llave mágica no son válidas.");
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
            showAlert("Error", "No se pudo cargar la sala de selección.");
        }
    }

    private void navigateToEditor(int userId, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/character_editor.fxml"));
            Parent root = loader.load();
            
            CharacterEditorController ctrl = loader.getController();
            ctrl.setInitData(userId, 1); // Empezamos siempre en el Slot 1 para nuevos

            ejecutarCambioEscena(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo cargar el oráculo de creación.");
        }
    }

    private void ejecutarCambioEscena(Parent nextRoot) {
        Stage stage = (Stage) usernameField.getScene().getWindow();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), usernameField.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Scene nextScene = new Scene(nextRoot, 1280, 720);
            
            // Inyectar sonido global de teclado en la nueva escena
            nextScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                try { SoundManager.playKeyClick(); } catch(Exception ignored) {}
            });
            
            stage.setScene(nextScene);
            stage.centerOnScreen();

            nextRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), nextRoot);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML 
    public void handleOpenSettings() {
        try {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent registerRoot = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), usernameField.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                stage.getScene().setRoot(registerRoot);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), registerRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
                fadeIn.play();
            });
            fadeOut.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarLogo() {
        URL logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null && sidebarLogo != null) {
            sidebarLogo.setImage(new Image(logoUrl.toExternalForm()));
        }
    }

    @FXML public void handleForgotPassword() { 
        System.out.println("🔮 Invocando recuperación de runa a través del oráculo..."); 
    }
}