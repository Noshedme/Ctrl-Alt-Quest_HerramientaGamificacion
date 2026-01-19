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
    private static AudioClip clickSound;
    private static AudioClip errorSound;
    private static AudioClip hoverSound;
    private MediaPlayer musicPlayer;

    // --- BLOQUE ESTÁTICO PARA PRECARGAR EFECTOS DE SONIDO ---
    static {
        loadEffects();
    }

    private static void loadEffects() {
        try {
            keySound = loadAudioClip("/assets/sounds/key_press.wav");
            clickSound = loadAudioClip("/assets/sounds/click.mp3");
            errorSound = loadAudioClip("/assets/sounds/error.mp3");
            hoverSound = loadAudioClip("/assets/sounds/hover.mp3");
        } catch (Exception e) {
            System.err.println("❌ Error al precargar sonidos: " + e.getMessage());
        }
    }

    private static AudioClip loadAudioClip(String path) {
        URL url = SoundManager.class.getResource(path);
        if (url != null) {
            return new AudioClip(url.toExternalForm());
        }
        System.err.println("⚠️ No se encontró el recurso de audio: " + path);
        return null;
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

    // --- MÉTODOS DE CONTROL DE MÚSICA ---

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

    // --- MÉTODOS ESTÁTICOS PARA EFECTOS (Lo que pedían los controladores) ---

    public static void playKeyClick() {
        if (SettingsController.isTypingSoundEnabled && keySound != null) {
            playEffect(keySound, 0.2);
        }
    }

    public static void playClickSound() {
        if (clickSound != null) {
            playEffect(clickSound, 0.5);
        }
    }

    public static void playErrorSound() {
        if (errorSound != null) {
            playEffect(errorSound, 0.6);
        }
    }

    public static void playHoverSound() {
        if (hoverSound != null) {
            playEffect(hoverSound, 0.3);
        }
    }

    /**
     * Lógica interna para reproducir un efecto sin solapamiento brusco.
     */
    private static void playEffect(AudioClip clip, double volume) {
        try {
            if (clip.isPlaying()) {
                clip.stop();
            }
            clip.setVolume(volume);
            clip.play();
        } catch (Exception e) {
            System.err.println("❌ Error al reproducir efecto: " + e.getMessage());
        }
    }
}