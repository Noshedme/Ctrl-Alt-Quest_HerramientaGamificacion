package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.ui.utils.KeyBindings;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controlador para la vista de atajos de teclado
 * Muestra todos los atajos disponibles en la aplicación
 */
public class KeyBindingsViewController {
    
    @FXML private BorderPane mainPane;
    @FXML private VBox bindingsContainer;
    @FXML private Button closeButton;
    
    @FXML
    public void initialize() {
        loadKeyBindings();
    }
    
    /**
     * Carga y muestra todos los atajos disponibles
     */
    private void loadKeyBindings() {
        bindingsContainer.getChildren().clear();
        
        for (KeyBindings.KeyBindingInfo binding : KeyBindings.ALL_BINDINGS) {
            VBox bindingBox = createBindingCard(binding);
            bindingsContainer.getChildren().add(bindingBox);
        }
    }
    
    /**
     * Crea una tarjeta visual para un atajo específico
     */
    private VBox createBindingCard(KeyBindings.KeyBindingInfo binding) {
        VBox card = new VBox(8);
        card.setStyle("-fx-border-color: #44475a; -fx-border-width: 1; -fx-border-radius: 5; " +
                      "-fx-background-color: #282a36; -fx-padding: 15;");
        
        // Nombre del atajo
        Label nameLabel = new Label(binding.name);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f1fa8c;");
        
        // Combinación de teclas
        Label keyLabel = new Label(binding.keyCombination);
        keyLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; " +
                         "-fx-text-fill: #50fa7b; -fx-font-weight: bold;");
        
        // Descripción
        Label descLabel = new Label(binding.description);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8be9fd; -fx-wrap-text: true;");
        descLabel.setWrapText(true);
        
        card.getChildren().addAll(nameLabel, keyLabel, descLabel);
        return card;
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.close();
    }
}
