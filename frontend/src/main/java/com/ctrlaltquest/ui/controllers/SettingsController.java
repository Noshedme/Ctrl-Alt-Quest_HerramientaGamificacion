package com.ctrlaltquest.ui.controllers;

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

    // Variables de estado compartidas
    public static boolean isTypingSoundEnabled = true;
    public static boolean isVideoPaused = false;

    private LoginController loginController;
    private RegisterController registerController;

    @FXML
    public void initialize() {
        // Sincronizar con el estado actual
        checkTypingSound.setSelected(isTypingSoundEnabled);
        checkPauseVideo.setSelected(isVideoPaused);
        
        // Animación de entrada suave
        if (mainContainer != null) {
            mainContainer.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), mainContainer);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    public void setLoginController(LoginController ctrl) { this.loginController = ctrl; }
    public void setRegisterController(RegisterController ctrl) { this.registerController = ctrl; }

    @FXML
    private void handlePauseVideo() {
        isVideoPaused = checkPauseVideo.isSelected();
        if (loginController != null) loginController.setVideoPlaying(!isVideoPaused);
        if (registerController != null) registerController.setVideoPlaying(!isVideoPaused);
        System.out.println(isVideoPaused ? "⏸ Tiempo congelado." : "▶ Tiempo reanudado.");
    }

    @FXML
    private void handleClose() {
        isTypingSoundEnabled = checkTypingSound.isSelected();
        Stage stage = (Stage) checkTypingSound.getScene().getWindow();
        stage.close();
    }
}