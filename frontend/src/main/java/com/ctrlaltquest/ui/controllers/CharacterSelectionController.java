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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.DialogPane;
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
import java.util.Optional;

public class CharacterSelectionController {

    @FXML private VBox slot1, slot2, slot3;
    @FXML private Label name1, name2, name3;
    @FXML private Label detail1, detail2, detail3;
    @FXML private StackPane container1, container2, container3;
    @FXML private Button btnDelete1, btnDelete2, btnDelete3;
    @FXML private MediaView bgMedia;
    @FXML private Button btnSettings;

    private MediaPlayer mediaPlayer;
    private int currentUserId;
    private String currentUsername;
    private Map<Integer, Character> personajes;

    @FXML
    public void initialize() {
        if (bgMedia != null) {
            bgMedia.setEffect(new javafx.scene.effect.GaussianBlur(15));
            setupBackground();
        }
        setupHoverAnimations();
        SoundManager.getInstance().synchronizeMusic();
    }

    private void setupBackground() {
        URL videoUrl = getClass().getResource("/assets/videos/login_bg.mp4");
        if (videoUrl != null) {
            try {
                Media media = new Media(videoUrl.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                bgMedia.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                mediaPlayer.setRate(0.5);

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
                applyScaleAnimation(slot, 1.03);
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
        actualizarSlot(1, personajes.get(1), name1, detail1, container1, btnDelete1);
        actualizarSlot(2, personajes.get(2), name2, detail2, container2, btnDelete2);
        actualizarSlot(3, personajes.get(3), name3, detail3, container3, btnDelete3);
    }

    private void actualizarSlot(int index, Character c, Label lblName, Label lblDetail, StackPane container, Button btnDelete) {
        container.getChildren().clear();
        if (c != null) {
            lblName.setText(c.getName().toUpperCase());
            lblName.getStyleClass().add("card-category-runic");
            lblDetail.setText("NIVEL " + c.getLevel() + " - " + obtenerNombreClase(c.getClassId()));
            lblDetail.getStyleClass().add("card-text-pixel");
            btnDelete.setVisible(true);
            renderizarPersonaje(container, c);
        } else {
            lblName.setText("VACÍO");
            lblDetail.setText("Slot disponible");
            btnDelete.setVisible(false);
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
                body.setSmooth(false);
                container.getChildren().add(body);
            }
        } catch (Exception e) {
            System.err.println("❌ Error render sprite: " + e.getMessage());
        }
    }

    private String obtenerNombreClase(int classId) {
        return switch (classId) {
            case 1 -> "PROGRAMADOR";
            case 2 -> "LECTOR";
            case 3 -> "ANALISTA";
            default -> "PROFESIONAL";
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

    @FXML private void handleDelete1() { confirmarEliminacion(1); }
    @FXML private void handleDelete2() { confirmarEliminacion(2); }
    @FXML private void handleDelete3() { confirmarEliminacion(3); }

    private void confirmarEliminacion(int slotIndex) {
        SoundManager.playKeyClick();
        Character c = personajes.get(slotIndex);
        if (c == null) return;

        showConfirmAlert("Eliminar Perfil", "¿Estás seguro?", () -> {
            if (CharacterDAO.deleteCharacter(c.getId())) {
                cargarPersonajes();
            }
        });
    }

    private void showConfirmAlert(String title, String content, Runnable onConfirm) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ctrl + Alt + Quest");
            alert.setHeaderText(title);
            alert.setContentText(content);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                onConfirm.run();
            }
        });
    }

    @FXML 
    public void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController settingsCtrl = loader.getController();
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
            
            // 1. CARGAR EL LOADER PRIMERO (Si falla aquí, saltará al catch y no dejará la pantalla blanca)
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // 2. INYECTAR DATOS
            if (fxmlPath.contains("character_editor")) {
                CharacterEditorController ctrl = loader.getController();
                ctrl.setInitData(currentUserId, (Integer) data);
            } else if (fxmlPath.contains("home")) {
                HomeController ctrl = loader.getController();
                ctrl.initPlayerData((Character) data);
            }

            // 3. ANIMACIÓN DE SALIDA
            FadeTransition ft = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }
                
                Scene scene = new Scene(root, 1280, 720);
                stage.setScene(scene);
                
                root.setOpacity(0);
                FadeTransition fi = new FadeTransition(Duration.millis(500), root);
                fi.setToValue(1.0);
                fi.play();
            });
            ft.play();

        } catch (Exception e) {
            System.err.println("❌ Error crítico al cambiar escena: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleBack() {
        SoundManager.playKeyClick();
        cambiarEscena("/fxml/login.fxml", null);
    }
}