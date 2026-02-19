package com.ctrlaltquest.ui.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ctrlaltquest.dao.AchievementsDAO;
import com.ctrlaltquest.dao.ActivityDAO;
import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.ActivityMonitorService;
import com.ctrlaltquest.services.EventContextualListener;
import com.ctrlaltquest.services.EventContextualService;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.services.XPChangeListener;
import com.ctrlaltquest.services.XPSyncService;
import com.ctrlaltquest.ui.utils.EventContextualUI;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * 🎮 HOME CONTROLLER - HUB PRINCIPAL DEL JUEGO
 * IMPLEMENTA XPChangeListener para sincronización en tiempo real
 * IMPLEMENTA EventContextualListener para reaccionar a eventos dinámicos
 * ==========================================
 */
public class HomeController implements XPChangeListener, EventContextualListener {

    // ==========================================
    // SECCIÓN 1: ELEMENTOS DE UI (FXML)
    // ==========================================
    
    @FXML private BorderPane mainLayout;
    @FXML private StackPane contentArea;
    @FXML private VBox loadingLayer;
    
    // Header Superior (HUD del Jugador)
    @FXML private Label lblUsername;
    @FXML private Label lblClass;
    @FXML private Label lblLevel;
    @FXML private Label lblCoins;
    @FXML private Label lblXPText;
    @FXML private ImageView imgAvatarSmall;
    @FXML private ProgressBar xpBar;
    
    // Sidebar Derecha (Monitor de Actividad)
    @FXML private Label lblCurrentApp;
    @FXML private Label lblAppStatus;
    @FXML private Label lblHealthStreak;
    @FXML private ProgressBar healthBar;
    
    // Fondo Multimedia
    @FXML private MediaView backgroundVideo;
    
    // ==========================================
    // SECCIÓN 2: VARIABLES DE ESTADO
    // ==========================================
    
    private MediaPlayer videoPlayer;
    private Character currentCharacter;
    private int dbSessionId = -1;
    
    private final ActivityMonitorService monitorService = ActivityMonitorService.getInstance();
    private boolean isMonitoring = true;
    private Thread monitorThread;
    
    // Sistema de Caché de Vistas (Performance)
    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private String currentViewName = "";
    
    // ==========================================
    // SECCIÓN 3: EASTER EGGS & LOGROS SECRETOS
    // ==========================================
    
    private int avatarClickCount = 0;
    private long lastAvatarClickTime = 0;
    
    private final List<KeyCode> konamiCode = Arrays.asList(
        KeyCode.UP, KeyCode.UP, 
        KeyCode.DOWN, KeyCode.DOWN, 
        KeyCode.LEFT, KeyCode.RIGHT, 
        KeyCode.LEFT, KeyCode.RIGHT, 
        KeyCode.B, KeyCode.A
    );
    private int konamiIndex = 0;
    private long lastKonamiInputTime = 0;
    
    // ==========================================
    // SECCIÓN 4: INICIALIZACIÓN
    // ==========================================

    @FXML
    public void initialize() {
        System.out.println("🎮 [HomeController] Inicializando...");
        
        configurarFondo();
        configurarTooltips();
        iniciarMonitoreoActividad();
        configurarEfectosAvatar(); // <-- NUEVO: Prepara el avatar para ser interactivo
        
        try {
            SoundManager.getInstance().synchronizeMusic();
        } catch (Exception e) {}

        Platform.runLater(() -> {
            loadView("dashboard_view");
            
            if (mainLayout.getScene() != null) {
                setupInputListeners(mainLayout.getScene());
                aplicarEstilos(mainLayout.getScene());
            } else {
                mainLayout.sceneProperty().addListener((obs, old, newScene) -> {
                    if (newScene != null) {
                        setupInputListeners(newScene);
                        aplicarEstilos(newScene);
                    }
                });
            }
        });
    }

    private void configurarTooltips() {
        if (xpBar != null) Tooltip.install(xpBar, new Tooltip("Experiencia hasta el próximo nivel"));
        if (healthBar != null) Tooltip.install(healthBar, new Tooltip("Racha de días productivos"));
        if (imgAvatarSmall != null) Tooltip.install(imgAvatarSmall, new Tooltip("Clic para cambiar foto. (O 50 clics para un secreto)"));
    }
    
    /**
     * Da un efecto visual al avatar cuando pasas el mouse por encima para que el usuario
     * sepa que puede hacer clic en él para cambiar su foto.
     */
    private void configurarEfectosAvatar() {
        if (imgAvatarSmall != null) {
            imgAvatarSmall.setOnMouseEntered(e -> {
                imgAvatarSmall.setEffect(new DropShadow(15, Color.rgb(163, 53, 238, 0.8)));
                imgAvatarSmall.setStyle("-fx-cursor: hand;");
                ScaleTransition st = new ScaleTransition(Duration.millis(150), imgAvatarSmall);
                st.setToX(1.05); st.setToY(1.05); st.play();
            });
            
            imgAvatarSmall.setOnMouseExited(e -> {
                imgAvatarSmall.setEffect(null);
                ScaleTransition st = new ScaleTransition(Duration.millis(150), imgAvatarSmall);
                st.setToX(1.0); st.setToY(1.0); st.play();
            });
        }
    }

    private void aplicarEstilos(Scene scene) {
        try {
            String authCss = getClass().getResource("/styles/auth.css").toExternalForm();
            String homeCss = getClass().getResource("/styles/home.css").toExternalForm();
            
            if (!scene.getStylesheets().contains(authCss)) scene.getStylesheets().add(authCss);
            if (!scene.getStylesheets().contains(homeCss)) scene.getStylesheets().add(homeCss);
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando CSS: " + e.getMessage());
        }
    }

    private void setupInputListeners(Scene scene) {
        // Konami Code
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKonamiInputTime > 2000) konamiIndex = 0;
            
            if (event.getCode() == konamiCode.get(konamiIndex)) {
                konamiIndex++;
                lastKonamiInputTime = currentTime;
                if (konamiIndex == konamiCode.size()) {
                    activarKonamiCode();
                    konamiIndex = 0;
                }
            } else {
                konamiIndex = 0;
            }
        });

        // Click Avatar (Sirve para cambiar foto Y para el Easter Egg)
        if (imgAvatarSmall != null) {
            imgAvatarSmall.setPickOnBounds(true);
            imgAvatarSmall.setOnMouseClicked(e -> {
                long currentTime = System.currentTimeMillis();
                
                // Si hace doble clic rápido o un solo clic, abrimos el selector de imágenes
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                     cambiarImagenPerfil();
                }
                
                // Lógica del Easter Egg (50 clics)
                if (currentTime - lastAvatarClickTime > 3000) avatarClickCount = 0;
                avatarClickCount++;
                lastAvatarClickTime = currentTime;
                animarClickAvatar();
                
                if (avatarClickCount == 50) {
                    intentarDesbloquearLogro(903, "Spammer de Clicks", "¡Cálmate con el mouse! Has clickeado 50 veces tu avatar.");
                    avatarClickCount = 0;
                }
            });
        }
    }
    
    // ==========================================
    // SECCIÓN 4.5: CAMBIO DE IMAGEN DE PERFIL
    // ==========================================
    
    /**
     * Abre un selector de archivos para que el usuario elija su propio avatar.
     */
    private void cambiarImagenPerfil() {
        if (currentCharacter == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Elige tu nuevo Avatar");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        // Mostrar la ventana de selección encima de la actual
        Stage stage = (Stage) mainLayout.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                // 1. Cargar la imagen seleccionada y ponerla en el ImageView
                String imageUri = selectedFile.toURI().toString();
                Image newAvatar = new Image(imageUri);
                imgAvatarSmall.setImage(newAvatar);
                
                // 2. Guardar la ruta en el modelo (Asume que agregaste un campo 'avatarUrl' o similar a la clase Character)
                // currentCharacter.setAvatarUrl(selectedFile.getAbsolutePath());
                // CharacterDAO.saveCharacter(currentCharacter);
                
                mostrarNotificacion("¡Genial!", "Tu foto de perfil ha sido actualizada.");
                
            } catch (Exception ex) {
                System.err.println("❌ Error al cargar la nueva imagen: " + ex.getMessage());
                mostrarNotificacion("Error", "No se pudo cargar la imagen seleccionada.");
            }
        }
    }

    private void animarClickAvatar() {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), imgAvatarSmall);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.85); st.setToY(0.85);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private void activarKonamiCode() {
        intentarDesbloquearLogro(901, "Konami Code Master", "↑↑↓↓←→←→BA - ¡Has activado el código legendario!");
        if (currentCharacter != null) {
            currentCharacter.setCoins(currentCharacter.getCoins() + 500);
            CharacterDAO.saveCharacter(currentCharacter);
            actualizarUI();
            mostrarNotificacion("¡BONUS!", "+500 monedas por descubrir el Konami Code!");
        }
    }

    private void intentarDesbloquearLogro(int achievementId, String title, String msg) {
        int userId = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            boolean esNuevo = AchievementsDAO.unlockAchievement(userId, achievementId);
            if (esNuevo) {
                Platform.runLater(() -> {
                    try { SoundManager.playSuccessSound(); } catch (Exception e) {}
                    mostrarAlertaLogro(title, msg);
                });
            }
        }).start();
    }

    private void mostrarAlertaLogro(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🏆 SECRET UNLOCKED!");
        alert.setHeaderText("🏆 " + title);
        alert.setContentText(content);
        alert.initStyle(StageStyle.UTILITY);
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/home.css").toExternalForm());
        } catch (Exception e) {}
        alert.show();
    }

    private void mostrarNotificacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initStyle(StageStyle.UTILITY);
        alert.show();
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(alert::close);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // ==========================================
    // SECCIÓN 5: GESTIÓN DE DATOS DEL JUGADOR
    // ==========================================

    public void initPlayerData(Character character) {
        if (character == null) return;
        this.currentCharacter = character;

        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance().getUserId();
                this.dbSessionId = ActivityDAO.iniciarSesion(userId);
                
                MissionsDAO.inicializarMisionesGlobalesParaUsuario(userId);
                
                XPSyncService.getInstance().addXPChangeListener(HomeController.this);
                
                // Suscribirse a los Eventos Dinámicos
                EventContextualService.getInstance().addEventListener(this);
                
                // Instanciar la UI que pinta los eventos
                new EventContextualUI();
                
                // Activar motor de eventos
                EventContextualService.getInstance().startEventGenerator(userId);
                
                LocalTime now = LocalTime.now();
                if (now.getHour() == 3 && now.getMinute() == 33) {
                    intentarDesbloquearLogro(902, "Viajero del Tiempo", "Has entrado a las 3:33 AM... La hora maldita. 👻");
                }
                
                monitorService.startMonitoring(userId);
                
            } catch (Exception e) {
                System.err.println("❌ Error en initPlayerData background: " + e.getMessage());
            }
        }).start();

        Platform.runLater(this::actualizarUI);
    }

    public void actualizarUI() {
        if (currentCharacter == null) return;
        try {
            lblUsername.setText(currentCharacter.getName().toUpperCase());
            lblClass.setText(obtenerNombreClase(currentCharacter.getClassId()));
            lblLevel.setText(String.valueOf(currentCharacter.getLevel()));
            lblCoins.setText(String.valueOf(currentCharacter.getCoins()));
            
            cargarAvatarSmall(currentCharacter.getClassId());
            
            int xpActual = currentCharacter.getCurrentXp();
            int xpRequerido = currentCharacter.getLevel() * 1000;
            double xpProgress = (double) xpActual / xpRequerido;
            xpBar.setProgress(xpProgress);
            lblXPText.setText(xpActual + " / " + xpRequerido + " XP");
            
            healthBar.setProgress(1.0);
            if (lblHealthStreak != null) {
                int racha = currentCharacter.getHealthStreak();
                lblHealthStreak.setText("RACHA: " + racha + (racha == 1 ? " DÍA" : " DÍAS"));
            }
        } catch (Exception e) {}
    }

    public void refreshCharacterData() {
        if (currentCharacter == null) return;
        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance().getUserId();
                Map<Integer, Character> characters = CharacterDAO.getCharactersByUser(userId);
                if (!characters.isEmpty()) {
                    Character refreshed = characters.get(currentCharacter.getSlotIndex());
                    if (refreshed != null) {
                        this.currentCharacter = refreshed;
                        Platform.runLater(this::actualizarUI);
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    private void cargarAvatarSmall(int classId) {
        try {
            // Nota: Aquí podrías chequear si currentCharacter.getAvatarUrl() tiene algo 
            // y cargar esa imagen primero. Si está vacío, cargar las por defecto.
            
            String path = "/assets/images/sprites/base/class_" + classId + ".png";
            URL url = getClass().getResource(path);
            if (url == null) {
                path = "/assets/images/sprites/class_" + classId + "_idle.png";
                url = getClass().getResource(path);
            }
            if (url != null) imgAvatarSmall.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {}
    }

    private String obtenerNombreClase(int classId) {
        return switch (classId) {
            case 1 -> "PROGRAMADOR";
            case 2 -> "LECTOR";
            case 3 -> "ESCRITOR";
            default -> "AVENTURERO TECH";
        };
    }

    // ==========================================
    // SECCIÓN 6: NAVEGACIÓN ENTRE VISTAS
    // ==========================================

    private void loadView(String viewName) {
        if (viewName.equals(currentViewName)) return;
        try {
            Node nextView = viewCache.get(viewName);
            Object controller = controllerCache.get(viewName);
            
            if (nextView == null) {
                String path = "/fxml/views/" + viewName + ".fxml";
                URL url = getClass().getResource(path);
                if (url == null) return;

                FXMLLoader loader = new FXMLLoader(url);
                nextView = loader.load();
                controller = loader.getController();
                
                viewCache.put(viewName, nextView);
                controllerCache.put(viewName, controller);
            }

            if (controller != null) injectCharacterData(controller);

            animarCambioDeVista(nextView);
            currentViewName = viewName;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void injectCharacterData(Object controller) {
        int userId = SessionManager.getInstance().getUserId();
        // Inyección a sub-controladores
        if (controller instanceof com.ctrlaltquest.ui.controllers.views.CharacterPanelController) {
            ((com.ctrlaltquest.ui.controllers.views.CharacterPanelController) controller).setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ProfileViewController) {
            ((com.ctrlaltquest.ui.controllers.views.ProfileViewController) controller).setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.DashboardViewController) {
            com.ctrlaltquest.ui.controllers.views.DashboardViewController dashCtrl = (com.ctrlaltquest.ui.controllers.views.DashboardViewController) controller;
            dashCtrl.setUserId(userId);
            dashCtrl.setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.MissionsViewController) {
            ((com.ctrlaltquest.ui.controllers.views.MissionsViewController) controller).setUserId(userId);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ActivityViewController) {
            com.ctrlaltquest.ui.controllers.views.ActivityViewController activityCtrl = (com.ctrlaltquest.ui.controllers.views.ActivityViewController) controller;
            activityCtrl.setUserId(userId);
            activityCtrl.setHomeController(this);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.AchievementsViewController) {
            ((com.ctrlaltquest.ui.controllers.views.AchievementsViewController) controller).setUserId(userId);
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
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
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
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
    }

    // ==========================================
    // SECCIÓN 7: ACCIONES DEL MENÚ
    // ==========================================

    private void playClick() {
        try { SoundManager.playClickSound(); } catch(Exception e){}
    }

    @FXML private void showDashboard() { playClick(); loadView("dashboard_view"); }
    @FXML private void showActivity() { playClick(); loadView("activity_view"); }
    @FXML private void showMissions() { playClick(); loadView("missions_view"); }
    @FXML private void showStore() { playClick(); loadView("store_view"); }
    @FXML private void showInventory() { playClick(); loadView("character_panel"); }
    @FXML private void showStats() { playClick(); loadView("achievements_view"); }
    @FXML private void showProfile() { playClick(); loadView("profile_view"); }

    @FXML
    private void showSettingsModal() {
        try {
            playClick();
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
            settingsStage.setTitle("Ajustes");
            settingsStage.show();
        } catch (IOException e) {}
    }

    // ==========================================
    // SECCIÓN 8: MONITOREO DE ACTIVIDAD
    // ==========================================

    private void iniciarMonitoreoActividad() {
        monitorThread = new Thread(() -> {
            while (isMonitoring) {
                try {
                    String currentTitle = monitorService.getActiveWindowTitle();
                    boolean isProductive = monitorService.isProductive(currentTitle);
                    
                    // Notificar al sistema de eventos en qué estamos trabajando
                    EventContextualService.getInstance().updateCurrentActivity(currentTitle);

                    Platform.runLater(() -> actualizarPanelActividad(currentTitle, isProductive));
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {}
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void actualizarPanelActividad(String currentTitle, boolean isProductive) {
        if (lblCurrentApp != null) {
            String display = currentTitle.length() > 30 ? currentTitle.substring(0, 30) + "..." : currentTitle;
            lblCurrentApp.setText(display);
        }
        
        if (lblAppStatus != null) {
            if (isProductive) {
                lblAppStatus.setText("✓ PRODUCTIVO (+XP)");
                lblAppStatus.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: #90EE90; -fx-padding: 4 12; -fx-background-radius: 6; -fx-font-weight: bold;");
            } else {
                lblAppStatus.setText("○ OCIO / NEUTRAL");
                lblAppStatus.setStyle("-fx-background-color: #5a2d2d; -fx-text-fill: #FFB6C1; -fx-padding: 4 12; -fx-background-radius: 6; -fx-font-weight: bold;");
            }
        }
    }

    // ==========================================
    // SECCIÓN 9: VIDEO DE FONDO
    // ==========================================

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
                
                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.play();
                }
            }
        } catch (Exception e) {}
    }

    public void setVideoPlaying(boolean shouldPlay) {
        if (videoPlayer == null) return;
        Platform.runLater(() -> {
            if (shouldPlay) videoPlayer.play();
            else videoPlayer.pause();
        });
    }

    // ==========================================
    // SECCIÓN 10: CIERRE DE SESIÓN
    // ==========================================

    @FXML
    private void handleLogout() {
        playClick();
        if (loadingLayer != null) {
            loadingLayer.setVisible(true);
            loadingLayer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), loadingLayer);
            ft.setToValue(1.0); ft.play();
        }

        isMonitoring = false;
        monitorService.stopMonitoring();

        new Thread(() -> {
            try {
                if (dbSessionId != -1) ActivityDAO.cerrarSesion(dbSessionId);
                Thread.sleep(800);
                Platform.runLater(this::volverALogin);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void volverALogin() {
        try {
            cleanup();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainLayout.getScene().getWindow();
            
            FadeTransition fadeOut = new FadeTransition(Duration.millis(600), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.getScene().setRoot(root);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), root);
                fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
            });
            fadeOut.play();
        } catch (IOException e) {}
    }

    private void cleanup() {
        try {
            if (videoPlayer != null) {
                videoPlayer.stop(); videoPlayer.dispose(); videoPlayer = null;
            }
            viewCache.clear(); controllerCache.clear();
            XPSyncService.getInstance().removeXPChangeListener(this);
            EventContextualService.getInstance().removeEventListener(this);
            EventContextualService.getInstance().stopEventGenerator(SessionManager.getInstance().getUserId());
            SessionManager.getInstance().logout();
        } catch (Exception e) {}
    }
    
    // ==========================================
    // IMPLEMENTACIÓN DE XPChangeListener
    // ==========================================
    
    @Override
    public void onXPChanged(int userId, XPSyncService.XPChangeEvent event) {
        if (currentCharacter == null || SessionManager.getInstance().getUserId() != userId) return;
        
        Platform.runLater(() -> {
            try {
                XPSyncService.UserXPData data = XPSyncService.getInstance().getUserXPData(userId);
                if (data == null) return;
                
                double progress = XPSyncService.getInstance().getUserXPProgress(userId);
                xpBar.setProgress(progress);
                lblXPText.setText(data.currentXP + " / " + data.xpRequired + " XP");
                
                if (event.xpGained > 0) {
                    ScaleTransition scaleUp = new ScaleTransition(Duration.millis(300), xpBar);
                    scaleUp.setToY(1.1); scaleUp.setCycleCount(2); scaleUp.setAutoReverse(true); scaleUp.play();
                    showXPGainNotification(event.xpGained, event.source);
                }
            } catch (Exception e) {}
        });
    }
    
    @Override
    public void onLevelUp(int userId, int newLevel) {
        if (currentCharacter == null || SessionManager.getInstance().getUserId() != userId) return;
        
        Platform.runLater(() -> {
            try {
                lblLevel.setText(String.valueOf(newLevel));
                currentCharacter.setLevel(newLevel);
                xpBar.setProgress(0.0);
                
                XPSyncService.UserXPData data = XPSyncService.getInstance().getUserXPData(userId);
                if (data != null) lblXPText.setText(data.currentXP + " / " + data.xpRequired + " XP");
                
                playLevelUpAnimation();
                try { SoundManager.playLevelUpSound(); } catch (Exception e) {}
                mostrarNotificacion("🎉 ¡SUBIDA DE NIVEL!", "¡Felicidades! Acabas de alcanzar NIVEL " + newLevel + "! 🎊");
            } catch (Exception e) {}
        });
    }
    
    private void playLevelUpAnimation() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), lblLevel);
        scale.setToX(1.3); scale.setToY(1.3); scale.setCycleCount(2); scale.setAutoReverse(true);
        FadeTransition fade = new FadeTransition(Duration.millis(500), lblLevel);
        fade.setFromValue(1.0); fade.setToValue(0.7); fade.setCycleCount(2); fade.setAutoReverse(true);
        scale.play(); fade.play();
    }
    
    private void showXPGainNotification(int xpAmount, String source) {
        Label notifLabel = new Label("+ " + xpAmount + " XP");
        notifLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #FFD700;");
        notifLabel.setOpacity(1.0);
        StackPane.setAlignment(notifLabel, javafx.geometry.Pos.CENTER);
        FadeTransition fade = new FadeTransition(Duration.millis(1500), notifLabel);
        fade.setFromValue(1.0); fade.setToValue(0.0); fade.play();
    }

    // ==========================================
    // IMPLEMENTACIÓN DE EventContextualListener
    // ==========================================

    @Override
    public void onEventGenerated(int userId, EventContextualService.ContextualEvent event) { }

    @Override
    public void onEventStarted(int userId, EventContextualService.ContextualEvent event) { }

    @Override
    public void onEventProgressUpdated(int userId, EventContextualService.ContextualEvent event, int currentProgress, int target) { }

    @Override
    public void onEventCriticalPhase(int userId, EventContextualService.ContextualEvent event) { }

    @Override
    public void onEventCompleted(int userId, EventContextualService.ContextualEvent event, CompletionStatus status, int xpReward, int coinReward) {
        // Al terminar un evento, pedimos a la BD que actualice la interfaz por si ganamos oro o nivel.
        if (status == CompletionStatus.VICTORY) {
            refreshCharacterData();
        }
    }
}