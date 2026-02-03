package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.models.StoreOffer;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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

import java.util.ArrayList;
import java.util.List;

public class StoreViewController {

    @FXML private Label lblUserBalance;
    @FXML private VBox goldStoreContainer;    // Cambiado de FlowPane a VBox
    @FXML private VBox premiumStoreContainer; // Cambiado de FlowPane a VBox
    @FXML private StackPane battlePassBanner;

    @FXML
    public void initialize() {
        lblUserBalance.setText("1,250 💰");
        cargarOfertas();
        animarBannerPromocional();
    }

    private void animarBannerPromocional() {
        if (battlePassBanner != null) {
            ScaleTransition st = new ScaleTransition(Duration.seconds(3), battlePassBanner);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.01); st.setToY(1.01);
            st.setAutoReverse(true);
            st.setCycleCount(ScaleTransition.INDEFINITE);
            st.play();
        }
    }

    private void cargarOfertas() {
        goldStoreContainer.getChildren().clear();
        premiumStoreContainer.getChildren().clear();

        // Datos Simulados
        List<StoreOffer> goldOffers = new ArrayList<>();
        goldOffers.add(new StoreOffer(1, "Poción de Café", "Aumenta la productividad x2 durante 1 hora. Esencial para entregas.", 150, false, "☕"));
        goldOffers.add(new StoreOffer(2, "Teclado Mecánico", "Cosmético: Añade efectos de sonido clicky al escribir código.", 500, false, "⌨"));
        goldOffers.add(new StoreOffer(3, "Licencia Lofi", "Desbloquea la estación de música de concentración en el dashboard.", 300, false, "🎵"));
        goldOffers.add(new StoreOffer(4, "Gafas Anti-Bug", "Resalta errores de sintaxis básicos con un brillo rojo.", 800, false, "👓"));

        List<StoreOffer> premiumOffers = new ArrayList<>();
        premiumOffers.add(new StoreOffer(5, "Pack Fundador", "5000 Monedas de Oro + Título único 'Pionero' + Marco de Avatar.", 1.99, true, "💎"));
        premiumOffers.add(new StoreOffer(6, "Skin: Cyberpunk", "Tema de interfaz completo con neones y efectos CRT.", 2.99, true, "🤖"));

        // Renderizado Oro
        int delay = 0;
        for (StoreOffer offer : goldOffers) {
            HBox row = crearFilaOferta(offer);
            goldStoreContainer.getChildren().add(row);
            animarEntrada(row, delay);
            delay += 80;
        }

        // Renderizado Premium
        delay = 0;
        for (StoreOffer offer : premiumOffers) {
            HBox row = crearFilaOferta(offer);
            premiumStoreContainer.getChildren().add(row);
            animarEntrada(row, delay);
            delay += 80;
        }
    }

    /**
     * FABRICA DE FILAS: Crea una barra horizontal estilo RPG.
     */
    private HBox crearFilaOferta(StoreOffer offer) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        
        // Colores según tipo
        String accentColor = offer.isPremium() ? "#a335ee" : "#d4af37";
        String glowColor = offer.isPremium() ? "rgba(163, 53, 238, 0.1)" : "rgba(212, 175, 55, 0.1)";
        
        // Estilo Base Transparente
        row.setStyle(
            "-fx-background-color: rgba(30, 20, 40, 0.6);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: transparent transparent " + accentColor + " transparent;" +
            "-fx-border-width: 0 0 1 0;" // Solo línea inferior sutil
        );
        row.setOpacity(0); // Para animación

        // 1. Icono Izquierdo
        StackPane iconPane = new StackPane();
        Circle bgCircle = new Circle(25, Color.web("rgba(0,0,0,0.3)"));
        bgCircle.setStroke(Color.web(accentColor));
        bgCircle.setStrokeWidth(1.5);
        
        // Usamos el imagePath como texto emoji por simplicidad, o podrías cargar imagen real
        Label iconLabel = new Label(offer.getImagePath().length() < 5 ? offer.getImagePath() : offer.getTitle().substring(0,1));
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        
        iconPane.getChildren().addAll(bgCircle, iconLabel);

        // 2. Info Central (Título y Descripción)
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Georgia';");
        
        Label desc = new Label(offer.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        
        infoBox.getChildren().addAll(title, desc);

        // 3. Botón de Compra (Derecha)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnBuy = new Button();
        btnBuy.setPrefWidth(140);
        btnBuy.getStyleClass().add("btn-nav"); // Estilo base limpio
        
        if (offer.isPremium()) {
            btnBuy.setText("$" + offer.getPrice());
            btnBuy.setStyle("-fx-border-color: " + accentColor + "; -fx-text-fill: " + accentColor + "; -fx-background-color: rgba(163, 53, 238, 0.1);");
            btnBuy.setOnAction(e -> simularPagoReal(offer));
        } else {
            btnBuy.setText((int)offer.getPrice() + " 💰");
            btnBuy.setStyle("-fx-border-color: " + accentColor + "; -fx-text-fill: " + accentColor + "; -fx-background-color: rgba(212, 175, 55, 0.1);");
            btnBuy.setOnAction(e -> simularCompraOro(offer));
        }
        
        // Tooltip
        Tooltip.install(row, new Tooltip(offer.getDescription()));

        row.getChildren().addAll(iconPane, infoBox, spacer, btnBuy);

        // INTERACTIVIDAD
        configurarHover(row, accentColor);

        return row;
    }

    private void configurarHover(HBox row, String accentColor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), row);
        
        row.setOnMouseEntered(e -> {
            st.setToX(1.01); st.setToY(1.01); st.playFromStart();
            
            // Iluminar fondo y borde completo
            row.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + accentColor + ";" +
                "-fx-border-width: 1;"
            );
            row.setEffect(new DropShadow(10, Color.web(accentColor)));
        });

        row.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            
            // Volver a estado normal (solo borde inferior)
            row.setStyle(
                "-fx-background-color: rgba(30, 20, 40, 0.6);" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: transparent transparent " + accentColor + " transparent;" +
                "-fx-border-width: 0 0 1 0;"
            );
            row.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        // Deslizar desde la izquierda
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setFromX(-50);
        tt.setToX(0);
        tt.setDelay(Duration.millis(delayMillis));

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMillis));

        tt.play();
        ft.play();
    }

    // --- LÓGICA DE COMPRA ---
    private void simularCompraOro(StoreOffer offer) {
        SoundManager.playClickSound();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mercader");
        alert.setHeaderText("¡Adquisición Completada!");
        alert.setContentText("Has comprado: " + offer.getTitle() + "\nSe descontaron " + (int)offer.getPrice() + " monedas de tu cuenta.");
        alert.show();
    }

    private void simularPagoReal(StoreOffer offer) {
        SoundManager.playClickSound();
        try {
            // Cargar el FXML del formulario de pago
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/payment_form.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // Obtener el controlador
            com.ctrlaltquest.ui.PaymentFormController paymentController = loader.getController();
            
            // Pasar datos del producto al formulario
            paymentController.setProductoInfo(
                offer.getTitle(),
                offer.getPrice(),
                offer.getDescription()
            );
            
            // Crear ventana modal
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("💳 Formulario de Pago Seguro");
            stage.setScene(new javafx.scene.Scene(root, 500, 600));
            stage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/assets/images/sprites/icon.png")
            ));
            stage.setResizable(false);
            stage.show();
            
        } catch (Exception e) {
            System.err.println("❌ Error abriendo formulario de pago: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo abrir el formulario de pago");
            alert.setContentText("Por favor intenta de nuevo. Error: " + e.getMessage());
            alert.show();
        }
    }
}