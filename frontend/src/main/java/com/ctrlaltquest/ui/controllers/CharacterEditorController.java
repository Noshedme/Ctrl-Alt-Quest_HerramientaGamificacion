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
    
    @FXML private ImageView fullCharacterView;
    @FXML private StackPane spritePreview;
    
    @FXML private ToggleButton toggleMale, toggleFemale;
    @FXML private Label lblSkinName; 
    @FXML private ChoiceBox<String> classSelector;

    private MediaPlayer mediaPlayer;
    private int userId;
    private int slotIndex;
    
    private String currentGender = "female"; 
    private int skinIndex = 0;

    // --- LISTAS EXACTAS DE SKINS ---
    
    private final List<String> femaleSkins = Arrays.asList(
        "arquera_fuego_female",
        "arquera_hielo_female",
        "arquera_veneno_female",
        "dk_muerte_female",
        "dk_peste_female",
        "maga_fuego_female",
        "maga_hielo_female",
        "maga_vacio_female",
        "war_fuego_female",
        "war_luz_female",
        "war_sangre_female",
        "body_female"
    );

    private final List<String> maleSkins = Arrays.asList(
        "body_male",
        "dk_muerte_male",
        "dk_peste_male",
        "dk_sangre_male",
        "mago_fuego",
        "mago_hielo",
        "mago_luz",
        "mago_vacio",
        "picaro_armas",
        "picaro_veneno"
    );

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
            toggleFemale.setSelected(true); // Default Female
        }

        SoundManager.getInstance().synchronizeMusic();
        refreshAvatar();
    }

    /**
     * Devuelve la lista activa según el género seleccionado.
     */
    private List<String> getCurrentList() {
        return currentGender.equals("male") ? maleSkins : femaleSkins;
    }

    private void refreshAvatar() {
        try {
            List<String> activeList = getCurrentList();
            
            // Seguridad: Si cambiamos de lista y el índice se sale, lo reseteamos
            if (skinIndex >= activeList.size()) {
                skinIndex = 0;
            }

            // Obtenemos el nombre EXACTO del archivo de la lista
            String fileName = activeList.get(skinIndex);
            
            // Actualizar etiqueta visual (limpiamos guiones bajos para que se vea bonito)
            if (lblSkinName != null) {
                String displayName = fileName.replace("_female", "").replace("_male", "").replace("_", " ").toUpperCase();
                lblSkinName.setText(displayName);
            }

            // Cargar imagen directa
            updateImage(fullCharacterView, fileName);

            if (spritePreview != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), spritePreview);
                ft.setFromValue(0.85); ft.setToValue(1.0); ft.play();
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error al actualizar avatar: " + e.getMessage());
        }
    }

    private void updateImage(ImageView layer, String fileName) {
        if (layer == null) return;
        
        Image img = loadImage("bases/" + fileName);
        
        // Fallback simple: Si falla, intenta cargar el body del género actual
        if (img == null) {
            System.err.println("❌ No se encontró la imagen: " + fileName);
            img = loadImage("bases/body_" + currentGender);
        }

        if (img != null) {
            layer.setImage(img);
            layer.setFitHeight(380); 
            layer.setPreserveRatio(true);
            layer.setSmooth(false); 
        }
    }

    private Image loadImage(String path) {
        String fullPath = "/assets/images/sprites/" + path + ".png";
        InputStream is = getClass().getResourceAsStream(fullPath);
        if (is == null) return null;
        return new Image(is);
    }

    // --- CONTROLES DE UI ---

    @FXML 
    private void setMale() { 
        currentGender = "male";
        skinIndex = 0; // Reset index al cambiar género para evitar errores de rango
        playSelectionSound();
        refreshAvatar(); 
    }
    
    @FXML 
    private void setFemale() { 
        currentGender = "female"; 
        skinIndex = 0; // Reset index al cambiar género
        playSelectionSound();
        refreshAvatar(); 
    }

    @FXML 
    private void nextSkin() { 
        List<String> activeList = getCurrentList();
        skinIndex = (skinIndex + 1) % activeList.size(); 
        playSelectionSound(); 
        refreshAvatar(); 
    }
    
    @FXML 
    private void prevSkin() { 
        List<String> activeList = getCurrentList();
        skinIndex = (skinIndex - 1 + activeList.size()) % activeList.size(); 
        playSelectionSound(); 
        refreshAvatar(); 
    }

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
        if (name.isEmpty() || name.length() < 3) {
            showAlert("Nombre inválido", "El nombre debe tener al menos 3 caracteres.");
            return;
        }

        Character newChar = new Character();
        newChar.setUserId(userId);
        newChar.setName(name);
        newChar.setSlotIndex(slotIndex);
        newChar.setLevel(1);
        newChar.setClassId(classSelector.getSelectionModel().getSelectedIndex() + 1);
        
        // --- GUARDADO DIRECTO ---
        // Obtenemos el nombre directamente de la lista activa. Cero lógica compleja.
        String skinToSave = getCurrentList().get(skinIndex);
        
        System.out.println("💾 Guardando personaje con skin: " + skinToSave);
        newChar.setSkin(skinToSave);

        if (CharacterDAO.saveCharacter(newChar)) {
            regresarEscena("/fxml/character_selection.fxml");
        } else {
            showAlert("Error Crítico", "No se pudo guardar en la base de datos.");
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
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Editor");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.initOwner(rootContainer.getScene().getWindow());
        alert.show();
    }
}