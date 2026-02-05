package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.ctrlaltquest.dao.ActivityDAO;
import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.ActivityMonitorService;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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

public class HomeController {

    // --- ELEMENTOS DE UI ---
    @FXML private BorderPane mainLayout;
    @FXML private StackPane contentArea;
    @FXML private VBox loadingLayer;
    
    // Header Superior (HUD)
    @FXML private Label lblUsername, lblClass, lblLevel, lblCoins, lblXPText;
    @FXML private ImageView imgAvatarSmall;
    @FXML private ProgressBar xpBar;
    
    // Sidebar Derecha (Monitor)
    @FXML private Label lblCurrentApp, lblAppStatus, lblHealthStreak;
    @FXML private ProgressBar healthBar;
    
    // Fondo Multimedia
    @FXML private MediaView backgroundVideo;
    
    // --- VARIABLES DE LÓGICA ---
    private MediaPlayer videoPlayer;
    private Character currentCharacter;
    private int dbSessionId = -1; // ID de la sesión en PostgreSQL
    
    // Servicios y Caché
    private final ActivityMonitorService monitorService = ActivityMonitorService.getInstance(); 
    private boolean isMonitoring = true; 
    
    // ✅ CACHÉ DE VISTAS Y CONTROLADORES
    private Map<String, Node> viewCache = new HashMap<>(); 
    private Map<String, Object> controllerCache = new HashMap<>(); // Nuevo mapa para controladores

    @FXML
    public void initialize() {
        configurarFondo();
        iniciarMonitoreoActividad();
        SoundManager.getInstance().synchronizeMusic();

        // Cargar Dashboard inicial
        Platform.runLater(() -> loadView("dashboard_view"));

        // Inyectar las hojas de estilo de auth para que las vistas internas usen el mismo tema
        mainLayout.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                try {
                    String authCss = getClass().getResource("/styles/auth.css").toExternalForm();
                    String homeCss = getClass().getResource("/styles/home.css").toExternalForm();
                    if (!newScene.getStylesheets().contains(authCss)) newScene.getStylesheets().add(authCss);
                    if (!newScene.getStylesheets().contains(homeCss)) newScene.getStylesheets().add(homeCss);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudieron cargar hojas de estilo: " + e.getMessage());
                }
            }
        });
    }

    public void initPlayerData(Character character) {
        if (character == null) return;
        this.currentCharacter = character;

        // --- CONEXIÓN DB: Registrar inicio de sesión en activity_sessions ---
        new Thread(() -> {
            int userId = SessionManager.getInstance().getUserId();
            this.dbSessionId = ActivityDAO.iniciarSesion(userId);
            System.out.println("✅ Sesión BD iniciada: ID " + dbSessionId);
            
            // Inicializar mission_progress para todas las misiones GLOBALES del usuario
            System.out.println("🔄 Inicializando mission_progress para misiones globales...");
            MissionsDAO.inicializarMisionesGlobalesParaUsuario(userId);
            
            // Iniciar monitoreo lógico
            monitorService.startMonitoring(userId);
        }).start();

        Platform.runLater(() -> {
            // Actualizar Textos del HUD
            lblUsername.setText(character.getName().toUpperCase());
            lblClass.setText(obtenerNombreClase(character.getClassId()));
            lblLevel.setText(String.valueOf(character.getLevel()));
            lblCoins.setText(String.valueOf(character.getCoins()));
            
            // Cargar Avatar Pequeño
            cargarAvatarSmall(character.getClassId());
            
            // Actualizar Barras
            double xpProgress = (double) character.getCurrentXp() / 1000.0;
            xpBar.setProgress(xpProgress);
            lblXPText.setText(character.getCurrentXp() + " / 1000 XP");

            healthBar.setProgress(1.0);
            if(lblHealthStreak != null) 
                lblHealthStreak.setText("RACHA: " + character.getHealthStreak() + " DÍAS");
        });
    }

    private void cargarAvatarSmall(int classId) {
        try {
            String path = "/assets/images/sprites/base/class_" + classId + ".png";
            URL url = getClass().getResource(path);
            if (url != null) imgAvatarSmall.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar avatar pequeño: " + e.getMessage());
        }
    }

    // --- SISTEMA DE NAVEGACIÓN ---

    private void loadView(String viewName) {
        try {
            // Intentar obtener vista y controlador del caché
            Node nextView = viewCache.get(viewName);
            Object controller = controllerCache.get(viewName);
            
            if (nextView == null) {
                // Vista nueva - cargar desde FXML
                String path = "/fxml/views/" + viewName + ".fxml";
                URL url = getClass().getResource(path);
                if (url == null) {
                    System.err.println("⚠️ Vista no encontrada: " + path);
                    return; 
                }

                FXMLLoader loader = new FXMLLoader(url);
                nextView = loader.load();
                controller = loader.getController();
                
                // ✅ Cachear AMBOS: vista y controlador
                viewCache.put(viewName, nextView);
                controllerCache.put(viewName, controller);
            }

            // ✅ SIEMPRE inyectar datos (vital para actualizar vistas cacheadas)
            if (controller != null) {
                injectCharacterData(controller);
            }

            animarCambioDeVista(nextView);
            
        } catch (IOException e) {
            System.err.println("❌ Error navegando a " + viewName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inyecta los datos necesarios en el controlador de la vista.
     * Se llama CADA VEZ que se muestra una vista (nueva o cacheada).
     */
    private void injectCharacterData(Object controller) {
        int userId = SessionManager.getInstance().getUserId();
        System.out.println("🔍 DEBUG [HomeController]: Inyectando datos a controlador: " + controller.getClass().getSimpleName());
        System.out.println("🔍 DEBUG [HomeController]: userId = " + userId);

        if (controller instanceof com.ctrlaltquest.ui.controllers.views.CharacterPanelController) {
            ((com.ctrlaltquest.ui.controllers.views.CharacterPanelController) controller).setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ProfileViewController) {
            ((com.ctrlaltquest.ui.controllers.views.ProfileViewController) controller).setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.DashboardViewController) {
            System.out.println("✅ DashboardViewController detectado, inyectando userId");
            ((com.ctrlaltquest.ui.controllers.views.DashboardViewController) controller).setUserId(userId);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.MissionsViewController) {
            System.out.println("✅ MissionsViewController detectado, inyectando userId");
            ((com.ctrlaltquest.ui.controllers.views.MissionsViewController) controller).setUserId(userId);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ActivityViewController) {
            System.out.println("✅ ActivityViewController detectado, inyectando userId");
            ((com.ctrlaltquest.ui.controllers.views.ActivityViewController) controller).setUserId(userId);
        }
    }

    private void animarCambioDeVista(Node nextView) {
        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().add(nextView);
            fadeIn(nextView);
            return;
        }

        Node currentView = contentArea.getChildren().get(0);
        if (currentView == nextView) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(nextView);
            fadeIn(nextView);
        });
        fadeOut.play();
    }

    private void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // --- ACCIONES DEL MENÚ ---
    @FXML private void showDashboard() { SoundManager.playClickSound(); loadView("dashboard_view"); }
    @FXML private void showActivity() { SoundManager.playClickSound(); loadView("activity_view"); }
    @FXML private void showMissions() { SoundManager.playClickSound(); loadView("missions_view"); }
    @FXML private void showStore()    { SoundManager.playClickSound(); loadView("store_view"); }
    @FXML private void showInventory(){ SoundManager.playClickSound(); loadView("character_panel"); }
    @FXML private void showStats()    { SoundManager.playClickSound(); loadView("achievements_view"); }
    @FXML private void showSettings() { SoundManager.playClickSound(); loadView("profile_view"); }

    /**
     * Abre la ventana de ajustes como modal y enlaza este controlador
     * para que los cambios (p.ej. pausar video) se reflejen en tiempo real.
     */
    @FXML
    private void showSettingsModal() {
        try {
            SoundManager.playClickSound();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController settingsCtrl = loader.getController();
            settingsCtrl.setHomeController(this);

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(mainLayout.getScene().getWindow());
            settingsStage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (IOException e) {
            System.err.println("❌ Error al abrir ajustes: " + e.getMessage());
        }
    }

    /**
     * Permite a SettingsController controlar la reproducción del video.
     */
    public void setVideoPlaying(boolean shouldPlay) {
        if (videoPlayer == null) return;
        Platform.runLater(() -> {
            if (shouldPlay) videoPlayer.play();
            else videoPlayer.pause();
        });
    }

    // --- MONITOREO VISUAL (UI) ---
    private void iniciarMonitoreoActividad() {
        Thread monitorThread = new Thread(() -> {
            while (isMonitoring) {
                try {
                    String currentTitle = monitorService.getActiveWindowTitle();
                    boolean isProductive = monitorService.isProductive(currentTitle);

                    Platform.runLater(() -> {
                        if (lblCurrentApp != null) {
                            String display = currentTitle.length() > 22 ? currentTitle.substring(0, 22) + "..." : currentTitle;
                            lblCurrentApp.setText(display);
                        }
                        
                        if (lblAppStatus != null) {
                            if (isProductive) {
                                lblAppStatus.setText("ESTADO: PRODUCTIVO (+XP)");
                                lblAppStatus.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4;");
                            } else {
                                lblAppStatus.setText("ESTADO: OCIO / NEUTRAL");
                                lblAppStatus.setStyle("-fx-background-color: #5a2d2d; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4;");
                            }
                        }
                    });
                    Thread.sleep(2000); 
                } catch (InterruptedException e) { break; 
                } catch (Exception e) { System.err.println("Error monitor UI: " + e.getMessage()); }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private String obtenerNombreClase(int classId) {
        return switch (classId) {
            case 1 -> "PROGRAMADOR";
            case 2 -> "LECTOR";
            case 3 -> "ESCRITOR";
            default -> "AVENTURERO TECH";
        };
    }

    private void configurarFondo() {
        try {
            URL videoUrl = getClass().getResource("/assets/videos/main_hub.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                videoPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(videoPlayer);
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                videoPlayer.setRate(0.6); 
                backgroundVideo.setEffect(new GaussianBlur(20)); 
                
                if (SettingsController.isVideoPaused) videoPlayer.pause();
                else videoPlayer.play();
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar el video de fondo: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SoundManager.playClickSound();
        if (loadingLayer != null) {
            loadingLayer.setVisible(true);
            loadingLayer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), loadingLayer);
            ft.setToValue(1.0);
            ft.play();
        }

        isMonitoring = false;
        monitorService.stopMonitoring();

        new Thread(() -> {
            if (dbSessionId != -1) {
                ActivityDAO.cerrarSesion(dbSessionId);
            }

            try { Thread.sleep(800); } catch (InterruptedException e) {}

            Platform.runLater(() -> {
                try {
                    if (videoPlayer != null) { videoPlayer.stop(); videoPlayer.dispose(); }
                    SessionManager.getInstance().logout();

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) mainLayout.getScene().getWindow();
                    
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(600), stage.getScene().getRoot());
                    fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(e -> {
                        stage.getScene().setRoot(root);
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), root);
                        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
                        fadeIn.play();
                    });
                    fadeOut.play();
                } catch (IOException e) { e.printStackTrace(); }
            });
        }).start();
    }
}