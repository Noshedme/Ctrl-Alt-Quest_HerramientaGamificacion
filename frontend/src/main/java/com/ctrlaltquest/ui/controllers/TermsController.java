package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class TermsController {

    @FXML private CheckBox agreeCheckBox;
    @FXML private Button btnContinue;
    @FXML private ScrollPane scrollPane;

    @FXML
    public void initialize() {
        // Inicialmente el botón está deshabilitado
        btnContinue.setDisable(true);
        
        // Opcional: Hacer el ScrollPane transparente por código si el CSS falla
        if(scrollPane != null) {
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }
    }

    @FXML
    private void handleCheck() {
        // Habilita el botón solo si el checkbox está marcado
        boolean isSelected = agreeCheckBox.isSelected();
        btnContinue.setDisable(!isSelected);
        
        // Opcional: Efecto visual en el botón
        if (isSelected) {
            btnContinue.setOpacity(1.0);
        } else {
            btnContinue.setOpacity(0.5);
        }
    }

    @FXML
    private void handleContinue() {
        try {
            // Cargar el Login con transición suave
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) btnContinue.getScene().getWindow();

            // Efecto de desvanecimiento de salida (Fade Out)
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), btnContinue.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                // Cambiar escena
                Scene loginScene = new Scene(loginRoot, 1280, 720);
                
                // Re-aplicar sonidos globales
                loginScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> SoundManager.playKeyClick());
                
                stage.setScene(loginScene);
                
                // Efecto de entrada (Fade In)
                loginRoot.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            
            fadeOut.play();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar Login: " + e.getMessage());
        }
    }

    @FXML
    private void handleExit() {
        // Cierra la aplicación completamente
        Platform.exit();
        System.exit(0);
    }
}