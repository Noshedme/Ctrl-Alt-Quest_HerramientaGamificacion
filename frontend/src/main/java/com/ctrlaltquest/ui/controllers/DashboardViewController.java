package com.ctrlaltquest.ui.controllers.views;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.dao.DashboardDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.ui.utils.Toast;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

    @FXML private javafx.scene.layout.BorderPane mainLayout;

    // Contenedores principales para animar
    @FXML private HBox newsContainer;
    @FXML private VBox missionsCard;
    @FXML private VBox chartCard;

    // Recuadros expandibles
    @FXML private VBox chartCardContainer;
    @FXML private VBox usageCardContainer;
    @FXML private javafx.scene.control.Button btnExpandChart;
    @FXML private javafx.scene.control.Button btnExpandList;

    private int userId = -1;
    private Character currentCharacter;
    private List<DashboardDAO.AppUsage> currentUsageData;

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
                    XYChart.Series<String, Number> serie = DashboardDAO.getRendimientoSemanal(userId);
                    Platform.runLater(() -> {
                        try {
                            if (streakChart != null) {
                                streakChart.getData().clear();
                                streakChart.getData().add(serie);
                                streakChart.setLegendVisible(false);
                                aplicarEstilosStreakChart(serie);
                            }

                            Map<Integer, Character> chars = CharacterDAO.getCharactersByUser(userId);
                            if (!chars.isEmpty()) {
                                Character c = chars.values().iterator().next();
                                if (lblStreakNumber != null) {
                                    lblStreakNumber.setText(String.valueOf(c.getHealthStreak()));
                                    animarNumeroRacha(lblStreakNumber);
                                }
                            }
                            Toast.info("Actualizado", "Datos de racha actualizados.");
                        } catch (Exception ex) {}
                    });
                } catch (Exception ex) {}
                return null;
            }
        };
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    public void setPlayerData(Character character) {
        this.currentCharacter = character;
        if (character != null && lblName != null) {
            Platform.runLater(() -> {
                typewriterEffect(lblName, character.getName().toUpperCase(), 30);
                lblClass.setText("CLASE: " + (character.getClassId() > 0 ? "CLASE_" + character.getClassId() : "--"));
                cargarAvatarPreview(character);
                animarEntradaDashboard();
            });
        }
    }

    @FXML
    public void initialize() {
        if (newsContainer != null) newsContainer.setOpacity(0);
        if (chartCard != null) chartCard.setOpacity(0);

        if (lblName != null) lblName.setText("SINTONIZANDO...");
        if (lblDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d MMM");
            lblDate.setText(LocalDate.now().format(fmt).toUpperCase());
        }

        try {
            StackPane root = (StackPane) mainLayout.getScene().getRoot();
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            Toast.initialize(toastContainer);
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar Toast: " + e.getMessage());
        }

        if (usagePie != null) {
            usagePie.setAnimated(false);
        }

        // Aplicar clip circular al avatar una vez que el nodo está listo
        if (ivProfile != null) {
            ivProfile.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) aplicarClipCircular();
            });
            // También intentar aplicar inmediatamente por si ya hay escena
            aplicarClipCircular();
        }
    }

    /**
     * Recorta el ImageView en círculo para que la imagen no sobresalga
     * del borde redondeado del StackPane contenedor.
     */
    private void aplicarClipCircular() {
        if (ivProfile == null) return;
        double radio = 42.5; // Mitad del fitWidth/fitHeight (85px / 2)
        Circle clip = new Circle(radio, radio, radio);
        ivProfile.setClip(clip);
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
            if (lblName != null) typewriterEffect(lblName, "HOLA DE NUEVO, " + taskName.getValue(), 50);
            animarEntradaDashboard();
        });

        Thread th = new Thread(taskName);
        th.setDaemon(true);
        th.start();
    }

    private void cargarDatosDashboard() {
        if (userId == -1) return;

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

        Task<List<DashboardDAO.AppUsage>> taskUsage = new Task<>() {
            @Override
            protected List<DashboardDAO.AppUsage> call() {
                return DashboardDAO.getAppUsageSummary(userId);
            }
        };

        taskUsage.setOnSucceeded(e -> {
            List<DashboardDAO.AppUsage> usage = taskUsage.getValue();
            currentUsageData = usage;
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

                String[] colors = {
                    "#FF3D3D", "#00C9B1", "#FFB800", "#C840FF", "#FF6000",
                    "#00E676", "#FF0066", "#00B0FF", "#AAFF00", "#FF80AB"
                };

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                int colorIdx = 0;

                for (DashboardDAO.AppUsage u : usage) {
                    double percent = (double) u.seconds / (double) total * 100.0;
                    String label = u.appName + "\n(" + (int) percent + "%)";
                    PieChart.Data d = new PieChart.Data(label, u.seconds);
                    pieData.add(d);

                    HBox detailRow = new HBox(12);
                    detailRow.setAlignment(Pos.CENTER_LEFT);
                    detailRow.setStyle("-fx-padding: 12 10; -fx-background-radius: 10; -fx-background-color: rgba(0,0,0,0.15);");

                    Region colorDot = new Region();
                    String color = colors[colorIdx % colors.length];
                    colorDot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
                    colorDot.setPrefWidth(12);
                    colorDot.setPrefHeight(12);

                    Label appName = new Label(u.appName);
                    appName.setStyle("-fx-text-fill: #fff; -fx-font-size: 12px; -fx-font-weight: bold;");
                    appName.setMaxWidth(110);
                    appName.setWrapText(false);
                    appName.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

                    Label timeLabel = new Label(formatSeconds(u.seconds));
                    timeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");

                    ProgressBar progressBar = new ProgressBar((double) u.seconds / total);
                    progressBar.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: rgba(0,0,0,0.3);");
                    progressBar.setPrefHeight(4);
                    progressBar.setMaxWidth(100);
                    HBox.setHgrow(progressBar, Priority.SOMETIMES);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    detailRow.getChildren().addAll(colorDot, appName, spacer, progressBar, timeLabel);

                    final String finalColor = color;
                    detailRow.setOnMouseEntered(ev -> {
                        ScaleTransition scaleHover = new ScaleTransition(Duration.millis(150), detailRow);
                        scaleHover.setToX(1.02);
                        scaleHover.setToY(1.02);
                        scaleHover.play();
                        detailRow.setStyle("-fx-padding: 12 10; -fx-background-radius: 10; -fx-background-color: rgba(" + extraerRGB(finalColor) + ", 0.25); -fx-border-color: " + finalColor + "; -fx-border-radius: 10; -fx-border-width: 1.5;");
                        detailRow.setEffect(new DropShadow(15, Color.web(finalColor, 0.5)));
                    });

                    detailRow.setOnMouseExited(ev -> {
                        ScaleTransition scaleNormal = new ScaleTransition(Duration.millis(150), detailRow);
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
                    usagePie.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");

                    colorIdx = 0;
                    for (PieChart.Data d : pieData) {
                        String color = colors[colorIdx % colors.length];
                        Node sliceNode = d.getNode();
                        if (sliceNode != null) {
                            sliceNode.setStyle("-fx-pie-color: " + color + ";");

                            ScaleTransition entradaScale = new ScaleTransition(Duration.millis(700 + colorIdx * 100), sliceNode);
                            entradaScale.setFromX(0.5);
                            entradaScale.setFromY(0.5);
                            entradaScale.setToX(1.0);
                            entradaScale.setToY(1.0);
                            entradaScale.setInterpolator(Interpolator.EASE_OUT);
                            entradaScale.play();

                            final String finalColor = color;
                            sliceNode.setOnMouseEntered(ev -> {
                                ScaleTransition hoverScale = new ScaleTransition(Duration.millis(250), sliceNode);
                                hoverScale.setToX(1.15);
                                hoverScale.setToY(1.15);
                                hoverScale.setInterpolator(Interpolator.EASE_OUT);
                                hoverScale.play();
                                DropShadow brillanteShadow = new DropShadow(40, Color.web(finalColor, 1.0));
                                brillanteShadow.setRadius(25);
                                sliceNode.setEffect(brillanteShadow);
                                sliceNode.setCursor(javafx.scene.Cursor.HAND);
                                mostrarTooltipSegmento(d, finalColor);
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

                            sliceNode.setOnMouseClicked(ev -> {
                                mostrarTooltipSegmento(d, finalColor);
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

                    // Estilos oscuros para leyenda y etiquetas del PieChart
                    Platform.runLater(() -> {
                        usagePie.lookupAll(".chart-pie-label").forEach(n ->
                            n.setStyle("-fx-fill: #e0e0e0; -fx-font-size: 10px; -fx-font-weight: bold;")
                        );
                        usagePie.lookupAll(".chart-legend").forEach(n ->
                            n.setStyle("-fx-background-color: transparent;")
                        );
                    });
                }
            });
        });

        Thread t1 = new Thread(taskMisiones); t1.setDaemon(true); t1.start();
        Thread t3 = new Thread(taskUsage); t3.setDaemon(true); t3.start();
        refreshStreakData();
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
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 10;");

        Label icon = new Label("●");
        String diffColor = switch (m.dificultad) {
            case "DIFICIL" -> "#ff6b6b";
            case "MEDIA" -> "#f7d27a";
            default -> "#4ade80";
        };
        icon.setStyle("-fx-text-fill: " + diffColor + "; -fx-font-size: 14px;");

        VBox textBox = new VBox(2);
        Label lblTitle = new Label(m.titulo);
        lblTitle.setStyle("-fx-text-fill: #eeeeee; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label lblSub = new Label(m.dificultad);
        lblSub.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
        textBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER_RIGHT);

        ProgressBar pb = new ProgressBar(m.progreso);
        pb.setPrefWidth(100);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent: " + diffColor + "; -fx-control-inner-background: rgba(0,0,0,0.5);");

        Label lblPct = new Label((int) (m.progreso * 100) + "%");
        lblPct.setStyle("-fx-text-fill: " + diffColor + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        progressBox.getChildren().addAll(lblPct, pb);
        row.getChildren().addAll(icon, textBox, spacer, progressBox);

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

    private void mostrarTooltipSegmento(PieChart.Data data, String color) {
        if (data.getNode() != null) {
            Node node = data.getNode();
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
            if (url != null && ivProfile != null) {
                ivProfile.setImage(new Image(url.toExternalForm()));
                // Re-aplicar clip tras cargar imagen
                aplicarClipCircular();
            }
        } catch (Exception ex) {
            // no crítico
        }
    }

    // --- ANIMACIONES ---

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
        if (lblName != null) {
            lblName.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), lblName);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale1 = new ScaleTransition(Duration.millis(400), lblName);
            scale1.setFromX(0.85); scale1.setFromY(0.85);
            scale1.setToX(1.05); scale1.setToY(1.05);
            scale1.setInterpolator(Interpolator.EASE_OUT);

            new Timeline(new KeyFrame(Duration.millis(420), ev -> {
                ScaleTransition scale2 = new ScaleTransition(Duration.millis(180), lblName);
                scale2.setToX(1.0); scale2.setToY(1.0); scale2.play();
            })).play();

            fadeIn.play(); scale1.play();
        }

        if (newsContainer != null) {
            newsContainer.setOpacity(0);
            newsContainer.setScaleX(0.5); newsContainer.setScaleY(0.5);
            new Timeline(new KeyFrame(Duration.millis(200), ev -> {
                FadeTransition fadeNews = new FadeTransition(Duration.millis(400), newsContainer);
                fadeNews.setFromValue(0.0); fadeNews.setToValue(1.0);
                fadeNews.setInterpolator(Interpolator.EASE_OUT);

                ScaleTransition scaleNews = new ScaleTransition(Duration.millis(450), newsContainer);
                scaleNews.setFromX(0.85); scaleNews.setFromY(0.85);
                scaleNews.setToX(1.0); scaleNews.setToY(1.0);
                scaleNews.setInterpolator(Interpolator.EASE_OUT);

                fadeNews.play(); scaleNews.play();
            })).play();
        }

        if (chartCard != null) {
            chartCard.setOpacity(0);
            chartCard.setScaleX(0.5); chartCard.setScaleY(0.5);
            new Timeline(new KeyFrame(Duration.millis(400), ev -> {
                FadeTransition fadeChart = new FadeTransition(Duration.millis(500), chartCard);
                fadeChart.setFromValue(0.0); fadeChart.setToValue(1.0);
                fadeChart.setInterpolator(Interpolator.EASE_OUT);

                ScaleTransition scaleChart = new ScaleTransition(Duration.millis(500), chartCard);
                scaleChart.setFromX(0.9); scaleChart.setFromY(0.9);
                scaleChart.setToX(1.0); scaleChart.setToY(1.0);
                scaleChart.setInterpolator(Interpolator.EASE_OUT);

                fadeChart.play(); scaleChart.play();

                new Timeline(new KeyFrame(Duration.millis(600), ev2 -> {
                    DropShadow brillanteShadow = new DropShadow(18, Color.rgb(163, 53, 238, 0.65));
                    chartCard.setEffect(brillanteShadow);
                    new Timeline(new KeyFrame(Duration.millis(300), ev3 -> {
                        chartCard.setEffect(new DropShadow(12, Color.rgb(163, 53, 238, 0.35)));
                    })).play();
                })).play();
            })).play();
        }
    }

    @FXML
    public void animarCardEntrada(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(160), source);
        st.setToX(1.03); st.setToY(1.03);
        st.setInterpolator(Interpolator.EASE_OUT);
        source.setEffect(new DropShadow(18, Color.rgb(163, 53, 238, 0.7)));
        ScaleTransition pulsate = new ScaleTransition(Duration.millis(140), source);
        pulsate.setToX(1.06); pulsate.setToY(1.06);
        pulsate.setCycleCount(1); pulsate.setAutoReverse(true);
        pulsate.play(); st.play();
    }

    @FXML
    public void animarCardSalida(MouseEvent e) {
        Node source = (Node) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(160), source);
        st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_IN);
        source.setEffect(new DropShadow(10, Color.rgb(163, 53, 238, 0.35)));
        st.play();
    }

    private void aplicarEstilosStreakChart(XYChart.Series<String, Number> serie) {
        if (serie == null || serie.getNode() == null) return;
        Node serieNode = serie.getNode();
        serieNode.setStyle("-fx-stroke: #ff4757; -fx-stroke-width: 3;");
        serieNode.setEffect(new DropShadow(15, Color.rgb(255, 71, 87, 0.7)));

        for (XYChart.Data<String, Number> data : serie.getData()) {
            Node dataNode = data.getNode();
            if (dataNode != null) {
                dataNode.setStyle("-fx-padding: 5;");
                dataNode.setOnMouseEntered(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(140), dataNode);
                    st.setToX(1.15); st.setToY(1.15); st.play();
                    dataNode.setEffect(new DropShadow(10, Color.rgb(255, 71, 87, 0.75)));
                });
                dataNode.setOnMouseExited(ev -> {
                    ScaleTransition st = new ScaleTransition(Duration.millis(140), dataNode);
                    st.setToX(1.0); st.setToY(1.0); st.play();
                    dataNode.setEffect(null);
                });
            }
        }
    }

    private void animarNumeroRacha(Label lbl) {
        if (lbl == null) return;
        lbl.setScaleX(0.85); lbl.setScaleY(0.85);
        ScaleTransition st = new ScaleTransition(Duration.millis(350), lbl);
        st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        lbl.setEffect(new DropShadow(12, Color.rgb(255, 71, 87, 0.6)));
        st.play();
        new Timeline(new KeyFrame(Duration.millis(420), ev -> lbl.setEffect(null))).play();
    }

    // --- MODALES EXPANDIBLES ---

    @FXML
    public void abrirGraficoExpandido() {
        if (currentUsageData == null || currentUsageData.isEmpty()) {
            Toast.warning("Aviso", "Sin datos de uso disponibles.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/views/chart_expanded_modal.fxml"));
            javafx.scene.Parent root = loader.load();

            ChartExpandedModalController chartCtrl = loader.getController();
            chartCtrl.setData(userId, currentUsageData);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("📈 Distribución Horaria Detallada");
            stage.setScene(new javafx.scene.Scene(root, 1000, 800));
            stage.initStyle(javafx.stage.StageStyle.DECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            chartCtrl.setStage(stage);

            try {
                root.getStylesheets().add(getClass().getResource("/styles/home.css").toExternalForm());
            } catch (Exception ex) {}

            root.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setFromValue(0.0); fade.setToValue(1.0);
            stage.showingProperty().addListener((obs, oldVal, newVal) -> { if (newVal) fade.play(); });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.error("Error", "No se pudo abrir el gráfico expandido.");
        }
    }

    @FXML
    public void abrirListaExpandida() {
        if (currentUsageData == null || currentUsageData.isEmpty()) {
            Toast.warning("Aviso", "Sin datos de uso disponibles.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/views/list_expanded_modal.fxml"));
            javafx.scene.Parent root = loader.load();

            ListExpandedModalController listCtrl = loader.getController();
            listCtrl.setData(userId, currentUsageData);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("📊 Desglose Completo de Actividades");
            stage.setScene(new javafx.scene.Scene(root, 900, 750));
            stage.initStyle(javafx.stage.StageStyle.DECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            listCtrl.setStage(stage);

            try {
                root.getStylesheets().add(getClass().getResource("/styles/home.css").toExternalForm());
            } catch (Exception ex) {}

            root.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setFromValue(0.0); fade.setToValue(1.0);
            stage.showingProperty().addListener((obs, oldVal, newVal) -> { if (newVal) fade.play(); });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.error("Error", "No se pudo abrir la lista expandida.");
        }
    }

    @FXML
    public void animarCardTablaEntrada(javafx.scene.input.MouseEvent e) {
        Node source = (Node) e.getSource();
        if (source == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(150), source);
        st.setToX(1.02); st.setToY(1.02);
        st.setInterpolator(Interpolator.EASE_OUT);
        source.setEffect(new DropShadow(25, Color.rgb(163, 53, 238, 0.8)));
        source.setCursor(javafx.scene.Cursor.HAND);
        st.play();
    }

    @FXML
    public void animarCardTablaSalida(javafx.scene.input.MouseEvent e) {
        Node source = (Node) e.getSource();
        if (source == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(150), source);
        st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_IN);
        source.setEffect(new DropShadow(15, Color.rgb(163, 53, 238, 0.4)));
        source.setCursor(javafx.scene.Cursor.DEFAULT);
        st.play();
    }
}