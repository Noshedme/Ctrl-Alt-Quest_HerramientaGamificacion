package com.ctrlaltquest.ui.utils;

import com.ctrlaltquest.ui.controllers.SettingsController;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

/**
 * SoundManager - Gestor Global de Audio (Singleton)
 */
public class SoundManager {

    private static SoundManager instance;

    private AudioClip clickSound;
    private AudioClip successSound;
    private AudioClip levelUpSound;
    private AudioClip errorSound;
    private AudioClip gritoSound; // 🎁 El Grito

    private MediaPlayer musicPlayer;

    // Estado del item "El Grito"
    private static boolean gritoActivado = false;

    private SoundManager() {
        clickSound   = loadSound("click.mp3");
        successSound = loadSound("success.mp3");
        levelUpSound = loadSound("levelup.mp3");
        errorSound   = loadSound("error.mp3");
        gritoSound   = loadSound("grito.mp3"); // puede ser null si no existe aún

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
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // ── Efectos estándar ──────────────────────────────────────────────────────

    public static void playClickSound() {
        // Si El Grito está activado, cada clic suena el grito
        if (gritoActivado) {
            playGrito();
            return;
        }
        playEffect(getInstance().clickSound, 0.5);
    }

    public static void playKeyClick() {
        if (SettingsController.isTypingSoundEnabled) {
            // El grito NO se activa al escribir, solo en clics de mouse
            playEffect(getInstance().clickSound, 0.2);
        }
    }

    public static void playSuccessSound() { playEffect(getInstance().successSound, 0.7); }
    public static void playLevelUpSound() { playEffect(getInstance().levelUpSound, 0.8); }
    public static void playErrorSound()   { playEffect(getInstance().errorSound,   0.6); }
    public static void playHoverSound()   {}

    public static void playEventSound()    { playSuccessSound(); }
    public static void playEventWinSound() { playLevelUpSound(); }
    public static void playEventFailSound(){ playErrorSound();   }

    // ── El Grito ─────────────────────────────────────────────────────────────

    /** Reproduce el grito directamente (sin importar el estado de activación). */
    public static void playGrito() {
        playEffect(getInstance().gritoSound, 0.9);
    }

    /** Activa o desactiva El Grito. */
    public static void setGritoActivado(boolean activado) {
        gritoActivado = activado;
        System.out.println("😱 [SoundManager] El Grito: " + (activado ? "ACTIVADO" : "DESACTIVADO"));
    }

    public static boolean isGritoActivado() {
        return gritoActivado;
    }

    /** True si el archivo grito.mp3 fue cargado correctamente. */
    public static boolean isGritoDisponible() {
        return getInstance().gritoSound != null;
    }

    // ── Música ───────────────────────────────────────────────────────────────

    public void synchronizeMusic() {
        if (musicPlayer == null) return;
        if (SettingsController.isMusicEnabled) {
            if (musicPlayer.getStatus() != MediaPlayer.Status.PLAYING) musicPlayer.play();
        } else {
            musicPlayer.pause();
        }
    }

    private static void playEffect(AudioClip clip, double volume) {
        if (clip == null) return;
        try {
            if (clip.isPlaying()) clip.stop();
            clip.setVolume(volume);
            clip.play();
        } catch (Exception ignored) {}
    }
}