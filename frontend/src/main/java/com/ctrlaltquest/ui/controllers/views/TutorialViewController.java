package com.ctrlaltquest.ui.controllers.views;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.ui.controllers.HomeController;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * 📚 TUTORIAL VIEW CONTROLLER
 * Gestiona un carrusel interactivo de tutorial con imágenes y descripciones
 */
public class TutorialViewController {

    @FXML private VBox slideContainer;
    @FXML private HBox dotsContainer;
    @FXML private Label pageLabel;
    @FXML private Label quickTip;
    @FXML private MediaView backgroundVideo;

    private List<TutorialSlide> slides;
    private int currentSlideIndex = 0;
    private StackPane currentSlideNode;
    private MediaPlayer videoPlayer;
    
    // Referencia al controlador principal para navegación
    private HomeController homeController;

    @FXML
    public void initialize() {
        System.out.println("📚 [TutorialViewController] Inicializando Tutorial...");
        
        if (backgroundVideo != null) {
            backgroundVideo.setEffect(new javafx.scene.effect.GaussianBlur(15));
            configurarVideo();
        }
        
        SoundManager.getInstance().synchronizeMusic();
        
        // Inicializar Toast cuando la escena esté lista
        slideContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                initializeToast();
            }
        });
        
        loadTutorialSlides();
        showSlide(0);
    }
    
    /**
     * Inyecta la referencia al controlador principal para permitir navegación
     */
    public void setHomeController(HomeController controller) {
        this.homeController = controller;
    }
    
    private void initializeToast() {
        try {
            StackPane root = (StackPane) slideContainer.getScene().getRoot();
            
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
                
                videoPlayer.setOnReady(() -> videoPlayer.play());
            } catch (Exception e) {
                System.err.println("❌ Error carga video: " + e.getMessage());
            }
        }
    }

    /**
     * Carga todas las diapositivas del tutorial
     */
    private void loadTutorialSlides() {
        slides = new ArrayList<>();

        // Diapositiva 1: Inicio
        slides.add(new TutorialSlide(
            "¡Bienvenido a Ctrl+Alt+Quest!",
            "Esta es tu central de control gamificada. Aquí puedes monitorear tu actividad de codificación, ganar XP y desbloquear recompensas.",
            "dashboard",
            "Pantallazo principal mostrando el dashboard del sistema"
        ));

        // Diapositiva 2: Sistema de Misiones
        slides.add(new TutorialSlide(
            "📜 Sistema de Misiones",
            "Las misiones son tareas diarias que puedes completar. Completa misiones para ganar XP, experiencia y desbloquear logros especiales.",
            "missions",
            "Vista de misiones disponibles y en progreso"
        ));

        // Diapositiva 3: Ganancias de XP
        slides.add(new TutorialSlide(
            "⭐ Ganancia de Experiencia",
            "Ganas XP completando misiones y siendo productivo. Cada nivel tiene un requisito de XP diferente. ¡Sube de nivel para desbloquear mejoras!",
            "xp_system",
            "Sistema de XP mostrando barra de progreso"
        ));

        // Diapositiva 4: Rastreador de Actividad
        slides.add(new TutorialSlide(
            "📡 Rastreador de Actividad",
            "El sistema monitorea tu ventana activa. Las aplicaciones productivas (IDEs, editores) te dan +XP. Las actividades de ocio no generan XP.",
            "activity",
            "Monitor de actividad en tiempo real"
        ));

        // Diapositiva 5: Tienda y Recompensas
        slides.add(new TutorialSlide(
            "🛒 Tienda de Recompensas",
            "Gasta monedas en la tienda para comprar skins de personajes, efectos especiales y items cosméticos. Las monedas se ganan completando misiones.",
            "store",
            "Catálogo de items y recompensas disponibles"
        ));

        // Diapositiva 6: Logros y Trofeos
        slides.add(new TutorialSlide(
            "🏆 Logros Desbloqueables",
            "Desbloquea logros completando tareas especiales, secretos y desafíos. Los logros no solo son premios, ¡también cuentan tu historia!",
            "achievements",
            "Galería de logros y trofeos desbloqueados"
        ));

        // Diapositiva 7: Personaje y Perfil
        slides.add(new TutorialSlide(
            "👤 Tu Identidad Digital",
            "Edita tu perfil, cambia tu avatar, y personaliza tu experiencia. Tu personaje evoluciona contigo mientras avanzas en el juego.",
            "profile",
            "Panel de personalización del perfil"
        ));

        // Diapositiva 8: Atajos de Teclado
        slides.add(new TutorialSlide(
            "⌨️ Atajos del Teclado",
            "Domina los atajos del teclado para navegar más rápido: WASD para movimiento, ESC para menú, y muchos más. ¡Ve a Configuración para verlos todos!",
            "keybindings",
            "Tabla de atajos de teclado disponibles"
        ));

        // Diapositiva 9: Consejos Finales
        slides.add(new TutorialSlide(
            "🎮 Consejos Finales",
            "Sé consistente: completa misiones diarias, mantén tu racha viva, y participa en eventos especiales. ¡El verdadero poder viene de la dedicación!",
            "tips",
            "Consejos motivacionales para mejorar"
        ));

        crearIndicadores();
    }

    /**
     * Crea los puntos indicadores del carrusel
     */
    private void crearIndicadores() {
        dotsContainer.getChildren().clear();
        
        for (int i = 0; i < slides.size(); i++) {
            final int index = i;
            Button dot = new Button();
            dot.setStyle(
                "-fx-background-radius: 50%; " +
                "-fx-min-width: 12px; " +
                "-fx-min-height: 12px; " +
                "-fx-padding: 0; " +
                "-fx-background-color: " + (i == 0 ? "#f7d27a" : "rgba(255,255,255,0.2)") + "; " +
                "-fx-cursor: hand;" +
                "-fx-border-color: rgba(255,255,255,0.3); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 50%;"
            );
            dot.setOnAction(e -> {
                SoundManager.playClickSound();
                showSlide(index);
            });
            dotsContainer.getChildren().add(dot);
        }
    }

    /**
     * Muestra la diapositiva en el índice especificado
     */
    @FXML
    public void nextSlide() {
        SoundManager.playClickSound();
        if (currentSlideIndex < slides.size() - 1) {
            showSlide(currentSlideIndex + 1);
        } else {
            Toast.info("Final del Tutorial", "¡Has completado el tutorial! Explora el juego.");
        }
    }

    @FXML
    public void previousSlide() {
        SoundManager.playClickSound();
        if (currentSlideIndex > 0) {
            showSlide(currentSlideIndex - 1);
        }
    }

    private void showSlide(int index) {
        if (index < 0 || index >= slides.size()) return;

        currentSlideIndex = index;
        TutorialSlide slide = slides.get(index);

        // Fade out si hay una diapositiva anterior
        if (currentSlideNode != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentSlideNode);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                slideContainer.getChildren().clear();
                crearDiapositiva(slide);
            });
            fadeOut.play();
        } else {
            crearDiapositiva(slide);
        }

        // Actualizar indicadores
        actualizarIndicadores();
        
        // Actualizar consejo rápido
        quickTip.setText(slide.quickTip);
        
        // Actualizar etiqueta de página
        pageLabel.setText("Diapositiva " + (index + 1) + "/" + slides.size());
    }

    /**
     * Crea la diapositiva visual con imagen y descripción
     */
    private void crearDiapositiva(TutorialSlide slide) {
        currentSlideNode = new StackPane();
        currentSlideNode.setStyle("-fx-background-color: transparent;");

        // Contenedor principal
        VBox slideContent = new VBox();
        slideContent.setSpacing(20);
        slideContent.setAlignment(Pos.TOP_CENTER);
        slideContent.setStyle("-fx-background-color: rgba(26, 15, 38, 0.9); -fx-background-radius: 40; -fx-padding: 30; -fx-border-color: #f7d27a; -fx-border-radius: 40; -fx-border-width: 2;");

        // Título
        Label title = new Label(slide.title);
        title.getStyleClass().addAll("auth-title-hero");

        // Imagen
        ImageView imageView = new ImageView();
        imageView.setFitHeight(300);
        imageView.setFitWidth(500);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-cursor: hand;");

        // Cargar imagen (con fallback a icono)
        URL imageUrl = getClass().getResource("/assets/images/tutorial/" + slide.imageName + ".png");
        if (imageUrl != null) {
            imageView.setImage(new Image(imageUrl.toExternalForm()));
        } else {
            // Fallback: crear un rectángulo con texto
            VBox placeholder = new VBox();
            placeholder.setStyle(
                "-fx-background-color: rgba(138, 43, 226, 0.15); " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: rgba(138, 43, 226, 0.5); " +
                "-fx-border-radius: 10; " +
                "-fx-border-width: 2; " +
                "-fx-padding: 20;"
            );
            placeholder.setAlignment(Pos.CENTER);
            placeholder.setPrefHeight(300);
            placeholder.setPrefWidth(500);
            
            Label placeholderText = new Label("📸\n" + slide.imageName);
            placeholderText.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-text-fill: #8a2be2; " +
                "-fx-text-alignment: center;"
            );
            placeholder.getChildren().add(placeholderText);
            
            slideContent.getChildren().add(placeholder);
        }

        if (imageUrl != null) {
            slideContent.getChildren().add(imageView);
        }

        // Descripción
        Label description = new Label(slide.description);
        description.setWrapText(true);
        description.getStyleClass().addAll("card-text-pixel");

        slideContent.getChildren().addAll(title, description);

        currentSlideNode.getChildren().add(slideContent);

        // Efecto fade in
        currentSlideNode.setOpacity(0);
        slideContainer.getChildren().add(currentSlideNode);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), currentSlideNode);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Actualiza los indicadores de página (puntos)
     */
    private void actualizarIndicadores() {
        for (int i = 0; i < dotsContainer.getChildren().size(); i++) {
            Button dot = (Button) dotsContainer.getChildren().get(i);
            if (i == currentSlideIndex) {
                dot.setStyle(
                    "-fx-background-radius: 50%; " +
                    "-fx-min-width: 12px; " +
                    "-fx-min-height: 12px; " +
                    "-fx-padding: 0; " +
                    "-fx-background-color: #f7d27a; " +
                    "-fx-cursor: hand; " +
                    "-fx-border-color: #f7d27a; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 50%;"
                );
            } else {
                dot.setStyle(
                    "-fx-background-radius: 50%; " +
                    "-fx-min-width: 12px; " +
                    "-fx-min-height: 12px; " +
                    "-fx-padding: 0; " +
                    "-fx-background-color: rgba(255,255,255,0.2); " +
                    "-fx-cursor: hand; " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 50%;"
                );
            }
        }
    }

    /**
     * Clase interna para representar una diapositiva del tutorial
     */
    private static class TutorialSlide {
        String title;
        String description;
        String imageName;
        String quickTip;

        TutorialSlide(String title, String description, String imageName, String quickTip) {
            this.title = title;
            this.description = description;
            this.imageName = imageName;
            this.quickTip = quickTip;
        }
    }

    @FXML
    private void handleBack() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showDashboard();
        }
    }
    
    // ==========================================
    // SECCIÓN: MÉTODOS DE NAVEGACIÓN DEL MENÚ LATERAL
    // ==========================================
    
    @FXML
    private void showDashboard() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showDashboard();
        } else {
            Toast.info("Dashboard", "Cargando tablero de control");
        }
    }
    
    @FXML
    private void showMissions() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showMissions();
        } else {
            Toast.info("Misiones", "Cargando misiones disponibles");
        }
    }
    
    @FXML
    private void showActivity() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showActivity();
        } else {
            Toast.info("Actividad", "Abriendo monitor de actividad");
        }
    }
    
    @FXML
    private void showStore() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showStore();
        } else {
            Toast.info("Tienda", "Explorando tienda de recompensas");
        }
    }
    
    @FXML
    private void showInventory() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showInventory();
        } else {
            Toast.info("Inventario", "Revisando tu arsenal");
        }
    }
    
    @FXML
    private void showStats() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showStats();
        } else {
            Toast.info("Logros", "Consultando logros desbloqueados");
        }
    }
    
    @FXML
    private void showTutorial() {
        SoundManager.playClickSound();
        Toast.info("Tutorial", "Ya estás en el tutorial");
    }
    
    @FXML
    private void showProfile() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.showProfile();
        } else {
            Toast.info("Perfil", "Cargando tu tarjeta de jugador");
        }
    }
    
    @FXML
    private void handleLogout() {
        SoundManager.playClickSound();
        if (homeController != null) {
            homeController.handleLogout();
        } else {
            Toast.warning("Sesión", "Cerrando sesión...");
        }
    }
}