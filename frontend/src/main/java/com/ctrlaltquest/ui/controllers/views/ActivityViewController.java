package com.ctrlaltquest.ui.controllers.views;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.ActivityLog;
import com.ctrlaltquest.services.ActivityMonitorService;
import com.ctrlaltquest.ui.controllers.HomeController; // Import necesario

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ActivityViewController {

    @FXML private ToggleButton btnToggleMonitor;
    @FXML private Label lblMonitorStatus;
    @FXML private Button btnCaptureActivity;
    
    // Radar UI
    @FXML private Circle statusIndicator;
    @FXML private Circle radarPulse;
    @FXML private Rectangle decorationLine;
    private ParallelTransition radarAnimation;
    
    // Datos en Vivo
    @FXML private Label lblCurrentWindow;
    @FXML private Label lblProductivity;
    @FXML private Label lblSessionTime;
    @FXML private Label lblSessionXP;
    @FXML private ProgressBar signalBar;

    // Tabla Terminal
    @FXML private TableView<ActivityLog> tableLog;
    @FXML private TableColumn<ActivityLog, String> colTime;
    @FXML private TableColumn<ActivityLog, String> colApp;
    @FXML private TableColumn<ActivityLog, String> colStatus;
    @FXML private TableColumn<ActivityLog, String> colDuration;

    private final ActivityMonitorService monitorService = new ActivityMonitorService();
    private ObservableList<ActivityLog> logs = FXCollections.observableArrayList();
    
    private boolean isTracking = false;
    private Thread trackingThread;
    
    // Variables de sesión
    private long sessionSeconds = 0;
    private int sessionXP = 0;
    private String lastApp = "";
    private final Random random = new Random();
    private int currentUserId = 1;
    
    // ✅ REFERENCIA AL CONTROLADOR PRINCIPAL PARA ACTUALIZAR LA BARRA DE XP
    private HomeController homeController;

    @FXML
    public void initialize() {
        setupTable();
        configurarAnimaciones();
        
        if (btnCaptureActivity != null) {
            btnCaptureActivity.setOnAction(event -> handleCaptureActivity());
        }
        
        stopTrackingUI();
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
    }
    
    // ✅ MÉTODO PARA CONECTAR CON EL HOME
    public void setHomeController(HomeController controller) {
        this.homeController = controller;
    }

    private void setupTable() {
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colApp.setCellValueFactory(new PropertyValueFactory<>("appName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        
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
                        setStyle("-fx-text-fill: #4ade80; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff6b6b; -fx-font-family: 'Consolas';");
                    }
                }
            }
        });
        
        tableLog.setItems(logs);
    }

    private void configurarAnimaciones() {
        ScaleTransition scale = new ScaleTransition(Duration.seconds(2), radarPulse);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(3.0); scale.setToY(3.0);
        
        FadeTransition fade = new FadeTransition(Duration.seconds(2), radarPulse);
        fade.setFromValue(0.6);
        fade.setToValue(0.0);
        
        radarAnimation = new ParallelTransition(scale, fade);
        radarAnimation.setCycleCount(Animation.INDEFINITE);
        
        if (decorationLine != null) {
            TranslateTransition scan = new TranslateTransition(Duration.seconds(3), decorationLine);
            scan.setByX(200);
            scan.setAutoReverse(true);
            scan.setCycleCount(Animation.INDEFINITE);
            scan.play();
        }
    }

    @FXML
    private void handleToggleMonitor() {
        if (btnToggleMonitor.isSelected()) {
            startTracking();
        } else {
            stopTracking();
        }
    }

    @FXML
    private void handleCaptureActivity() {
        // Captura manual instantánea
        String title = monitorService.getActiveWindowTitle();
        boolean productive = monitorService.isProductive(title);
        
        flashEffect(btnCaptureActivity, Color.web("#4ade80"));
        
        // ✅ GUARDAR EN BD MANUALMENTE
        if (productive) {
            UserDAO.otorgarRecompensas(currentUserId, 5, 1); // 5 XP, 1 Moneda
            MissionsDAO.actualizarProgreso(currentUserId, "time_productivity", 60); // Asumimos 1 min
            
            // ✅ AVISAR AL HOME QUE ACTUALICE LA BARRA SUPERIOR
            if (homeController != null) {
                homeController.refreshCharacterData();
            }
        }
        
        String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logs.add(0, new ActivityLog(timeNow, "MANUAL: " + title, productive ? "PRODUCTIVO" : "OCIO", "-"));
    }

    private void startTracking() {
        isTracking = true;
        
        btnToggleMonitor.setText("■ DETENER RASTREO");
        btnToggleMonitor.setStyle("-fx-background-color: rgba(255, 68, 68, 0.2); -fx-text-fill: #ff6b6b; -fx-border-color: #ff6b6b;");
        
        lblMonitorStatus.setText(">> SISTEMA ONLINE <<");
        lblMonitorStatus.setStyle("-fx-text-fill: #4ade80; -fx-effect: dropshadow(three-pass-box, rgba(74,222,128,0.6), 10, 0, 0, 0);");
        
        statusIndicator.setFill(Color.web("#4ade80"));
        statusIndicator.setEffect(new Glow(1.0));
        radarAnimation.play();

        trackingThread = new Thread(this::trackingLoop);
        trackingThread.setDaemon(true);
        trackingThread.start();
    }

    private void trackingLoop() {
        while (isTracking) {
            try {
                String title = monitorService.getActiveWindowTitle();
                boolean productive = monitorService.isProductive(title);
                String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                // ✅ 1. ACTUALIZAR BASE DE DATOS (XP y Misiones)
                // Hacemos esto fuera del Platform.runLater para no bloquear la UI
                if (productive) {
                    // Cada ciclo es de 1 segundo aprox.
                    // Acumulamos: Damos XP cada 5 segundos para no saturar BD
                    if (sessionSeconds % 5 == 0) {
                        // 2 XP cada 5 segs
                        UserDAO.otorgarRecompensas(currentUserId, 2, 0); 
                        // 5 segundos de progreso en misiones de productividad
                        MissionsDAO.actualizarProgreso(currentUserId, "time_productivity", 5);
                        
                        // Actualizar variable local visual
                        sessionXP += 2;
                        
                        // ✅ AVISAR AL HOME QUE ACTUALICE LA BARRA SUPERIOR
                        if (homeController != null) {
                            homeController.refreshCharacterData();
                        }
                    }
                }

                // ✅ 2. ACTUALIZAR UI
                Platform.runLater(() -> {
                    updateUI(title, productive);
                    updateSignal();
                    
                    if (!title.equals(lastApp)) {
                        logs.add(0, new ActivityLog(timeNow, title, productive ? "PRODUCTIVO" : "OCIO", "Detectado"));
                        if (logs.size() > 100) logs.remove(logs.size() - 1);
                        lastApp = title;
                    }
                    
                    sessionSeconds++;
                    long hh = sessionSeconds / 3600;
                    long mm = (sessionSeconds % 3600) / 60;
                    long ss = sessionSeconds % 60;
                    lblSessionTime.setText(String.format("%02d:%02d:%02d", hh, mm, ss));
                    
                    if (productive && sessionSeconds % 5 == 0) {
                        lblSessionXP.setText("+" + sessionXP + " XP");
                        flashLabel(lblSessionXP);
                    }
                });

                Thread.sleep(1000);
            } catch (InterruptedException e) { break; }
        }
    }

    private void stopTracking() {
        isTracking = false;
        stopTrackingUI();
        if(radarAnimation != null) radarAnimation.stop();
        if(radarPulse != null) radarPulse.setOpacity(0);
    }

    private void stopTrackingUI() {
        if(btnToggleMonitor != null) {
            btnToggleMonitor.setText("▶ INICIAR RASTREO");
            btnToggleMonitor.setStyle("-fx-background-color: rgba(74, 222, 128, 0.2); -fx-text-fill: #4ade80; -fx-border-color: #4ade80;");
        }
        if(lblMonitorStatus != null) {
            lblMonitorStatus.setText("SISTEMA EN ESPERA");
            lblMonitorStatus.setStyle("-fx-text-fill: #888;");
        }
        if(statusIndicator != null) {
            statusIndicator.setFill(Color.GRAY);
            statusIndicator.setEffect(null);
        }
        if(signalBar != null) signalBar.setProgress(0);
        if(lblCurrentWindow != null) lblCurrentWindow.setText("---");
        if(lblProductivity != null) {
            lblProductivity.setText("OFFLINE");
            lblProductivity.setStyle("-fx-background-color: #333; -fx-text-fill: #777; -fx-background-radius: 4;");
        }
    }

    private void updateUI(String title, boolean productive) {
        if(lblCurrentWindow != null) {
            lblCurrentWindow.setText(title.length() > 40 ? title.substring(0, 40) + "..." : title);
        }
        if(lblProductivity != null) {
            if (productive) {
                lblProductivity.setText("PRODUCTIVO");
                lblProductivity.setStyle("-fx-background-color: rgba(74, 222, 128, 0.2); -fx-text-fill: #4ade80; -fx-border-color: #4ade80; -fx-border-radius: 4;");
            } else {
                lblProductivity.setText("OCIO / NEUTRAL");
                lblProductivity.setStyle("-fx-background-color: rgba(255, 107, 107, 0.2); -fx-text-fill: #ff6b6b; -fx-border-color: #ff6b6b; -fx-border-radius: 4;");
            }
        }
    }
    
    private void updateSignal() {
        double noise = 0.4 + (random.nextDouble() * 0.6); 
        if(signalBar != null) signalBar.setProgress(noise);
    }
    
    private void flashEffect(Node node, Color color) {
        DropShadow glow = new DropShadow(20, color);
        node.setEffect(glow);
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> node.setEffect(null));
        pause.play();
    }
    
    private void flashLabel(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), label);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.2); st.setToY(1.2);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}