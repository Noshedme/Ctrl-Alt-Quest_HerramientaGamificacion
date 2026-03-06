package com.ctrlaltquest.ui.controllers.views;

import java.util.List;
import java.util.stream.Collectors;

import com.ctrlaltquest.dao.DashboardDAO;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ListExpandedModalController {
    @FXML private VBox applicationsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label lblAppCount;
    @FXML private Label lblTotalTime;
    @FXML private Label lblTopApp;
    @FXML private Label lblAvgTime;

    private int userId = -1;
    private Stage stage;
    private List<DashboardDAO.AppUsage> allUsageData;

    public void setData(int userId, List<DashboardDAO.AppUsage> usageData) {
        this.userId = userId;
        this.allUsageData = usageData;
        cargarListaExpandida(usageData);
        setupSearchAndFilter();
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

    private void setupSearchAndFilter() {
        // Configurar ComboBox de ordenamiento
        ObservableList<String> sortOptions = FXCollections.observableArrayList(
            "Mayor a Menor Tiempo",
            "Menor a Mayor Tiempo",
            "Alfabético (A-Z)",
            "Alfabético (Z-A)"
        );
        sortCombo.setItems(sortOptions);
        sortCombo.setValue("Mayor a Menor Tiempo");

        // Listener para búsqueda
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrarYOrdenar());

        // Listener para ordenamiento
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> filtrarYOrdenar());
    }

    private void filtrarYOrdenar() {
        String searchText = searchField.getText().toLowerCase();
        String sortType = sortCombo.getValue();

        List<DashboardDAO.AppUsage> filtered = allUsageData.stream()
            .filter(u -> u.appName.toLowerCase().contains(searchText))
            .collect(Collectors.toList());

        // Aplicar ordenamiento
        switch (sortType) {
            case "Mayor a Menor Tiempo":
                filtered.sort((a, b) -> Long.compare(b.seconds, a.seconds));
                break;
            case "Menor a Mayor Tiempo":
                filtered.sort((a, b) -> Long.compare(a.seconds, b.seconds));
                break;
            case "Alfabético (A-Z)":
                filtered.sort((a, b) -> a.appName.compareTo(b.appName));
                break;
            case "Alfabético (Z-A)":
                filtered.sort((a, b) -> b.appName.compareTo(a.appName));
                break;
        }

        mostrarLista(filtered);
    }

    private void cargarListaExpandida(List<DashboardDAO.AppUsage> usageData) {
        Platform.runLater(() -> {
            mostrarLista(usageData);
            actualizarEstadisticas(usageData);
        });
    }

    private void mostrarLista(List<DashboardDAO.AppUsage> usageData) {
        applicationsContainer.getChildren().clear();

        long total = usageData.stream().mapToLong(u -> u.seconds).sum();
        if (total == 0) {
            Label empty = new Label("No hay datos de uso disponibles.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            applicationsContainer.getChildren().add(empty);
            return;
        }

        String[] colors = {
            "#FF6B6B", "#4ECDC4", "#FFD93D", "#A335EE", "#FF8C42",
            "#6BCB77", "#FF006E", "#00D4FF", "#A0FF00", "#FFB6C1"
        };

        int colorIdx = 0;
        int delayIdx = 0;

        for (DashboardDAO.AppUsage u : usageData) {
            double percent = (double) u.seconds / (double) total * 100.0;
            String color = colors[colorIdx % colors.length];

            // Crear tarjeta de aplicación ampliada
            VBox appCard = crearTarjetaAplicacion(u, percent, color, delayIdx);
            applicationsContainer.getChildren().add(appCard);
            colorIdx++;
            delayIdx += 80;
        }
    }

    private VBox crearTarjetaAplicacion(DashboardDAO.AppUsage u, double percent, String color, int delayIdx) {
        VBox card = new VBox(15);
        card.setStyle("-fx-padding: 25; -fx-background-radius: 18; -fx-background-color: linear-gradient(to right, rgba(" + extraerRGB(color) + ", 0.2), rgba(" + extraerRGB(color) + ", 0.05) 50%, rgba(0,0,0,0.05)); -fx-border-color: " + color + "; -fx-border-radius: 18; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(" + extraerRGB(color) + ", 0.5), 20, 0, 0, 4);");
        card.setPrefHeight(140);

        // ENCABEZADO: Nombre y tiempo
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);

        // Indicador de color con efecto de brillo
        Region colorDot = new Region();
        colorDot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, " + color + ", 12, 0, 0, 0);");
        colorDot.setPrefWidth(20);
        colorDot.setPrefHeight(20);

        // Nombre de app grande y brillante
        Label appName = new Label(u.appName);
        appName.setStyle("-fx-text-fill: #f7d27a; -fx-font-size: 20px; -fx-font-weight: 900; -fx-letter-spacing: 1.5px;");
        appName.setMaxWidth(350);
        appName.setWrapText(false);
        appName.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Tiempo y porcentaje con estilos vibrantes
        VBox timeBox = new VBox(5);
        timeBox.setAlignment(Pos.CENTER_RIGHT);
        Label timeLabel = new Label(formatSeconds(u.seconds));
        timeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: 900; -fx-effect: dropshadow(three-pass-box, rgba(" + extraerRGB(color) + ", 0.6), 10, 0, 0, 0);");
        Label percentLabel = new Label(String.format("%.1f%%", percent));
        percentLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        timeBox.getChildren().addAll(timeLabel, percentLabel);

        header.getChildren().addAll(colorDot, appName, spacer, timeBox);

        // BARRA DE PROGRESO mejorada
        ProgressBar progressBar = new ProgressBar(percent / 100.0);
        progressBar.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: rgba(0,0,0,0.2); -fx-padding: 0;");
        progressBar.setPrefHeight(6);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: rgba(0,0,0,0.3);");

        // Información adicional
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setStyle("-fx-padding: 12 0 0 0;");

        Label minLabel = new Label("⏱️  " + (u.seconds / 60) + " minutos");
        minLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label secLabel = new Label("(" + (u.seconds % 60) + "s)");
        secLabel.setStyle("-fx-text-fill: " + color + "80; -fx-font-size: 11px;");

        infoBox.getChildren().addAll(minLabel, secLabel);

        // Agregar elementos a la tarjeta
        card.getChildren().addAll(header, progressBar, infoBox);

        // Animación de entrada suave
        card.setOpacity(0);
        card.setTranslateY(25);

        TranslateTransition tt = new TranslateTransition(Duration.millis(600), card);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayIdx));
        tt.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ft = new FadeTransition(Duration.millis(600), card);
        ft.setToValue(1.0);
        ft.setDelay(Duration.millis(delayIdx));

        // Hover effects espectaculares
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-padding: 25; -fx-background-radius: 18; -fx-background-color: linear-gradient(to right, rgba(" + extraerRGB(color) + ", 0.35), rgba(" + extraerRGB(color) + ", 0.1) 50%, rgba(0,0,0,0.1)); -fx-border-color: " + color + "; -fx-border-radius: 18; -fx-border-width: 3; -fx-effect: dropshadow(three-pass-box, rgba(" + extraerRGB(color) + ", 0.8), 30, 0, 0, 6);");
            ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
            scale.setToX(1.03);
            scale.setToY(1.03);
            scale.setInterpolator(Interpolator.EASE_OUT);
            scale.play();
            card.setCursor(javafx.scene.Cursor.HAND);
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-padding: 25; -fx-background-radius: 18; -fx-background-color: linear-gradient(to right, rgba(" + extraerRGB(color) + ", 0.2), rgba(" + extraerRGB(color) + ", 0.05) 50%, rgba(0,0,0,0.05)); -fx-border-color: " + color + "; -fx-border-radius: 18; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(" + extraerRGB(color) + ", 0.5), 20, 0, 0, 4);");
            ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_IN);
            scale.play();
            card.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        tt.play();
        ft.play();

        return card;
    }

    private void actualizarEstadisticas(List<DashboardDAO.AppUsage> usageData) {
        if (usageData.isEmpty()) return;

        long total = usageData.stream().mapToLong(u -> u.seconds).sum();
        DashboardDAO.AppUsage topApp = usageData.stream()
            .max((a, b) -> Long.compare(a.seconds, b.seconds))
            .orElse(null);

        lblAppCount.setText(String.valueOf(usageData.size()));
        lblTotalTime.setText(formatSeconds(total));
        if (topApp != null) {
            lblTopApp.setText(topApp.appName);
        }
        long avgSeconds = total / usageData.size();
        lblAvgTime.setText(formatSeconds(avgSeconds));
    }

    private String formatSeconds(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }

    private String extraerRGB(String hexColor) {
        String hex = hexColor.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return r + ", " + g + ", " + b;
    }
}
