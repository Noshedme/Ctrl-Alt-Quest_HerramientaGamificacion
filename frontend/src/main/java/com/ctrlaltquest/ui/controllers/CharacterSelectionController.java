package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    @FXML private Button btnSettings;

    private MediaPlayer mediaPlayer;
    private int currentUserId;
    private String currentUsername;
    private Map<Integer, Character> personajes;

    @FXML
    public void initialize() {
        // Al igual que en el login, aplicamos desenfoque para que resalten las cartas
        if (bgMedia != null) {
            bgMedia.setEffect(new javafx.scene.effect.GaussianBlur(15));
            setupBackground();
        }
        setupHoverAnimations();
        SoundManager.getInstance().synchronizeMusic();
    }

    private void setupBackground() {
        URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4"); // Usamos el mismo video para coherencia
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                bgMedia.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                mediaPlayer.setRate(0.5); // Velocidad relajada

                // Respetar el estado global de pausa del video
                if (SettingsController.isVideoPaused) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.setOnReady(() -> mediaPlayer.play());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error video selección: " + e.getMessage());
            }
        }
    }

    // Método para que el SettingsController pueda pausar/reproducir este video
    public void setVideoPlaying(boolean play) {
        if (mediaPlayer != null) {
            if (play) mediaPlayer.play();
            else mediaPlayer.pause();
        }
    }

    private void setupHoverAnimations() {
        VBox[] slots = {slot1, slot2, slot3};
        for (VBox slot : slots) {
            slot.setOnMouseEntered(e -> {
                SoundManager.playKeyClick();
                applyScaleAnimation(slot, 1.03); // Escala sutil según el CSS
            });
            slot.setOnMouseExited(e -> applyScaleAnimation(slot, 1.0));
        }
    }

    private void applyScaleAnimation(VBox node, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(250), node);
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
        this.personajes = CharacterDAO.getCharactersByUser(currentUserId);
        
        actualizarSlot(1, personajes.get(1), name1, detail1, container1);
        actualizarSlot(2, personajes.get(2), name2, detail2, container2);
        actualizarSlot(3, personajes.get(3), name3, detail3, container3);
    }

    private void actualizarSlot(int index, Character c, Label lblName, Label lblDetail, StackPane container) {
        container.getChildren().clear();
        
        if (c != null) {
            lblName.setText(c.getName().toUpperCase());
            lblName.getStyleClass().add("card-category-runic");
            lblDetail.setText("NIVEL " + c.getLevel() + " - " + obtenerNombreClase(c.getClassId()));
            lblDetail.getStyleClass().add("card-text-pixel");
            
            renderizarPersonaje(container, c);
        } else {
            lblName.setText("VACÍO");
            lblDetail.setText("Slot disponible");
            
            Label addIcon = new Label("+");
            addIcon.getStyleClass().add("empty-slot-label");
            container.getChildren().add(addIcon);
        }
    }

    private void renderizarPersonaje(StackPane container, Character c) {
        try {
            String path = "/assets/images/sprites/base/class_" + c.getClassId() + ".png";
            URL imgUrl = getClass().getResource(path);
            
            if (imgUrl != null) {
                ImageView body = new ImageView(new Image(imgUrl.toExternalForm()));
                body.setFitHeight(180);
                body.setPreserveRatio(true);
                body.setSmooth(false); // Pixel art nítido
                container.getChildren().add(body);
            }
        } catch (Exception e) {
            System.err.println("❌ Error render sprite: " + e.getMessage());
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
        SoundManager.playKeyClick();
        Character seleccionado = personajes.get(slotIndex);
        if (seleccionado == null) {
            cambiarEscena("/fxml/character_editor.fxml", slotIndex);
        } else {
            cambiarEscena("/fxml/home.fxml", seleccionado);
        }
    }

    @FXML 
    public void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            
            SettingsController settingsCtrl = loader.getController();
            // Le pasamos este controlador para que pueda pausar el video de selección
            settingsCtrl.setSelectionController(this); 

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(slot1.getScene().getWindow());
            settingsStage.initStyle(StageStyle.TRANSPARENT); 
            
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT); 
            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (IOException e) {
            System.err.println("❌ Error ajustes: " + e.getMessage());
        }
    }

    private void cambiarEscena(String fxmlPath, Object data) {
        try {
            Stage stage = (Stage) slot1.getScene().getWindow();
            
            FadeTransition ft = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                try {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.dispose();
                    }

                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    Parent root = loader.load();

                    if (fxmlPath.contains("character_editor")) {
                        CharacterEditorController ctrl = loader.getController();
                        ctrl.setInitData(currentUserId, (Integer) data);
                    } else if (fxmlPath.contains("home")) {
                        // Aquí iría tu lógica para el Home
                    }

                    Scene scene = new Scene(root, 1280, 720);
                    stage.setScene(scene);
                    
                    root.setOpacity(0);
                    FadeTransition fi = new FadeTransition(Duration.millis(500), root);
                    fi.setToValue(1.0);
                    fi.play();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            ft.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        SoundManager.playKeyClick();
        try {
            if (mediaPlayer != null) { 
                mediaPlayer.stop(); 
                mediaPlayer.dispose(); 
            }
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) slot1.getScene().getWindow();
            
            FadeTransition ft = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                stage.getScene().setRoot(loginRoot);
                loginRoot.setOpacity(0);
                FadeTransition fi = new FadeTransition(Duration.millis(400), loginRoot);
                fi.setToValue(1.0);
                fi.play();
            });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}