package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class CharacterSelectionController {

    @FXML private VBox slot1, slot2, slot3;
    @FXML private Label name1, name2, name3;
    @FXML private Label detail1, detail2, detail3;
    @FXML private StackPane container1, container2, container3;
    @FXML private MediaView bgMedia;

    private MediaPlayer mediaPlayer;
    private int currentUserId;
    private String currentUsername;
    private Map<Integer, Character> personajes;

    @FXML
    public void initialize() {
        setupBackground();
        setupHoverAnimations();
    }

    private void setupBackground() {
        URL videoUrl = getClass().getResource("/assets/videos/selection_bg.mp4");
        if (videoUrl != null) {
            try {
                mediaPlayer = new MediaPlayer(new Media(videoUrl.toExternalForm()));
                bgMedia.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                mediaPlayer.play();
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo reproducir el video de fondo: " + e.getMessage());
            }
        }
    }

    /**
     * Añade efectos visuales y de sonido cuando el mouse entra en una tarjeta.
     */
    private void setupHoverAnimations() {
        VBox[] slots = {slot1, slot2, slot3};
        for (VBox slot : slots) {
            slot.setOnMouseEntered(e -> {
                try { SoundManager.playKeyClick(); } catch (Exception ignored) {}
                applyScaleAnimation(slot, 1.05);
            });
            slot.setOnMouseExited(e -> applyScaleAnimation(slot, 1.0));
        }
    }

    private void applyScaleAnimation(VBox node, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setToX(scale);
        st.setToY(scale);
        st.play();
    }

    public void initData(int userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
        cargarPersonajes();
    }

    private void cargarPersonajes() {
        // Obtenemos los personajes mapeados por slot_index
        this.personajes = CharacterDAO.getCharactersByUser(currentUserId);
        
        actualizarSlot(1, personajes.get(1), name1, detail1, container1);
        actualizarSlot(2, personajes.get(2), name2, detail2, container2);
        actualizarSlot(3, personajes.get(3), name3, detail3, container3);
    }

    private void actualizarSlot(int index, Character c, Label lblName, Label lblDetail, StackPane container) {
        container.getChildren().clear();
        
        if (c != null) {
            lblName.setText(c.getName().toUpperCase());
            // Aplicamos estilo dorado rúnico al nombre si existe el PJ
            lblName.getStyleClass().add("char-name");
            lblDetail.setText("NIVEL " + c.getLevel() + " - " + obtenerNombreClase(c.getClassId()));
            lblDetail.getStyleClass().add("char-detail");
            
            renderizarPersonaje(container, c);
        } else {
            lblName.setText("VACÍO");
            lblDetail.setText("Slot disponible");
            
            Label addIcon = new Label("+");
            addIcon.getStyleClass().add("add-icon");
            container.getChildren().add(addIcon);
        }
    }

    private void renderizarPersonaje(StackPane container, Character c) {
        try {
            // Buscamos el sprite base según la clase (guerrero, mago, pícaro)
            String path = "/assets/images/sprites/base/class_" + c.getClassId() + ".png";
            URL imgUrl = getClass().getResource(path);
            
            if (imgUrl != null) {
                ImageView body = new ImageView(new Image(imgUrl.toExternalForm()));
                
                // Configuración Pixel-Perfect
                body.setFitHeight(180);
                body.setPreserveRatio(true);
                body.setSmooth(false); // Crucial para que no se vea borroso el pixel art
                
                container.getChildren().add(body);
            } else {
                Label placeholder = new Label("👤");
                placeholder.setStyle("-fx-font-size: 80px; -fx-text-fill: rgba(247, 210, 122, 0.1);");
                container.getChildren().add(placeholder);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al renderizar sprite: " + e.getMessage());
        }
    }

    private String obtenerNombreClase(int classId) {
        return switch (classId) {
            case 1 -> "GUERRERO";
            case 2 -> "MAGO";
            case 3 -> "PÍCARO";
            default -> "AVENTURERO";
        };
    }

    @FXML private void handleSlot1() { irACreacionOSala(1); }
    @FXML private void handleSlot2() { irACreacionOSala(2); }
    @FXML private void handleSlot3() { irACreacionOSala(3); }

    private void irACreacionOSala(int slotIndex) {
        try { SoundManager.playKeyClick(); } catch (Exception ignored) {}

        Character seleccionado = personajes.get(slotIndex);
        if (seleccionado == null) {
            cambiarEscena("/fxml/character_editor.fxml", slotIndex);
        } else {
            cambiarEscena("/fxml/home.fxml", seleccionado);
        }
    }

    private void cambiarEscena(String fxmlPath, Object data) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Lógica de inyección de datos
            if (fxmlPath.contains("character_editor")) {
                CharacterEditorController ctrl = loader.getController();
                ctrl.setInitData(currentUserId, (Integer) data);
            } else if (fxmlPath.contains("home")) {
                HomeController ctrl = loader.getController();
                ctrl.setPlayerCharacter((Character) data);
            }

            Stage stage = (Stage) slot1.getScene().getWindow();
            
            // Transición cinematográfica de salida (Fade Out)
            FadeTransition ft = new FadeTransition(Duration.millis(400), slot1.getScene().getRoot());
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                Scene scene = new Scene(root, 1280, 720);
                stage.setScene(scene);
                
                // Fade in de la nueva pantalla
                root.setOpacity(0);
                FadeTransition fi = new FadeTransition(Duration.millis(500), root);
                fi.setToValue(1.0);
                fi.play();
            });
            ft.play();

        } catch (IOException e) {
            System.err.println("❌ Error al cambiar de escena: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            if (mediaPlayer != null) { 
                mediaPlayer.stop(); 
                mediaPlayer.dispose(); 
            }
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) slot1.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}