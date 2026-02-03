package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SettingsController {

    @FXML private VBox mainContainer;
    @FXML private CheckBox checkTypingSound;
    @FXML private CheckBox checkPauseVideo;
    @FXML private CheckBox checkBackgroundMusic;

    // Estados estáticos para que persistan entre cambios de pantalla
    public static boolean isTypingSoundEnabled = true;
    public static boolean isVideoPaused = false;
    public static boolean isMusicEnabled = true;

    // Referencias a los distintos controladores para control de video en tiempo real
    private LoginController loginController;
    private RegisterController registerController;
    private CharacterSelectionController selectionController;
    private CharacterEditorController editorController; // Preparado para el siguiente paso

    @FXML
    public void initialize() {
        // Cargar estados actuales en los CheckBox
        checkTypingSound.setSelected(isTypingSoundEnabled);
        checkPauseVideo.setSelected(isVideoPaused);
        checkBackgroundMusic.setSelected(isMusicEnabled);
        
        if (mainContainer != null) {
            mainContainer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), mainContainer);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    // --- MÉTODOS SETTER PARA INYECCIÓN DE CONTROLADORES ---
    
    public void setLoginController(LoginController ctrl) { 
        this.loginController = ctrl; 
    }
    
    public void setRegisterController(RegisterController ctrl) { 
        this.registerController = ctrl; 
    }

    /**
     * Este es el método que faltaba y causaba el error en CharacterSelectionController
     */
    public void setSelectionController(CharacterSelectionController ctrl) { 
        this.selectionController = ctrl; 
    }

    /**
     * Preparado para cuando editemos el creador de personajes
     */
    public void setEditorController(CharacterEditorController ctrl) { 
        this.editorController = ctrl; 
    }

    // --- MANEJADORES DE EVENTOS (ON ACTION) ---

    @FXML
    private void handlePauseVideo() {
        isVideoPaused = checkPauseVideo.isSelected();
        boolean shouldPlay = !isVideoPaused;

        // Notificar a cualquier controlador activo que pause o reanude su video
        if (loginController != null) loginController.setVideoPlaying(shouldPlay);
        if (registerController != null) registerController.setVideoPlaying(shouldPlay);
        if (selectionController != null) selectionController.setVideoPlaying(shouldPlay);
        if (editorController != null) editorController.setVideoPlaying(shouldPlay);
    }

    @FXML
    private void handleToggleMusic() {
        isMusicEnabled = checkBackgroundMusic.isSelected();
        // Sincronización inmediata a través del Singleton
        SoundManager.getInstance().synchronizeMusic();
    }

    @FXML
    private void handleToggleTypingSound() {
        isTypingSoundEnabled = checkTypingSound.isSelected();
        // SoundManager consultará esta variable estática en la próxima pulsación
    }

    @FXML
    private void handleClose() {
        // Guardar estado final antes de cerrar
        isTypingSoundEnabled = checkTypingSound.isSelected();
        
        Stage stage = (Stage) mainContainer.getScene().getWindow();
        
        // Transición de salida antes de cerrar la ventana modal
        FadeTransition ft = new FadeTransition(Duration.millis(200), mainContainer);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> stage.close());
        ft.play();
    }
}