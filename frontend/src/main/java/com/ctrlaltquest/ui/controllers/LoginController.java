package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordHidden;
    @FXML private TextField passwordShown;
    @FXML private Button btnTogglePassword;
    @FXML private ImageView sidebarLogo;
    @FXML private MediaView backgroundVideo;

    // Elementos del carrusel
    @FXML private StackPane carouselContainer;
    @FXML private VBox newsSlider;
    
    private MediaPlayer mediaPlayer;
    private boolean isPasswordVisible = false;
    private int currentNewsIndex = 0;

    @FXML
    public void initialize() {
        // 1. Aplicar desenfoque al video de fondo para resaltar el panel
        backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
        
        // 2. Cargar el Logo de la Orden
        URL logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null && sidebarLogo != null) {
            sidebarLogo.setImage(new Image(logoUrl.toExternalForm()));
        }

        configurarVideo();
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
                mediaPlayer.setRate(1.1); 
                mediaPlayer.setOnReady(() -> mediaPlayer.play());
            } catch (Exception e) {
                System.err.println("Error carga video: " + e.getMessage());
            }
        }
    }

    private void setupCarousel() {
        Rectangle clip = new Rectangle(440, 450); 
        carouselContainer.setClip(clip);

        int totalNews = newsSlider.getChildren().size();
        double newsHeight = 450; 

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            if (totalNews > 0) {
                currentNewsIndex = (currentNewsIndex + 1) % totalNews;
                TranslateTransition slide = new TranslateTransition(Duration.millis(1200), newsSlider);
                slide.setToY(-currentNewsIndex * newsHeight);
                slide.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
                slide.play();
            }
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
        String pass = isPasswordVisible ? passwordShown.getText() : passwordHidden.getText();
        System.out.println("Intento de acceso - Aventurero: " + usernameField.getText());
    }

@FXML 
public void handleGoToRegister() {
    // 1. Obtenemos la raíz actual y el stage
    Parent currentRoot = usernameField.getScene().getRoot();
    Stage stage = (Stage) usernameField.getScene().getWindow();

    // 2. Transición de desvanecimiento de salida
    javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(500), currentRoot);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);
    
    fadeOut.setOnFinished(event -> {
        try {
            // 3. Cargar la nueva vista
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent registerRoot = loader.load();
            
            // IMPORTANTE: Asegurarnos de que el root de registro sea invisible al inicio
            registerRoot.setOpacity(0.0);
            
            // 4. Cambiar la raíz de la escena existente
            stage.getScene().setRoot(registerRoot);
            
            // 5. Transición de entrada
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(500), registerRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            // Si el video te da problemas, detén el MediaPlayer de login aquí antes de entrar
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            
            fadeIn.play();
            
        } catch (IOException e) {
            System.err.println("Error al cargar register.fxml: " + e.getMessage());
            e.printStackTrace();
            // Si falla, restauramos la opacidad del login para no quedar atrapados en blanco
            currentRoot.setOpacity(1.0);
        }
    });
    fadeOut.play();
}

    @FXML public void handleForgotPassword() { 
        System.out.println("Iniciando búsqueda de llave perdida..."); 
    }
}