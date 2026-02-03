package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.dao.DashboardDAO; // IMPORTANTE
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

public class DashboardViewController {

    @FXML private Label lblWelcome;
    @FXML private VBox activeMissionsContainer;
    @FXML private BarChart<String, Number> productivityChart;
    @FXML private HBox newsContainer;

    private int userId = -1;

    /**
     * Llamado por HomeController para pasar el ID del usuario actual
     */
    public void setUserId(int userId) {
        this.userId = userId;
        cargarDatosReales();
    }

    @FXML
    public void initialize() {
        animarEntradaDashboard();
    }

    private void cargarDatosReales() {
        if (userId == -1) return;

        // Tarea 1: Cargar Misiones desde BD
        Task<List<DashboardDAO.MisionResumen>> taskMisiones = new Task<>() {
            @Override
            protected List<DashboardDAO.MisionResumen> call() {
                return DashboardDAO.getMisionesActivas(userId);
            }
        };

        taskMisiones.setOnSucceeded(e -> {
            activeMissionsContainer.getChildren().clear();
            List<DashboardDAO.MisionResumen> misiones = taskMisiones.getValue();
            
            if (misiones.isEmpty()) {
                Label empty = new Label("No hay misiones activas. ¡Ve al tablero!");
                empty.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                activeMissionsContainer.getChildren().add(empty);
            } else {
                for (DashboardDAO.MisionResumen m : misiones) {
                    crearFilaMisionCompacta(m.titulo, m.progreso);
                }
            }
        });

        // Tarea 2: Cargar Gráfica desde BD
        Task<XYChart.Series<String, Number>> taskChart = new Task<>() {
            @Override
            protected XYChart.Series<String, Number> call() {
                return DashboardDAO.getRendimientoSemanal(userId);
            }
        };

        taskChart.setOnSucceeded(e -> {
            productivityChart.getData().clear();
            productivityChart.getData().add(taskChart.getValue());
            estilizarGrafica();
        });

        // Ejecutar en hilos separados
        new Thread(taskMisiones).start();
        new Thread(taskChart).start();
    }

    private void crearFilaMisionCompacta(String titulo, double progreso) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(10));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8;");

        // Título
        Label lblTitle = new Label(titulo);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Barra compacta
        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        
        ProgressBar pb = new ProgressBar(progreso);
        pb.setPrefWidth(100);
        pb.setPrefHeight(6);
        
        // Color dinámico según progreso
        String color = progreso > 0.7 ? "#4ade80" : (progreso > 0.3 ? "#f7d27a" : "#ff6b6b");
        pb.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: rgba(0,0,0,0.5);");
        
        Label lblPct = new Label((int)(progreso * 100) + "%");
        lblPct.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px;");
        
        progressBox.getChildren().addAll(pb, lblPct);

        row.getChildren().addAll(lblTitle, spacer, progressBox);
        activeMissionsContainer.getChildren().add(row);
    }

    private void estilizarGrafica() {
        Platform.runLater(() -> {
            for (Node n : productivityChart.lookupAll(".default-color0.chart-bar")) {
                n.setStyle("-fx-bar-fill: #f7d27a; -fx-effect: dropshadow(three-pass-box, rgba(247, 210, 122, 0.4), 10, 0, 0, 0);");
            }
        });
    }

    // --- ANIMACIONES ---

    private void animarEntradaDashboard() {
        if (newsContainer != null) {
            newsContainer.setOpacity(0);
            newsContainer.setTranslateY(20);
            TranslateTransition tt = new TranslateTransition(Duration.millis(600), newsContainer);
            tt.setToY(0);
            FadeTransition ft = new FadeTransition(Duration.millis(600), newsContainer);
            ft.setToValue(1.0);
            tt.play(); ft.play();
        }
    }

    @FXML
    public void animarCardEntrada(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), source);
        st.setToX(1.05); st.setToY(1.05); st.play();
        source.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.5)));
    }

    @FXML
    public void animarCardSalida(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), source);
        st.setToX(1.0); st.setToY(1.0); st.play();
        source.setEffect(null);
    }
}