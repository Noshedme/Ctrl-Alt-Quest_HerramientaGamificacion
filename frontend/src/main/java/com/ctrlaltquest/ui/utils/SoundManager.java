package com.ctrlaltquest.ui.utils;

import com.ctrlaltquest.ui.controllers.SettingsController;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

/**
 * SoundManager - Gestor Global de Audio (Singleton)
 * Se encarga de la música ambiental y los efectos de sonido de la interfaz.
 */
public class SoundManager {
    private static SoundManager instance;
    private static AudioClip keySound;
    private MediaPlayer musicPlayer;

    // --- BLOQUE ESTÁTICO PARA EFECTOS DE SONIDO ---
    static {
        try {
            // Asegúrate de que el nombre del archivo coincida exactamente (key_press.wav o key_click.mp3)
            URL soundUrl = SoundManager.class.getResource("/assets/sounds/key_press.wav");
            if (soundUrl != null) {
                keySound = new AudioClip(soundUrl.toExternalForm());
                keySound.setVolume(0.2); // Volumen bajo para que no sea molesto
            } else {
                System.err.println("⚠️ No se encontró el archivo de sonido de teclas.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al precargar sonidos: " + e.getMessage());
        }
    }

    // --- CONSTRUCTOR (Singleton) ---
    private SoundManager() {
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

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    // --- MÉTODOS DE CONTROL ---

    /**
     * Sincroniza el estado de la música con la configuración global.
     */
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
     * Reproduce el sonido de tecla. 
     * Este es el Portero: si la opción está desactivada, no suena nada.
     */
    public static void playKeyClick() {
        // VALIDACIÓN CRÍTICA: Aquí es donde se soluciona tu problema
        if (SettingsController.isTypingSoundEnabled && keySound != null) {
            // Usamos una pequeña validación para no saturar el canal de audio si se escribe muy rápido
            if (!keySound.isPlaying()) {
                keySound.play();
            } else {
                // Si ya está sonando, lo detenemos y reiniciamos para que se sienta reactivo
                keySound.stop();
                keySound.play();
            }
        }
    }
}