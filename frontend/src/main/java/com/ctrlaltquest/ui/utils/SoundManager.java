package com.ctrlaltquest.ui.utils;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {
    private static AudioClip keySound;

    static {
        URL soundUrl = SoundManager.class.getResource("/assets/sounds/key_press.wav");
        if (soundUrl != null) {
            keySound = new AudioClip(soundUrl.toExternalForm());
            keySound.setVolume(0.3); // Volumen bajo para que no sea molesto
        }
    }

    public static void playKeyClick() {
        if (keySound != null) {
            keySound.play();
        }
    }
}