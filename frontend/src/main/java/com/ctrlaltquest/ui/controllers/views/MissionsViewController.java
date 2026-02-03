package com.ctrlaltquest.ui.controllers.views;

import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.models.Mission;
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;

public class MissionsViewController {

    @FXML private VBox dailyContainer;
    @FXML private VBox weeklyContainer;
    @FXML private VBox classContainer;

    private int userId = -1;

    /**
     * Punto de entrada desde el controlador principal.
     * Al setear el ID, inicia la carga de datos reales.
     */
    public void setUserId(int userId) {
        System.out.println("🔍 DEBUG: MissionsViewController.setUserId(" + userId + ")");
        this.userId = userId;
        cargarMisionesReales();
    }

    @FXML
    public void initialize() {
        // Se deja vacío o con un estado inicial, esperando a que setUserId sea llamado.
        limpiarContenedores();
    }

    private void cargarMisionesReales() {
        System.out.println("🔍 DEBUG: cargarMisionesReales() - userId = " + userId);
        if (userId == -1) {
            System.out.println("❌ ERROR: userId es -1, abortando carga");
            return;
        }

        limpiarContenedores();

        // Mostrar indicador de carga temporal
        Label loading = new Label("Consultando el oráculo...");
        loading.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 10;");
        dailyContainer.getChildren().add(loading);

        // Tarea en segundo plano para no congelar la UI
        Task<List<Mission>> task = new Task<>() {
            @Override
            protected List<Mission> call() {
                System.out.println("🔍 DEBUG: Ejecutando MissionsDAO.getMisionesUsuario(" + userId + ")");
                List<Mission> result = MissionsDAO.getMisionesUsuario(userId);
                System.out.println("🔍 DEBUG: Misiones cargadas: " + (result == null ? "null" : result.size()));
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            limpiarContenedores(); // Quitar mensaje de carga
            List<Mission> misiones = task.getValue();
            System.out.println("🔍 DEBUG: onSucceeded - Misiones recibidas: " + (misiones == null ? "null" : misiones.size()));

            if (misiones == null || misiones.isEmpty()) {
                System.out.println("⚠️ ADVERTENCIA: No se encontraron misiones para userId=" + userId);
                mostrarMensajeVacio(dailyContainer, "No hay misiones diarias activas.");
                mostrarMensajeVacio(weeklyContainer, "Descansa, no hay encargos semanales.");
                mostrarMensajeVacio(classContainer, "Tu historia continúa pronto...");
                return;
            }

            int delayIndex = 0;
            for (Mission m : misiones) {
                System.out.println("✅ Procesando misión: " + m.getTitle() + " (tipo: " + m.getType() + ", progress: " + m.getProgress() + ")");
                HBox tarjeta = crearFilaMision(m);

                // Clasificación por tipo
                switch (m.getType()) {
                    case "DIARIA" -> {
                        System.out.println("   → Añadida a TAB DIARIAS");
                        dailyContainer.getChildren().add(tarjeta);
                    }
                    case "SEMANAL" -> {
                        System.out.println("   → Añadida a TAB SEMANALES");
                        weeklyContainer.getChildren().add(tarjeta);
                    }
                    case "CLASE" -> {
                        System.out.println("   → Añadida a TAB CLASE");
                        classContainer.getChildren().add(tarjeta);
                    }
                    default -> {
                        System.out.println("   ⚠️ Tipo desconocido: " + m.getType() + ", añadida a DIARIAS (default)");
                        dailyContainer.getChildren().add(tarjeta);
                    }
                }

                // Animación en cascada
                animarEntrada(tarjeta, delayIndex * 80);
                delayIndex++;
            }
        });

        task.setOnFailed(e -> {
            System.err.println("❌ ERROR en task.setOnFailed:");
            task.getException().printStackTrace();
            limpiarContenedores();
            mostrarMensajeVacio(dailyContainer, "Error de conexión con el gremio.");
        });

        new Thread(task).start();
    }

    private void limpiarContenedores() {
        dailyContainer.getChildren().clear();
        weeklyContainer.getChildren().clear();
        classContainer.getChildren().clear();
    }

    private void mostrarMensajeVacio(VBox container, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-padding: 10;");
        container.getChildren().add(lbl);
    }

    /**
     * FÁBRICA DE FILAS: Combina el estilo visual del Base con la lógica de botones del DAO.
     */
    private HBox crearFilaMision(Mission m) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));

        // Determinar colores según estado
        boolean isComplete = m.isCompleted();
        // Nota: isComplete en DB significa "Reclamada/Finalizada".
        // Si progress >= 1.0 pero !isComplete, significa que está lista para reclamar.
        
        String borderColor = isComplete ? "#f7d27a" : "rgba(255,255,255,0.1)";
        String bgColor = isComplete ? "rgba(45, 90, 39, 0.1)" : "rgba(30, 20, 40, 0.6)";

        row.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: transparent transparent " + borderColor + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );
        row.setOpacity(0); // Para animación de entrada

        // 1. Icono de Estado (Izquierda)
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(isComplete ? "#f7d27a" : "#666"));
        circle.setStrokeWidth(2);

        Label iconLabel = new Label(isComplete ? "✔" : "!");
        iconLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + (isComplete ? "#f7d27a" : "#666") + ";");

        if (isComplete) {
            circle.setFill(Color.web("rgba(247, 210, 122, 0.2)"));
        }

        iconPane.getChildren().addAll(circle, iconLabel);

        // 2. Información (Centro)
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblTitle = new Label(m.getTitle());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Georgia';");

        Label lblDesc = new Label(m.getDescription());
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        // Badges de Recompensa
        HBox rewardsBox = new HBox(10);
        rewardsBox.getChildren().addAll(
            crearBadge("✨ " + m.getXpReward() + " XP", "#a335ee"),
            crearBadge("💰 " + m.getCoinReward(), "#ffd700")
        );

        infoBox.getChildren().addAll(lblTitle, lblDesc, rewardsBox);

        // 3. Progreso y Acción (Derecha)
        VBox actionBox = new VBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMinWidth(150);

        // Barra de Progreso
        VBox progressContainer = new VBox(5);
        ProgressBar pb = new ProgressBar(m.getProgress());
        pb.setPrefWidth(140);
        pb.setPrefHeight(8);
        
        // Color de la barra: Verde si está completa, Azul si está en progreso
        String barColor = (m.getProgress() >= 1.0) ? "#2d5a27" : "#0070dd";
        pb.setStyle("-fx-accent: " + barColor + "; -fx-control-inner-background: rgba(0,0,0,0.5);");

        Label lblProgress = new Label((int)(Math.min(m.getProgress(), 1.0) * 100) + "%");
        lblProgress.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");

        progressContainer.getChildren().addAll(pb, lblProgress);
        progressContainer.setAlignment(Pos.CENTER_RIGHT);

        // Botón de Acción (Lógica combinada)
        Button btnAction = new Button();
        btnAction.setPrefWidth(140);
        btnAction.getStyleClass().add("btn-nav");

        if (isComplete) {
            // Caso 1: Ya reclamada en DB
            btnAction.setText("COMPLETADA");
            btnAction.setDisable(true);
            btnAction.setStyle("-fx-background-color: #444; -fx-text-fill: #888;");
        } else if (m.getProgress() >= 1.0) {
            // Caso 2: Progreso al 100% pero no reclamada -> Botón Activo
            btnAction.setText("RECLAMAR");
            btnAction.setStyle("-fx-background-color: linear-gradient(to bottom, #f7d27a, #d4a017); -fx-text-fill: #1a0f26; -fx-font-weight: bold; -fx-cursor: hand;");
            
            btnAction.setOnAction(e -> {
                SoundManager.playClickSound();
                
                // Lógica asíncrona para actualizar DB
                Task<Void> claimTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        MissionsDAO.reclamarMision(m.getId());
                        return null;
                    }
                };
                
                claimTask.setOnSucceeded(ev -> {
                    btnAction.setText("¡HECHO!");
                    btnAction.setDisable(true);
                    btnAction.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white;");
                    // Opcional: Notificar al MainController para actualizar monedas/XP en la UI principal
                });
                
                new Thread(claimTask).start();
            });
        } else {
            // Caso 3: En progreso
            btnAction.setText("EN PROGRESO");
            btnAction.setDisable(true);
            btnAction.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #666; -fx-border-color: #444;");
        }

        actionBox.getChildren().addAll(btnAction, progressContainer);

        // Ensamblaje final de la fila
        row.getChildren().addAll(iconPane, infoBox, actionBox);

        // Interactividad (Hover effects)
        configurarEfectosHover(row, borderColor);

        return row;
    }

    private Label crearBadge(String text, String colorHex) {
        Label badge = new Label(text);
        badge.setStyle(
            "-fx-text-fill: " + colorHex + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-border-color: " + colorHex + "; -fx-border-radius: 4; -fx-padding: 2 6; -fx-background-color: rgba(0,0,0,0.2);"
        );
        return badge;
    }

    private void configurarEfectosHover(HBox row, String glowColor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), row);

        row.setOnMouseEntered(e -> {
            st.setToX(1.01); st.setToY(1.01); st.playFromStart();
            row.setStyle(row.getStyle().replace("-fx-background-color: rgba(30, 20, 40, 0.6);", "-fx-background-color: rgba(255,255,255,0.05);"));
            
            if (!glowColor.contains("rgba")) { // Si tiene color (completada)
                row.setEffect(new DropShadow(10, Color.web(glowColor)));
            }
        });

        row.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            row.setStyle(row.getStyle().replace("-fx-background-color: rgba(255,255,255,0.05);", "-fx-background-color: rgba(30, 20, 40, 0.6);"));
            row.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        node.setTranslateX(-30); // Viene de la izquierda
        
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setToX(0);
        
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0); ft.setToValue(1);

        PauseTransition delay = new PauseTransition(Duration.millis(delayMillis));
        delay.setOnFinished(e -> {
            tt.play();
            ft.play();
        });
        delay.play();
    }
}