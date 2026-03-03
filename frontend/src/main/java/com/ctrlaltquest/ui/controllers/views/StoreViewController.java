package com.ctrlaltquest.ui.controllers.views;

import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.StoreOffer;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class StoreViewController {

    @FXML private Label lblUserBalance;
    @FXML private GridPane itemsGrid; // Cambiado de VBox a GridPane para layout de tarjetas
    @FXML private ScrollPane scrollContainer;

    private int currentUserId;

    @FXML
    public void initialize() {
        this.currentUserId = SessionManager.getInstance().getUserId();
        actualizarSaldoVisual();
        cargarOfertas();
        
        // inicializar Toast container
        try {
            StackPane root = (StackPane) lblUserBalance.getScene().getRoot();
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            Toast.initialize(toastContainer);
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception ignored) {}
        
        // Animación suave del scroll al cargar
        if(scrollContainer != null) {
            scrollContainer.setVvalue(0);
            FadeTransition ft = new FadeTransition(Duration.millis(800), scrollContainer);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();
        }
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
            lblUserBalance.setText((balance >= 0 ? balance : "---") + " 💰");
            
            // Efecto de brillo en el saldo
            ScaleTransition st = new ScaleTransition(Duration.millis(200), lblUserBalance);
            st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.1); st.setToY(1.1);
            st.setAutoReverse(true); st.setCycleCount(2);
            st.play();
        });

        new Thread(balanceTask).start();
    }

    private void cargarOfertas() {
        itemsGrid.getChildren().clear();

        List<StoreOffer> goldOffers = new ArrayList<>();
        // Items simulados (idealmente vendrían de BD)
        goldOffers.add(new StoreOffer(1, "Poción de Café", "Aumenta la productividad x2 (1h)", 150, false, "☕"));
        goldOffers.add(new StoreOffer(2, "Teclado Mecánico", "Efectos de sonido clicky", 500, false, "⌨"));
        goldOffers.add(new StoreOffer(3, "Licencia Lofi", "Música de concentración", 300, false, "🎵"));
        goldOffers.add(new StoreOffer(4, "Gafas Anti-Bug", "Resalta errores de sintaxis", 800, false, "👓"));
        goldOffers.add(new StoreOffer(5, "Skin: Modo Oscuro", "Personalización visual avatar", 1200, false, "🌑"));
        goldOffers.add(new StoreOffer(6, "Mouse Gamer", "Precisión +10%", 450, false, "🖱"));

        int col = 0;
        int row = 0;
        int delay = 0;

        for (StoreOffer offer : goldOffers) {
            VBox card = crearTarjetaItem(offer);
            itemsGrid.add(card, col, row);
            animarEntrada(card, delay);
            
            delay += 100;
            col++;
            if (col > 2) { // 3 columnas
                col = 0;
                row++;
            }
        }
    }

    private VBox crearTarjetaItem(StoreOffer offer) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(220);
        card.setPrefHeight(280);
        
        // Estilo de tarjeta RPG
        String rarityColor = (offer.getPrice() > 1000) ? "#a335ee" : (offer.getPrice() > 400 ? "#4ade80" : "#f7d27a"); // Morado, Verde, Dorado
        
        card.setStyle(
            "-fx-background-color: rgba(20, 20, 30, 0.7);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.1);" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);"
        );

        // 1. Imagen / Icono
        StackPane imageContainer = new StackPane();
        Rectangle bg = new Rectangle(80, 80, Color.web("rgba(255,255,255,0.05)"));
        bg.setArcWidth(20); bg.setArcHeight(20);
        
        Label icon = new Label(offer.getImagePath());
        icon.setStyle("-fx-font-size: 40px;");
        
        imageContainer.getChildren().addAll(bg, icon);

        // 2. Título y Precio
        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-wrap-text: true; -fx-text-alignment: CENTER;");
        
        Label price = new Label(String.valueOf((int)offer.getPrice()));
        price.setGraphic(new Label("💰")); // Icono moneda
        price.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold; -fx-font-size: 16px;");

        // 3. Descripción (Oculta parcialmente o tooltip)
        Label desc = new Label(offer.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-text-alignment: CENTER;");
        desc.setPrefHeight(40); // Limitar altura

        // 4. Botón Comprar
        Button btnBuy = new Button("ADQUIRIR");
        btnBuy.getStyleClass().add("btn-nav");
        btnBuy.setPrefWidth(120);
        btnBuy.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: " + rarityColor + "; -fx-border-color: " + rarityColor + "; -fx-cursor: hand;");
        
        btnBuy.setOnAction(e -> procesarCompra(offer, card, btnBuy));

        // Separador
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imageContainer, title, desc, spacer, price, btnBuy);
        
        // Hover Effect
        configurarHover(card, rarityColor);
        
        return card;
    }

    private void configurarHover(VBox card, String glowColor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
        card.setOnMouseEntered(e -> {
            st.setToX(1.05); st.setToY(1.05); st.playFromStart();
            card.setEffect(new DropShadow(20, Color.web(glowColor)));
            card.setStyle(card.getStyle().replace("rgba(20, 20, 30, 0.7)", "rgba(40, 40, 50, 0.9)"));
        });
        card.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            card.setEffect(null);
            card.setStyle(card.getStyle().replace("rgba(40, 40, 50, 0.9)", "rgba(20, 20, 30, 0.7)"));
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        node.setOpacity(0);
        node.setTranslateY(30);
        
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setToY(0);
        
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setToValue(1);
        
        tt.setDelay(Duration.millis(delayMillis));
        ft.setDelay(Duration.millis(delayMillis));
        
        tt.play(); ft.play();
    }

    private void procesarCompra(StoreOffer offer, VBox card, Button btn) {
        SoundManager.playClickSound();
        int price = (int) offer.getPrice();

        Task<Boolean> purchaseTask = new Task<>() {
            @Override
            protected Boolean call() {
                int currentBalance = UserDAO.obtenerBalanceMonedas(currentUserId);
                if (currentBalance >= price) {
                    if (UserDAO.actualizarMonedas(currentUserId, currentBalance - price)) {
                        UserDAO.registrarTransaccionCoins(currentUserId, -price, "Compra: " + offer.getTitle(), "store", offer.getId());
                        return true;
                    }
                }
                return false;
            }
        };

        purchaseTask.setOnSucceeded(e -> {
            if (purchaseTask.getValue()) {
                SoundManager.playSuccessSound();
                actualizarSaldoVisual();
                Toast.success("Compra Exitosa", "Has comprado: " + offer.getTitle());
                
                // Animación de éxito en la tarjeta
                btn.setText("¡COMPRADO!");
                btn.setDisable(true);
                btn.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-border-color: transparent;");
                
                // Efecto visual de flash
                Glow glow = new Glow(0.8);
                card.setEffect(glow);
                FadeTransition ft = new FadeTransition(Duration.millis(500), card);
                ft.setOnFinished(ev -> card.setEffect(null));
                ft.play();
                
            } else {
                SoundManager.playErrorSound();
                Toast.error("Fondos Insuficientes", "No tienes suficientes monedas para " + offer.getTitle());
                
                // Animación de error (vibración)
                TranslateTransition tt = new TranslateTransition(Duration.millis(50), card);
                tt.setByX(10); tt.setCycleCount(6); tt.setAutoReverse(true);
                tt.play();
                
                btn.setText("SIN FONDOS");
                btn.setStyle("-fx-border-color: #ff6b6b; -fx-text-fill: #ff6b6b;");
                
                // Restaurar texto después de un momento
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (Exception ex) {}
                    Platform.runLater(() -> {
                        btn.setText("ADQUIRIR");
                        btn.setStyle("-fx-border-color: #f7d27a; -fx-text-fill: #f7d27a;");
                    });
                }).start();
            }
        });

        new Thread(purchaseTask).start();
    }
}