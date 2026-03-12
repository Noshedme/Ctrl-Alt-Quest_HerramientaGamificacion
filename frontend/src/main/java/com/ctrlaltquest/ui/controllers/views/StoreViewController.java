package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;

public class StoreViewController {

    @FXML private Label      lblUserBalance;
    @FXML private GridPane   itemsGrid;
    @FXML private ScrollPane scrollContainer;

    private int currentUserId;

    private static final int    GRITO_PRECIO  = 300;
    private static final String GRITO_IMG     = "/assets/images/store/el_grito.png";
    private static final String PELIGRO_IMG   = "/assets/images/store/peligro.png";
    private static final String RISA_SOUND    = "/assets/sounds/risa_malevola.mp3";

    private static boolean gritoCompradoEnSesion = false;

    // ════════════════════════════════════════════════════════════════════════
    // INIT
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        this.currentUserId = SessionManager.getInstance().getUserId();
        actualizarSaldoVisual();
        cargarTienda();

        try {
            StackPane root = (StackPane) lblUserBalance.getScene().getRoot();
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            Toast.initialize(toastContainer);
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception ignored) {}

        if (scrollContainer != null) {
            scrollContainer.setVvalue(0);
            FadeTransition ft = new FadeTransition(Duration.millis(800), scrollContainer);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SALDO
    // ════════════════════════════════════════════════════════════════════════

    private void actualizarSaldoVisual() {
        Task<Integer> t = new Task<>() {
            @Override protected Integer call() {
                return UserDAO.obtenerBalanceMonedas(currentUserId);
            }
        };
        t.setOnSucceeded(e -> {
            int balance = t.getValue();
            lblUserBalance.setText((balance >= 0 ? balance : "---") + " 💰");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), lblUserBalance);
            st.setFromX(1); st.setFromY(1); st.setToX(1.1); st.setToY(1.1);
            st.setAutoReverse(true); st.setCycleCount(2); st.play();
        });
        new Thread(t).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TIENDA
    // ════════════════════════════════════════════════════════════════════════

    private void cargarTienda() {
        itemsGrid.getChildren().clear();

        VBox cardGrito   = crearTarjetaGrito();
        VBox cardPeligro = crearTarjetaPeligro();

        itemsGrid.add(cardGrito,   0, 0);
        itemsGrid.add(cardPeligro, 1, 0);

        animarEntrada(cardGrito,   0);
        animarEntrada(cardPeligro, 150);
    }

    // ════════════════════════════════════════════════════════════════════════
    // TARJETA: EL GRITO
    // ════════════════════════════════════════════════════════════════════════

    private VBox crearTarjetaGrito() {
        boolean yaComprado = gritoCompradoEnSesion;

        StackPane imgContainer = new StackPane();
        Rectangle bgImg = new Rectangle(110, 110, Color.web("rgba(163,53,238,0.12)"));
        bgImg.setArcWidth(22); bgImg.setArcHeight(22);
        Node imgNode;
        var imgUrl = getClass().getResource(GRITO_IMG);
        if (imgUrl != null) {
            ImageView iv = new ImageView(new Image(imgUrl.toExternalForm()));
            iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true);
            imgNode = iv;
        } else {
            Label emoji = new Label("😱"); emoji.setStyle("-fx-font-size: 54px;");
            imgNode = emoji;
        }
        imgContainer.getChildren().addAll(bgImg, imgNode);

        Label badge = new Label("✦ ÉPICO");
        badge.setStyle("-fx-text-fill: #a335ee; -fx-font-size: 10px; -fx-font-weight: bold;" +
                       "-fx-background-color: rgba(163,53,238,0.15);" +
                       "-fx-background-radius: 20; -fx-padding: 2 10;");

        Label title = new Label("El Grito");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");

        Label desc = new Label("Cada clic que des resonará con el terror de un grito eterno.\nActívalo cuando te atrevas.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px; -fx-text-alignment: CENTER;");

        Label price = new Label(GRITO_PRECIO + " 💰");
        price.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold; -fx-font-size: 15px;");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnComprar = new Button(yaComprado ? "YA LO TIENES" : "ADQUIRIR  " + GRITO_PRECIO + " 💰");
        btnComprar.setPrefWidth(200);
        btnComprar.setDisable(yaComprado);
        btnComprar.setStyle(estiloComprar("#a335ee"));

        Button btnToggle = crearBtnToggle();
        btnToggle.setVisible(yaComprado);
        btnToggle.setManaged(yaComprado);

        btnComprar.setOnAction(e -> procesarCompraGrito(btnComprar, btnToggle));

        VBox card = new VBox(14);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(24));
        card.setPrefWidth(260); card.setPrefHeight(400);
        card.setStyle(estiloCard("rgba(163, 53, 238, 0.6)"));
        card.getChildren().addAll(imgContainer, badge, title, desc, spacer, price, btnComprar, btnToggle);
        configurarHover(card, "#a335ee");
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    // TARJETA: ☠️ NO COMPRAR, PELIGRO
    // ════════════════════════════════════════════════════════════════════════

    private VBox crearTarjetaPeligro() {

        // ── Imagen con fallback emoji ────────────────────────────────────────
        StackPane imgContainer = new StackPane();
        Rectangle bgImg = new Rectangle(110, 110, Color.web("rgba(255,0,0,0.08)"));
        bgImg.setArcWidth(22); bgImg.setArcHeight(22);

        Node imgNode;
        URL imgUrl = getClass().getResource(PELIGRO_IMG);
        if (imgUrl != null) {
            ImageView iv = new ImageView(new Image(imgUrl.toExternalForm()));
            iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true);
            imgNode = iv;
        } else {
            Label emoji = new Label("☠️"); emoji.setStyle("-fx-font-size: 54px;");
            imgNode = emoji;
        }
        imgContainer.getChildren().addAll(bgImg, imgNode);

        // ── Badge parpadeante ────────────────────────────────────────────────
        Label badge = new Label("⚠ PELIGRO");
        badge.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-weight: bold;" +
                       "-fx-background-color: rgba(239,68,68,0.15);" +
                       "-fx-background-radius: 20; -fx-padding: 2 10;");
        Timeline parpadeo = new Timeline(
            new KeyFrame(Duration.millis(0),    e -> badge.setOpacity(1.0)),
            new KeyFrame(Duration.millis(600),  e -> badge.setOpacity(0.15)),
            new KeyFrame(Duration.millis(1200), e -> badge.setOpacity(1.0))
        );
        parpadeo.setCycleCount(Timeline.INDEFINITE);
        parpadeo.play();

        Label title = new Label("NO COMPRAR");
        title.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 18px;");

        Label desc = new Label("No tengo ni idea de lo que hace esto.\nTe lo advierto.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-text-alignment: CENTER;");

        Label price = new Label("GRATIS");
        price.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 15px;");

        Label warning = new Label("En serio. No lo hagas.");
        warning.setStyle("-fx-text-fill: rgba(239,68,68,0.5); -fx-font-size: 10px; -fx-font-style: italic;");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnComprar = new Button("ADQUIRIR  (GRATIS)");
        btnComprar.setPrefWidth(200);
        btnComprar.setStyle(estiloComprar("#ef4444"));
        btnComprar.setOnAction(e -> ejecutarTrampa(btnComprar));

        VBox card = new VBox(14);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(24));
        card.setPrefWidth(260); card.setPrefHeight(400);
        card.setStyle(estiloCard("rgba(239, 68, 68, 0.6)"));
        card.getChildren().addAll(imgContainer, badge, title, desc, spacer, price, warning, btnComprar);
        configurarHover(card, "#ef4444");
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 😈 LA TRAMPA
    // ════════════════════════════════════════════════════════════════════════

    private void ejecutarTrampa(Button btn) {
        btn.setDisable(true);
        btn.setText("...");

        // 1. Toast final
        Toast.error("😈", "Te lo advertí.");

        // 2. Cargar y reproducir la risa malévola ANTES del fade
        AudioClip risa = null;
        try {
            URL risaUrl = getClass().getResource(RISA_SOUND);
            if (risaUrl != null) {
                risa = new AudioClip(risaUrl.toExternalForm());
                risa.setVolume(1.0);
                risa.play();
            }
        } catch (Exception ignored) {}

        final AudioClip risaFinal = risa;

        // 3. Esperar 2 segundos mientras la risa suena, luego fade + exit
        PauseTransition espera = new PauseTransition(Duration.millis(2000));
        espera.setOnFinished(e -> {
            Stage stage = (Stage) btn.getScene().getWindow();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                if (risaFinal != null) risaFinal.stop();
                Platform.exit();
            });
            fadeOut.play();
        });
        espera.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOGGLE GRITO
    // ════════════════════════════════════════════════════════════════════════

    private Button crearBtnToggle() {
        boolean activo = SoundManager.isGritoActivado();
        Button btn = new Button(activo ? "😱  DESACTIVAR" : "😶  ACTIVAR");
        btn.setPrefWidth(200);
        btn.setStyle(estiloToggle(activo));
        btn.setOnAction(e -> {
            boolean nuevo = !SoundManager.isGritoActivado();
            SoundManager.setGritoActivado(nuevo);
            btn.setText(nuevo ? "😱  DESACTIVAR" : "😶  ACTIVAR");
            btn.setStyle(estiloToggle(nuevo));
            if (nuevo) SoundManager.playGrito();
            Toast.info("El Grito", nuevo ? "¡Activado! 😱 Cada clic gritará." : "Desactivado. El grito duerme.");
        });
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════════
    // COMPRA GRITO
    // ════════════════════════════════════════════════════════════════════════

    private void procesarCompraGrito(Button btnComprar, Button btnToggle) {
        SoundManager.playClickSound();
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() {
                int balance = UserDAO.obtenerBalanceMonedas(currentUserId);
                if (balance < GRITO_PRECIO) return false;
                boolean ok = UserDAO.actualizarMonedas(currentUserId, balance - GRITO_PRECIO);
                if (ok) UserDAO.registrarTransaccionCoins(
                    currentUserId, -GRITO_PRECIO, "Compra: El Grito", "store", 1);
                return ok;
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue()) {
                gritoCompradoEnSesion = true;
                SoundManager.setGritoActivado(true);
                SoundManager.playGrito();
                actualizarSaldoVisual();
                Toast.success("¡ADQUIRIDO!", "El Grito ahora habita en cada uno de tus clics.");
                btnComprar.setText("YA LO TIENES");
                btnComprar.setDisable(true);
                btnToggle.setVisible(true); btnToggle.setManaged(true);
                btnToggle.setText("😱  DESACTIVAR");
                btnToggle.setStyle(estiloToggle(true));
                VBox card = (VBox) btnComprar.getParent();
                card.setEffect(new Glow(0.9));
                FadeTransition ft = new FadeTransition(Duration.millis(600), card);
                ft.setOnFinished(ev -> card.setEffect(null)); ft.play();
            } else {
                SoundManager.playErrorSound();
                Toast.error("Fondos Insuficientes", "Necesitas " + GRITO_PRECIO + " 💰 para El Grito.");
                VBox card = (VBox) btnComprar.getParent();
                TranslateTransition tt = new TranslateTransition(Duration.millis(50), card);
                tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true); tt.play();
                btnComprar.setText("SIN FONDOS");
                btnComprar.setStyle("-fx-border-color: #ff6b6b; -fx-text-fill: #ff6b6b;" +
                                    "-fx-border-radius: 8; -fx-background-radius: 8;" +
                                    "-fx-background-color: rgba(0,0,0,0.2);");
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (Exception ex) {}
                    Platform.runLater(() -> {
                        btnComprar.setText("ADQUIRIR  " + GRITO_PRECIO + " 💰");
                        btnComprar.setStyle(estiloComprar("#a335ee"));
                    });
                }).start();
            }
        });
        new Thread(t).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    // ESTILOS
    // ════════════════════════════════════════════════════════════════════════

    private String estiloCard(String borderColor) {
        return "-fx-background-color: rgba(20, 20, 30, 0.85);" +
               "-fx-background-radius: 14;" +
               "-fx-border-color: " + borderColor + ";" +
               "-fx-border-radius: 14;" +
               "-fx-border-width: 1.5;";
    }

    private String estiloComprar(String color) {
        return "-fx-background-color: rgba(0,0,0,0.2); -fx-text-fill: " + color +
               "; -fx-border-color: " + color +
               "; -fx-border-radius: 8; -fx-background-radius: 8;" +
               " -fx-cursor: hand; -fx-font-weight: bold;";
    }

    private String estiloToggle(boolean activo) {
        String c = activo ? "#ef4444" : "#4ade80";
        return "-fx-background-color: rgba(0,0,0,0.2); -fx-text-fill: " + c +
               "; -fx-border-color: " + c +
               "; -fx-border-radius: 8; -fx-background-radius: 8;" +
               " -fx-cursor: hand; -fx-font-weight: bold;";
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════════════════

    private void configurarHover(VBox card, String color) {
        ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
        card.setOnMouseEntered(e -> {
            st.setToX(1.04); st.setToY(1.04); st.playFromStart();
            card.setEffect(new DropShadow(25, Color.web(color)));
        });
        card.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            card.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delay) {
        node.setOpacity(0); node.setTranslateY(30);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setToY(0); tt.setDelay(Duration.millis(delay)); tt.play();
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setToValue(1); ft.setDelay(Duration.millis(delay)); ft.play();
    }
}