package com.ctrlaltquest.ui.utils;

import java.util.HashMap;
import java.util.Map;

import com.ctrlaltquest.ui.controllers.SettingsController;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Gestor Global de Atajos de Teclado (Singleton)
 * Maneja todos los atajos de teclado que funcionan globalmente en la aplicación
 */
public class KeyBindingManager {
    
    private static KeyBindingManager instance;
    private Map<String, Runnable> keyBindingActions;
    private Stage primaryStage;
    private boolean videoControlEnabled = false;
    private Runnable videoToggleCallback;
    
    // Variables para estado de muteo
    private boolean musicMuted = false;
    private boolean soundsMuted = false;
    
    private KeyBindingManager() {
        this.keyBindingActions = new HashMap<>();
        registerDefaultActions();
    }
    
    /**
     * Obtiene la instancia única del gestor
     */
    public static KeyBindingManager getInstance() {
        if (instance == null) {
            instance = new KeyBindingManager();
        }
        return instance;
    }
    
    /**
     * Registra las acciones por defecto para los atajos
     */
    private void registerDefaultActions() {
        // Mutear música
        registerAction("TOGGLE_MUSIC", () -> {
            musicMuted = !musicMuted;
            SettingsController.isMusicEnabled = !musicMuted;
            SoundManager.getInstance().synchronizeMusic();
            showNotification("Música: " + (SettingsController.isMusicEnabled ? "ACTIVADA ♪" : "DESACTIVADA 🔇"));
        });
        
        // Mutear sonidos
        registerAction("TOGGLE_SOUNDS", () -> {
            soundsMuted = !soundsMuted;
            SettingsController.isTypingSoundEnabled = !soundsMuted;
            showNotification("Sonidos: " + (SettingsController.isTypingSoundEnabled ? "ACTIVADOS ✓" : "DESACTIVADOS ✗"));
        });
        
        // Mutear todo
        registerAction("TOGGLE_ALL_AUDIO", () -> {
            boolean shouldMute = !musicMuted && !soundsMuted;
            musicMuted = shouldMute;
            soundsMuted = shouldMute;
            SettingsController.isMusicEnabled = !musicMuted;
            SettingsController.isTypingSoundEnabled = !soundsMuted;
            SoundManager.getInstance().synchronizeMusic();
            showNotification("Audio Global: " + (shouldMute ? "DESACTIVADO 🔇" : "ACTIVADO ♪"));
        });
        
        // Pausar/Reanudar video
        registerAction("TOGGLE_VIDEO", () -> {
            if (videoControlEnabled && videoToggleCallback != null) {
                videoToggleCallback.run();
            }
        });
        
        // Pantalla completa
        registerAction("FULLSCREEN", () -> {
            if (primaryStage != null) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
                showNotification("Pantalla Completa: " + (primaryStage.isFullScreen() ? "ACTIVADA" : "DESACTIVADA"));
            }
        });
        
        // Salir
        registerAction("QUIT_APP", () -> {
            if (primaryStage != null) {
                primaryStage.close();
            }
        });
    }
    
    /**
     * Registra un nuevo atajo de teclado con su acción
     */
    public void registerAction(String actionName, Runnable action) {
        keyBindingActions.put(actionName, action);
    }
    
    /**
     * Obtiene la acción asociada a un atajo
     */
    public Runnable getAction(String actionName) {
        return keyBindingActions.get(actionName);
    }
    
    /**
     * Ejecuta una acción por nombre
     */
    public void executeAction(String actionName) {
        Runnable action = keyBindingActions.get(actionName);
        if (action != null) {
            action.run();
        }
    }
    
    /**
     * Inicializa el gestor de atajos en la escena especificada
     */
    public void initializeKeyBindings(Stage stage, Scene scene) {
        this.primaryStage = stage;
        
        // Agregar event filter para capturar eventos de teclado
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }
    
    /**
     * Manejador de pulsaciones de teclado
     */
    private void handleKeyPress(KeyEvent event) {
        boolean ctrl = event.isControlDown();
        boolean alt = event.isAltDown();
        boolean shift = event.isShiftDown();
        
        switch (event.getCode()) {
            // Ctrl + M: Mutear todo
            case M:
                if (ctrl && !alt && !shift) {
                    executeAction("TOGGLE_ALL_AUDIO");
                    event.consume();
                }
                // Alt + M: Mutear música
                else if (alt && !ctrl && !shift) {
                    executeAction("TOGGLE_MUSIC");
                    event.consume();
                }
                break;
            
            // Alt + S: Mutear sonidos
            case S:
                if (alt && !ctrl && !shift) {
                    executeAction("TOGGLE_SOUNDS");
                    event.consume();
                }
                break;
            
            // Space: Pausar video
            case SPACE:
                if (!ctrl && !alt && !shift) {
                    executeAction("TOGGLE_VIDEO");
                    event.consume();
                }
                break;
            
            // Alt + V: Pausar video (alternativo)
            case V:
                if (alt && !ctrl && !shift) {
                    executeAction("TOGGLE_VIDEO");
                    event.consume();
                }
                break;
            
            // F11: Pantalla completa
            case F11:
                if (!ctrl && !alt && !shift) {
                    executeAction("FULLSCREEN");
                    event.consume();
                }
                break;
            
            // Ctrl + Q: Salir
            case Q:
                if (ctrl && !alt && !shift) {
                    executeAction("QUIT_APP");
                    event.consume();
                }
                break;
            
            // Ctrl + K: Mostrar atajos (será implementado por el controlador que lo use)
            case K:
                if (ctrl && !alt && !shift) {
                    // Buscar si hay una vista de keybindings
                    showKeybindingsDialog();
                    event.consume();
                }
                break;
            
            default:
                break;
        }
    }
    
    /**
     * Habilita el control de video con callback
     */
    public void enableVideoControl(Runnable toggleCallback) {
        this.videoControlEnabled = true;
        this.videoToggleCallback = toggleCallback;
    }
    
    /**
     * Deshabilita el control de video
     */
    public void disableVideoControl() {
        this.videoControlEnabled = false;
        this.videoToggleCallback = null;
    }
    
    /**
     * Muestra una notificación en pantalla (será mejorada en futuras versiones)
     */
    private void showNotification(String message) {
        System.out.println("⌨️ ATAJO: " + message);
        // TODO: Implementar toastNotification visual
    }
    
    /**
     * Muestra el diálogo de atajos disponibles
     */
    private void showKeybindingsDialog() {
        StringBuilder message = new StringBuilder("📋 ATAJOS DE TECLADO DISPONIBLES\n");
        message.append("═════════════════════════════════\n\n");
        
        for (KeyBindings.KeyBindingInfo binding : KeyBindings.ALL_BINDINGS) {
            message.append(String.format("%-25s %s\n", binding.keyCombination, binding.name));
            message.append(String.format("   → %s\n\n", binding.description));
        }
        
        System.out.println(message.toString());
        // TODO: Mostrar esto en una ventana modal en lugar de consola
    }
    
    /**
     * Obtiene el estado de muteo de música
     */
    public boolean isMusicMuted() {
        return musicMuted;
    }
    
    /**
     * Obtiene el estado de muteo de sonidos
     */
    public boolean isSoundsMuted() {
        return soundsMuted;
    }
}
