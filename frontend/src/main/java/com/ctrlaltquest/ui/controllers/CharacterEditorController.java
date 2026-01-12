package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class CharacterEditorController {

    @FXML private TextField nameField;
    @FXML private StackPane spritePreview;

    private int userId;
    private int slotIndex;
    private int selectedClassId = 1; // 1: Guerrero por defecto

    /**
     * Recibe los datos necesarios desde el SelectionController.
     */
    public void setInitData(int userId, int slotIndex) {
        this.userId = userId;
        this.slotIndex = slotIndex;
        updatePreview(); // Cargar preview inicial
    }

    @FXML
    public void initialize() {
        // Configuraciones iniciales si fueran necesarias
    }

    // --- SELECCIÓN DE CLASES ---

    @FXML
    private void selectWarrior() {
        this.selectedClassId = 1;
        playSelectionSound();
        updatePreview();
    }

    @FXML
    private void selectMage() {
        this.selectedClassId = 2;
        playSelectionSound();
        updatePreview();
    }

    @FXML
    private void selectRogue() {
        this.selectedClassId = 3;
        playSelectionSound();
        updatePreview();
    }

    /**
     * Actualiza la imagen del personaje en el StackPane.
     */
    private void updatePreview() {
        spritePreview.getChildren().clear();
        
        try {
            String path = "/assets/images/sprites/base/class_" + selectedClassId + ".png";
            URL imgUrl = getClass().getResource(path);
            
            if (imgUrl != null) {
                ImageView view = new ImageView(new Image(imgUrl.toExternalForm()));
                
                // Configuración Pixel-Perfect para el editor
                view.setFitHeight(300); 
                view.setPreserveRatio(true);
                view.setSmooth(false); 
                
                spritePreview.getChildren().add(view);
                
                // Efecto visual de "aparición"
                FadeTransition ft = new FadeTransition(Duration.millis(300), view);
                ft.setFromValue(0.4);
                ft.setToValue(1.0);
                ft.play();
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar preview: " + e.getMessage());
        }
    }

    // --- ACCIONES PRINCIPALES ---

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();

        if (name.isEmpty() || name.length() < 3) {
            System.out.println("⚠️ El nombre es demasiado corto.");
            // Aquí podrías disparar una animación de sacudida en el TextField
            return;
        }

        // Creamos el objeto modelo
        Character newChar = new Character();
        newChar.setUserId(userId);
        newChar.setName(name);
        newChar.setClassId(selectedClassId);
        newChar.setSlotIndex(slotIndex);
        newChar.setLevel(1);

        // Guardamos en DB mediante el DAO
        boolean success = CharacterDAO.saveCharacter(newChar);

        if (success) {
            System.out.println("✅ Personaje forjado con éxito.");
            volverALaSeleccion();
        } else {
            System.err.println("❌ Error al guardar en la base de datos.");
        }
    }

    @FXML
    private void handleCancel() {
        volverALaSeleccion();
    }

    private void volverALaSeleccion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/character_selection.fxml"));
            Parent root = loader.load();
            
            // Re-inyectamos los datos del usuario para que la lista se refresque
            CharacterSelectionController controller = loader.getController();
            // Nota: Aquí necesitarías tener guardado el username en una sesión global 
            // o pasarlo entre controllers. Por ahora usamos un placeholder.
            controller.initData(userId, "Jugador");

            Stage stage = (Stage) nameField.getScene().getWindow();
            
            // Transición de salida
            FadeTransition ft = new FadeTransition(Duration.millis(400), nameField.getScene().getRoot());
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                Scene scene = new Scene(root, 1280, 720);
                stage.setScene(scene);
            });
            ft.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playSelectionSound() {
        try {
            SoundManager.playKeyClick();
        } catch (Exception ignored) {}
    }
}