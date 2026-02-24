package com.ctrlaltquest.ui.controllers.views;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

                // Colores vibrantes y coherentes
                String[] colors = {
                    "#FF6B6B", "#4ECDC4", "#FFD93D", "#A335EE", "#FF8C42",
                    "#6BCB77", "#FF006E", "#00D4FF", "#A0FF00", "#FFB6C1"
                };

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                int colorIdx = 0;
                
                for (DashboardDAO.AppUsage u : usage) {
                    double percent = (double) u.seconds / (double) total * 100.0;
                    
                    // MOSTRAR: "App Name (15%)" EN EL GRÁFICO
                    String label = u.appName + "\n(" + (int)percent + "%)";
                    PieChart.Data d = new PieChart.Data(label, u.seconds);
                    pieData.add(d);

                    // PANEL DE DETALLES - MEJORADO CON COLORES Y ANIMACIONES
                    HBox detailRow = new HBox(12);
                    detailRow.setAlignment(Pos.CENTER_LEFT);
                    detailRow.setStyle("-fx-padding: 12 10; -fx-background-radius: 10; -fx-background-color: rgba(0,0,0,0.15);");
                    
                    // Indicador de color con efecto pulsante
                    Region colorDot = new Region();
                    String color = colors[colorIdx % colors.length];
                    colorDot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
                    colorDot.setPrefWidth(12);
                    colorDot.setPrefHeight(12);
                    
                    // Nombre de app
                    Label appName = new Label(u.appName);
                    appName.setStyle("-fx-text-fill: #fff; -fx-font-size: 12px; -fx-font-weight: bold;");
                    appName.setMaxWidth(110);
                    appName.setWrapText(false);
                    appName.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                    
                    // Tiempo y porcentaje
                    Label timeLabel = new Label(formatSeconds(u.seconds));
                    timeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
                    
                    // Barra de progreso visual
                    ProgressBar progressBar = new ProgressBar((double) u.seconds / total);
                    progressBar.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: rgba(0,0,0,0.3);");
                    progressBar.setPrefHeight(4);
                    progressBar.setMaxWidth(100);
                    HBox.setHgrow(progressBar, Priority.SOMETIMES);
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    detailRow.getChildren().addAll(colorDot, appName, spacer, progressBar, timeLabel);
                    
                    // Efecto hover en el panel de detalles con transición suave
                    detailRow.setOnMouseEntered(ev -> {
                        final ScaleTransition scaleHover = new ScaleTransition(Duration.millis(150), detailRow);
                        scaleHover.setToX(1.02);
                        scaleHover.setToY(1.02);
                        scaleHover.play();
                        
                        detailRow.setStyle("-fx-padding: 12 10; -fx-background-radius: 10; -fx-background-color: rgba(" + extraerRGB(color) + ", 0.25); -fx-border-color: " + color + "; -fx-border-radius: 10; -fx-border-width: 1.5;");
                        detailRow.setEffect(new DropShadow(15, Color.web(color, 0.5)));
                    });
                    
                    detailRow.setOnMouseExited(ev -> {
                        final ScaleTransition scaleNormal = new ScaleTransition(Duration.millis(150), detailRow);
                        scaleNormal.setToX(1.0);
                        scaleNormal.setToY(1.0);
                        scaleNormal.play();
                        
                        detailRow.setStyle("-fx-padding: 12 10; -fx-background-radius: 10; -fx-background-color: rgba(0,0,0,0.15);");
                        detailRow.setEffect(null);
                    });
                    
                    usageDetails.getChildren().add(detailRow);
                    colorIdx++;
                }

                if (usagePie != null) {
                    usagePie.setData(pieData);
                    usagePie.setStyle("-fx-font-size: 10px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

                    // APLICAR COLORES Y EFECTOS A LOS SEGMENTOS DEL GRÁFICO
                    colorIdx = 0;
                    for (PieChart.Data d : pieData) {
                        String color = colors[colorIdx % colors.length];
                        Node sliceNode = d.getNode();
                        if (sliceNode != null) {
                            sliceNode.setStyle("-fx-pie-color: " + color + ";");
                            
                            // Animación inicial de entrada - cada slice entra secuencialmente
                            ScaleTransition entradaScale = new ScaleTransition(Duration.millis(700 + colorIdx * 100), sliceNode);
                            entradaScale.setFromX(0.5);
                            entradaScale.setFromY(0.5);
                            entradaScale.setToX(1.0);
                            entradaScale.setToY(1.0);
                            entradaScale.setInterpolator(Interpolator.EASE_OUT);
                            entradaScale.play();
                            
                            // Interactividad: Hover con efectos ESPECTACULARES
                            sliceNode.setOnMouseEntered(ev -> {
                                // Amplificar el segmento de forma suave
                                ScaleTransition hoverScale = new ScaleTransition(Duration.millis(250), sliceNode);
                                hoverScale.setToX(1.15);
                                hoverScale.setToY(1.15);
                                hoverScale.setInterpolator(Interpolator.EASE_OUT);
                                hoverScale.play();
                                
                                // Sombra brillante y dinámica
                                DropShadow brillanteShadow = new DropShadow(40, Color.web(color, 1.0));
                                brillanteShadow.setRadius(25);
                                sliceNode.setEffect(brillanteShadow);
                                
                                // Cursor de mano
                                sliceNode.setCursor(javafx.scene.Cursor.HAND);
                                
                                // Mostrar información en tooltip
                                mostrarTooltipSegmento(d, color);
                            });
                            
                            sliceNode.setOnMouseExited(ev -> {
                                // Volver al tamaño normal suavemente
                                ScaleTransition normalScale = new ScaleTransition(Duration.millis(200), sliceNode);
                                normalScale.setToX(1.0);
                                normalScale.setToY(1.0);
                                normalScale.setInterpolator(Interpolator.EASE_OUT);
                                normalScale.play();
                                
                                sliceNode.setEffect(null);
                                sliceNode.setCursor(javafx.scene.Cursor.DEFAULT);
                            });
                            
                            // Click para mostrar detalles con pulso
                            sliceNode.setOnMouseClicked(ev -> {
                                mostrarTooltipSegmento(d, color);
                                
                                // Efecto de pulso al click
                                ScaleTransition pulse1 = new ScaleTransition(Duration.millis(150), sliceNode);
                                pulse1.setToX(1.20);
                                pulse1.setToY(1.20);
                                pulse1.setOnFinished(finishEvent -> {
                                    ScaleTransition pulse2 = new ScaleTransition(Duration.millis(150), sliceNode);
                                    pulse2.setToX(1.15);
                                    pulse2.setToY(1.15);
                                    pulse2.play();
                                });
                                pulse1.play();
                            });
                        }
                        colorIdx++;
                    }
                }
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

    /**
     * Extrae los valores RGB de un color hex para usarlos en estilos CSS
     */
    private String extraerRGB(String hexColor) {
        String hex = hexColor.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return r + ", " + g + ", " + b;
    }

    /**
     * Muestra un tooltip interactivo al hacer clic en un segmento del gráfico
     * con efectos de animación profesionales
     */
    private void mostrarTooltipSegmento(PieChart.Data data, String color) {
        if (data.getNode() != null) {
            Node node = data.getNode();
            
            // Efecto de bounce personalizado
            ScaleTransition scale1 = new ScaleTransition(Duration.millis(150), node);
            scale1.setToX(1.25);
            scale1.setToY(1.25);
            scale1.setInterpolator(Interpolator.EASE_OUT);
            
            scale1.setOnFinished(e -> {
                ScaleTransition scale2 = new ScaleTransition(Duration.millis(150), node);
                scale2.setToX(1.20);
                scale2.setToY(1.20);
                scale2.play();
            });
            
            scale1.play();
        }
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
        // 🎆 ANIMACIÓN ÉPICA USANDO JAVAFX NATIVO 🎆
        
        // Entrada del nombre con efecto "bounce" personalizado
        if (lblName != null) {
            lblName.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), lblName);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            
            // Sutil bounce: escala ligera para dar "toque" sin exagerar
            ScaleTransition scale1 = new ScaleTransition(Duration.millis(400), lblName);
            scale1.setFromX(0.85);
            scale1.setFromY(0.85);
            scale1.setToX(1.05);
            scale1.setToY(1.05);
            scale1.setInterpolator(Interpolator.EASE_OUT);
            
            Timeline bounceBack = new Timeline(
                new KeyFrame(Duration.millis(420), ev -> {
                    ScaleTransition scale2 = new ScaleTransition(Duration.millis(180), lblName);
                    scale2.setToX(1.0);
                    scale2.setToY(1.0);
                    scale2.play();
                })
            );
            
            fadeIn.play();
            scale1.play();
            bounceBack.play();
        }
        
        // Entrada de eventos con flip y delay
        if (newsContainer != null) {
            newsContainer.setOpacity(0);
            newsContainer.setScaleX(0.5);
            newsContainer.setScaleY(0.5);
            
            Timeline startFlip = new Timeline(
                new KeyFrame(Duration.millis(200), ev -> {
                    FadeTransition fadeNews = new FadeTransition(Duration.millis(400), newsContainer);
                    fadeNews.setFromValue(0.0);
                    fadeNews.setToValue(1.0);
                    fadeNews.setInterpolator(Interpolator.EASE_OUT);
                    
                    ScaleTransition scaleNews = new ScaleTransition(Duration.millis(450), newsContainer);
                    scaleNews.setFromX(0.85);
                    scaleNews.setFromY(0.85);
                    scaleNews.setToX(1.0);
                    scaleNews.setToY(1.0);
                    scaleNews.setInterpolator(Interpolator.EASE_OUT);
                    
                    fadeNews.play();
                    scaleNews.play();
                })
            );
            startFlip.play();
        }
        
        // Entrada del gráfico con rotación + zoom - ESPECTACULAR
        if (chartCard != null) {
            chartCard.setOpacity(0);
            chartCard.setScaleX(0.5);
            chartCard.setScaleY(0.5);
            chartCard.setRotate(0);
            
            Timeline startChart = new Timeline(
                new KeyFrame(Duration.millis(400), ev -> {
                    FadeTransition fadeChart = new FadeTransition(Duration.millis(500), chartCard);
                    fadeChart.setFromValue(0.0);
                    fadeChart.setToValue(1.0);
                    fadeChart.setInterpolator(Interpolator.EASE_OUT);
                    
                    ScaleTransition scaleChart = new ScaleTransition(Duration.millis(500), chartCard);
                    scaleChart.setFromX(0.9);
                    scaleChart.setFromY(0.9);
                    scaleChart.setToX(1.0);
                    scaleChart.setToY(1.0);
                    scaleChart.setInterpolator(Interpolator.EASE_OUT);
                    
                    // Rotación muy leve para dar dinamismo sin exagerar
                    javafx.animation.RotateTransition rotateChart = new javafx.animation.RotateTransition(Duration.millis(500), chartCard);
                    rotateChart.setFromAngle(0);
                    rotateChart.setToAngle(6);
                    rotateChart.setInterpolator(Interpolator.EASE_OUT);
                    
                    fadeChart.play();
                    scaleChart.play();
                    rotateChart.play();
                    
                    // Efecto de brillo más sutil
                    Timeline glowEffect = new Timeline(
                        new KeyFrame(Duration.millis(600), ev2 -> {
                            DropShadow brillanteShadow = new DropShadow(18, Color.rgb(163, 53, 238, 0.65));
                            chartCard.setEffect(brillanteShadow);
                            
                            // Desvanecimiento del brillo
                            Timeline glowFade = new Timeline(
                                new KeyFrame(Duration.millis(300), ev3 -> {
                                    DropShadow normalGlow = new DropShadow(12, Color.rgb(163, 53, 238, 0.35));
                                    chartCard.setEffect(normalGlow);
                                })
                            );
                            glowFade.play();
                        })
                    );
                    glowEffect.play();
                })
            );
            startChart.play();
        }
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
        
        // Animación de escala sutil
        ScaleTransition st = new ScaleTransition(Duration.millis(160), source);
        st.setToX(1.03);
        st.setToY(1.03);
        st.setInterpolator(Interpolator.EASE_OUT);
        
        // Efecto de sombra ligero
        DropShadow shadow = new DropShadow(18, Color.rgb(163, 53, 238, 0.7));
        source.setEffect(shadow);
        
        // Pequeño pulso opcional, menos intenso
        ScaleTransition pulsate = new ScaleTransition(Duration.millis(140), source);
        pulsate.setToX(1.06);
        pulsate.setToY(1.06);
        pulsate.setCycleCount(1);
        pulsate.setAutoReverse(true);
        pulsate.play();
        
        st.play();
    }

    @FXML
    public void animarCardSalida(MouseEvent e) {
        Node source = (Node) e.getSource();
        
        // Volver al tamaño normal
        ScaleTransition st = new ScaleTransition(Duration.millis(160), source);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_IN);
        
        // Sombra normal
        DropShadow normalShadow = new DropShadow(10, Color.rgb(163, 53, 238, 0.35));
        source.setEffect(normalShadow);
        
        st.play();
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
                    ScaleTransition st = new ScaleTransition(Duration.millis(140), dataNode);
                    st.setToX(1.15); st.setToY(1.15); st.play();
                    
                    DropShadow shadow = new DropShadow(10, Color.rgb(255, 71, 87, 0.75));
                    dataNode.setEffect(shadow);
                });
                
                dataNode.setOnMouseExited(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(140), dataNode);
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
        
        lbl.setScaleX(0.85); lbl.setScaleY(0.85);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(350), lbl);
        st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        
        DropShadow shadow = new DropShadow(12, Color.rgb(255, 71, 87, 0.6));
        lbl.setEffect(shadow);
        
        st.play();
        
        // Remover efecto después de la animación
        new Timeline(
            new KeyFrame(Duration.millis(420), ev -> lbl.setEffect(null))
        ).play();
    }
}