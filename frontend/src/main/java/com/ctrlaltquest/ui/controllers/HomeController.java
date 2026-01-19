package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class HomeController {

    @FXML private Label lblUsername, lblClass, lblLevel, lblCoins, lblXPText, lblCurrentApp, lblAppStatus, lblHealthStreak;
    @FXML private ProgressBar xpBar, healthBar;
    @FXML private VBox missionsContainer;
    @FXML private MediaView backgroundVideo;
    
    private MediaPlayer videoPlayer;
    private Character currentCharacter;

    @FXML
    public void initialize() {
        configurarFondo();
        iniciarMonitoreoActividad();
        
        // Sincronizar audio global (música de fondo)
        SoundManager.getInstance().synchronizeMusic();
    }

    /**
     * Método clave: Recibe los datos del personaje desde el selector
     */
    public void initPlayerData(Character character) {
        if (character == null) return;
        this.currentCharacter = character;

        Platform.runLater(() -> {
            lblUsername.setText(character.getName().toUpperCase());
            lblClass.setText(obtenerNombreClase(character.getClassId()));
            lblLevel.setText(String.valueOf(character.getLevel()));
            lblCoins.setText(String.valueOf(character.getCoins()));
            
            // Configuración de Barra de XP (Ejemplo: 1000 XP por nivel)
            double xpProgress = (double) character.getCurrentXp() / 1000.0;
            xpBar.setProgress(xpProgress);
            lblXPText.setText(character.getCurrentXp() + " / 1000 XP");

            // Barra de Salud / Racha
            healthBar.setProgress(1.0); // Por defecto lleno al iniciar
            if(lblHealthStreak != null) lblHealthStreak.setText("RACHA: " + character.getHealthStreak() + " DÍAS");
        });
    }

    private String obtenerNombreClase(int classId) {
        return switch (classId) {
            case 1 -> "PROGRAMADOR";
            case 2 -> "LECTOR";
            case 3 -> "ESCRITOR";
            default -> "AVENTURERO TECH";
        };
    }

    private void configurarFondo() {
        try {
            URL videoUrl = getClass().getResource("/assets/videos/main_hub.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                videoPlayer = new MediaPlayer(media);
                backgroundVideo.setMediaPlayer(videoPlayer);
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                videoPlayer.setRate(0.6); 
                backgroundVideo.setEffect(new GaussianBlur(20));

                if (SettingsController.isVideoPaused) {
                    videoPlayer.pause();
                } else {
                    videoPlayer.play();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar el video de fondo: " + e.getMessage());
        }
    }

    private void iniciarMonitoreoActividad() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                // Simulación de monitoreo (Aquí conectarás tu servicio de JNA o procesos)
                Platform.runLater(() -> {
                    if (lblCurrentApp != null) lblCurrentApp.setText("IDE: IntelliJ IDEA");
                    if (lblAppStatus != null) lblAppStatus.setText("ESTADO: PRODUCTIVO (+XP)");
                });
                try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
            }
        });
        monitorThread.setDaemon(true); // Se cierra al cerrar la app
        monitorThread.start();
    }

    // --- ACCIONES DE NAVEGACIÓN ---

    @FXML private void showMissions() { SoundManager.playClickSound(); }
    @FXML private void showStore() { SoundManager.playClickSound(); }
    @FXML private void showInventory() { SoundManager.playClickSound(); }
    @FXML private void showStats() { SoundManager.playClickSound(); }
    
    @FXML 
    private void showSettings() { 
        SoundManager.playClickSound();
        // Lógica para abrir modal de settings similar a la de selección
    }

    @FXML
    private void handleLogout() {
        SoundManager.playClickSound();
        try {
            if (videoPlayer != null) {
                videoPlayer.stop();
                videoPlayer.dispose();
            }
            SessionManager.getInstance().logout();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblUsername.getScene().getWindow();
            
            FadeTransition ft = new FadeTransition(Duration.millis(500), root);
            ft.setFromValue(0); ft.setToValue(1);
            stage.getScene().setRoot(root);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}