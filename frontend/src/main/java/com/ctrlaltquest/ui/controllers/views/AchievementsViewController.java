package com.ctrlaltquest.ui.controllers.views;

import java.util.List;

import com.ctrlaltquest.dao.AchievementsDAO;
import com.ctrlaltquest.models.Achievement;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

public class AchievementsViewController {

    @FXML private Label lblTotalUnlocked;
    @FXML private Label lblCompletionPct;
    @FXML private ProgressBar barCompletion; // Nueva barra de progreso global
    @FXML private VBox listGeneral;
    @FXML private VBox listSecrets;

    private int userId = -1;

    public void setUserId(int userId) {
        this.userId = userId;
        cargarLogrosReales();
    }

    @FXML
    public void initialize() {
        if(lblTotalUnlocked != null) lblTotalUnlocked.setText("...");
    }

    private void cargarLogrosReales() {
        if (userId == -1) return;

        listGeneral.getChildren().clear();
        listSecrets.getChildren().clear();

        Task<List<Achievement>> task = new Task<>() {
            @Override
            protected List<Achievement> call() {
                return AchievementsDAO.getAllAvailableAchievements(userId);
            }
        };

        task.setOnSucceeded(e -> {
            List<Achievement> logros = task.getValue();
            if (logros == null || logros.isEmpty()) return;

            int total = logros.size();
            int unlockedCount = (int) logros.stream().filter(Achievement::isUnlocked).count();

            // Actualizar header
            lblTotalUnlocked.setText(unlockedCount + " / " + total);
            double pct = total > 0 ? (double) unlockedCount / total : 0;
            lblCompletionPct.setText((int)(pct * 100) + "%");
            if(barCompletion != null) barCompletion.setProgress(pct);

            int delayIndex = 0;
            for (Achievement a : logros) {
                HBox row = crearFilaLogro(a);
                
                if (a.isHidden()) {
                    listSecrets.getChildren().add(row);
                } else {
                    listGeneral.getChildren().add(row);
                }
                
                animarEntrada(row, delayIndex * 60);
                delayIndex++;
            }
        });

        new Thread(task).start();
    }

    private HBox crearFilaLogro(Achievement a) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 25, 15, 25));
        
        boolean isUnlocked = a.isUnlocked();
        boolean isSecret = a.isHidden(); 
        
        // Colores temáticos
        String themeColor = isSecret ? "#a335ee" : "#f7d27a"; // Morado para secretos, Dorado para normales
        String borderColor = isUnlocked ? themeColor : "#444";
        String bgColor = isUnlocked ? "rgba(30, 40, 30, 0.6)" : "rgba(20, 20, 25, 0.4)";

        row.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 0 0 0 4;" + // Borde izquierdo indicador
            "-fx-background-radius: 4;" +
            "-fx-border-radius: 4;"
        );
        row.setOpacity(0); 

        // 1. Icono / Medalla
        StackPane iconStack = new StackPane();
        
        // Fondo del icono
        Circle bgCircle = new Circle(24);
        bgCircle.setFill(Color.web(isUnlocked ? themeColor : "#333"));
        bgCircle.setOpacity(isUnlocked ? 0.2 : 1.0);
        bgCircle.setStroke(Color.web(borderColor));
        
        // Símbolo
        String symbol = isSecret && !isUnlocked ? "?" : (a.getTitle().substring(0, 1).toUpperCase());
        Label iconLabel = new Label(isUnlocked ? "🏆" : symbol);
        iconLabel.setStyle("-fx-text-fill: " + (isUnlocked ? themeColor : "#666") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        if (isUnlocked) {
            bgCircle.setEffect(new DropShadow(15, Color.web(themeColor)));
        }

        iconStack.getChildren().addAll(bgCircle, iconLabel);

        // 2. Info Textual
        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        String titleText = (isSecret && !isUnlocked) ? "LOGRO OCULTO" : a.getTitle();
        String descText = (isSecret && !isUnlocked) ? "Sigue jugando para descubrir este secreto." : a.getDescription();

        Label lblTitle = new Label(titleText);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + (isUnlocked ? "white" : "#888") + ";");
        
        Label lblDesc = new Label(descText);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa; -fx-font-style: " + ((isSecret && !isUnlocked) ? "italic" : "normal") + ";");

        textBox.getChildren().addAll(lblTitle, lblDesc);

        // 3. Estado / Fecha (Simulado)
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        
        if (isUnlocked) {
            Label badge = new Label("DESBLOQUEADO");
            badge.setStyle("-fx-background-color: " + themeColor + "; -fx-text-fill: #1a1a1a; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 10;");
            statusBox.getChildren().add(badge);
        } else {
            Label badge = new Label("BLOQUEADO");
            badge.setStyle("-fx-background-color: #444; -fx-text-fill: #aaa; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 10;");
            statusBox.getChildren().add(badge);
        }

        row.getChildren().addAll(iconStack, textBox, statusBox);
        
        // Efectos Hover
        configurarHover(row, isUnlocked);

        return row;
    }

    private void configurarHover(HBox row, boolean isUnlocked) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), row);
        
        row.setOnMouseEntered(e -> {
            st.setToX(1.02); st.setToY(1.02); st.playFromStart();
            row.setEffect(new Glow(0.3));
            row.setStyle(row.getStyle().replace("rgba(20, 20, 25, 0.4)", "rgba(40, 40, 50, 0.6)"));
        });

        row.setOnMouseExited(e -> {
            st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            row.setEffect(null);
            row.setStyle(row.getStyle().replace("rgba(40, 40, 50, 0.6)", "rgba(20, 20, 25, 0.4)"));
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setFromY(20); tt.setToY(0);
        tt.setDelay(Duration.millis(delayMillis));

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMillis));

        tt.play(); ft.play();
    }
}