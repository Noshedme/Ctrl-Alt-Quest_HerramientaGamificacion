package com.ctrlaltquest.ui.controllers.views;

import java.util.List;

import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.models.Mission;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
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
import javafx.scene.effect.Glow;
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
        
        // Indicador de carga
        Label loading = new Label("Sincronizando con el gremio...");
        loading.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 20;");
        dailyContainer.getChildren().add(loading);

        Task<List<Mission>> task = new Task<>() {
            @Override
            protected List<Mission> call() {
                return MissionsDAO.getMisionesUsuario(userId);
            }
        };

        task.setOnSucceeded(e -> {
            limpiarContenedores(); 
            List<Mission> misiones = task.getValue();

            if (misiones == null || misiones.isEmpty()) {
                mostrarMensajeVacio(dailyContainer, "Nada por hoy. El reino está a salvo.");
                return;
            }

            int activeCount = 0;
            int delayIndex = 0;

            for (Mission m : misiones) {
                if (!m.isCompleted()) activeCount++;

                HBox tarjeta = crearFilaMision(m);
                String tipo = (m.getType() == null) ? "OTRO" : m.getType().toUpperCase().trim();

                switch (tipo) {
                    case "DIARIA" -> dailyContainer.getChildren().add(tarjeta);
                    case "SEMANAL" -> weeklyContainer.getChildren().add(tarjeta);
                    case "CLASE", "HISTORIA" -> classContainer.getChildren().add(tarjeta);
                    default -> dailyContainer.getChildren().add(tarjeta);
                }

                // Animación en cascada
                animarEntrada(tarjeta, delayIndex * 80);
                delayIndex++;
            }

            if(lblActiveMissions != null) {
                lblActiveMissions.setText(String.valueOf(activeCount));
            }
        });

        task.setOnFailed(e -> {
            limpiarContenedores();
            mostrarMensajeVacio(dailyContainer, "Error de conexión con la base de datos.");
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
        lbl.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-font-size: 14px; -fx-padding: 20;");
        container.getChildren().add(lbl);
    }

    private HBox crearFilaMision(Mission m) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));

        boolean isComplete = m.isCompleted();
        boolean isReadyToClaim = m.getProgress() >= 1.0 && !isComplete;
        
        // Estilos base RPG
        String baseStyle = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;";
        String bgColor = isComplete ? "rgba(20, 30, 20, 0.4)" : "rgba(30, 20, 40, 0.4)";
        String borderColor = isReadyToClaim ? "#f7d27a" : (isComplete ? "#4ade80" : "rgba(255,255,255,0.1)");

        row.setStyle(baseStyle + "-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor + ";");
        row.setOpacity(0); // Para animación inicial

        // 1. Icono de Estado
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(20);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(borderColor));
        circle.setStrokeWidth(2);

        Label iconLabel = new Label(isComplete ? "✔" : (isReadyToClaim ? "🎁" : "⚔"));
        iconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + borderColor + ";");
        
        if (isReadyToClaim) {
            // Efecto de brillo si está lista para reclamar
            circle.setEffect(new DropShadow(10, Color.web("#f7d27a")));
        }
        
        iconPane.getChildren().addAll(circle, iconLabel);

        // 2. Información
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblTitle = new Label(m.getTitle());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblDesc = new Label(m.getDescription());
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        // Badges de recompensa
        HBox rewardsBox = new HBox(8);
        if (m.getXpReward() > 0) rewardsBox.getChildren().add(crearBadge("+" + m.getXpReward() + " XP", "#a335ee"));
        if (m.getCoinReward() > 0) rewardsBox.getChildren().add(crearBadge("+" + m.getCoinReward() + " G", "#ffd700"));
        
        infoBox.getChildren().addAll(lblTitle, lblDesc, rewardsBox);

        // 3. Acción y Progreso
        VBox actionBox = new VBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMinWidth(140);

        // Barra de progreso
        VBox progressContainer = new VBox(4);
        progressContainer.setAlignment(Pos.CENTER_RIGHT);
        
        ProgressBar pb = new ProgressBar(Math.min(m.getProgress(), 1.0));
        pb.setPrefWidth(120); 
        pb.setPrefHeight(6);
        
        String barColor = isReadyToClaim || isComplete ? "#4ade80" : "#3b82f6";
        pb.setStyle("-fx-accent: " + barColor + "; -fx-control-inner-background: rgba(0,0,0,0.5);");

        Label lblProgress = new Label((int)(Math.min(m.getProgress(), 1.0) * 100) + "%");
        lblProgress.setStyle("-fx-text-fill: " + barColor + "; -fx-font-size: 9px; -fx-font-weight: bold;");

        progressContainer.getChildren().addAll(pb, lblProgress);

        // Botón de Acción
        Button btnAction = new Button();
        btnAction.setPrefWidth(120);
        btnAction.getStyleClass().add("btn-nav"); // Clase base

        if (isComplete) {
            btnAction.setText("COMPLETADA");
            btnAction.setDisable(true);
            btnAction.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-border-color: #444; -fx-opacity: 0.7;");
        } else if (isReadyToClaim) {
            btnAction.setText("RECLAMAR");
            // Estilo dorado llamativo
            btnAction.setStyle("-fx-background-color: linear-gradient(to bottom, #f7d27a, #d4a017); -fx-text-fill: #1a0f26; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(247, 210, 122, 0.4), 10, 0, 0, 0);");
            
            btnAction.setOnAction(e -> {
                SoundManager.playSuccessSound(); // Sonido de moneda/éxito
                
                // Animación de pulsación
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btnAction);
                st.setFromX(1.0); st.setFromY(1.0); st.setToX(0.95); st.setToY(0.95);
                st.setAutoReverse(true); st.setCycleCount(2);
                st.play();

                // Lógica de reclamación
                Task<Void> claimTask = new Task<>() {
                    @Override protected Void call() {
                        MissionsDAO.reclamarMision(userId, m.getId());
                        return null;
                    }
                };
                
                claimTask.setOnSucceeded(ev -> {
                    // Actualizar visualmente la tarjeta a "Completada" sin recargar todo
                    btnAction.setText("¡RECOGIDO!");
                    btnAction.setDisable(true);
                    btnAction.setStyle("-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-border-color: transparent;");
                    row.setStyle(baseStyle + "-fx-background-color: rgba(20, 30, 20, 0.4); -fx-border-color: #4ade80;");
                    circle.setStroke(Color.web("#4ade80"));
                    iconLabel.setText("✔");
                    iconLabel.setTextFill(Color.web("#4ade80"));
                    circle.setEffect(null);
                    
                    // Actualizar contador global si es necesario
                    if(lblActiveMissions != null) {
                        try {
                            int current = Integer.parseInt(lblActiveMissions.getText());
                            lblActiveMissions.setText(String.valueOf(Math.max(0, current - 1)));
                        } catch (Exception ex) {}
                    }
                });
                
                new Thread(claimTask).start();
            });
        } else {
            btnAction.setText("EN PROGRESO");
            btnAction.setDisable(true);
            btnAction.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #888; -fx-border-color: transparent;");
        }

        actionBox.getChildren().addAll(btnAction, progressContainer);
        row.getChildren().addAll(iconPane, infoBox, actionBox);
        
        configurarHover(row, isReadyToClaim);
        return row;
    }

    private Label crearBadge(String text, String colorHex) {
        Label badge = new Label(text);
        badge.setStyle(
            "-fx-text-fill: " + colorHex + "; " +
            "-fx-font-size: 10px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-color: " + colorHex + "; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 2 8; " +
            "-fx-background-color: rgba(0,0,0,0.3); " +
            "-fx-background-radius: 10;"
        );
        return badge;
    }

    private void configurarHover(HBox row, boolean isInteractive) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), row);
        
        row.setOnMouseEntered(e -> {
            st.setToX(1.01); st.setToY(1.01); st.playFromStart();
            row.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.5)));
            if (isInteractive) row.setEffect(new Glow(0.2));
        });
        
        row.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            row.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        node.setTranslateY(20); // Empieza un poco abajo
        
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setToY(0);
        
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0); ft.setToValue(1);
        
        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setDelay(Duration.millis(delayMillis));
        pt.play();
    }
}