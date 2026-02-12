package com.ctrlaltquest.ui.controllers.views;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.models.Item;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CharacterPanelController {

    // --- DATOS DEL PERSONAJE ---
    @FXML private Label lblClassName, lblCharName, lblLevel;
    
    // --- PESTAÑA STATS ---
    @FXML private Label lblStrVal, lblDexVal, lblIntVal;
    @FXML private ProgressBar barStr, barDex, barInt;
    
    // --- AVATAR & PAPER DOLL ---
    @FXML private ImageView imgAvatar;
    @FXML private StackPane slotHead, slotChest, slotWeapon, slotLegs;
    
    // --- PESTAÑA INVENTARIO ---
    @FXML private FlowPane inventoryGrid;
    @FXML private Label lblSelectedItemName, lblSelectedItemType;
    @FXML private Label lblSelectedItemDesc;
    @FXML private Button btnEquip, btnUse;
    @FXML private StackPane selectedItemPreview; 

    private Character characterData;
    private Item selectedItem;

    @FXML
    public void initialize() {
        // --- CORRECCIÓN DEL ERROR ---
        // Instalamos los Tooltips via código porque FXML no soporta <tooltip> dentro de contenedores Pane
        installTooltip(slotHead, "Cabeza");
        installTooltip(slotChest, "Torso");
        installTooltip(slotWeapon, "Mano Principal");
        installTooltip(slotLegs, "Piernas");

        resetDetalles();
        cargarInventarioSimulado();
    }
    
    private void installTooltip(StackPane node, String text) {
        if (node != null) Tooltip.install(node, new Tooltip(text));
    }

    public void setPlayerData(Character c) {
        this.characterData = c;
        if (c != null) {
            lblCharName.setText(c.getName().toUpperCase());
            lblClassName.setText(obtenerNombreClase(c.getClassId()));
            lblLevel.setText("NVL. " + c.getLevel());
            
            cargarAvatar(c.getClassId());
            actualizarStatsSimulados(c.getClassId());
        }
    }

    // --- LÓGICA VISUAL ---

    private void actualizarStatsSimulados(int classId) {
        double str = 0.2, dex = 0.2, intel = 0.2;
        switch (classId) {
            case 1: str=0.2; dex=0.4; intel=0.9; break; 
            case 2: str=0.3; dex=0.8; intel=0.5; break; 
            case 3: str=0.8; dex=0.3; intel=0.4; break; 
        }
        animarBarra(barStr, lblStrVal, str);
        animarBarra(barDex, lblDexVal, dex);
        animarBarra(barInt, lblIntVal, intel);
    }

    private void animarBarra(ProgressBar bar, Label lblVal, double targetProgress) {
        if(bar == null) return;
        bar.setProgress(0);
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e -> bar.setProgress(0)),
            new KeyFrame(Duration.millis(800), e -> {
                bar.setProgress(targetProgress);
                lblVal.setText(String.valueOf((int)(targetProgress * 100)));
            })
        );
        timeline.play();
    }

    private void cargarAvatar(int classId) {
        try {
            String path = "/assets/images/sprites/class_" + classId + "_idle.png";
            URL url = getClass().getResource(path);
            if (url != null) imgAvatar.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {
            System.err.println("No avatar found: " + e.getMessage());
        }
    }

    // --- INVENTARIO ---

    private void cargarInventarioSimulado() {
        if(inventoryGrid == null) return;
        inventoryGrid.getChildren().clear();
        
        List<Item> items = new ArrayList<>();
        items.add(new Item(1, "Gafas de Focus", "HELMET", "Aumenta la concentración visual.", "RARE", false));
        items.add(new Item(2, "Camiseta NullP", "CHEST", "Protección contra excepciones.", "COMMON", true)); 
        items.add(new Item(3, "Café Infinito", "CONSUMABLE", "Recupera energía al instante.", "EPIC", false));
        items.add(new Item(4, "Teclado RGB", "WEAPON", "Velocidad de escritura +20%.", "LEGENDARY", false));
        items.add(new Item(5, "Joggers Cómodos", "LEGS", "Agilidad en silla +5.", "COMMON", false));
        items.add(new Item(6, "Mouse Vertical", "WEAPON", "Ergonomía +10.", "RARE", false));

        int delay = 0;
        for (Item item : items) {
            StackPane slot = crearSlotInventario(item);
            inventoryGrid.getChildren().add(slot);
            
            if(item.isEquipped()) actualizarVisualEquipo(item);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(300), slot);
            st.setFromX(0); st.setFromY(0); st.setToX(1); st.setToY(1);
            st.setDelay(Duration.millis(delay));
            st.play();
            delay += 50;
        }
    }

    private StackPane crearSlotInventario(Item item) {
        StackPane slot = new StackPane();
        slot.setPrefSize(60, 60);
        
        String colorHex = obtenerColorRareza(item.getRarity());
        slot.setStyle("-fx-background-color: rgba(30, 30, 40, 0.8); -fx-border-color: " + colorHex + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label icon = new Label(item.getName().substring(0,1));
        icon.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        slot.getChildren().add(icon);

        if (item.isEquipped()) {
            Label eqTag = new Label("E");
            eqTag.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: #2d5a27; -fx-padding: 1 3; -fx-background-radius: 2;");
            StackPane.setAlignment(eqTag, Pos.TOP_RIGHT);
            slot.getChildren().add(eqTag);
            slot.setOpacity(0.6);
        }

        slot.setOnMouseEntered(e -> {
            slot.setEffect(new Glow(0.8));
            slot.setScaleX(1.1); slot.setScaleY(1.1);
        });
        slot.setOnMouseExited(e -> {
            slot.setEffect(null);
            slot.setScaleX(1.0); slot.setScaleY(1.0);
        });
        slot.setOnMouseClicked(e -> {
            SoundManager.playClickSound();
            seleccionarItem(item);
        });

        Tooltip.install(slot, new Tooltip(item.getName()));
        return slot;
    }

    private void seleccionarItem(Item item) {
        this.selectedItem = item;
        String color = obtenerColorRareza(item.getRarity());
        
        lblSelectedItemName.setText(item.getName());
        lblSelectedItemName.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        lblSelectedItemType.setText(item.getType() + " • " + item.getRarity());
        lblSelectedItemDesc.setText(item.getDescription());
        
        selectedItemPreview.getChildren().clear();
        Label bigIcon = new Label(item.getName().substring(0,1));
        bigIcon.setStyle("-fx-font-size: 40px; -fx-text-fill: " + color + ";");
        selectedItemPreview.getChildren().add(bigIcon);
        selectedItemPreview.setStyle("-fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: rgba(0,0,0,0.3);");

        boolean isConsumable = "CONSUMABLE".equals(item.getType());
        btnUse.setDisable(!isConsumable);
        
        if (item.isEquipped()) {
            btnEquip.setText("DESEQUIPAR");
            btnEquip.setDisable(false);
            btnEquip.setStyle("-fx-base: #ff6b6b;");
        } else if (!isConsumable) {
            btnEquip.setText("EQUIPAR");
            btnEquip.setDisable(false);
            btnEquip.setStyle("-fx-base: #4ade80;");
        } else {
            btnEquip.setText("EQUIPAR");
            btnEquip.setDisable(true);
        }
        
        btnEquip.setOnAction(e -> {
            SoundManager.playClickSound();
            item.setEquipped(!item.isEquipped());
            actualizarVisualEquipo(item);
            cargarInventarioSimulado();
            seleccionarItem(item);
        });
    }

    private void actualizarVisualEquipo(Item item) {
        StackPane targetSlot = switch(item.getType()) {
            case "HELMET" -> slotHead;
            case "CHEST" -> slotChest;
            case "WEAPON" -> slotWeapon;
            case "LEGS" -> slotLegs;
            default -> null;
        };

        if (targetSlot != null) {
            targetSlot.getChildren().clear();
            if (item.isEquipped()) {
                Label icon = new Label(item.getName().substring(0,1));
                icon.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");
                targetSlot.getChildren().add(icon);
                
                String color = obtenerColorRareza(item.getRarity());
                targetSlot.setStyle("-fx-background-color: rgba(20,20,30,0.8); -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                targetSlot.setEffect(new DropShadow(10, Color.web(color)));
            } else {
                Label placeholder = new Label(item.getType().substring(0,1));
                placeholder.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");
                targetSlot.getChildren().add(placeholder);
                targetSlot.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
                targetSlot.setEffect(null);
            }
        }
    }

    private void resetDetalles() {
        if(lblSelectedItemName != null) {
            lblSelectedItemName.setText("SELECCIONA UN OBJETO");
            lblSelectedItemType.setText("---");
            lblSelectedItemDesc.setText("Haz clic en tu inventario para ver detalles.");
            btnEquip.setDisable(true);
            btnUse.setDisable(true);
            selectedItemPreview.getChildren().clear();
            selectedItemPreview.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        }
    }

    private String obtenerNombreClase(int id) {
        return switch(id) { case 1->"FULL STACK"; case 2->"BACKEND"; default->"NOOB"; };
    }
    
    private String obtenerColorRareza(String rarity) {
        return switch (rarity) {
            case "LEGENDARY" -> "#ff8000"; 
            case "EPIC" -> "#a335ee";      
            case "RARE" -> "#0070dd";      
            default -> "#aaaaaa";          
        };
    }
}