package com.ctrlaltquest.ui.controllers.views;

import java.util.List;
import java.util.Map;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.dao.DashboardDAO;
import com.ctrlaltquest.models.Character;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controlador para la modal completa del reporte semanal.
 * Muestra estadísticas, gráficas interactivas y opciones de exportación.
 */
public class ReportModalController {

    @FXML private Label lblReportStreak;
    @FXML private Label lblReportXP;
    @FXML private Label lblReportMissions;
    @FXML private Label lblReportScreenTime;
    @FXML private LineChart<String, Number> xpWeeklyChart;
    @FXML private PieChart usageBreakdownPie;
    @FXML private VBox usageBreakdownList;
    @FXML private FlowPane achievementsFlow;
    
    private int userId = -1;
    private Character currentCharacter;
    private Stage modalStage;

    public void setData(int userId, Character character) {
        this.userId = userId;
        this.currentCharacter = character;
        cargarDatos();
    }

    public void setStage(Stage stage) {
        this.modalStage = stage;
    }

    @FXML
    public void initialize() {
        // Estilos de gráficas
        if (xpWeeklyChart != null) {
            xpWeeklyChart.setLegendVisible(false);
            xpWeeklyChart.setAnimated(true);
        }
        if (usageBreakdownPie != null) {
            usageBreakdownPie.setAnimated(true);
            usageBreakdownPie.setLegendVisible(false);
        }
    }

    /**
     * Carga todos los datos de la modal en paralelo (3 tareas)
     */
    private void cargarDatos() {
        if (userId == -1) return;

        // TAREA 1: Estadísticas (streak, XP, misiones, tiempo)
        Task<Map<String, Object>> taskStats = new Task<>() {
            @Override
            protected Map<String, Object> call() {
                Map<String, Object> stats = new java.util.HashMap<>();
                
                // Streak actual
                if (currentCharacter != null) {
                    stats.put("streak", currentCharacter.getHealthStreak());
                } else {
                    Map<Integer, Character> chars = CharacterDAO.getCharactersByUser(userId);
                    if (!chars.isEmpty()) {
                        stats.put("streak", chars.values().iterator().next().getHealthStreak());
                    }
                }
                
                // XP semanal (con consulta a BD)
                // Por ahora hardcodeamos, pero se puede mejorar con un DAO
                stats.put("xp", 1250);
                stats.put("missions", 12);
                stats.put("screentime", "42h 30m");
                
                return stats;
            }
        };

        taskStats.setOnSucceeded(e -> {
            Map<String, Object> stats = taskStats.getValue();
            Platform.runLater(() -> {
                if (lblReportStreak != null) lblReportStreak.setText(String.valueOf(stats.get("streak")));
                if (lblReportXP != null) lblReportXP.setText(String.valueOf(stats.get("xp")));
                if (lblReportMissions != null) lblReportMissions.setText(String.valueOf(stats.get("missions")));
                if (lblReportScreenTime != null) lblReportScreenTime.setText(String.valueOf(stats.get("screentime")));
                
                // Animar entrada de valores
                animarLabel(lblReportStreak);
                animarLabel(lblReportXP);
                animarLabel(lblReportMissions);
                animarLabel(lblReportScreenTime);
            });
        });

        // TAREA 2: Gráfica XP semanal
        Task<Void> taskXPChart = new Task<>() {
            @Override
            protected Void call() {
                XYChart.Series<String, Number> serieXP = DashboardDAO.getRendimientoSemanal(userId);
                Platform.runLater(() -> {
                    if (xpWeeklyChart != null) {
                        xpWeeklyChart.getData().clear();
                        xpWeeklyChart.getData().add(serieXP);
                        // Aplicar estilos dinámicos a los puntos
                        if (serieXP.getNode() != null) {
                            serieXP.getNode().setStyle("-fx-stroke: #a335ee; -fx-stroke-width: 3;");
                        }
                    }
                });
                return null;
            }
        };

        // TAREA 3: Uso de aplicaciones (pie chart)
        Task<Void> taskUsagePie = new Task<>() {
            @Override
            protected Void call() {
                List<DashboardDAO.AppUsage> usage = DashboardDAO.getAppUsageSummary(userId);
                Platform.runLater(() -> {
                    if (usageBreakdownPie != null) {
                        usageBreakdownPie.getData().clear();
                        
                        long total = usage.stream().mapToLong(u -> u.seconds).sum();
                        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                        
                        for (DashboardDAO.AppUsage u : usage) {
                            double pct = (double) u.seconds / total * 100.0;
                            String label = u.appName + " (" + (int)pct + "%)";
                            pieData.add(new PieChart.Data(label, u.seconds));
                        }
                        
                        usageBreakdownPie.setData(pieData);
                        
                        // Detalle lateral
                        if (usageBreakdownList != null) {
                            usageBreakdownList.getChildren().clear();
                            for (DashboardDAO.AppUsage u : usage) {
                                Label lbl = new Label("• " + u.appName + " — " + formatSeconds(u.seconds));
                                lbl.setStyle("-fx-text-fill: #ddd; -fx-font-size: 11px;");
                                usageBreakdownList.getChildren().add(lbl);
                            }
                        }
                    }
                });
                return null;
            }
        };

        Thread t1 = new Thread(taskStats); t1.setDaemon(true); t1.start();
        Thread t2 = new Thread(taskXPChart); t2.setDaemon(true); t2.start();
        Thread t3 = new Thread(taskUsagePie); t3.setDaemon(true); t3.start();
    }

    /**
     * Anima la entrada de valores con fade + scale
     */
    private void animarLabel(Label lbl) {
        if (lbl == null) return;
        lbl.setOpacity(0);
        lbl.setScaleX(0.5); lbl.setScaleY(0.5);
        
        FadeTransition ft = new FadeTransition(Duration.millis(500), lbl);
        ft.setToValue(1.0);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(500), lbl);
        st.setToX(1.0); st.setToY(1.0);
        
        ft.play(); st.play();
    }

    /**
     * Exporta el reporte como CSV
     */
    @FXML
    public void exportAsCSV() {
        if (userId == -1) return;
        
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setInitialFileName("reporte_" + System.currentTimeMillis() + ".csv");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Archive", "*.csv"));
            
            java.io.File dest = fc.showSaveDialog(modalStage != null ? modalStage : null);
            if (dest != null) {
                boolean ok = com.ctrlaltquest.dao.MissionsDAO.exportMissionHistoryToCSV(userId, dest);
                mostrarMensaje(ok ? "Éxito" : "Error", ok ? "CSV exportado correctamente." : "No se pudo exportar el CSV.");
            }
        } catch (Exception e) {
            mostrarMensaje("Error", "No se pudo exportar: " + e.getMessage());
        }
    }

    /**
     * Cierra la modal
     */
    @FXML
    public void closeModal() {
        if (modalStage != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(300), modalStage.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> modalStage.close());
            ft.play();
        }
    }

    /**
     * Muestra un mensaje simple
     */
    private void mostrarMensaje(String titulo, String contenido) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.setContentText(contenido);
            a.show();
        });
    }

    /**
     * Formatea segundos en horas:minutos:segundos
     */
    private String formatSeconds(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }
}
