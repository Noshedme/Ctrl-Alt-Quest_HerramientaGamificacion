package com.ctrlaltquest.ui.examples;

import com.ctrlaltquest.ui.utils.KeyBindingManager;

import javafx.scene.media.MediaPlayer;

/**
 * Ejemplos de cómo integrar atajos de teclado en controladores existentes
 * Estos ejemplos muestran patrones recomendados para diferentes situaciones
 */
public class KeyBindingsExamples {
    
    // ============ EJEMPLO 1: INTEGRACIÓN EN VISTA CON VIDEO ============
    
    /**
     * Ejemplo de cómo integrar control de video en HomeController
     */
    public static class HomeControllerExample {
        
        private MediaPlayer videoPlayer;
        private KeyBindingManager keyBindingManager;
        
        public void initialize() {
            // 1. Obtener el gestor de atajos
            keyBindingManager = KeyBindingManager.getInstance();
            
            // 2. Registrar callback para pausa/reanudación de video
            keyBindingManager.enableVideoControl(() -> {
                toggleVideoPlayback();
            });
        }
        
        /**
         * Pausa o reanuda el video
         */
        private void toggleVideoPlayback() {
            if (videoPlayer == null) return;
            
            MediaPlayer.Status status = videoPlayer.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                videoPlayer.pause();
                System.out.println("🎬 Video pausado (Espacio)");
            } else if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.STOPPED) {
                videoPlayer.play();
                System.out.println("▶️ Video reanudado (Espacio)");
            }
        }
        
        /**
         * IMPORTANTE: Llamar cuando se oculta la vista
         */
        public void onViewHidden() {
            keyBindingManager.disableVideoControl();
        }
    }
    
    
    // ============ EJEMPLO 2: ACCIONES PERSONALIZADAS ============
    
    /**
     * Ejemplo de registrar acciones personalizadas en un controlador
     */
    public static class CustomActionsExample {
        
        public void setupCustomKeyBindings() {
            KeyBindingManager manager = KeyBindingManager.getInstance();
            
            // Ejemplo: Ctrl+P para pausar todo (video y música)
            manager.registerAction("PAUSE_ALL", () -> {
                System.out.println("⏸️ Pausando todo...");
                // Tu código aquí
            });
            
            // Ejemplo: Ctrl+R para reiniciar nivel
            manager.registerAction("RESTART_LEVEL", () -> {
                System.out.println("🔄 Reiniciando nivel...");
                // Tu código aquí
            });
        }
    }
    
    
    // ============ EJEMPLO 3: AUDIO FEEDBACK ============
    
    /**
     * Ejemplo de agregar feedback de audio a los atajos
     */
    public static class AudioFeedbackExample {
        
        public void handleAudioShortcuts() {
            KeyBindingManager manager = KeyBindingManager.getInstance();
            
            // Sobrescribir la acción de muteo de música
            manager.registerAction("TOGGLE_MUSIC", () -> {
                boolean hidden = manager.isMusicMuted();
                
                if (hidden) {
                    System.out.println("🔊 Música activada");
                    // Reproducir sonido de "música activada"
                } else {
                    System.out.println("🔇 Música desactivada");
                    // Reproducir sonido de "música desactivada"
                }
            });
        }
    }
    
    
    // ============ EJEMPLO 4: NOTIFICACIONES VISUALES (FUTURO) ============
    
    /**
     * Ejemplo de cómo implementar notificaciones visuales
     * (Cuando se implemente ToastNotification)
     */
    public static class NotificationsExample {
        
        public void setupNotifications() {
            KeyBindingManager manager = KeyBindingManager.getInstance();
            
            // Cuando se implemente showNotification en KeyBindingManager
            // Se podrán hacer cosas como:
            
            /*
            manager.registerAction("TOGGLE_MUSIC", () -> {
                boolean isMuted = manager.isMusicMuted();
                
                ToastNotificationManager.show(
                    isMuted ? "🔊 Música activada" : "🔇 Música desactivada",
                    3000  // duracion en ms
                );
            });
            */
        }
    }
    
    
    // ============ EJEMPLO 5: RESTRICCIÓN DE ATAJOS POR VISTA ============
    
    /**
     * Ejemplo de cómo restricción atajos a ciertas vistas
     */
    public static class ViewSpecificShortcutsExample {
        
        private boolean isActiveView = false;
        
        public void onViewOpen() {
            isActiveView = true;
            
            KeyBindingManager manager = KeyBindingManager.getInstance();
            
            // Registrar acción solo para esta vista
            manager.registerAction("VIEW_SPECIFIC_ACTION", () -> {
                if (isActiveView) {
                    System.out.println("Acción específica de esta vista ejecutada");
                }
            });
        }
        
        public void onViewClose() {
            isActiveView = false;
        }
    }
    
    
    // ============ EJEMPLO 6: PATRÓN COMPLETO EN CONTROLADOR ============
    
    /**
     * Ejemplo completo de integración en un controlador real
     */
    public static class CompleteControllerExample {
        
        private MediaPlayer videoPlayer;
        private KeyBindingManager keyBindingManager;
        
        // EN INITIALIZE
        public void initialize() {
            // 1. Obtener instancia
            keyBindingManager = KeyBindingManager.getInstance();
            
            // 2. Habilitar control de video
            keyBindingManager.enableVideoControl(() -> toggleVideo());
            
            // 3. Registrar acciones personalizadas si las necesitas
            registerCustomActions();
            
            System.out.println("✅ Controller inicializado - Atajos listos");
        }
        
        // MÉTODO PRIVADO PARA ACCIONES PERSONALIZADAS
        private void registerCustomActions() {
            // Aquí puedes registrar cualquier atajo personalizado
            // que necesite tu controlador específico
        }
        
        // MÉTODO PARA TOGGLE DE VIDEO
        private void toggleVideo() {
            if (videoPlayer == null) return;
            
            switch (videoPlayer.getStatus()) {
                case PLAYING:
                    videoPlayer.pause();
                    break;
                case PAUSED:
                case STOPPED:
                    videoPlayer.play();
                    break;
                default:
                    break;
            }
        }
        
        // CLEANUP CUANDO SALIMOS DE LA VISTA
        public void cleanup() {
            if (keyBindingManager != null) {
                keyBindingManager.disableVideoControl();
            }
        }
    }
    
    
    // ============ EJEMPLO 7: TESTS UNITARIOS ============
    
    /**
     * Ejemplo de cómo testear los atajos (pseudocódigo)
     */
    public static class KeyBindingsTests {
        
        /*
        @Test
        public void testToggleMusicShortcut() {
            KeyBindingManager manager = KeyBindingManager.getInstance();
            assertFalse(manager.isMusicMuted());
            
            // Simular presión de Alt+M
            manager.executeAction("TOGGLE_MUSIC");
            assertTrue(manager.isMusicMuted());
        }
        
        @Test
        public void testToggleSoundsShortcut() {
            KeyBindingManager manager = KeyBindingManager.getInstance();
            assertTrue(SettingsController.isTypingSoundEnabled);
            
            // Simular presión de Alt+S
            manager.executeAction("TOGGLE_SOUNDS");
            assertFalse(SettingsController.isTypingSoundEnabled);
        }
        
        @Test
        public void testVideoControlCallback() {
            KeyBindingManager manager = KeyBindingManager.getInstance();
            
            // Registrar callback
            manager.enableVideoControl(() -> {
                // Mock video toggle
            });
            
            // Ejecutar acción
            manager.executeAction("TOGGLE_VIDEO");
            
            // Verificar que se ejecutó
        }
        */
    }
    
    
    // ============ REFERENCE ============
    
    /**
     * Referencia rápida de atajos disponibles:
     * 
     * AUDIO:
     *   Alt + M      → Mutear música
     *   Alt + S      → Mutear sonidos
     *   Ctrl + M     → Mutear todo
     * 
     * VIDEO:
     *   Espacio      → Pausar/Reanudar
     *   Alt + V      → Pausar/Reanudar (alt)
     * 
     * APLICACIÓN:
     *   F11          → Pantalla completa
     *   Ctrl + K     → Mostrar atajos
     *   Ctrl + Q     → Salir
     */
}
