package com.ctrlaltquest.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private MediaView backgroundVideo;

    @FXML
    private ToggleButton muteButton;

    @FXML
    private ImageView sidebarLogo;

    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        var logoUrl = getClass().getResource("/assets/images/logo.png");
        if (logoUrl != null) {
            sidebarLogo.setImage(new Image(logoUrl.toExternalForm()));
        }

        var videoUrl = getClass().getResource("/assets/videos/introVideo.mp4");
        if (videoUrl != null) {
            Media media = new Media(videoUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundVideo.setMediaPlayer(mediaPlayer);
            mediaPlayer.setAutoPlay(true);
        }
        Media media = new Media(getClass().getResource("/assets/videos/introVideo.mp4").toExternalForm());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backgroundVideo.setMediaPlayer(mediaPlayer);
        mediaPlayer.setAutoPlay(true);
        updateMuteState(false);
    }

    public void handleLogin() {
        // TODO: Integrar con backend.
    }

    public void handleGoToRegister() {
        // TODO: Navegar a registro.
    }

    public void handleForgotPassword() {
        // TODO: Navegar a recuperación.
    }

    public void handleToggleMute() {
        updateMuteState(muteButton.isSelected());
    }

    private void updateMuteState(boolean muted) {
        if (mediaPlayer != null) {
            mediaPlayer.setMute(muted);
        }
        if (muteButton != null) {
            muteButton.setText(muted ? "🔇" : "🔊");
        }
    }
}
