package com.ctrlaltquest.ui.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ctrlaltquest.ui.controllers.SettingsController;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * KeyBindingManager — Gestor global de atajos de teclado.
 *
 * Problema original: al llamar WindowManager.changeScene() se crea una nueva
 * Scene, y el event filter registrado en la escena anterior se pierde.
 *
 * Solución: escuchar stage.sceneProperty() para re-registrar el handler
 * automáticamente cada vez que cambie la escena.
 */
public class KeyBindingManager {

    private static KeyBindingManager instance;
    private final Map<String, Runnable> actions = new HashMap<>();

    private Stage  primaryStage;
    private boolean videoControlEnabled  = false;
    private Runnable videoToggleCallback = null;

    private boolean musicMuted  = false;
    private boolean soundsMuted = false;

    private KeyBindingManager() {
        registrarAcciones();
    }

    public static KeyBindingManager getInstance() {
        if (instance == null) instance = new KeyBindingManager();
        return instance;
    }

    // ════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Llamar una sola vez desde AppLauncher.
     * Se registra en sceneProperty() para que los atajos funcionen en TODAS
     * las escenas, incluso después de WindowManager.changeScene().
     */
    public void initializeKeyBindings(Stage stage, Scene initialScene) {
        this.primaryStage = stage;

        // Registrar en la escena inicial
        registrarEnEscena(initialScene);

        // Re-registrar automáticamente cada vez que la escena cambie
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) registrarEnEscena(newScene);
        });
    }

    private void registrarEnEscena(Scene scene) {
        // Eliminar listener anterior si existe (evita duplicados)
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACCIONES
    // ════════════════════════════════════════════════════════════════════════

    private void registrarAcciones() {

        // Alt + M — Mutear música
        registerAction("TOGGLE_MUSIC", () -> {
            musicMuted = !musicMuted;
            SettingsController.isMusicEnabled = !musicMuted;
            SoundManager.getInstance().synchronizeMusic();
            log("Música: " + (SettingsController.isMusicEnabled ? "ACTIVADA ♪" : "DESACTIVADA 🔇"));
        });

        // Alt + S — Mutear sonidos
        registerAction("TOGGLE_SOUNDS", () -> {
            soundsMuted = !soundsMuted;
            SettingsController.isTypingSoundEnabled = !soundsMuted;
            log("Sonidos: " + (SettingsController.isTypingSoundEnabled ? "ACTIVADOS ✓" : "DESACTIVADOS ✗"));
        });

        // Ctrl + M — Mutear todo
        registerAction("TOGGLE_ALL_AUDIO", () -> {
            boolean shouldMute = !musicMuted && !soundsMuted;
            musicMuted  = shouldMute;
            soundsMuted = shouldMute;
            SettingsController.isMusicEnabled        = !musicMuted;
            SettingsController.isTypingSoundEnabled  = !soundsMuted;
            SoundManager.getInstance().synchronizeMusic();
            log("Audio Global: " + (shouldMute ? "DESACTIVADO 🔇" : "ACTIVADO ♪"));
        });

        // Space / Alt + V — Pausar video de fondo
        registerAction("TOGGLE_VIDEO", () -> {
            if (videoControlEnabled && videoToggleCallback != null) {
                videoToggleCallback.run();
                log("Video: alternado");
            }
        });

        // F11 — Pantalla completa
        registerAction("FULLSCREEN", () -> {
            if (primaryStage != null) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
                log("Pantalla Completa: " + (primaryStage.isFullScreen() ? "ON" : "OFF"));
            }
        });

        // Ctrl + Q — Cerrar app
        registerAction("QUIT_APP", () -> {
            if (primaryStage != null) primaryStage.close();
        });

        // Ctrl + K — Mostrar ventana de atajos
        registerAction("SHOW_KEYBINDINGS", () -> Platform.runLater(this::abrirVistaAtaljos));
    }

    public void registerAction(String name, Runnable action) {
        actions.put(name, action);
    }

    public void executeAction(String name) {
        Runnable a = actions.get(name);
        if (a != null) a.run();
    }

    // ════════════════════════════════════════════════════════════════════════
    // HANDLER DE TECLADO
    // ════════════════════════════════════════════════════════════════════════

    private void handleKeyPress(KeyEvent e) {
        boolean ctrl  = e.isControlDown();
        boolean alt   = e.isAltDown();
        boolean shift = e.isShiftDown();

        switch (e.getCode()) {

            case M:
                if (ctrl && !alt && !shift)       { executeAction("TOGGLE_ALL_AUDIO"); e.consume(); }
                else if (alt && !ctrl && !shift)  { executeAction("TOGGLE_MUSIC");     e.consume(); }
                break;

            case S:
                if (alt && !ctrl && !shift)       { executeAction("TOGGLE_SOUNDS");    e.consume(); }
                break;

            case SPACE:
                // Solo pausar video si no hay un campo de texto enfocado
                if (!ctrl && !alt && !shift && !hayTextFieldEnfocado()) {
                    executeAction("TOGGLE_VIDEO"); e.consume();
                }
                break;

            case V:
                if (alt && !ctrl && !shift)       { executeAction("TOGGLE_VIDEO");     e.consume(); }
                break;

            case F11:
                if (!ctrl && !alt && !shift)      { executeAction("FULLSCREEN");       e.consume(); }
                break;

            case Q:
                if (ctrl && !alt && !shift)       { executeAction("QUIT_APP");         e.consume(); }
                break;

            case K:
                if (ctrl && !alt && !shift)       { executeAction("SHOW_KEYBINDINGS"); e.consume(); }
                break;

            default:
                break;
        }
    }

    /**
     * Evita que Space pause el video cuando el usuario está escribiendo en un TextField.
     */
    private boolean hayTextFieldEnfocado() {
        if (primaryStage == null || primaryStage.getScene() == null) return false;
        javafx.scene.Node focused = primaryStage.getScene().getFocusOwner();
        return focused instanceof javafx.scene.control.TextField
            || focused instanceof javafx.scene.control.TextArea
            || focused instanceof javafx.scene.control.PasswordField;
    }

    // ════════════════════════════════════════════════════════════════════════
    // VENTANA DE ATAJOS (Ctrl + K)
    // ════════════════════════════════════════════════════════════════════════

    private void abrirVistaAtaljos() {
        try {
            // Evitar abrir dos ventanas a la vez
            boolean yaAbierta = Stage.getWindows().stream()
                .anyMatch(w -> w instanceof Stage &&
                               "Atajos de Teclado".equals(((Stage) w).getTitle()) &&
                               w.isShowing());
            if (yaAbierta) return;

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/views/keybindings_view.fxml"));
            Parent root = loader.load();

            Stage modal = new Stage();
            modal.setTitle("Atajos de Teclado");
            modal.initModality(Modality.APPLICATION_MODAL);
            if (primaryStage != null) modal.initOwner(primaryStage);
            modal.initStyle(StageStyle.DECORATED);
            modal.setResizable(false);
            modal.setScene(new Scene(root));
            modal.show();

        } catch (IOException ex) {
            System.err.println("❌ [KeyBindingManager] No se pudo abrir keybindings_view.fxml: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTROL DE VIDEO Y UTILIDADES
    // ════════════════════════════════════════════════════════════════════════

    public void enableVideoControl(Runnable toggleCallback) {
        this.videoControlEnabled = true;
        this.videoToggleCallback = toggleCallback;
    }

    public void disableVideoControl() {
        this.videoControlEnabled = false;
        this.videoToggleCallback = null;
    }

    public boolean isMusicMuted()  { return musicMuted;  }
    public boolean isSoundsMuted() { return soundsMuted; }

    private void log(String msg) { System.out.println("⌨️  [KeyBinding] " + msg); }
}