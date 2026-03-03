package com.ctrlaltquest.ui.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Definición centralizada de todos los atajos de teclado del sistema
 * Estos atajos funcionan globalmente en toda la aplicación
 */
public class KeyBindings {
    
    // ============ CONTROLES DE AUDIO ============
    /** Alt + M: Mutear/Desmutear toda la música */
    public static final KeyCombination TOGGLE_MUSIC = 
        new KeyCodeCombination(KeyCode.M, KeyCombination.ALT_DOWN);
    
    /** Alt + S: Mutear/Desmutear sonidos */
    public static final KeyCombination TOGGLE_SOUNDS = 
        new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN);
    
    /** Ctrl + M: Mutear/Desmutear TODO */
    public static final KeyCombination TOGGLE_ALL_AUDIO = 
        new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN);
    
    // ============ CONTROLES DE VIDEO ============
    /** Space (Espacio): Pausar/Reanudar video de fondo */
    public static final KeyCombination TOGGLE_VIDEO = 
        new KeyCodeCombination(KeyCode.SPACE);
    
    /** Alt + V: Pausar/Reanudar video de fondo (alternativo) */
    public static final KeyCombination TOGGLE_VIDEO_ALT = 
        new KeyCodeCombination(KeyCode.V, KeyCombination.ALT_DOWN);
    
    // ============ CONTROLES DE APLICACIÓN ============
    /** Ctrl + Q: Salir de la aplicación */
    public static final KeyCombination QUIT_APP = 
        new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
    
    /** F11: Pantalla completa (fullscreen) */
    public static final KeyCombination FULLSCREEN = 
        new KeyCodeCombination(KeyCode.F11);
    
    /** Ctrl + H: Abrir/Cerrar ayuda */
    public static final KeyCombination SHOW_HELP = 
        new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN);
    
    /** Ctrl + K: Mostrar todos los atajos disponibles */
    public static final KeyCombination SHOW_KEYBINDINGS = 
        new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN);
    
    // ============ DEFINICIONES DESCRIPTIVAS ============
    
    public static class KeyBindingInfo {
        public String name;
        public String keyCombination;
        public String description;
        public KeyCombination keyCombo;
        
        public KeyBindingInfo(String name, String keyCombination, String description, KeyCombination keyCombo) {
            this.name = name;
            this.keyCombination = keyCombination;
            this.description = description;
            this.keyCombo = keyCombo;
        }
    }
    
    /**
     * Lista de todos los atajos disponibles con sus descripciones
     * Útil para mostrar en un menú de ayuda
     */
    public static final KeyBindingInfo[] ALL_BINDINGS = {
        // Audio
        new KeyBindingInfo(
            "Mutear Música",
            "Alt + M",
            "Activa/Desactiva la música de fondo del sistema",
            TOGGLE_MUSIC
        ),
        new KeyBindingInfo(
            "Mutear Sonidos",
            "Alt + S",
            "Activa/Desactiva todos los efectos de sonido (clics, éxito, etc.)",
            TOGGLE_SOUNDS
        ),
        new KeyBindingInfo(
            "Mutear Todo",
            "Ctrl + M",
            "Activa/Desactiva toda la música y sonidos del sistema",
            TOGGLE_ALL_AUDIO
        ),
        // Video
        new KeyBindingInfo(
            "Pausar/Reanudar Video",
            "Espacio",
            "Pausa o reanuda el video de fondo actual",
            TOGGLE_VIDEO
        ),
        new KeyBindingInfo(
            "Pausar/Reanudar Video (Alt)",
            "Alt + V",
            "Otra forma de pausar/reanudar el video de fondo",
            TOGGLE_VIDEO_ALT
        ),
        // Aplicación
        new KeyBindingInfo(
            "Pantalla Completa",
            "F11",
            "Activa/Desactiva el modo de pantalla completa",
            FULLSCREEN
        ),
        new KeyBindingInfo(
            "Mostrar Ayuda",
            "Ctrl + H",
            "Abre el menú de ayuda y tutoriales",
            SHOW_HELP
        ),
        new KeyBindingInfo(
            "Mostrar Atajos",
            "Ctrl + K",
            "Muestra todos los atajos de teclado disponibles",
            SHOW_KEYBINDINGS
        ),
        new KeyBindingInfo(
            "Salir",
            "Ctrl + Q",
            "Cierra la aplicación (si está permitido en esa vista)",
            QUIT_APP
        )
    };
    
    /**
     * Obtiene información sobre un atajo específico
     */
    public static KeyBindingInfo getBindingInfo(String name) {
        for (KeyBindingInfo binding : ALL_BINDINGS) {
            if (binding.name.equals(name)) {
                return binding;
            }
        }
        return null;
    }
}
