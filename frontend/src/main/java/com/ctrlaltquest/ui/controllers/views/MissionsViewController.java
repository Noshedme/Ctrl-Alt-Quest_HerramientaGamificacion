package com.ctrlaltquest.ui.controllers.views;

import java.util.List;

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

public class MissionsViewController {

    @FXML private VBox dailyContainer;
    @FXML private VBox weeklyContainer;
    @FXML private VBox classContainer;
    @FXML private Label lblActiveMissions; 

    private int userId = -1;

    public void setUserId(int userId) {
        System.out.println("🔍 Vista Misiones: Usuario ID recibido = " + userId);
        this.userId = userId;
        cargarMisionesReales();
    }

    @FXML
    public void initialize() {
        limpiarContenedores();
        if(lblActiveMissions != null) lblActiveMissions.setText("---");
    }

    private void cargarMisionesReales() {
        if (userId == -1) return;

        limpiarContenedores();

        Label loading = new Label("Consultando el oráculo...");
        loading.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 10;");
        dailyContainer.getChildren().add(loading);

        Task<List<Mission>> task = new Task<>() {
            @Override
            protected List<Mission> call() {
                // Llama al DAO actualizado que hace el LEFT JOIN
                return MissionsDAO.getMisionesUsuario(userId);
            }
        };

        task.setOnSucceeded(e -> {
            limpiarContenedores(); 
            List<Mission> misiones = task.getValue();

            if (misiones == null || misiones.isEmpty()) {
                mostrarMensajeVacio(dailyContainer, "No hay misiones diarias activas.");
                mostrarMensajeVacio(weeklyContainer, "Descansa, no hay encargos semanales.");
                mostrarMensajeVacio(classContainer, "Tu historia continúa pronto...");
                if(lblActiveMissions != null) lblActiveMissions.setText("0");
                return;
            }

            int activeCount = 0;
            int delayIndex = 0;

            for (Mission m : misiones) {
                // Contar solo las no completadas
                if (!m.isCompleted()) activeCount++;

                // Crear la tarjeta visual
                HBox tarjeta = crearFilaMision(m);

                // Clasificación Segura (Mayúsculas)
                String tipo = (m.getType() == null) ? "OTRO" : m.getType().toUpperCase().trim();

                switch (tipo) {
                    case "DIARIA" -> dailyContainer.getChildren().add(tarjeta);
                    case "SEMANAL" -> weeklyContainer.getChildren().add(tarjeta);
                    case "CLASE", "HISTORIA" -> classContainer.getChildren().add(tarjeta);
                    default -> dailyContainer.getChildren().add(tarjeta);
                }

                // Animación
                animarEntrada(tarjeta, delayIndex * 50);
                delayIndex++;
            }

            // Actualizar contador
            if(lblActiveMissions != null) {
                lblActiveMissions.setText(String.valueOf(activeCount));
            }
        });

        task.setOnFailed(e -> {
            limpiarContenedores();
            mostrarMensajeVacio(dailyContainer, "Error de conexión con la base de datos.");
            System.err.println("❌ Error cargando misiones:");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void limpiarContenedores() {
        if(dailyContainer != null) dailyContainer.getChildren().clear();
        if(weeklyContainer != null) weeklyContainer.getChildren().clear();
        if(classContainer != null) classContainer.getChildren().clear();
    }

    private void mostrarMensajeVacio(VBox container, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-padding: 10;");
        container.getChildren().add(lbl);
    }

    private HBox crearFilaMision(Mission m) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));

        boolean isComplete = m.isCompleted();
        
        // Estilos condicionales
        String borderColor = isComplete ? "#f7d27a" : "rgba(255,255,255,0.1)";
        String bgColor = isComplete ? "rgba(45, 90, 39, 0.2)" : "rgba(30, 20, 40, 0.6)";

        row.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: transparent transparent " + borderColor + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );
        row.setOpacity(0); 

        // 1. Icono Estado
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(isComplete ? "#f7d27a" : "#666"));
        circle.setStrokeWidth(2);

        Label iconLabel = new Label(isComplete ? "✔" : "!");
        iconLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + (isComplete ? "#f7d27a" : "#666") + ";");

        if (isComplete) circle.setFill(Color.web("rgba(247, 210, 122, 0.2)"));
        iconPane.getChildren().addAll(circle, iconLabel);

        // 2. Textos
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblTitle = new Label(m.getTitle());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");

        Label lblDesc = new Label(m.getDescription());
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        HBox rewardsBox = new HBox(10);
        rewardsBox.getChildren().addAll(
            crearBadge("✨ " + m.getXpReward() + " XP", "#a335ee"),
            crearBadge("💰 " + m.getCoinReward(), "#ffd700")
        );

        infoBox.getChildren().addAll(lblTitle, lblDesc, rewardsBox);

        // 3. Progreso y Botón
        VBox actionBox = new VBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMinWidth(150);

        VBox progressContainer = new VBox(5);
        ProgressBar pb = new ProgressBar(m.getProgress());
        pb.setPrefWidth(140); 
        pb.setPrefHeight(8);
        
        String barColor = (m.getProgress() >= 1.0) ? "#2d5a27" : "#0070dd";
        pb.setStyle("-fx-accent: " + barColor + "; -fx-control-inner-background: rgba(0,0,0,0.5);");

        Label lblProgress = new Label((int)(Math.min(m.getProgress(), 1.0) * 100) + "%");
        lblProgress.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");

        progressContainer.getChildren().addAll(pb, lblProgress);
        progressContainer.setAlignment(Pos.CENTER_RIGHT);

        Button btnAction = new Button();
        btnAction.setPrefWidth(140);
        btnAction.getStyleClass().add("btn-nav");

        if (isComplete) {
            btnAction.setText("COMPLETADA");
            btnAction.setDisable(true);
            btnAction.setStyle("-fx-background-color: #444; -fx-text-fill: #888;");
        } else if (m.getProgress() >= 1.0) {
            // LISTA PARA RECLAMAR
            btnAction.setText("RECLAMAR");
            btnAction.setStyle("-fx-background-color: linear-gradient(to bottom, #f7d27a, #d4a017); -fx-text-fill: #1a0f26; -fx-font-weight: bold; -fx-cursor: hand;");
            
            btnAction.setOnAction(e -> {
                SoundManager.playClickSound();
                
                Task<Void> claimTask = new Task<>() {
                    @Override protected Void call() {
                        // 🔥 IMPORTANTE: Pasamos userId para saber quién reclama
                        MissionsDAO.reclamarMision(userId, m.getId());
                        return null;
                    }
                };
                
                claimTask.setOnSucceeded(ev -> {
                    btnAction.setText("¡HECHO!");
                    btnAction.setDisable(true);
                    btnAction.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white;");
                    // Aquí podrías lanzar un evento para actualizar las monedas en el HUD
                });
                
                new Thread(claimTask).start();
            });
        } else {
            btnAction.setText("EN PROGRESO");
            btnAction.setDisable(true);
            btnAction.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #666; -fx-border-color: #444;");
        }

        actionBox.getChildren().addAll(btnAction, progressContainer);
        row.getChildren().addAll(iconPane, infoBox, actionBox);
        
        configurarEfectosHover(row, borderColor);
        return row;
    }

    private Label crearBadge(String text, String colorHex) {
        Label badge = new Label(text);
        badge.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: " + colorHex + "; -fx-border-radius: 4; -fx-padding: 2 6; -fx-background-color: rgba(0,0,0,0.2);");
        return badge;
    }

    private void configurarEfectosHover(HBox row, String glowColor) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), row);
        row.setOnMouseEntered(e -> {
            st.setToX(1.01); st.setToY(1.01); st.playFromStart();
            row.setStyle(row.getStyle().replace("-fx-background-color: rgba(30, 20, 40, 0.6);", "-fx-background-color: rgba(255,255,255,0.05);"));
            if (!glowColor.contains("rgba")) row.setEffect(new DropShadow(10, Color.web(glowColor)));
        });
        row.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            row.setStyle(row.getStyle().replace("-fx-background-color: rgba(255,255,255,0.05);", "-fx-background-color: rgba(30, 20, 40, 0.6);"));
            row.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        node.setTranslateX(-30);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setToX(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0); ft.setToValue(1);
        
        PauseTransition delay = new PauseTransition(Duration.millis(delayMillis));
        delay.setOnFinished(e -> { tt.play(); ft.play(); });
        delay.play();
    }
}