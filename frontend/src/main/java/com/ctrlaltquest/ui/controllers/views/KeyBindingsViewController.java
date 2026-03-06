package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.ui.utils.KeyBindings;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
        // inicializar Toast container
        try {
            StackPane root = (StackPane) mainPane.getScene().getRoot();
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            Toast.initialize(toastContainer);
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception ignored) {}
        Toast.info("Atajos", "Lista de atajos cargada.");
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
        card.getStyleClass().add("keybinding-card");
        
        // Nombre del atajo
        Label nameLabel = new Label(binding.name);
        nameLabel.getStyleClass().add("keybinding-name");
        
        // Combinación de teclas
        Label keyLabel = new Label(binding.keyCombination);
        keyLabel.getStyleClass().add("key-combination");
        
        // Descripción
        Label descLabel = new Label(binding.description);
        descLabel.getStyleClass().add("keybinding-description");
        descLabel.setWrapText(true);
        
        card.getChildren().addAll(nameLabel, keyLabel, descLabel);
        return card;
    }
    
    @FXML
    private void handleClose() {
        Toast.info("Atajos", "Cerrando vista de atajos.");
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.close();
    }
}
