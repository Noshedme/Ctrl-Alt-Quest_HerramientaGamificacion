package com.ctrlaltquest.ui.controllers.views;

import java.util.List;
import java.util.Map;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.dao.DashboardDAO;
import com.ctrlaltquest.models.Character;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class DashboardViewController {

    @FXML private Label lblWelcome;
    @FXML private Label lblSubtitle;
    @FXML private VBox activeMissionsContainer;
    @FXML private BarChart<String, Number> productivityChart;
    
    // Contenedores principales para animar
    @FXML private HBox newsContainer;
    @FXML private VBox missionsCard;
    @FXML private VBox chartCard;

    private int userId = -1;

    public void setUserId(int userId) {
        this.userId = userId;
        cargarNombrePersonaje();
        cargarDatosDashboard();
    }

    @FXML
    public void initialize() {
        // Ocultar elementos iniciales para la entrada dramática
        if(newsContainer != null) newsContainer.setOpacity(0);
        if(missionsCard != null) missionsCard.setOpacity(0);
        if(chartCard != null) chartCard.setOpacity(0);
        
        if(lblWelcome != null) lblWelcome.setText("SINTONIZANDO...");
    }

    // --- LÓGICA DE DATOS ---

    private void cargarNombrePersonaje() {
        Task<String> taskName = new Task<>() {
            @Override
            protected String call() {
                Map<Integer, Character> chars = CharacterDAO.getCharactersByUser(userId);
                if (!chars.isEmpty()) {
                    return chars.values().iterator().next().getName().toUpperCase();
                }
                return "HÉROE";
            }
        };

        taskName.setOnSucceeded(e -> {
            if(lblWelcome != null) typewriterEffect(lblWelcome, "HOLA DE NUEVO, " + taskName.getValue(), 50);
            animarEntradaDashboard();
        });

        new Thread(taskName).start();
    }

    private void cargarDatosDashboard() {
        if (userId == -1) return;

        // Tarea Misiones
        Task<List<DashboardDAO.MisionResumen>> taskMisiones = new Task<>() {
            @Override
            protected List<DashboardDAO.MisionResumen> call() {
                return DashboardDAO.getMisionesActivas(userId);
            }
        };

        taskMisiones.setOnSucceeded(e -> {
            if (activeMissionsContainer != null) {
                activeMissionsContainer.getChildren().clear();
                List<DashboardDAO.MisionResumen> lista = taskMisiones.getValue();
                
                if (lista.isEmpty()) {
                    mostrarMensajeVacio();
                } else {
                    int delay = 0;
                    for (DashboardDAO.MisionResumen m : lista) {
                        crearFilaMision(m, delay);
                        delay += 150;
                    }
                }
            }
        });

        // Tarea Gráfica
        Task<XYChart.Series<String, Number>> taskChart = new Task<>() {
            @Override
            protected XYChart.Series<String, Number> call() {
                return DashboardDAO.getRendimientoSemanal(userId);
            }
        };

        taskChart.setOnSucceeded(e -> {
            if (productivityChart != null) {
                productivityChart.getData().clear();
                if (taskChart.getValue() != null) {
                    productivityChart.getData().add(taskChart.getValue());
                    estilizarGrafica();
                }
            }
        });

        new Thread(taskMisiones).start();
        new Thread(taskChart).start();
    }

    // --- GENERACIÓN DE UI DINÁMICA ---

    private void mostrarMensajeVacio() {
        Label empty = new Label("No hay misiones activas.\n¡Ve al tablón y acepta un desafío!");
        empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-text-alignment: CENTER; -fx-font-size: 12px;");
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        activeMissionsContainer.getChildren().add(empty);
    }

    private void crearFilaMision(DashboardDAO.MisionResumen m, int delayMillis) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        
        // Estilo base: Cristal oscuro
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 10;");

        // Icono dificultad
        Label icon = new Label("●");
        String diffColor = switch(m.dificultad) {
            case "DIFICIL" -> "#ff6b6b";
            case "MEDIA" -> "#f7d27a";
            default -> "#4ade80";
        };
        icon.setStyle("-fx-text-fill: " + diffColor + "; -fx-font-size: 14px;");

        // Info
        VBox textBox = new VBox(2);
        Label lblTitle = new Label(m.titulo);
        lblTitle.setStyle("-fx-text-fill: #eeeeee; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label lblSub = new Label(m.dificultad);
        lblSub.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
        textBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Barra Progreso
        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        
        ProgressBar pb = new ProgressBar(m.progreso);
        pb.setPrefWidth(100);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent: " + diffColor + "; -fx-control-inner-background: rgba(0,0,0,0.5);");
        
        Label lblPct = new Label((int)(m.progreso * 100) + "%");
        lblPct.setStyle("-fx-text-fill: " + diffColor + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        progressBox.getChildren().addAll(lblPct, pb);

        row.getChildren().addAll(icon, textBox, spacer, progressBox);

        // Interactividad
        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color: rgba(247, 210, 122, 0.08); -fx-background-radius: 10; -fx-border-color: #f7d27a; -fx-border-radius: 10;");
            row.setEffect(new Glow(0.3));
            row.setTranslateX(5);
        });
        row.setOnMouseExited(e -> {
            row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 10;");
            row.setEffect(null);
            row.setTranslateX(0);
        });

        // Animación Entrada
        row.setOpacity(0);
        row.setTranslateX(-20);
        
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), row);
        tt.setToX(0);
        tt.setDelay(Duration.millis(delayMillis));
        
        FadeTransition ft = new FadeTransition(Duration.millis(400), row);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMillis));
        
        tt.play(); ft.play();

        activeMissionsContainer.getChildren().add(row);
    }

    private void estilizarGrafica() {
        Platform.runLater(() -> {
            if (productivityChart == null) return;
            
            // CORRECCIÓN: Usamos -fx-bar-fill para soportar gradientes
            for (Node n : productivityChart.lookupAll(".default-color0.chart-bar")) {
                n.setStyle("-fx-bar-fill: linear-gradient(to top, #6a1b9a, #f7d27a); " +
                           "-fx-background-radius: 5 5 0 0; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(247, 210, 122, 0.3), 10, 0, 0, 0);");
                
                ScaleTransition st = new ScaleTransition(Duration.millis(800), n);
                st.setFromY(0);
                st.setToY(1);
                st.play();
                
                n.setOnMouseEntered(e -> n.setEffect(new Glow(0.8)));
                n.setOnMouseExited(e -> n.setEffect(null));
            }
        });
    }

    // --- ANIMACIONES GENERALES ---

    private void typewriterEffect(Label label, String text, int delay) {
        label.setText("");
        final StringBuilder sb = new StringBuilder();
        Timeline timeline = new Timeline();
        
        for (int i = 0; i < text.length(); i++) {
            final int index = i;
            KeyFrame kf = new KeyFrame(Duration.millis(i * delay), e -> {
                sb.append(text.charAt(index));
                label.setText(sb.toString());
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    private void animarEntradaDashboard() {
        animarElemento(newsContainer, 0, -30);
        animarElemento(missionsCard, 200, -30);
        animarElemento(chartCard, 400, 30);
    }

    private void animarElemento(Node node, int delay, double offset) {
        if (node == null) return;
        node.setTranslateY(offset);
        
        TranslateTransition tt = new TranslateTransition(Duration.millis(800), node);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        tt.setDelay(Duration.millis(delay));
        
        FadeTransition ft = new FadeTransition(Duration.millis(800), node);
        ft.setToValue(1.0);
        ft.setDelay(Duration.millis(delay));
        
        tt.play(); ft.play();
    }

    @FXML
    public void animarCardEntrada(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), source);
        st.setToX(1.03); st.setToY(1.03); st.play();
        source.setEffect(new DropShadow(20, Color.rgb(163, 53, 238, 0.4)));
    }

    @FXML
    public void animarCardSalida(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), source);
        st.setToX(1.0); st.setToY(1.0); st.play();
        source.setEffect(null);
    }
}