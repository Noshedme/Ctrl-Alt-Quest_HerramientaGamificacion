package com.ctrlaltquest.ui.utils;

import com.ctrlaltquest.ui.controllers.SettingsController;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

/**
 * SoundManager - Gestor Global de Audio (Singleton)
 * Versión optimizada: Carga dinámica y reutilización de recursos.
 */
public class SoundManager {

    private static SoundManager instance;

    // --- EFECTOS DE SONIDO ---
    private AudioClip clickSound;
    private AudioClip successSound;
    private AudioClip levelUpSound;
    private AudioClip errorSound;
    // Hover sound eliminado explícitamente para ahorrar recursos (opcional)

    // --- MÚSICA DE FONDO ---
    private MediaPlayer musicPlayer;

    // --- CONSTRUCTOR ---
    private SoundManager() {
        // 1. Cargar Efectos (Usando nombres simplificados del segundo código)
        clickSound = loadSound("click.mp3");
        successSound = loadSound("success.mp3");
        levelUpSound = loadSound("levelup.mp3");
        errorSound = loadSound("error.mp3");

        // 2. Cargar Música de Fondo (Mantenemos la lógica robusta del Base)
        try {
            URL musicUrl = getClass().getResource("/assets/sounds/login_theme.mp3");
            if (musicUrl != null) {
                Media music = new Media(musicUrl.toExternalForm());
                musicPlayer = new MediaPlayer(music);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.setVolume(0.4);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar música global: " + e.getMessage());
        }
    }

    /**
     * Helper simplificado para cargar sonidos desde /assets/sounds/
     */
    private AudioClip loadSound(String filename) {
        try {
            URL url = getClass().getResource("/assets/sounds/" + filename);
            return (url != null) ? new AudioClip(url.toExternalForm()) : null;
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar sonido: " + filename);
            return null;
        }
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    // --- MÉTODOS PÚBLICOS DE EFECTOS ---

    public static void playClickSound() {
        // Reproducir click normal
        playEffect(getInstance().clickSound, 0.5);
    }

    public static void playKeyClick() {
        if (SettingsController.isTypingSoundEnabled) {
            // Reutilizamos el sonido de click con volumen bajo (0.2) como se pidió
            playEffect(getInstance().clickSound, 0.2);
        }
    }

    public static void playSuccessSound() {
        playEffect(getInstance().successSound, 0.7);
    }

    public static void playLevelUpSound() {
        playEffect(getInstance().levelUpSound, 0.8);
    }

    public static void playErrorSound() {
        playEffect(getInstance().errorSound, 0.6);
    }

    public static void playHoverSound() {
        // Opcional: Si quieres sonido al pasar el mouse, descomenta abajo usando el click muy suave
        // playEffect(getInstance().clickSound, 0.1); 
    }
    
    // ========== SONIDOS DE EVENTOS CONTEXTUALES ==========
    
    public static void playEventSound() {
        // Sonido cuando aparece un evento contextual
        playSuccessSound();
    }
    
    public static void playEventWinSound() {
        // Sonido cuando se completa un evento exitosamente
        playLevelUpSound();
    }
    
    public static void playEventFailSound() {
        // Sonido cuando se falla un evento
        playErrorSound();
    }

    // --- CONTROL DE MÚSICA (Integración con SettingsController) ---

    public void synchronizeMusic() {
        if (musicPlayer == null) return;

        if (SettingsController.isMusicEnabled) {
            if (musicPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                musicPlayer.play();
            }
        } else {
            musicPlayer.pause();
        }
    }

    /**
     * Helper seguro para reproducir audio con volumen específico.
     */
    private static void playEffect(AudioClip clip, double volume) {
        if (clip == null) return;
        try {
            if (clip.isPlaying()) clip.stop();
            clip.setVolume(volume);
            clip.play();
        } catch (Exception e) {
            // Ignoramos errores de reproducción momentáneos
        }
    }
}