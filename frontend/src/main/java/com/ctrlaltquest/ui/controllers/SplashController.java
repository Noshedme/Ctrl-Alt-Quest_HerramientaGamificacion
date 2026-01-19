package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.ui.utils.SoundManager;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {
    @FXML private ProgressBar loadingBar;
    @FXML private Label loadingText;
    @FXML private StackPane root;
    @FXML private MediaView introVideo;
    @FXML private ImageView splashLogo;

    private MediaPlayer mediaPlayer;
    private double progress = 0.0;
    private final int TOTAL_CYCLES = 60; // Controla la duración de la barra

    private final String[] rpgTips = {
        "Inicializando el stack productivo...",
        "Configurando tu interfaz de desarrollo...",
        "Optimizando herramientas de productividad...",
        "Cargando aplicaciones de escritorio...",
        "Preparando tu espacio de trabajo...",
        "Activando monitores de aplicaciones...",
        "Sincronizando tu área de trabajo..."
    };
    private final Random random = new Random();

    @FXML
    public void initialize() {
        // 1. Cargar Logo
        URL logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null) {
            splashLogo.setImage(new Image(logoUrl.toExternalForm()));
            splashLogo.setSmooth(true);
            aplicarEfectoRespiracion();
        }

        // 2. Configurar Video de Fondo
        URL videoUrl = getClass().getResource("/assets/videos/introVideo.mp4");
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                introVideo.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setOnReady(() -> mediaPlayer.play());
                mediaPlayer.setOnError(() -> System.err.println("Error de video detectado."));
            } catch (Exception e) {
                System.err.println("No se pudo inicializar el video: " + e.getMessage());
            }
        }

        // 3. Iniciar barra de progreso
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> updateProgress()));
        timeline.setCycleCount(TOTAL_CYCLES); 
        timeline.setOnFinished(event -> playFadeOut());
        timeline.play();
    }

    private void aplicarEfectoRespiracion() {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(3), splashLogo);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.08); pulse.setToY(1.08);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private void updateProgress() {
        progress = Math.min(1.0, progress + (1.0 / TOTAL_CYCLES));
        loadingBar.setProgress(progress);
        
        if (Math.round(progress * TOTAL_CYCLES) % 15 == 0) {
            loadingText.setText(rpgTips[random.nextInt(rpgTips.length)]);
        }
    }

    private void playFadeOut() {
        loadingText.setText("¡Sistema listo!");
        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        // Al terminar el desvanecimiento, cargamos la siguiente escena (Términos)
        fadeOut.setOnFinished(event -> loadTermsScene());
        fadeOut.play();
    }

    private void loadTermsScene() {
        try {
            // Buscamos el archivo de acuerdos
            URL fxmlUrl = getClass().getResource("/fxml/terms_conditions.fxml");
            
            if (fxmlUrl == null) {
                System.err.println("❌ ERROR: No se encontró /fxml/terms_conditions.fxml. Saltando al Login...");
                loadLoginDirectly(); 
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent termsRoot = loader.load();
            
            // Limpiar media player para liberar recursos
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Stage stage = (Stage) root.getScene().getWindow();
            if (stage != null) {
                Scene scene = new Scene(termsRoot, 1280, 720);
                
                // Inyectar sonido de teclado global
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> SoundManager.playKeyClick());

                termsRoot.setOpacity(0.0);
                stage.setScene(scene);
                stage.centerOnScreen();
                
                // Transición de entrada suave para la nueva ventana
                FadeTransition fadeIn = new FadeTransition(Duration.millis(800), termsRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        } catch (IOException e) {
            System.err.println("❌ Fallo crítico cargando términos: " + e.getMessage());
            e.printStackTrace();
            loadLoginDirectly(); // Fallback por si acaso
        }
    }

    private void loadLoginDirectly() {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) root.getScene().getWindow();
            if (stage != null) {
                Scene scene = new Scene(loginRoot, 1280, 720);
                scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> SoundManager.playKeyClick());
                stage.setScene(scene);
            }
        } catch (IOException ex) {
            System.err.println("❌ Fallo total: No se pudo cargar ni Términos ni Login.");
        }
    }
}