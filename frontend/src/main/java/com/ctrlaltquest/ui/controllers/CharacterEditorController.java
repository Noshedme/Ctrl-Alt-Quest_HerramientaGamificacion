package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class CharacterEditorController {

    @FXML private TextField nameField;
    @FXML private StackPane rootContainer;
    @FXML private MediaView bgMedia;
    
    // Capas del Avatar
    @FXML private ImageView layerBody, layerHead, layerChest, layerLegs, layerFeet;
    @FXML private StackPane spritePreview;
    
    @FXML private ToggleButton toggleMale, toggleFemale;
    @FXML private Label lblHeadName, lblChestName, lblLegsName;
    @FXML private ChoiceBox<String> classSelector;

    private MediaPlayer mediaPlayer;
    private int userId;
    private int slotIndex;
    
    private String currentGender = "male";
    private int headIndex = 0;
    private int chestIndex = 0;
    private int legsIndex = 0;

    private final List<String> headItems = Arrays.asList("none", "prog_visor", "read_monocle", "write_beret");
    private final List<String> chestItems = Arrays.asList("basic", "prog_hoodie", "read_tunic", "write_shirt");
    private final List<String> legsItems = Arrays.asList("basic", "prog_pants", "read_skirt", "write_dark");

    @FXML
    public void initialize() {
        if (bgMedia != null) {
            bgMedia.setEffect(new javafx.scene.effect.GaussianBlur(15));
            setupBackground();
        }
        
        if (classSelector != null) {
            classSelector.getItems().clear();
            classSelector.getItems().addAll("Programador", "Lector", "Escritor");
            classSelector.setValue("Programador");
            classSelector.setOnAction(e -> playSelectionSound());
        }

        ToggleGroup genderGroup = new ToggleGroup();
        if (toggleMale != null && toggleFemale != null) {
            toggleMale.setToggleGroup(genderGroup);
            toggleFemale.setToggleGroup(genderGroup);
            toggleMale.setSelected(true);
        }

        SoundManager.getInstance().synchronizeMusic();
        refreshAvatar();
    }

    private void refreshAvatar() {
        try {
            // 1. Cuerpo Base
            updateLayer(layerBody, "bases/body_" + currentGender);
            
            // 2. Cabeza
            String head = headItems.get(headIndex);
            if (lblHeadName != null) lblHeadName.setText(head.replace("_", " ").toUpperCase());
            updateLayer(layerHead, head.equals("none") ? null : "head/" + head);

            // 3. Torso
            String chest = chestItems.get(chestIndex);
            if (lblChestName != null) lblChestName.setText(chest.replace("_", " ").toUpperCase());
            updateLayer(layerChest, "chest/" + chest);

            // 4. Piernas
            String legs = legsItems.get(legsIndex);
            if (lblLegsName != null) lblLegsName.setText(legs.replace("_", " ").toUpperCase());
            updateLayer(layerLegs, "legs/" + legs);

            // Animación suave al cambiar
            if (spritePreview != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), spritePreview);
                ft.setFromValue(0.85); ft.setToValue(1.0); ft.play();
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error al actualizar capas: " + e.getMessage());
        }
    }

    /**
     * Actualiza una capa específica asegurando el tamaño y alineación.
     */
    private void updateLayer(ImageView layer, String path) {
        if (layer == null) return;
        
        if (path == null) {
            layer.setImage(null);
            return;
        }

        Image img = loadImage(path);
        if (img != null) {
            layer.setImage(img);
            layer.setFitHeight(380); // Mismo tamaño para todas las capas
            layer.setPreserveRatio(true);
            layer.setSmooth(false); // Estilo Pixel Art nítido
        }
    }

    private Image loadImage(String path) {
        String fullPath = "/assets/images/sprites/" + path + ".png";
        InputStream is = getClass().getResourceAsStream(fullPath);
        if (is == null) {
            System.err.println("❌ No se encontró la imagen: " + fullPath);
            return null; 
        }
        return new Image(is);
    }

    @FXML private void setMale() { currentGender = "male"; refreshAvatar(); }
    @FXML private void setFemale() { currentGender = "female"; refreshAvatar(); }

    @FXML private void nextHead() { headIndex = (headIndex + 1) % headItems.size(); playSelectionSound(); refreshAvatar(); }
    @FXML private void prevHead() { headIndex = (headIndex - 1 + headItems.size()) % headItems.size(); playSelectionSound(); refreshAvatar(); }
    
    @FXML private void nextChest() { chestIndex = (chestIndex + 1) % chestItems.size(); playSelectionSound(); refreshAvatar(); }
    @FXML private void prevChest() { chestIndex = (chestIndex - 1 + chestItems.size()) % chestItems.size(); playSelectionSound(); refreshAvatar(); }
    
    @FXML private void nextLegs() { legsIndex = (legsIndex + 1) % legsItems.size(); playSelectionSound(); refreshAvatar(); }
    @FXML private void prevLegs() { legsIndex = (legsIndex - 1 + legsItems.size()) % legsItems.size(); playSelectionSound(); refreshAvatar(); }

    @FXML
    public void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController settingsCtrl = loader.getController();
            settingsCtrl.setEditorController(this);

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(rootContainer.getScene().getWindow());
            settingsStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setupBackground() {
        URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4");
        if (videoUrl != null) {
            mediaPlayer = new MediaPlayer(new Media(videoUrl.toExternalForm()));
            bgMedia.setMediaPlayer(mediaPlayer);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            mediaPlayer.setRate(0.5);
            if (!SettingsController.isVideoPaused) mediaPlayer.play();
        }
    }

    public void setVideoPlaying(boolean play) {
        if (mediaPlayer != null) {
            if (play) mediaPlayer.play();
            else mediaPlayer.pause();
        }
    }

    public void setInitData(int userId, int slotIndex) {
        this.userId = userId;
        this.slotIndex = slotIndex;
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty() || name.length() < 3) return;

        Character newChar = new Character();
        newChar.setUserId(userId);
        newChar.setName(name);
        newChar.setSlotIndex(slotIndex);
        newChar.setLevel(1);
        newChar.setClassId(classSelector.getSelectionModel().getSelectedIndex() + 1);

        if (CharacterDAO.saveCharacter(newChar)) {
            regresarEscena("/fxml/character_selection.fxml");
        }
    }

    @FXML private void handleCancel() { regresarEscena("/fxml/login.fxml"); }

    private void regresarEscena(String fxmlPath) {
        try {
            if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            if (fxmlPath.contains("character_selection")) {
                CharacterSelectionController controller = loader.getController();
                controller.initData(userId, "Jugador");
            }
            Stage stage = (Stage) rootContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void playSelectionSound() { SoundManager.playKeyClick(); }
}