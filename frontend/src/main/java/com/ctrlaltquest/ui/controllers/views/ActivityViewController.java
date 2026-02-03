package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.models.ActivityLog;
import com.ctrlaltquest.services.ActivityMonitorService;
import com.ctrlaltquest.services.MissionProgressService;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class ActivityViewController {

    @FXML private ToggleButton btnToggleMonitor;
    @FXML private Label lblMonitorStatus;
    @FXML private Button btnCaptureActivity;  // 🎯 NUEVO - Botón para capturar actividad
    
    // Radar UI
    @FXML private Circle statusIndicator;
    @FXML private Circle radarPulse;
    private ParallelTransition radarAnimation;
    
    // Datos en Vivo
    @FXML private Label lblCurrentWindow;
    @FXML private Label lblProductivity;
    @FXML private Label lblSessionTime;
    @FXML private Label lblSessionXP;
    @FXML private ProgressBar signalBar;

    // Tabla
    @FXML private TableView<ActivityLog> tableLog;
    @FXML private TableColumn<ActivityLog, String> colTime;
    @FXML private TableColumn<ActivityLog, String> colApp;
    @FXML private TableColumn<ActivityLog, String> colStatus;
    @FXML private TableColumn<ActivityLog, String> colDuration;

    private final ActivityMonitorService monitorService = new ActivityMonitorService();
    private final MissionProgressService missionService = MissionProgressService.getInstance();  // 🎯 NUEVO
    private ObservableList<ActivityLog> logs = FXCollections.observableArrayList();
    private boolean isTracking = false;
    private Thread trackingThread;
    private long sessionSeconds = 0;
    private String lastApp = "";
    private final Random random = new Random();
    private int currentUserId = 1; // 🎯 NUEVO - Se asignará desde HomeController

    @FXML
    public void initialize() {
        setupTable();
        configurarAnimacionRadar();
        
        // 🎯 NUEVO - Configurar botón de captura manual
        if (btnCaptureActivity != null) {
            btnCaptureActivity.setOnAction(event -> handleCaptureActivity());
        }
        
        // Estado inicial
        stopTrackingUI();
    }

    private void setupTable() {
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colApp.setCellValueFactory(new PropertyValueFactory<>("appName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        
        // Estilo de celda personalizado para el estatus (Colores)
        colStatus.setCellFactory(column -> new TableCell<ActivityLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("PRODUCTIVO")) {
                        setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff6b6b;");
                    }
                }
            }
        });
        
        tableLog.setItems(logs);
    }

    private void configuringRadarAnimation() {
        // Efecto de onda expansiva
        ScaleTransition scale = new ScaleTransition(Duration.seconds(1.5), radarPulse);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(2.5); scale.setToY(2.5);
        
        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), radarPulse);
        fade.setFromValue(0.8);
        fade.setToValue(0.0);
        
        radarAnimation = new ParallelTransition(scale, fade);
        radarAnimation.setCycleCount(ParallelTransition.INDEFINITE);
    }

    @FXML
    private void handleToggleMonitor() {
        if (btnToggleMonitor.isSelected()) {
            startTracking();
        } else {
            stopTracking();
        }
    }
    
    /**
     * 🎯 NUEVO - Captura actividad manualmente cuando el usuario presiona el botón.
     * Obtiene la app actual y envía a MissionProgressService.
     */
    @FXML
    private void handleCaptureActivity() {
        String title = monitorService.getActiveWindowTitle();
        boolean productive = monitorService.isProductive(title);
        
        System.out.println("📸 [ActivityViewController] Capturando actividad manualmente");
        System.out.println("   └─ App: " + title);
        System.out.println("   └─ Productiva: " + productive);
        
        // Enviar a MissionProgressService
        missionService.captureActivityManual(currentUserId, title, productive);
        
        // Feedback visual
        btnCaptureActivity.setStyle("-fx-background-color: #4ade80;");
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> btnCaptureActivity.setStyle(""));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 🎯 NUEVO - Setter para userId (llamado desde HomeController)
     */
    public void setUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("✅ [ActivityViewController] userId seteado: " + userId);
    }

    private void startTracking() {
        isTracking = true;
        
        // UI Updates
        btnToggleMonitor.setText("DETENER SECUENCIA");
        btnToggleMonitor.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
        lblMonitorStatus.setText("MONITOREO ACTIVO - REGISTRANDO DATOS");
        lblMonitorStatus.setStyle("-fx-text-fill: #4ade80;");
        
        statusIndicator.setFill(Color.web("#4ade80"));
        statusIndicator.setEffect(new Glow(0.8));
        radarAnimation.play();

        // Hilo de monitoreo
        trackingThread = new Thread(() -> {
            while (isTracking) {
                try {
                    // 1. Obtener datos reales
                    String title = monitorService.getActiveWindowTitle();
                    boolean productive = monitorService.isProductive(title);
                    String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    Platform.runLater(() -> {
                        updateUI(title, productive);
                        updateSignal(); // Efecto visual
                        
                        // Solo agregamos log si cambia la ventana
                        if (!title.equals(lastApp)) {
                            logs.add(0, new ActivityLog(timeNow, title, productive ? "PRODUCTIVO" : "OCIO", "En curso"));
                            // Limitar logs para no saturar memoria
                            if (logs.size() > 50) logs.remove(logs.size() - 1);
                            lastApp = title;
                        }
                        
                        // Cronómetro
                        sessionSeconds++;
                        long mm = (sessionSeconds % 3600) / 60;
                        long ss = sessionSeconds % 60;
                        lblSessionTime.setText(String.format("%02d:%02d", mm, ss));
                    });

                    Thread.sleep(1000);
                } catch (InterruptedException e) { break; }
            }
        });
        trackingThread.setDaemon(true);
        trackingThread.start();
    }

    private void stopTracking() {
        isTracking = false;
        stopTrackingUI();
        if (radarAnimation != null) radarAnimation.stop();
        radarPulse.setOpacity(0);
    }

    private void stopTrackingUI() {
        btnToggleMonitor.setText("INICIAR SECUENCIA");
        btnToggleMonitor.getStyleClass().remove("btn-danger");
        btnToggleMonitor.setStyle(""); // Reset al estilo CSS original
        
        lblMonitorStatus.setText("SISTEMA EN ESPERA");
        lblMonitorStatus.setStyle("-fx-text-fill: #888;");
        
        statusIndicator.setFill(Color.GRAY);
        statusIndicator.setEffect(null);
        signalBar.setProgress(0);
        
        lblCurrentWindow.setText("---");
        lblProductivity.setText("INACTIVO");
        lblProductivity.setStyle("-fx-background-color: #444; -fx-text-fill: #aaa;");
    }

    private void updateUI(String title, boolean productive) {
        lblCurrentWindow.setText(title.length() > 35 ? title.substring(0, 35) + "..." : title);
        
        if (productive) {
            lblProductivity.setText("PRODUCTIVO (+XP)");
            lblProductivity.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: #4ade80; -fx-border-color: #4ade80;");
        } else {
            lblProductivity.setText("OCIO / NEUTRAL");
            lblProductivity.setStyle("-fx-background-color: #5a2d2d; -fx-text-fill: #ff6b6b; -fx-border-color: #ff6b6b;");
        }
    }
    
    // Simula actividad de red/cpu en la barra
    private void updateSignal() {
        double noise = 0.3 + (random.nextDouble() * 0.7); // Valor entre 0.3 y 1.0
        signalBar.setProgress(noise);
    }
    
    // Helper para corregir el nombre del método en FXML si copiaste mal
    private void configurarAnimacionRadar() {
        configuringRadarAnimation(); 
    }
}