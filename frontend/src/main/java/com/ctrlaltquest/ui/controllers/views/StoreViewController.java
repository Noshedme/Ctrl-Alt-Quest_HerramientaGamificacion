package com.ctrlaltquest.ui.controllers.views;

import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.StoreOffer;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class StoreViewController {

    @FXML private Label lblUserBalance;
    @FXML private VBox goldStoreContainer;

    private int currentUserId;

    @FXML
    public void initialize() {
        // Obtener usuario actual
        this.currentUserId = SessionManager.getInstance().getUserId();
        
        // Cargar saldo real desde la base de datos
        actualizarSaldoVisual();
        
        // Cargar items
        cargarOfertas();
    }

    private void actualizarSaldoVisual() {
        Task<Integer> balanceTask = new Task<>() {
            @Override
            protected Integer call() {
                return UserDAO.obtenerBalanceMonedas(currentUserId);
            }
        };

        balanceTask.setOnSucceeded(e -> {
            int balance = balanceTask.getValue();
            if (balance >= 0) {
                lblUserBalance.setText(balance + " 💰");
            } else {
                lblUserBalance.setText("--- 💰");
            }
        });

        new Thread(balanceTask).start();
    }

    private void cargarOfertas() {
        goldStoreContainer.getChildren().clear();

        // En un futuro, esto podría venir de una tabla 'store_items' en la BD
        List<StoreOffer> goldOffers = new ArrayList<>();
        goldOffers.add(new StoreOffer(1, "Poción de Café", "Aumenta la productividad x2 durante 1 hora. Esencial para entregas.", 150, false, "☕"));
        goldOffers.add(new StoreOffer(2, "Teclado Mecánico", "Cosmético: Añade efectos de sonido clicky al escribir código.", 500, false, "⌨"));
        goldOffers.add(new StoreOffer(3, "Licencia Lofi", "Desbloquea la estación de música de concentración en el dashboard.", 300, false, "🎵"));
        goldOffers.add(new StoreOffer(4, "Gafas Anti-Bug", "Resalta errores de sintaxis básicos con un brillo rojo.", 800, false, "👓"));
        goldOffers.add(new StoreOffer(5, "Skin: Modo Oscuro", "Personalización visual completa para tu avatar.", 1200, false, "🌑"));

        // Renderizado
        int delay = 0;
        for (StoreOffer offer : goldOffers) {
            HBox row = crearFilaOferta(offer);
            goldStoreContainer.getChildren().add(row);
            animarEntrada(row, delay);
            delay += 80;
        }
    }

    /**
     * FABRICA DE FILAS
     */
    private HBox crearFilaOferta(StoreOffer offer) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        
        String accentColor = "#d4af37";
        
        row.setStyle(
            "-fx-background-color: rgba(30, 20, 40, 0.6);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: transparent transparent " + accentColor + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );
        row.setOpacity(0); 

        // 1. Icono
        StackPane iconPane = new StackPane();
        Circle bgCircle = new Circle(25, Color.web("rgba(0,0,0,0.3)"));
        bgCircle.setStroke(Color.web(accentColor));
        bgCircle.setStrokeWidth(1.5);
        
        Label iconLabel = new Label(offer.getImagePath().length() < 5 ? offer.getImagePath() : offer.getTitle().substring(0,1));
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        
        iconPane.getChildren().addAll(bgCircle, iconLabel);

        // 2. Info
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Georgia';");
        
        Label desc = new Label(offer.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        
        infoBox.getChildren().addAll(title, desc);

        // 3. Botón
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnBuy = new Button();
        btnBuy.setPrefWidth(140);
        btnBuy.getStyleClass().add("btn-nav");
        
        btnBuy.setText((int)offer.getPrice() + " 💰");
        btnBuy.setStyle("-fx-border-color: " + accentColor + "; -fx-text-fill: " + accentColor + "; -fx-background-color: rgba(212, 175, 55, 0.1);");
        
        // Acción de compra real
        btnBuy.setOnAction(e -> procesarCompra(offer));
        
        Tooltip.install(row, new Tooltip(offer.getDescription()));

        row.getChildren().addAll(iconPane, infoBox, spacer, btnBuy);
        configurarHover(row, accentColor);

        return row;
    }

    private void configurarHover(HBox row, String accentColor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), row);
        row.setOnMouseEntered(e -> {
            st.setToX(1.01); st.setToY(1.01); st.playFromStart();
            row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 8; -fx-border-color: " + accentColor + "; -fx-border-width: 1;");
            row.setEffect(new DropShadow(10, Color.web(accentColor)));
        });
        row.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            row.setStyle("-fx-background-color: rgba(30, 20, 40, 0.6); -fx-background-radius: 8; -fx-border-color: transparent transparent " + accentColor + " transparent; -fx-border-width: 0 0 1 0;");
            row.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setFromX(-50); tt.setToX(0); tt.setDelay(Duration.millis(delayMillis));
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMillis));
        tt.play(); ft.play();
    }

    // --- LÓGICA DE COMPRA TRANSACCIONAL ---
    private void procesarCompra(StoreOffer offer) {
        SoundManager.playClickSound();
        int price = (int) offer.getPrice();

        Task<Boolean> purchaseTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // 1. Obtener saldo actualizado (para evitar condiciones de carrera)
                int currentBalance = UserDAO.obtenerBalanceMonedas(currentUserId);
                
                if (currentBalance >= price) {
                    int newBalance = currentBalance - price;
                    
                    // 2. Actualizar saldo en BD
                    if (UserDAO.actualizarMonedas(currentUserId, newBalance)) {
                        // 3. Registrar transacción
                        UserDAO.registrarTransaccionCoins(currentUserId, -price, "Compra: " + offer.getTitle(), "store", offer.getId());
                        
                        // 4. (Opcional) Aquí deberías agregar el item al inventario del usuario
                        // InventoryDAO.addItem(currentUserId, offer.getItemId());
                        
                        return true;
                    }
                }
                return false;
            }
        };

        purchaseTask.setOnSucceeded(e -> {
            if (purchaseTask.getValue()) {
                SoundManager.playSuccessSound();
                actualizarSaldoVisual(); // Refrescar UI
                mostrarAlerta("¡Compra Exitosa!", "Has adquirido: " + offer.getTitle());
            } else {
                SoundManager.playErrorSound();
                mostrarAlerta("Saldo Insuficiente", "No tienes suficiente oro para este artículo.");
            }
        });

        purchaseTask.setOnFailed(e -> {
            SoundManager.playErrorSound();
            mostrarAlerta("Error", "No se pudo procesar la transacción.");
            e.getSource().getException().printStackTrace();
        });

        new Thread(purchaseTask).start();
    }

    private void mostrarAlerta(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mercado Negro");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.show();
    }
}