package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Random;

import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.WindowManager;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {

    @FXML private ProgressBar loadingBar;
    @FXML private Label       loadingText;
    @FXML private StackPane   root;
    @FXML private ImageView   splashLogo;
    @FXML private ImageView   splashBg;      // ← fondo login_bg.png

    private double progress  = 0.0;
    private final int TOTAL_CYCLES = 60;

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

        // ── 1. Fondo con parallax flotante ───────────────────────────────────
        if (splashBg != null) {
            URL bgUrl = getClass().getResource("/assets/images/login_bg.png");
            if (bgUrl != null) {
                splashBg.setImage(new Image(bgUrl.toExternalForm()));
                splashBg.setSmooth(true);
                // Blur suave para dar profundidad
                splashBg.setEffect(new GaussianBlur(8));
                iniciarParallax();
            }
        }

        // ── 2. Logo con pulso + brillo animado ───────────────────────────────
        if (splashLogo != null) {
            URL logoUrl = getClass().getResource("/assets/images/logo.png");
            if (logoUrl != null) {
                splashLogo.setImage(new Image(logoUrl.toExternalForm()));
                splashLogo.setSmooth(true);
            }
            iniciarAnimacionLogo();
        }

        // ── 3. Barra de progreso ─────────────────────────────────────────────
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> updateProgress())
        );
        timeline.setCycleCount(TOTAL_CYCLES);
        timeline.setOnFinished(e -> playFadeOut());
        timeline.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // ANIMACIONES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parallax: el fondo flota suavemente en diagonal (X e Y independientes)
     * dando sensación de profundidad sin video.
     */
    private void iniciarParallax() {
        // Escalar ligeramente para que al mover no aparezcan bordes
        splashBg.setScaleX(1.08);
        splashBg.setScaleY(1.08);

        // Movimiento horizontal lento
        TranslateTransition ttX = new TranslateTransition(Duration.seconds(8), splashBg);
        ttX.setFromX(-18); ttX.setToX(18);
        ttX.setCycleCount(Animation.INDEFINITE);
        ttX.setAutoReverse(true);
        ttX.play();

        // Movimiento vertical ligeramente más rápido → sensación de deriva
        TranslateTransition ttY = new TranslateTransition(Duration.seconds(11), splashBg);
        ttY.setFromY(-12); ttY.setToY(12);
        ttY.setCycleCount(Animation.INDEFINITE);
        ttY.setAutoReverse(true);
        ttY.play();

        // Zoom pulsante muy sutil encima del parallax
        ScaleTransition zoom = new ScaleTransition(Duration.seconds(14), splashBg);
        zoom.setFromX(1.08); zoom.setFromY(1.08);
        zoom.setToX(1.13);   zoom.setToY(1.13);
        zoom.setCycleCount(Animation.INDEFINITE);
        zoom.setAutoReverse(true);
        zoom.play();
    }

    /**
     * Logo: pulso de escala + brillo que late como un corazón.
     */
    private void iniciarAnimacionLogo() {
        // Pulso de escala (respiración)
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.5), splashLogo);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.09);  pulse.setToY(1.09);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        // Brillo que aumenta y disminuye (Glow animado vía Timeline)
        Glow glow = new Glow(0.0);
        splashLogo.setEffect(glow);

        Timeline glowTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(glow.levelProperty(), 0.1)),
            new KeyFrame(Duration.seconds(1.2), new KeyValue(glow.levelProperty(), 0.75)),
            new KeyFrame(Duration.seconds(2.5), new KeyValue(glow.levelProperty(), 0.1))
        );
        glowTimeline.setCycleCount(Animation.INDEFINITE);
        glowTimeline.play();

        // Fade-in de entrada del logo al iniciar
        splashLogo.setOpacity(0);
        FadeTransition logoFadeIn = new FadeTransition(Duration.millis(1200), splashLogo);
        logoFadeIn.setFromValue(0); logoFadeIn.setToValue(1); logoFadeIn.play();

        // Pequeña rotación oscilante (-2° a +2°) para dinamismo extra
        Timeline rotacion = new Timeline(
            new KeyFrame(Duration.ZERO,         new KeyValue(splashLogo.rotateProperty(),  0.0)),
            new KeyFrame(Duration.seconds(3),   new KeyValue(splashLogo.rotateProperty(), -2.0)),
            new KeyFrame(Duration.seconds(6),   new KeyValue(splashLogo.rotateProperty(),  2.0)),
            new KeyFrame(Duration.seconds(9),   new KeyValue(splashLogo.rotateProperty(),  0.0))
        );
        rotacion.setCycleCount(Animation.INDEFINITE);
        rotacion.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROGRESO Y TRANSICIÓN
    // ════════════════════════════════════════════════════════════════════════

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
        fadeOut.setOnFinished(e -> loadTermsScene());
        fadeOut.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // NAVEGACIÓN
    // ════════════════════════════════════════════════════════════════════════

    private void loadTermsScene() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/terms_conditions.fxml");
            if (fxmlUrl == null) {
                loadLoginDirectly();
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent termsRoot = loader.load();
            termsRoot.setOpacity(0.0);
            WindowManager.getInstance().changeScene(termsRoot);
            Stage stage = WindowManager.getInstance().getPrimaryStage();
            if (stage != null && stage.getScene() != null)
                stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, ev -> SoundManager.playKeyClick());
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), termsRoot);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
        } catch (IOException e) {
            System.err.println("❌ Fallo cargando términos: " + e.getMessage());
            loadLoginDirectly();
        }
    }

    private void loadLoginDirectly() {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            WindowManager.getInstance().changeScene(loginRoot);
            Stage stage = WindowManager.getInstance().getPrimaryStage();
            if (stage != null && stage.getScene() != null)
                stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> SoundManager.playKeyClick());
        } catch (IOException ex) {
            System.err.println("❌ Fallo total: No se pudo cargar ni Términos ni Login.");
        }
    }
}