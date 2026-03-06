package com.ctrlaltquest.ui.controllers.views;

import java.util.List;

import com.ctrlaltquest.dao.DashboardDAO;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ChartExpandedModalController {
    @FXML private PieChart chartExpanded;
    @FXML private Label lblTotalTime;
    @FXML private Label lblCategoryCount;
    @FXML private Label lblMaxCategory;
    @FXML private Label lblAvgCategory;

    private int userId = -1;
    private Stage stage;

    public void setData(int userId, List<DashboardDAO.AppUsage> usageData) {
        this.userId = userId;
        cargarGraficoExpandido(usageData);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void cerrarModal() {
        if (stage != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> stage.close());
            fade.play();
        }
    }

    private void cargarGraficoExpandido(List<DashboardDAO.AppUsage> usageData) {
        Platform.runLater(() -> {
            if (chartExpanded == null) return;

            long total = usageData.stream().mapToLong(u -> u.seconds).sum();
            
            // Colores vibrantes
            String[] colors = {
                "#FF6B6B", "#4ECDC4", "#FFD93D", "#A335EE", "#FF8C42",
                "#6BCB77", "#FF006E", "#00D4FF", "#A0FF00", "#FFB6C1"
            };

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            DashboardDAO.AppUsage maxUsage = null;
            int colorIdx = 0;

            for (DashboardDAO.AppUsage u : usageData) {
                double percent = (double) u.seconds / (double) total * 100.0;
                String label = u.appName + "\n(" + String.format("%.1f", percent) + "%)";
                PieChart.Data d = new PieChart.Data(label, u.seconds);
                pieData.add(d);

                if (maxUsage == null || u.seconds > maxUsage.seconds) {
                    maxUsage = u;
                }
                colorIdx++;
            }

            chartExpanded.setData(pieData);
            chartExpanded.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

            // Aplicar colores y animaciones
            colorIdx = 0;
            for (PieChart.Data d : pieData) {
                String color = colors[colorIdx % colors.length];
                Node sliceNode = d.getNode();
                if (sliceNode != null) {
                    sliceNode.setStyle("-fx-pie-color: " + color + ";");

                    // Animación de entrada
                    ScaleTransition entradaScale = new ScaleTransition(Duration.millis(700 + colorIdx * 100), sliceNode);
                    entradaScale.setFromX(0.5);
                    entradaScale.setFromY(0.5);
                    entradaScale.setToX(1.0);
                    entradaScale.setToY(1.0);
                    entradaScale.setInterpolator(Interpolator.EASE_OUT);
                    entradaScale.play();

                    // Interactividad hover
                    sliceNode.setOnMouseEntered(ev -> {
                        ScaleTransition hoverScale = new ScaleTransition(Duration.millis(250), sliceNode);
                        hoverScale.setToX(1.15);
                        hoverScale.setToY(1.15);
                        hoverScale.setInterpolator(Interpolator.EASE_OUT);
                        hoverScale.play();

                        DropShadow brillanteShadow = new DropShadow(40, Color.web(color, 1.0));
                        brillanteShadow.setRadius(25);
                        sliceNode.setEffect(brillanteShadow);
                        sliceNode.setCursor(javafx.scene.Cursor.HAND);
                    });

                    sliceNode.setOnMouseExited(ev -> {
                        ScaleTransition normalScale = new ScaleTransition(Duration.millis(200), sliceNode);
                        normalScale.setToX(1.0);
                        normalScale.setToY(1.0);
                        normalScale.setInterpolator(Interpolator.EASE_OUT);
                        normalScale.play();

                        sliceNode.setEffect(null);
                        sliceNode.setCursor(javafx.scene.Cursor.DEFAULT);
                    });
                }
                colorIdx++;
            }

            // Actualizar etiquetas de estadísticas
            lblTotalTime.setText(formatSeconds(total));
            lblCategoryCount.setText(String.valueOf(usageData.size()));
            if (maxUsage != null) {
                lblMaxCategory.setText(maxUsage.appName);
            }
            
            long avgSeconds = usageData.size() > 0 ? total / usageData.size() : 0;
            lblAvgCategory.setText(formatSeconds(avgSeconds));

            // Animar entrada de estadísticas
            animarEstadisticas();
        });
    }

    private void animarEstadisticas() {
        animarLabelEntrada(lblTotalTime, 0);
        animarLabelEntrada(lblCategoryCount, 100);
        animarLabelEntrada(lblMaxCategory, 200);
        animarLabelEntrada(lblAvgCategory, 300);
    }

    private void animarLabelEntrada(Label label, int delay) {
        if (label == null) return;
        label.setOpacity(0);
        label.setScaleX(0.8);
        label.setScaleY(0.8);

        FadeTransition fade = new FadeTransition(Duration.millis(500), label);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(delay));

        ScaleTransition scale = new ScaleTransition(Duration.millis(500), label);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setDelay(Duration.millis(delay));
        scale.setInterpolator(Interpolator.EASE_OUT);

        fade.play();
        scale.play();
    }

    private String formatSeconds(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }
}
