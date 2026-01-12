package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.models.Character; // <--- IMPORTANTE: Importar el modelo
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class HomeController {

    @FXML private ProgressBar hpBar;
    @FXML private ProgressBar xpBar;
    @FXML private ListView<String> questList;
    @FXML private Label currentAppLabel;
    @FXML private Label lblWelcome;

    private Character activeCharacter; // Guardamos el personaje activo

    @FXML
    public void initialize() {
        // Cargar misiones iniciales
        questList.getItems().addAll(
            "⚔️ Derrotar al Bug de la línea 42",
            "📜 Leer 10 páginas de documentación",
            "🧪 Poción de Cafeína (Completar reporte)"
        );
        
        animateBars(0.85, 0.30);
        iniciarMonitoreoLocal();
    }

    /**
     * ESTE ES EL MÉTODO QUE FALTA Y CAUSA EL ERROR DE MAVEN.
     * Recibe el personaje desde el CharacterSelectionController.
     */
    public void setPlayerCharacter(Character character) {
        this.activeCharacter = character;
        if (activeCharacter != null && lblWelcome != null) {
            lblWelcome.setText("HÉROE: " + activeCharacter.getName().toUpperCase());
            System.out.println("Cargando Dashboard para: " + activeCharacter.getName());
            
            // Actualizar barras basadas en el nivel del personaje
            double xpSimulada = (activeCharacter.getLevel() % 10) / 10.0;
            animateBars(0.95, xpSimulada);
        }
    }

    /**
     * Mantiene compatibilidad con el LoginController antiguo.
     */
    public void setPlayerInfo(String username) {
        if (lblWelcome != null) {
            lblWelcome.setText("BIENVENIDO, " + username.toUpperCase());
        }
    }

    private void animateBars(double hp, double xp) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(hpBar.progressProperty(), 0),
                new KeyValue(xpBar.progressProperty(), 0)
            ),
            new KeyFrame(Duration.millis(1200), 
                new KeyValue(hpBar.progressProperty(), hp),
                new KeyValue(xpBar.progressProperty(), xp)
            )
        );
        timeline.play();
    }

    private void iniciarMonitoreoLocal() {
        currentAppLabel.setText("App Detectada: VS Code (Ganando XP...)");
    }

    @FXML
    public void handleLogout() {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) hpBar.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}