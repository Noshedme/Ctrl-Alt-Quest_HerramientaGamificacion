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
import com.ctrlaltquest.services.EventContextualListener;
import com.ctrlaltquest.services.EventContextualService;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.services.XPChangeListener;
import com.ctrlaltquest.services.XPSyncService;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * HomeController — Hub principal del juego.
 *
 * Cambios relacionados con el sistema de eventos:
 *  - onEventStarted() abre el EventModalController en un Stage modal
 *  - onEventCompleted() refresca los datos del personaje
 *  - El modal se abre siempre en el hilo de JavaFX (Platform.runLater)
 */
public class HomeController implements XPChangeListener, EventContextualListener {

    // ══ UI (FXML) ═══════════════════════════════════════════════════════════

    @FXML private BorderPane mainLayout;
    @FXML private StackPane  contentArea;
    @FXML private VBox       loadingLayer;

    // Header
    @FXML private Label     lblUsername;
    @FXML private Label     lblClass;
    @FXML private Label     lblLevel;
    @FXML private Label     lblCoins;
    @FXML private Label     lblXPText;
    @FXML private ImageView imgAvatarSmall;
    @FXML private ProgressBar xpBar;

    // Sidebar actividad
    @FXML private Label     lblCurrentApp;
    @FXML private Label     lblAppStatus;
    @FXML private Label     lblHealthStreak;
    @FXML private ProgressBar healthBar;

    // Fondo
    @FXML private MediaView backgroundVideo;

    // ══ ESTADO ══════════════════════════════════════════════════════════════

    private MediaPlayer videoPlayer;
    private Character   currentCharacter;
    private int         dbSessionId = -1;

    private final ActivityMonitorService monitorService = ActivityMonitorService.getInstance();
    private boolean isMonitoring = true;
    private Thread  monitorThread;

    private final Map<String, Node>   viewCache       = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private String currentViewName = "";

    // Easter eggs
    private int  avatarClickCount   = 0;
    private long lastAvatarClickTime = 0;

    private final List<KeyCode> konamiCode = Arrays.asList(
        KeyCode.UP, KeyCode.UP, KeyCode.DOWN, KeyCode.DOWN,
        KeyCode.LEFT, KeyCode.RIGHT, KeyCode.LEFT, KeyCode.RIGHT,
        KeyCode.B, KeyCode.A
    );
    private int  konamiIndex         = 0;
    private long lastKonamiInputTime = 0;

    // Guardia para no abrir dos modales de evento a la vez
    private boolean eventModalOpen = false;

    // ══ INICIALIZACIÓN ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        System.out.println("🎮 [HomeController] Inicializando...");

        configurarFondo();
        configurarTooltips();
        iniciarMonitoreoActividad();
        configurarEfectosAvatar();

        // Toast container
        VBox toastContainer = new VBox();
        toastContainer.setPrefSize(400, 600);
        toastContainer.setStyle("-fx-background-color: transparent;");
        toastContainer.setMouseTransparent(true);
        Toast.initialize(toastContainer);
        Platform.runLater(() -> {
            try {
                StackPane root = (StackPane) mainLayout.getScene().getRoot();
                if (root != null && !root.getChildren().contains(toastContainer)) {
                    root.getChildren().add(toastContainer);
                    StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
                }
            } catch (Exception e) {
                System.err.println("Error al inicializar Toast: " + e.getMessage());
            }
        });

        try { SoundManager.getInstance().synchronizeMusic(); } catch (Exception e) {}

        Platform.runLater(() -> {
            loadView("dashboard_view");
            Toast.success("Bienvenido al Hub", "¡Controla tu aventura desde aquí!");

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
        if (xpBar != null)         Tooltip.install(xpBar,         new Tooltip("Experiencia hasta el próximo nivel"));
        if (healthBar != null)     Tooltip.install(healthBar,     new Tooltip("Racha de días productivos"));
        if (imgAvatarSmall != null) Tooltip.install(imgAvatarSmall, new Tooltip("Tu avatar de héroe"));
    }

    private void configurarEfectosAvatar() {
        if (imgAvatarSmall == null) return;
        aplicarClipCircular(imgAvatarSmall, 25);

        imgAvatarSmall.setOnMouseEntered(e -> {
            imgAvatarSmall.setEffect(new DropShadow(12, Color.rgb(163, 53, 238, 0.7)));
            ScaleTransition st = new ScaleTransition(Duration.millis(120), imgAvatarSmall);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        imgAvatarSmall.setOnMouseExited(e -> {
            imgAvatarSmall.setEffect(null);
            ScaleTransition st = new ScaleTransition(Duration.millis(120), imgAvatarSmall);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
    }

    private void aplicarClipCircular(ImageView imageView, double radius) {
        Circle clip = new Circle(radius);
        clip.setCenterX(radius);
        clip.setCenterY(radius);
        imageView.setClip(clip);
    }

    private void aplicarEstilos(Scene scene) {
        try {
            String authCss = getClass().getResource("/styles/auth.css").toExternalForm();
            String homeCss = getClass().getResource("/styles/home.css").toExternalForm();
            if (!scene.getStylesheets().contains(authCss)) scene.getStylesheets().add(authCss);
            if (!scene.getStylesheets().contains(homeCss))  scene.getStylesheets().add(homeCss);
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

        // Avatar — solo Easter Egg
        if (imgAvatarSmall != null) {
            imgAvatarSmall.setPickOnBounds(true);
            imgAvatarSmall.setOnMouseClicked(e -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAvatarClickTime > 3000) avatarClickCount = 0;
                avatarClickCount++;
                lastAvatarClickTime = currentTime;
                animarClickAvatar();
                if (avatarClickCount == 50) {
                    intentarDesbloquearLogro(903, "Spammer de Clicks",
                        "¡Cálmate! Has clickeado 50 veces tu avatar.");
                    avatarClickCount = 0;
                }
            });
        }
    }

    private void animarClickAvatar() {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), imgAvatarSmall);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.85);  st.setToY(0.85);
        st.setAutoReverse(true); st.setCycleCount(2); st.play();
    }

    private void activarKonamiCode() {
        intentarDesbloquearLogro(901, "Konami Code Master", "↑↑↓↓←→←→BA - ¡Código legendario!");
        if (currentCharacter != null) {
            currentCharacter.setCoins(currentCharacter.getCoins() + 500);
            CharacterDAO.saveCharacter(currentCharacter);
            actualizarUI();
            Toast.gold("¡BONUS!", "+500 monedas por el Konami Code!");
        }
    }

    private void intentarDesbloquearLogro(int id, String title, String msg) {
        int userId = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            boolean nuevo = AchievementsDAO.unlockAchievement(userId, id);
            if (nuevo) Platform.runLater(() -> {
                try { SoundManager.playSuccessSound(); } catch (Exception e) {}
                Toast.gold("🏆 " + title, msg);
            });
        }).start();
    }

    // ══ DATOS DEL JUGADOR ════════════════════════════════════════════════════

    public void initPlayerData(Character character) {
        if (character == null) return;
        this.currentCharacter = character;

        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance().getUserId();
                this.dbSessionId = ActivityDAO.iniciarSesion(userId);

                refreshCharacterData();
                MissionsDAO.inicializarMisionesGlobalesParaUsuario(userId);
                XPSyncService.getInstance().addXPChangeListener(HomeController.this);
                EventContextualService.getInstance().addEventListener(this);
                EventContextualService.getInstance().startEventGenerator(userId);

                LocalTime now = LocalTime.now();
                if (now.getHour() == 3 && now.getMinute() == 33)
                    intentarDesbloquearLogro(902, "Viajero del Tiempo", "Entraste a las 3:33 AM... 👻");

                monitorService.startMonitoring(userId);
            } catch (Exception e) {
                System.err.println("❌ Error en initPlayerData: " + e.getMessage());
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

            int xpActual    = currentCharacter.getCurrentXp();
            int xpRequerido = currentCharacter.getLevel() * 1000;
            xpBar.setProgress((double) xpActual / xpRequerido);
            lblXPText.setText(xpActual + " / " + xpRequerido + " XP");

            if (lblHealthStreak != null) {
                int racha = currentCharacter.getHealthStreak();
                lblHealthStreak.setText(String.valueOf(racha));
                healthBar.setProgress(Math.min(1.0, racha / 7.0));
            } else {
                healthBar.setProgress(0.0);
            }
        } catch (Exception ignored) {}
    }

    public void refreshCharacterData() {
        if (currentCharacter == null) return;
        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance().getUserId();
                Map<Integer, Character> chars = CharacterDAO.getCharactersByUser(userId);
                if (!chars.isEmpty()) {
                    Character refreshed = chars.get(currentCharacter.getSlotIndex());
                    if (refreshed != null) {
                        this.currentCharacter = refreshed;
                        Platform.runLater(this::actualizarUI);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void cargarAvatarSmall(int classId) {
        try {
            String path = "/assets/images/sprites/base/class_" + classId + ".png";
            URL url = getClass().getResource(path);
            if (url == null) url = getClass().getResource("/assets/images/sprites/class_" + classId + "_idle.png");
            if (url != null) {
                imgAvatarSmall.setImage(new Image(url.toExternalForm()));
                aplicarClipCircular(imgAvatarSmall, 25);
            }
        } catch (Exception ignored) {}
    }

    private String obtenerNombreClase(int classId) {
        return switch (classId) {
            case 1 -> "PROGRAMADOR";
            case 2 -> "LECTOR";
            case 3 -> "ESCRITOR";
            default -> "AVENTURERO TECH";
        };
    }

    // ══ NAVEGACIÓN ═══════════════════════════════════════════════════════════

    private void loadView(String viewName) {
        if (viewName.equals(currentViewName)) return;
        try {
            Node nextView   = viewCache.get(viewName);
            Object controller = controllerCache.get(viewName);

            if (nextView == null) {
                String path = "/fxml/views/" + viewName + ".fxml";
                URL url = getClass().getResource(path);
                if (url == null) return;
                FXMLLoader loader = new FXMLLoader(url);
                nextView    = loader.load();
                controller  = loader.getController();
                viewCache.put(viewName, nextView);
                controllerCache.put(viewName, controller);
            }

            if (controller != null) injectCharacterData(controller);
            animarCambioDeVista(nextView);
            currentViewName = viewName;
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void injectCharacterData(Object controller) {
        int userId = SessionManager.getInstance().getUserId();
        if (controller instanceof com.ctrlaltquest.ui.controllers.views.CharacterPanelController c)
            c.setPlayerData(currentCharacter);
        else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ProfileViewController c)
            c.setPlayerData(currentCharacter);
        else if (controller instanceof com.ctrlaltquest.ui.controllers.views.DashboardViewController c) {
            c.setUserId(userId); c.setPlayerData(currentCharacter);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.MissionsViewController c)
            c.setUserId(userId);
        else if (controller instanceof com.ctrlaltquest.ui.controllers.views.ActivityViewController c) {
            c.setUserId(userId); c.setHomeController(this);
        } else if (controller instanceof com.ctrlaltquest.ui.controllers.views.AchievementsViewController c)
            c.setUserId(userId);
        else if (controller instanceof com.ctrlaltquest.ui.controllers.views.TutorialViewController c)
            c.setHomeController(this);
    }

    private void animarCambioDeVista(Node nextView) {
        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().add(nextView); fadeIn(nextView); return;
        }
        Node current = contentArea.getChildren().get(0);
        if (current == nextView) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), current);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().setAll(nextView);
            fadeIn(nextView);
        });
        fadeOut.play();
    }

    private void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
    }

    // ══ MENÚ ═════════════════════════════════════════════════════════════════

    private void playClick() { try { SoundManager.playClickSound(); } catch (Exception e) {} }

    @FXML public void showDashboard()  { playClick(); loadView("dashboard_view"); }
    @FXML public void showActivity()   { playClick(); loadView("activity_view"); }
    @FXML public void showMissions()   { playClick(); loadView("missions_view"); }
    @FXML public void showStore()      { playClick(); loadView("store_view"); }
    @FXML public void showInventory()  { playClick(); loadView("character_panel"); }
    @FXML public void showStats()      { playClick(); loadView("achievements_view"); }
    @FXML public void showProfile()    { playClick(); loadView("profile_view"); }
    @FXML public void showTutorial()   { playClick(); loadView("tutorial_view"); }

    @FXML
    public void showSettingsModal() {
        try {
            playClick();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController sc = loader.getController();
            sc.setHomeController(this);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.initOwner(mainLayout.getScene().getWindow());
            st.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            st.setScene(scene);
            st.setTitle("Ajustes");
            st.show();
        } catch (IOException e) {
            System.err.println("Error al cargar settings: " + e.getMessage());
        }
    }

    @FXML
    private void resumeTracking() {
        playClick();
        isMonitoring = true;
        if (monitorThread == null || !monitorThread.isAlive()) iniciarMonitoreoActividad();
    }

    // ══ MONITOREO ════════════════════════════════════════════════════════════

    private void iniciarMonitoreoActividad() {
        monitorThread = new Thread(() -> {
            while (isMonitoring) {
                try {
                    String title      = monitorService.getActiveWindowTitle();
                    boolean productive = monitorService.isProductive(title);
                    EventContextualService.getInstance().updateCurrentActivity(title);
                    Platform.runLater(() -> actualizarPanelActividad(title, productive));
                    Thread.sleep(2000);
                } catch (InterruptedException e) { break; }
                catch (Exception ignored) {}
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void actualizarPanelActividad(String currentTitle, boolean isProductive) {
        if (lblCurrentApp != null) {
            String display = currentTitle.length() > 30
                ? currentTitle.substring(0, 30) + "..." : currentTitle;
            lblCurrentApp.setText(display);
        }
        if (lblAppStatus != null) {
            if (isProductive) {
                lblAppStatus.setText("✓ PRODUCTIVO (+XP)");
                lblAppStatus.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: #90EE90;" +
                    "-fx-padding: 4 12; -fx-background-radius: 6; -fx-font-weight: bold;");
            } else {
                lblAppStatus.setText("○ OCIO / NEUTRAL");
                lblAppStatus.setStyle("-fx-background-color: #5a2d2d; -fx-text-fill: #FFB6C1;" +
                    "-fx-padding: 4 12; -fx-background-radius: 6; -fx-font-weight: bold;");
            }
        }
    }

    // ══ VIDEO DE FONDO ═══════════════════════════════════════════════════════

    private void configurarFondo() {
        try {
            URL videoUrl = getClass().getResource("/assets/videos/main_hub.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                videoPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(videoPlayer);
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                videoPlayer.setRate(0.5);
                backgroundVideo.setEffect(new GaussianBlur(15));
                if (SettingsController.isVideoPaused) videoPlayer.pause();
                else videoPlayer.play();
            }
        } catch (Exception ignored) {}
    }

    public void setVideoPlaying(boolean shouldPlay) {
        if (videoPlayer == null) return;
        Platform.runLater(() -> {
            if (shouldPlay) videoPlayer.play(); else videoPlayer.pause();
        });
    }

    // ══ LOGOUT ═══════════════════════════════════════════════════════════════

    @FXML
    public void handleLogout() {
        playClick();
        if (loadingLayer != null) {
            loadingLayer.setVisible(true); loadingLayer.setOpacity(0);
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
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
        } catch (IOException ignored) {}
    }

    private void cleanup() {
        try {
            if (videoPlayer != null) { videoPlayer.stop(); videoPlayer.dispose(); videoPlayer = null; }
            viewCache.clear(); controllerCache.clear();
            XPSyncService.getInstance().removeXPChangeListener(this);
            EventContextualService.getInstance().removeEventListener(this);
            EventContextualService.getInstance().stopEventGenerator(SessionManager.getInstance().getUserId());
            SessionManager.getInstance().logout();
        } catch (Exception ignored) {}
    }

    // ══ XPChangeListener ═════════════════════════════════════════════════════

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
                    ScaleTransition st = new ScaleTransition(Duration.millis(200), xpBar);
                    st.setToY(1.06); st.setCycleCount(2); st.setAutoReverse(true); st.play();
                    Toast.success("XP Ganado", "+" + event.xpGained + " XP de " + event.source);
                }
            } catch (Exception ignored) {}
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

                ScaleTransition scale = new ScaleTransition(Duration.millis(350), lblLevel);
                scale.setToX(1.12); scale.setToY(1.12); scale.setCycleCount(1); scale.setAutoReverse(true);
                FadeTransition fade = new FadeTransition(Duration.millis(350), lblLevel);
                fade.setFromValue(1.0); fade.setToValue(0.85); fade.setCycleCount(1); fade.setAutoReverse(true);
                scale.play(); fade.play();

                try { SoundManager.playLevelUpSound(); } catch (Exception e) {}
                Toast.success("🎉 ¡SUBIDA DE NIVEL!", "¡Alcanzaste el NIVEL " + newLevel + "! 🎊");
            } catch (Exception ignored) {}
        });
    }

    // ══ EventContextualListener ══════════════════════════════════════════════

    @Override
    public void onEventGenerated(int userId, EventContextualService.ContextualEvent event) {
        // Pre-carga silenciosa — no hacemos nada en la UI todavía
    }

    /**
     * Este es el único método que abre el modal.
     * Se ejecuta en el hilo del scheduler, así que usamos Platform.runLater.
     */
    @Override
    public void onEventStarted(int userId, EventContextualService.ContextualEvent event) {
        Platform.runLater(() -> abrirEventoModal(userId, event));
    }

    /**
     * Abre el EventModalController en un Stage modal centrado.
     * Comprueba que no haya ya un modal abierto para evitar duplicados.
     */
    private void abrirEventoModal(int userId, EventContextualService.ContextualEvent event) {
        if (eventModalOpen) {
            System.out.println("⚠️ [HomeController] Modal ya abierto, ignorando nuevo evento.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/event_modal.fxml"));
            Parent root = loader.load();

            EventModalController controller = loader.getController();

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initOwner(mainLayout.getScene().getWindow());
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);
            modalStage.setTitle(event.title);

            // Registrar apertura / cierre
            eventModalOpen = true;
            modalStage.setOnHidden(e -> eventModalOpen = false);

            // Inicializar el controller con los datos del evento
            controller.setEvento(userId, event, modalStage);

            // Sonido de alerta
            try { SoundManager.playSuccessSound(); } catch (Exception ignored) {}

            modalStage.show();

            // Centrar respecto a la ventana principal
            Stage owner = (Stage) mainLayout.getScene().getWindow();
            modalStage.setX(owner.getX() + (owner.getWidth()  - modalStage.getWidth())  / 2);
            modalStage.setY(owner.getY() + (owner.getHeight() - modalStage.getHeight()) / 2);

        } catch (IOException e) {
            eventModalOpen = false;
            System.err.println("❌ [HomeController] Error abriendo event_modal.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onEventProgressUpdated(int userId, EventContextualService.ContextualEvent event,
                                       int current, int target) {
        // El progreso se muestra dentro del modal — no hace falta tocar el HUD aquí
    }

    @Override
    public void onEventCriticalPhase(int userId, EventContextualService.ContextualEvent event) {
        Platform.runLater(() ->
            Toast.warning("⚠️ FASE CRÍTICA", "¡El jefe se está enfureciendo!"));
    }

    @Override
    public void onEventCompleted(int userId, EventContextualService.ContextualEvent event,
                                 CompletionStatus status, int xpReward, int coinReward) {
        Platform.runLater(() -> {
            if (status == CompletionStatus.VICTORY) {
                refreshCharacterData();
                // El Toast de victoria ya lo lanza EventModalController
            }
        });
    }
}