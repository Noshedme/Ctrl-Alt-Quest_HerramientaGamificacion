package com.ctrlaltquest.ui.controllers.views;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.models.Item;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class CharacterPanelController {

    // Stats
    @FXML private Label lblClassName, lblCharName;
    @FXML private Label lblStrVal, lblDexVal, lblIntVal;
    @FXML private ProgressBar barStr, barDex, barInt;
    
    // Avatar & Paper Doll
    @FXML private ImageView imgAvatar;
    @FXML private StackPane slotHead, slotChest, slotWeapon, slotLegs;
    
    // Inventario
    @FXML private FlowPane inventoryGrid;
    @FXML private Label lblSelectedItemName, lblSelectedItemDesc;
    @FXML private Button btnEquip, btnUse;

    private Character characterData;
    private Item selectedItem;

    @FXML
    public void initialize() {
        cargarInventarioSimulado();
        resetDetalles();
    }

    public void setPlayerData(Character c) {
        this.characterData = c;
        if (c != null) {
            lblCharName.setText(c.getName().toUpperCase());
            lblClassName.setText(obtenerNombreClase(c.getClassId()));
            cargarAvatar(c.getClassId());
            
            // Simular Stats basados en clase (esto vendría de BD en real)
            actualizarStatsSimulados(c.getClassId());
        }
    }

    private void actualizarStatsSimulados(int classId) {
        // Lógica dummy para demos visuales
        double str = 0.3, dex = 0.3, intel = 0.3;
        
        switch (classId) {
            case 1: str=0.2; dex=0.4; intel=0.9; break; // Programador (Alta INT)
            case 2: str=0.3; dex=0.8; intel=0.5; break; // Lector (Alta DEX/Velocidad)
            case 3: str=0.7; dex=0.3; intel=0.6; break; // Escritor (Alta STR/Resistencia)
        }
        
        barStr.setProgress(str); lblStrVal.setText((int)(str*20) + "");
        barDex.setProgress(dex); lblDexVal.setText((int)(dex*20) + "");
        barInt.setProgress(intel); lblIntVal.setText((int)(intel*20) + "");
    }

    private void cargarAvatar(int classId) {
        try {
            String path = "/assets/images/sprites/base/class_" + classId + ".png";
            URL url = getClass().getResource(path);
            if (url != null) imgAvatar.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {}
    }

    private void cargarInventarioSimulado() {
        List<Item> items = new ArrayList<>();
        items.add(new Item(1, "Gafas de Focus", "HELMET", "Aumenta XP visual +10%. Esencial para sesiones largas.", "RARE", false));
        items.add(new Item(2, "Camiseta Java", "CHEST", "Resistencia a bugs +5. Cómoda y transpirable.", "COMMON", true));
        items.add(new Item(3, "Monster Energy", "CONSUMABLE", "Recupera racha perdida. Sabor digital.", "COMMON", false));
        items.add(new Item(4, "Teclado RGB", "WEAPON", "Velocidad de tipeo +20%. Clicky sound effect.", "EPIC", false));
        items.add(new Item(5, "Pantalones de Chandal", "LEGS", "Agilidad +2. Perfectos para home office.", "COMMON", false));

        inventoryGrid.getChildren().clear();
        
        int delay = 0;
        for (Item item : items) {
            StackPane slot = crearSlotInventario(item);
            slot.setOpacity(0); // Para animar
            inventoryGrid.getChildren().add(slot);
            
            // Si está equipado, actualizar visualmente el Paper Doll
            if (item.isEquipped()) actualizarSlotEquipado(item);
            
            animarEntrada(slot, delay);
            delay += 50; // 50ms cascada
        }
    }

    private StackPane crearSlotInventario(Item item) {
        StackPane slot = new StackPane();
        slot.setPrefSize(60, 60);
        
        // Color por Rareza
        String colorHex = switch (item.getRarity()) {
            case "EPIC" -> "#a335ee"; // Púrpura
            case "RARE" -> "#0070dd"; // Azul
            case "LEGENDARY" -> "#ff8000"; // Naranja
            default -> "#888888"; // Gris Común
        };
        
        // Estilo Base
        slot.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-border-color: " + colorHex + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Icono (Placeholder: Primera Letra)
        Label icon = new Label(item.getName().substring(0, 1)); 
        icon.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold; -fx-font-size: 20px;");
        slot.getChildren().add(icon);

        // Indicador de Equipado
        if (item.isEquipped()) {
            Rectangle equippedMark = new Rectangle(60, 60);
            equippedMark.setFill(Color.TRANSPARENT);
            equippedMark.setStroke(Color.LIMEGREEN);
            equippedMark.setStrokeWidth(2);
            equippedMark.setArcWidth(6); equippedMark.setArcHeight(6);
            slot.getChildren().add(equippedMark);
        }

        // Eventos
        configurarInteraccion(slot, item, colorHex);

        return slot;
    }

    private void configurarInteraccion(StackPane slot, Item item, String glowColor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), slot);
        
        slot.setOnMouseEntered(e -> {
            st.setToX(1.1); st.setToY(1.1); st.playFromStart();
            slot.setEffect(new DropShadow(10, Color.web(glowColor)));
            slot.setStyle(slot.getStyle().replace("-fx-background-color: rgba(0,0,0,0.6);", "-fx-background-color: rgba(255,255,255,0.1);"));
        });

        slot.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            slot.setEffect(null);
            slot.setStyle(slot.getStyle().replace("-fx-background-color: rgba(255,255,255,0.1);", "-fx-background-color: rgba(0,0,0,0.6);"));
        });

        slot.setOnMouseClicked(e -> {
            SoundManager.playClickSound();
            seleccionarItem(item);
        });
    }

    private void seleccionarItem(Item item) {
        this.selectedItem = item;
        lblSelectedItemName.setText(item.getName());
        lblSelectedItemName.setStyle("-fx-text-fill: " + obtenerColorRareza(item.getRarity()));
        lblSelectedItemDesc.setText(item.getDescription() + "\n\nRareza: " + item.getRarity());
        
        boolean isConsumable = "CONSUMABLE".equals(item.getType());
        btnEquip.setDisable(isConsumable || item.isEquipped());
        btnUse.setDisable(!isConsumable);
        
        if (item.isEquipped()) btnEquip.setText("EQUIPADO");
        else btnEquip.setText("EQUIPAR");
    }

    private void actualizarSlotEquipado(Item item) {
        StackPane targetSlot = switch (item.getType()) {
            case "HELMET" -> slotHead;
            case "CHEST" -> slotChest;
            case "WEAPON" -> slotWeapon;
            case "LEGS" -> slotLegs;
            default -> null;
        };

        if (targetSlot != null) {
            targetSlot.getChildren().clear();
            
            // Icono en el Paper Doll
            Label icon = new Label(item.getName().substring(0, 1));
            icon.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            // Borde coloreado en el slot del cuerpo
            String color = obtenerColorRareza(item.getRarity());
            targetSlot.setStyle("-fx-border-color: " + color + "; -fx-border-width: 2; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 8; -fx-border-radius: 8;");
            
            targetSlot.getChildren().add(icon);
        }
    }

    private void animarEntrada(Node node, int delay) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setFromY(20); tt.setToY(0);
        tt.setDelay(Duration.millis(delay));
        
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delay));
        
        tt.play(); ft.play();
    }

    private String obtenerNombreClase(int id) {
        return switch(id) { case 1->"PROGRAMADOR"; case 2->"LECTOR"; default->"AVENTURERO"; };
    }
    
    private String obtenerColorRareza(String rarity) {
        return switch (rarity) {
            case "EPIC" -> "#a335ee";
            case "RARE" -> "#0070dd";
            default -> "#aaaaaa";
        };
    }
    
    private void resetDetalles() {
        lblSelectedItemName.setText("Selecciona un objeto");
        lblSelectedItemDesc.setText("Haz clic en un ítem de tu mochila para ver sus detalles.");
        btnEquip.setDisable(true);
        btnUse.setDisable(true);
    }
}