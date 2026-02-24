package com.ctrlaltquest.ui.controllers.views;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.dao.DashboardDAO;
import com.ctrlaltquest.models.Character;

import javafx.animation.FadeTransition;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
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
    @FXML private ImageView ivProfile;
    @FXML private Label lblName;
    @FXML private Label lblClass;
    @FXML private Label lblDate;
    @FXML private VBox activeMissionsContainer;
    @FXML private PieChart usagePie;
    @FXML private VBox usageDetails;
    @FXML private LineChart<String, Number> streakChart;
    @FXML private Label lblStreakNumber;
    @FXML private javafx.scene.control.Button btnRefreshStreak;

    @FXML private VBox reportHeaderBox;
    @FXML private javafx.scene.control.Button btnExportReport;
    @FXML private javafx.scene.control.Button btnOpenReport;
    
    // Contenedores principales para animar
    @FXML private HBox newsContainer;
    @FXML private VBox missionsCard;
    @FXML private VBox chartCard;

    private int userId = -1;
    private Character currentCharacter;

    public void setUserId(int userId) {
        this.userId = userId;
        cargarNombrePersonaje();
        cargarDatosDashboard();
    }

    @FXML
    public void refreshStreakData() {
        if (userId == -1) return;

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Obtener serie de rendimiento
                    javafx.scene.chart.XYChart.Series<String, Number> serie = DashboardDAO.getRendimientoSemanal(userId);
                    Platform.runLater(() -> {
                        try {
                            if (streakChart != null) {
                                streakChart.getData().clear();
                                streakChart.getData().add(serie);
                                streakChart.setLegendVisible(false);
                                
                                // 🎨 Aplicar estilos dinámicos a la gráfica
                                aplicarEstilosStreakChart(serie);
                            }

                            // Actualizar número de racha desde CharacterDAO
                            Map<Integer, Character> chars = CharacterDAO.getCharactersByUser(userId);
                            if (!chars.isEmpty()) {
                                Character c = chars.values().iterator().next();
                                if (lblStreakNumber != null) {
                                    lblStreakNumber.setText(String.valueOf(c.getHealthStreak()));
                                    // Animar la actualización del número
                                    animarNumeroRacha(lblStreakNumber);
                                }
                            }
                        } catch (Exception ex) {}
                    });
                } catch (Exception ex) {
                    // ignore
                }
                return null;
            }
        };
        Thread th = new Thread(t); th.setDaemon(true); th.start();
    }

    /**
     * Recibe el objeto Character desde HomeController para mostrar datos inmediatamente
     */
    public void setPlayerData(Character character) {
        this.currentCharacter = character;
        if (character != null && lblName != null) {
            Platform.runLater(() -> {
                // Nombre principal
                typewriterEffect(lblName, character.getName().toUpperCase(), 30);
                lblClass.setText("CLASE: " + (character.getClassId() > 0 ? "CLASE_" + character.getClassId() : "--"));
                // Cargar avatar basado en clase o skin
                cargarAvatarPreview(character);
                animarEntradaDashboard();
            });
        }
    }

    @FXML
    public void initialize() {
        // Ocultar elementos iniciales para la entrada dramática
        if(newsContainer != null) newsContainer.setOpacity(0);
        if(missionsCard != null) missionsCard.setOpacity(0);
        if(chartCard != null) chartCard.setOpacity(0);
        
        if(lblName != null) lblName.setText("SINTONIZANDO...");
        if (lblDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d MMM");
            lblDate.setText(LocalDate.now().format(fmt).toUpperCase());
        }
        
        // Evitar animaciones por defecto de JavaFX que chocan con las nuestras
        if (usagePie != null) {
            usagePie.setAnimated(false);
        }
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
            if(lblName != null) typewriterEffect(lblName, "HOLA DE NUEVO, " + taskName.getValue(), 50);
            animarEntradaDashboard();
        });

        Thread th = new Thread(taskName);
        th.setDaemon(true); // Permite que la app cierre correctamente
        th.start();
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

        // Tarea Resumen de uso de aplicaciones (PieChart)
        Task<List<DashboardDAO.AppUsage>> taskUsage = new Task<>() {
            @Override
            protected List<DashboardDAO.AppUsage> call() {
                return DashboardDAO.getAppUsageSummary(userId);
            }
        };

        taskUsage.setOnSucceeded(e -> {
            List<DashboardDAO.AppUsage> usage = taskUsage.getValue();
            Platform.runLater(() -> {
                if (usagePie != null) usagePie.getData().clear();
                if (usageDetails != null) usageDetails.getChildren().clear();

                long total = usage.stream().mapToLong(u -> u.seconds).sum();
                if (total == 0) {
                    Label empty = new Label("Sin uso capturado hoy.");
                    empty.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
                    if (usageDetails != null) usageDetails.getChildren().add(empty);
                    return;
                }

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                for (DashboardDAO.AppUsage u : usage) {
                    double percent = (double) u.seconds / (double) total * 100.0;
                    String label = u.appName + " (" + (int)percent + "%)";
                    PieChart.Data d = new PieChart.Data(label, u.seconds);
                    pieData.add(d);

                    // Detalle lateral
                    Label detail = new Label(u.appName + " — " + formatSeconds(u.seconds));
                    detail.setStyle("-fx-text-fill: #ddd; -fx-font-size: 12px;");
                    usageDetails.getChildren().add(detail);
                }

                if (usagePie != null) usagePie.setData(pieData);

                // Interactividad: al hacer hover mostrar tooltip con tiempo
                pieData.forEach(d -> {
                    d.getNode().setOnMouseEntered(ev -> d.getNode().setScaleX(1.05));
                    d.getNode().setOnMouseExited(ev -> d.getNode().setScaleX(1.0));
                });
            });
        });

        Thread t1 = new Thread(taskMisiones); t1.setDaemon(true); t1.start();
        Thread t3 = new Thread(taskUsage); t3.setDaemon(true); t3.start();

        // Cargar racha semanal (widget interactivo)
        refreshStreakData();
    }

    @FXML
    public void handleExportCSVReport() {
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV files", "*.csv"));
            java.io.File dest = fc.showSaveDialog(null);
            if (dest == null) return;

            boolean ok = com.ctrlaltquest.dao.MissionsDAO.exportMissionHistoryToCSV(userId, dest);
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(ok ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR);
                a.setTitle(ok ? "Exportado" : "Error");
                a.setHeaderText(null);
                a.setContentText(ok ? "CSV exportado correctamente." : "No se pudo exportar el CSV.");
                a.show();
            });
        } catch (Exception e) {
            // mostrar error
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                a.setTitle("Error"); a.setHeaderText(null); a.setContentText("No se pudo exportar: " + e.getMessage()); a.show();
            });
        }
    }

    @FXML
    public void handleOpenReport() {
        abrirReportModal();
    }

    /**
     * Abre la modal de reporte completa con gráficas interactivas
     */
    private void abrirReportModal() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/views/report_modal.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ReportModalController reportCtrl = loader.getController();
            reportCtrl.setData(userId, currentCharacter);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Reporte Semanal Completo");
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.initStyle(javafx.stage.StageStyle.DECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            reportCtrl.setStage(stage);
            
            // Aplicar estilos
            try {
                String css = getClass().getResource("/styles/home.css").toExternalForm();
                root.getStylesheets().add(css);
            } catch (Exception ex) {}
            
            stage.show();
        } catch (Exception e) {
            System.err.println("Error abriendo reporte modal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- GENERACIÓN DE UI DINÁMICA ---

    private void mostrarMensajeVacio() {
        Label empty = new Label("No hay misiones activas.\n¡Ve al tablón y acepta un desafío!");
        empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-text-alignment: CENTER; -fx-font-size: 13px;");
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(20, 0, 20, 0));
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

        // Interactividad Hover de Misión
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
        // Ya no aplicamos estilizado específico a la antigua BarChart
    }

    private String formatSeconds(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }

    private void cargarAvatarPreview(Character c) {
        try {
            String path = "/assets/images/sprites/class_" + c.getClassId() + "_idle.png";
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                path = "/assets/images/sprites/class_" + c.getClassId() + ".png";
                url = getClass().getResource(path);
            }
            if (url == null) url = getClass().getResource("/assets/images/sprites/class_1_idle.png");
            if (url != null && ivProfile != null) ivProfile.setImage(new Image(url.toExternalForm()));
        } catch (Exception ex) {
            // no crítico
        }
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
        ScaleTransition st = new ScaleTransition(Duration.millis(200), source);
        st.setToX(1.04); st.setToY(1.04); st.play();
        source.setEffect(new DropShadow(25, Color.rgb(163, 53, 238, 0.6)));
    }

    @FXML
    public void animarCardSalida(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), source);
        st.setToX(1.0); st.setToY(1.0); st.play();
        source.setEffect(null);
    }

    /**
     * Aplica estilos dinámicos y animaciones al streak chart (línea con brillo)
     */
    private void aplicarEstilosStreakChart(XYChart.Series<String, Number> serie) {
        if (serie == null || serie.getNode() == null) return;
        
        Node serieNode = serie.getNode();
        serieNode.setStyle("-fx-stroke: #ff4757; -fx-stroke-width: 3;");
        
        // Efecto de brillo en la línea
        DropShadow glow = new DropShadow(15, Color.rgb(255, 71, 87, 0.7));
        serieNode.setEffect(glow);
        
        // Animar los puntos de datos (si existen)
        for (XYChart.Data<String, Number> data : serie.getData()) {
            Node dataNode = data.getNode();
            if (dataNode != null) {
                dataNode.setStyle("-fx-padding: 5;");
                
                // Círculos con efecto hover
                dataNode.setOnMouseEntered(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), dataNode);
                    st.setToX(1.3); st.setToY(1.3); st.play();
                    
                    DropShadow shadow = new DropShadow(10, Color.rgb(255, 71, 87, 0.9));
                    dataNode.setEffect(shadow);
                });
                
                dataNode.setOnMouseExited(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), dataNode);
                    st.setToX(1.0); st.setToY(1.0); st.play();
                    dataNode.setEffect(null);
                });
            }
        }
    }

    /**
     * Anima el número de racha con escala y brillo
     */
    private void animarNumeroRacha(Label lbl) {
        if (lbl == null) return;
        
        lbl.setScaleX(0.7); lbl.setScaleY(0.7);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(600), lbl);
        st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        
        DropShadow shadow = new DropShadow(20, Color.rgb(255, 71, 87, 0.8));
        lbl.setEffect(shadow);
        
        st.play();
        
        // Remover efecto después de la animación
        new Timeline(
            new KeyFrame(Duration.millis(650), e -> lbl.setEffect(null))
        ).play();
    }
}