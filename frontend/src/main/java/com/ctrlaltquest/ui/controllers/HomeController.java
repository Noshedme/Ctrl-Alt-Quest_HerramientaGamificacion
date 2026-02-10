package com.ctrlaltquest.ui.controllers;

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
import com.ctrlaltquest.services.SessionManager;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * 🎮 HOME CONTROLLER - HUB PRINCIPAL DEL JUEGO
 * ==========================================
 */
public class HomeController {

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
        SoundManager.getInstance().synchronizeMusic();

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

    /**
     * Configura tooltips informativos para los elementos de UI
     */
    private void configurarTooltips() {
        if (xpBar != null) {
            Tooltip.install(xpBar, new Tooltip("Experiencia hasta el próximo nivel"));
        }
        if (healthBar != null) {
            Tooltip.install(healthBar, new Tooltip("Racha de días productivos"));
        }
        if (imgAvatarSmall != null) {
            Tooltip.install(imgAvatarSmall, new Tooltip("Haz click 50 veces... 🤔"));
        }
    }

    /**
     * Aplica los estilos CSS a la escena
     */
    private void aplicarEstilos(Scene scene) {
        try {
            String authCss = getClass().getResource("/styles/auth.css").toExternalForm();
            String homeCss = getClass().getResource("/styles/home.css").toExternalForm();
            
            if (!scene.getStylesheets().contains(authCss)) {
                scene.getStylesheets().add(authCss);
            }
            if (!scene.getStylesheets().contains(homeCss)) {
                scene.getStylesheets().add(homeCss);
            }
            
            System.out.println("✅ Estilos CSS aplicados correctamente");
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando CSS: " + e.getMessage());
        }
    }

    /**
     * Configura listeners para Easter Eggs (Konami Code, clicks en avatar)
     */
    private void setupInputListeners(Scene scene) {
        // Listener para Konami Code
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            long currentTime = System.currentTimeMillis();
            
            // Reset si pasan más de 2 segundos entre teclas
            if (currentTime - lastKonamiInputTime > 2000) {
                konamiIndex = 0;
            }
            
            if (event.getCode() == konamiCode.get(konamiIndex)) {
                konamiIndex++;
                lastKonamiInputTime = currentTime;
                System.out.println("🕹️ Konami progreso: " + konamiIndex + "/" + konamiCode.size());
                
                if (konamiIndex == konamiCode.size()) {
                    activarKonamiCode();
                    konamiIndex = 0;
                }
            } else {
                konamiIndex = 0;
            }
        });

        // Listener para clicks en avatar (Easter Egg)
        if (imgAvatarSmall != null) {
            imgAvatarSmall.setPickOnBounds(true);
            imgAvatarSmall.setOnMouseClicked(e -> {
                long currentTime = System.currentTimeMillis();
                
                // Reset contador si pasan más de 3 segundos
                if (currentTime - lastAvatarClickTime > 3000) {
                    avatarClickCount = 0;
                }
                
                avatarClickCount++;
                lastAvatarClickTime = currentTime;
                
                // Pequeña animación de feedback
                animarClickAvatar();
                
                if (avatarClickCount == 50) {
                    intentarDesbloquearLogro(903, "Spammer de Clicks", 
                        "¡Cálmate con el mouse! Has clickeado 50 veces tu avatar.");
                    avatarClickCount = 0;
                }
            });
        }
    }

    /**
     * Animación de feedback al hacer click en el avatar
     */
    private void animarClickAvatar() {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), imgAvatarSmall);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.9);
        st.setToY(0.9);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    /**
     * Activa el Easter Egg del Konami Code
     */
    private void activarKonamiCode() {
        intentarDesbloquearLogro(901, "Konami Code Master", 
            "↑↑↓↓←→←→BA - ¡Has activado el código legendario!");
        
        // Bonus: 500 monedas extra
        if (currentCharacter != null) {
            currentCharacter.setCoins(currentCharacter.getCoins() + 500);
            CharacterDAO.saveCharacter(currentCharacter);
            actualizarUI();
            
            mostrarNotificacion("¡BONUS!", "+500 monedas por descubrir el Konami Code!");
        }
    }

    /**
     * Intenta desbloquear un logro. Si es nuevo, muestra alerta.
     */
    private void intentarDesbloquearLogro(int achievementId, String title, String msg) {
        int userId = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            boolean esNuevo = AchievementsDAO.unlockAchievement(userId, achievementId);
            if (esNuevo) {
                Platform.runLater(() -> {
                    SoundManager.playSuccessSound();
                    mostrarAlertaLogro(title, msg);
                });
            }
        }).start();
    }

    /**
     * Muestra una alerta de logro desbloqueado
     */
    private void mostrarAlertaLogro(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🏆 SECRET UNLOCKED!");
        alert.setHeaderText("🏆 " + title);
        alert.setContentText(content);
        alert.initStyle(StageStyle.UTILITY);
        
        // Aplicar estilos si es posible
        try {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(
                getClass().getResource("/styles/home.css").toExternalForm()
            );
        } catch (Exception e) {
            // Ignorar si falla
        }
        
        alert.show();
    }

    /**
     * Muestra una notificación temporal en pantalla
     */
    private void mostrarNotificacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initStyle(StageStyle.UTILITY);
        alert.show();
        
        // Auto-cerrar después de 3 segundos
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

    /**
     * Inicializa los datos del jugador en la UI y en el backend
     */
    public void initPlayerData(Character character) {
        if (character == null) {
            System.err.println("⚠️ Character es null en initPlayerData");
            return;
        }
        
        this.currentCharacter = character;
        System.out.println("✅ Personaje cargado: " + character.getName() + " (Nivel " + character.getLevel() + ")");

        // Inicialización en background thread
        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance().getUserId();
                
                // 1. Iniciar sesión en BD
                this.dbSessionId = ActivityDAO.iniciarSesion(userId);
                System.out.println("✅ Sesión BD iniciada: ID " + dbSessionId);
                
                // 2. Inicializar misiones globales del usuario
                MissionsDAO.inicializarMisionesGlobalesParaUsuario(userId);
                System.out.println("✅ Misiones globales inicializadas");
                
                // 3. Easter Egg: Hora maldita (3:33 AM)
                LocalTime now = LocalTime.now();
                if (now.getHour() == 3 && now.getMinute() == 33) {
                    intentarDesbloquearLogro(902, "Viajero del Tiempo", 
                        "Has entrado a las 3:33 AM... La hora maldita. 👻");
                }
                
                // 4. Iniciar monitoreo de actividad
                monitorService.startMonitoring(userId);
                System.out.println("✅ Monitoreo de actividad iniciado");
                
            } catch (Exception e) {
                System.err.println("❌ Error en initPlayerData background: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        // Actualizar UI en JavaFX thread
        Platform.runLater(this::actualizarUI);
    }

    /**
     * Actualiza todos los elementos de la UI con los datos actuales del personaje
     */
    public void actualizarUI() {
        if (currentCharacter == null) return;
        
        try {
            // Header - Información del personaje
            lblUsername.setText(currentCharacter.getName().toUpperCase());
            lblClass.setText(obtenerNombreClase(currentCharacter.getClassId()));
            lblLevel.setText(String.valueOf(currentCharacter.getLevel()));
            lblCoins.setText(String.valueOf(currentCharacter.getCoins()));
            
            // Avatar
            cargarAvatarSmall(currentCharacter.getClassId());
            
            // Barra de XP
            int xpActual = currentCharacter.getCurrentXp();
            int xpRequerido = currentCharacter.getLevel() * 1000;
            double xpProgress = (double) xpActual / xpRequerido;
            xpBar.setProgress(xpProgress);
            lblXPText.setText(xpActual + " / " + xpRequerido + " XP");
            
            // Barra de Salud/Racha
            healthBar.setProgress(1.0);
            if (lblHealthStreak != null) {
                int racha = currentCharacter.getHealthStreak();
                lblHealthStreak.setText("RACHA: " + racha + (racha == 1 ? " DÍA" : " DÍAS"));
            }
            
            System.out.println("✅ UI actualizada correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error actualizando UI: " + e.getMessage());
        }
    }

    /**
     * Refresca los datos del personaje desde la base de datos
     */
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
                        System.out.println("✅ Datos del personaje refrescados");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error refrescando personaje: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Carga el avatar pequeño del personaje según su clase
     */
    private void cargarAvatarSmall(int classId) {
        try {
            String path = "/assets/images/sprites/base/class_" + classId + ".png";
            URL url = getClass().getResource(path);
            
            // Fallback si no existe la imagen base
            if (url == null) {
                path = "/assets/images/sprites/class_" + classId + "_idle.png";
                url = getClass().getResource(path);
            }
            
            if (url != null) {
                imgAvatarSmall.setImage(new Image(url.toExternalForm()));
            } else {
                System.err.println("⚠️ Avatar no encontrado: " + path);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando avatar: " + e.getMessage());
        }
    }

    /**
     * Obtiene el nombre legible de una clase por su ID
     */
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

    /**
     * Carga una vista en el área de contenido con sistema de caché
     */
    private void loadView(String viewName) {
        if (viewName.equals(currentViewName)) {
            System.out.println("ℹ️ Vista ya cargada: " + viewName);
            return;
        }
        
        try {
            Node nextView = viewCache.get(viewName);
            Object controller = controllerCache.get(viewName);
            
            // Si no está en caché, cargarla
            if (nextView == null) {
                String path = "/fxml/views/" + viewName + ".fxml";
                URL url = getClass().getResource(path);
                
                if (url == null) {
                    System.err.println("⚠️ Vista no encontrada: " + path);
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                nextView = loader.load();
                controller = loader.getController();
                
                viewCache.put(viewName, nextView);
                controllerCache.put(viewName, controller);
                
                System.out.println("✅ Vista cargada y cacheada: " + viewName);
            } else {
                System.out.println("✅ Vista recuperada de caché: " + viewName);
            }

            // Inyectar datos al controlador
            if (controller != null) {
                injectCharacterData(controller);
            }

            // Animar transición
            animarCambioDeVista(nextView);
            currentViewName = viewName;
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando vista " + viewName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inyecta datos del personaje/usuario en el controlador de la vista
     */
    private void injectCharacterData(Object controller) {
        int userId = SessionManager.getInstance().getUserId();

        if (controller instanceof com.ctrlaltquest.ui.controllers.views.CharacterPanelController) {
            ((com.ctrlaltquest.ui.controllers.views.CharacterPanelController) controller).setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ProfileViewController) {
            ((com.ctrlaltquest.ui.controllers.views.ProfileViewController) controller).setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.DashboardViewController) {
            ((com.ctrlaltquest.ui.controllers.views.DashboardViewController) controller).setUserId(userId);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.MissionsViewController) {
            ((com.ctrlaltquest.ui.controllers.views.MissionsViewController) controller).setUserId(userId);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ActivityViewController) {
            // ✅ CORRECCIÓN CRÍTICA: Pasar 'this' al ActivityViewController
            com.ctrlaltquest.ui.controllers.views.ActivityViewController activityCtrl = (com.ctrlaltquest.ui.controllers.views.ActivityViewController) controller;
            activityCtrl.setUserId(userId);
            activityCtrl.setHomeController(this); // <-- ESTO PERMITE ACTUALIZAR LA BARRA DE XP
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.AchievementsViewController) {
            ((com.ctrlaltquest.ui.controllers.views.AchievementsViewController) controller).setUserId(userId);
        }
    }

    /**
     * Anima la transición entre vistas con fade in/out
     */
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

    /**
     * Animación de fade in para un nodo
     */
    private void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // ==========================================
    // SECCIÓN 7: ACCIONES DEL MENÚ DE NAVEGACIÓN
    // ==========================================

    @FXML private void showDashboard() { 
        SoundManager.playClickSound(); 
        loadView("dashboard_view"); 
    }
    
    @FXML private void showActivity() { 
        SoundManager.playClickSound(); 
        loadView("activity_view"); 
    }
    
    @FXML private void showMissions() { 
        SoundManager.playClickSound(); 
        loadView("missions_view"); 
    }
    
    @FXML private void showStore() { 
        SoundManager.playClickSound(); 
        loadView("store_view"); 
    }
    
    @FXML private void showInventory() { 
        SoundManager.playClickSound(); 
        loadView("character_panel"); 
    }
    
    @FXML private void showStats() { 
        SoundManager.playClickSound(); 
        loadView("achievements_view"); 
    }
    
    @FXML private void showProfile() { 
        SoundManager.playClickSound(); 
        loadView("profile_view"); 
    }

    /**
     * Abre el modal de configuración (icono de engranaje)
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
            settingsStage.setTitle("Ajustes");
            settingsStage.show();
            
            System.out.println("✅ Modal de ajustes abierto");
        } catch (IOException e) {
            System.err.println("❌ Error abriendo ajustes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // SECCIÓN 8: MONITOREO DE ACTIVIDAD
    // ==========================================

    /**
     * Inicia el thread de monitoreo de actividad en tiempo real
     */
    private void iniciarMonitoreoActividad() {
        monitorThread = new Thread(() -> {
            System.out.println("🔍 Thread de monitoreo iniciado");
            
            while (isMonitoring) {
                try {
                    String currentTitle = monitorService.getActiveWindowTitle();
                    boolean isProductive = monitorService.isProductive(currentTitle);

                    Platform.runLater(() -> actualizarPanelActividad(currentTitle, isProductive));
                    
                    Thread.sleep(2000); // Actualizar cada 2 segundos
                    
                } catch (InterruptedException e) {
                    System.out.println("⚠️ Thread de monitoreo interrumpido");
                    break;
                } catch (Exception e) {
                    System.err.println("❌ Error en monitor UI: " + e.getMessage());
                }
            }
            
            System.out.println("🛑 Thread de monitoreo finalizado");
        });
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Actualiza el panel lateral de actividad con la app actual
     */
    private void actualizarPanelActividad(String currentTitle, boolean isProductive) {
        if (lblCurrentApp != null) {
            String display = currentTitle.length() > 30 
                ? currentTitle.substring(0, 30) + "..." 
                : currentTitle;
            lblCurrentApp.setText(display);
        }
        
        if (lblAppStatus != null) {
            if (isProductive) {
                lblAppStatus.setText("✓ PRODUCTIVO (+XP)");
                lblAppStatus.setStyle(
                    "-fx-background-color: #2d5a27; " +
                    "-fx-text-fill: #90EE90; " +
                    "-fx-padding: 4 12; " +
                    "-fx-background-radius: 6; " +
                    "-fx-font-weight: bold;"
                );
            } else {
                lblAppStatus.setText("○ OCIO / NEUTRAL");
                lblAppStatus.setStyle(
                    "-fx-background-color: #5a2d2d; " +
                    "-fx-text-fill: #FFB6C1; " +
                    "-fx-padding: 4 12; " +
                    "-fx-background-radius: 6; " +
                    "-fx-font-weight: bold;"
                );
            }
        }
    }

    // ==========================================
    // SECCIÓN 9: VIDEO DE FONDO
    // ==========================================

    /**
     * Configura el video de fondo del hub principal
     */
    private void configurarFondo() {
        try {
            URL videoUrl = getClass().getResource("/assets/videos/main_hub.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                videoPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(videoPlayer);
                
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                videoPlayer.setRate(0.6); // Velocidad lenta para ambiente
                
                // Efecto de desenfoque para no distraer
                backgroundVideo.setEffect(new GaussianBlur(20));
                
                // Respetar configuración del usuario
                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.play();
                }
                
                System.out.println("✅ Video de fondo configurado");
            } else {
                // Manejo silencioso: Si no hay video, no pasa nada, se verá el color de fondo.
                System.out.println("ℹ️ Nota: Video de fondo no encontrado (main_hub.mp4)");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error configurando video: " + e.getMessage());
        }
    }

    /**
     * Control del video de fondo (play/pause)
     */
    public void setVideoPlaying(boolean shouldPlay) {
        if (videoPlayer == null) return;
        
        Platform.runLater(() -> {
            if (shouldPlay) {
                videoPlayer.play();
            } else {
                videoPlayer.pause();
            }
        });
    }

    // ==========================================
    // SECCIÓN 10: CIERRE DE SESIÓN Y CLEANUP
    // ==========================================

    /**
     * Maneja el cierre de sesión del usuario
     */
    @FXML
    private void handleLogout() {
        SoundManager.playClickSound();
        
        // Mostrar capa de carga
        if (loadingLayer != null) {
            loadingLayer.setVisible(true);
            loadingLayer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), loadingLayer);
            ft.setToValue(1.0);
            ft.play();
        }

        // Detener monitoreo
        isMonitoring = false;
        monitorService.stopMonitoring();

        // Cleanup en background
        new Thread(() -> {
            try {
                // Cerrar sesión en BD
                if (dbSessionId != -1) {
                    ActivityDAO.cerrarSesion(dbSessionId);
                    System.out.println("✅ Sesión BD cerrada: ID " + dbSessionId);
                }

                Thread.sleep(800); // Dar tiempo para animaciones

                Platform.runLater(this::volverALogin);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Vuelve a la pantalla de login
     */
    private void volverALogin() {
        try {
            // Limpiar recursos
            cleanup();

            // Cargar vista de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainLayout.getScene().getWindow();
            
            // Transición suave
            FadeTransition fadeOut = new FadeTransition(Duration.millis(600), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.getScene().setRoot(root);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
            
            System.out.println("✅ Sesión cerrada correctamente");
            
        } catch (IOException e) {
            System.err.println("❌ Error volviendo a login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Limpia todos los recursos antes de cerrar
     */
    private void cleanup() {
        try {
            // Detener video
            if (videoPlayer != null) {
                videoPlayer.stop();
                videoPlayer.dispose();
                videoPlayer = null;
            }
            
            // Limpiar caché de vistas
            viewCache.clear();
            controllerCache.clear();
            
            // Cerrar sesión
            SessionManager.getInstance().logout();
            
            System.out.println("✅ Recursos liberados correctamente");
        } catch (Exception e) {
            System.err.println("⚠️ Error en cleanup: " + e.getMessage());
        }
    }
}