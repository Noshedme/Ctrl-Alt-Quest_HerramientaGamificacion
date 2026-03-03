package com.ctrlaltquest.ui.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

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
            bgMedia.setEffect(new GaussianBlur(15));
            setupBackground();
        }
        
        if (classSelector != null) {
            classSelector.getItems().clear();
            classSelector.getItems().addAll("Programador", "Lector", "Escritor");
            classSelector.setValue("Programador");
            classSelector.setOnAction(e -> playSelectionSound());
            // Mejorar estilo del ChoiceBox
            classSelector.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-border-color: #3498db; -fx-border-radius: 5; -fx-background-radius: 5;");
        }

        ToggleGroup genderGroup = new ToggleGroup();
        if (toggleMale != null && toggleFemale != null) {
            toggleMale.setToggleGroup(genderGroup);
            toggleFemale.setToggleGroup(genderGroup);
            toggleFemale.setSelected(true); // Default Female
            // Mejorar botones con iconos y estilos
            toggleMale.setText("♂");
            toggleMale.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 10; -fx-border-radius: 5; -fx-background-radius: 5;");
            toggleFemale.setText("♀");
            toggleFemale.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 10; -fx-border-radius: 5; -fx-background-radius: 5;");
        }

        SoundManager.getInstance().synchronizeMusic();
        setupTypingSounds();
        setupHoverEffects();
        refreshAvatar();
        
        // Inicializar Toast cuando la escena esté lista
        rootContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                initializeToast();
            }
        });
    }
    
    private void initializeToast() {
        try {
            StackPane root = rootContainer;
            
            // Crear contenedor de Toast
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            
            // Inicializar el sistema de Toast
            Toast.initialize(toastContainer);
            
            // Añadir al root
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
            
            // Mostrar mensaje de bienvenida después de inicializar
            Toast.info("Bienvenido", "Estás creando tu primer personaje");
        } catch (Exception e) {
            System.err.println("Error al inicializar Toast: " + e.getMessage());
        }
    }

    private void setupTypingSounds() {
        if (nameField != null) {
            nameField.setOnKeyTyped(e -> SoundManager.playKeyClick());
            // Mejorar estilo del TextField
            nameField.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-border-color: #2980b9; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10;");
        }
    }

    private void setupHoverEffects() {
        // Efectos de hover para botones de género
        if (toggleMale != null) {
            toggleMale.setOnMouseEntered(e -> applyScaleAndGlow(toggleMale, 1.1, 0.5));
            toggleMale.setOnMouseExited(e -> applyScaleAndGlow(toggleMale, 1.0, 0.0));
        }
        if (toggleFemale != null) {
            toggleFemale.setOnMouseEntered(e -> applyScaleAndGlow(toggleFemale, 1.1, 0.5));
            toggleFemale.setOnMouseExited(e -> applyScaleAndGlow(toggleFemale, 1.0, 0.0));
        }
        // Asumiendo que hay botones para next/prev skin, pero como son métodos, agregar si hay @FXML Button nextBtn, prevBtn;
        // Por ahora, asumir que se llaman desde FXML
    }

    private void applyScaleAndGlow(ToggleButton button, double scale, double glowLevel) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
        st.setToX(scale);
        st.setToY(scale);
        st.play();

        Glow glow = new Glow(glowLevel);
        button.setEffect(glow);
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
                lblSkinName.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 16px; -fx-font-weight: bold;");
            }

            // Cargar imagen directa
            updateImage(fullCharacterView, fileName);

            if (spritePreview != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), spritePreview);
                ft.setFromValue(0.85); ft.setToValue(1.0); ft.play();
            }

            // Agregar efecto de sombra y glow al preview del personaje
            DropShadow shadow = new DropShadow(20, Color.GOLD);
            shadow.setInput(new Glow(0.3));
            fullCharacterView.setEffect(shadow);

        } catch (Exception e) {
            System.err.println("⚠️ Error al actualizar avatar: " + e.getMessage());
            Toast.error("Error de Avatar", "No se pudo actualizar la vista previa.");
        }
    }

    private void updateImage(ImageView layer, String fileName) {
        if (layer == null) return;
        
        Image img = loadImage("bases/" + fileName);
        
        // Fallback simple: Si falla, intenta cargar el body del género actual
        if (img == null) {
            System.err.println("❌ No se encontró la imagen: " + fileName);
            Toast.warning("Imagen no Encontrada", "Usando imagen por defecto.");
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
        skinIndex = 0;
        Toast.info("Género", "Cambiado a Masculino");
        playSelectionSound();
        refreshAvatar(); 
    }
    
    @FXML 
    private void setFemale() { 
        currentGender = "female"; 
        skinIndex = 0;
        Toast.info("Género", "Cambiado a Femenino");
        playSelectionSound();
        refreshAvatar(); 
    }

    @FXML 
    private void nextSkin() { 
        List<String> activeList = getCurrentList();
        skinIndex = (skinIndex + 1) % activeList.size(); 
        playSelectionSound(); 
        Toast.info("Apariencia", "Siguiente estilo seleccionado");
        refreshAvatar(); 
    }
    
    @FXML 
    private void prevSkin() { 
        List<String> activeList = getCurrentList();
        skinIndex = (skinIndex - 1 + activeList.size()) % activeList.size(); 
        playSelectionSound(); 
        Toast.info("Apariencia", "Estilo anterior seleccionado");
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
        } catch (IOException e) { 
            e.printStackTrace(); 
            Toast.error("Error de Ajustes", "No se pudo abrir la ventana de ajustes.");
        }
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
        } else {
            Toast.warning("Fondo no Cargado", "No se pudo cargar el video de fondo.");
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
            Toast.error("Nombre inválido", "El nombre debe tener al menos 3 caracteres.");
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
            Toast.success("Personaje Guardado", "¡Tu avatar está listo para la aventura!");
            regresarEscena("/fxml/character_selection.fxml");
        } else {
            Toast.error("Error Crítico", "No se pudo guardar en la base de datos.");
        }
    }

    @FXML private void handleCancel() { Toast.info("Cancelado", "Regresando al inicio..."); regresarEscena("/fxml/login.fxml"); }

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
            
            // Agregar fade transition para mejorar la transición
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                Scene nextScene = new Scene(root, 1280, 720);
                stage.setScene(nextScene);
                root.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (IOException e) { 
            e.printStackTrace(); 
            Toast.error("Error de Navegación", "No se pudo regresar a la escena anterior.");
        }
    }

    private void playSelectionSound() { SoundManager.playKeyClick(); }
    
    private void showAlert(String title, String content) {
        Toast.warning(title, content);
    }
}