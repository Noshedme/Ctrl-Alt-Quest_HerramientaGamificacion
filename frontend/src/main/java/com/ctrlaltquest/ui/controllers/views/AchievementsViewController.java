package com.ctrlaltquest.ui.controllers.views;

import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.models.Achievement;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class AchievementsViewController {

    @FXML private Label lblTotalUnlocked;
    @FXML private Label lblCompletionPct;
    @FXML private VBox listGeneral; // Antes grid, ahora lista vertical
    @FXML private VBox listSecrets;

    @FXML
    public void initialize() {
        cargarLogros();
    }

    private void cargarLogros() {
        List<Achievement> logros = new ArrayList<>();
        
        // Datos de Ejemplo
        logros.add(new Achievement(1, "Hola Mundo", "Inicia sesión por primera vez en el sistema.", true, false, "H"));
        logros.add(new Achievement(2, "Maratón de Código", "Programa 4 horas seguidas sin abrir YouTube o redes sociales.", false, false, "M"));
        logros.add(new Achievement(3, "Ratón de Biblioteca", "Lee documentación técnica durante 50 minutos acumulados.", true, false, "R"));
        logros.add(new Achievement(4, "Limpieza Digital", "Elimina 100 archivos innecesarios de tu escritorio.", false, false, "L"));

        // Easter Eggs
        logros.add(new Achievement(99, "Konami Code", "Ingresa el código secreto en el menú principal.", false, true, "?"));
        logros.add(new Achievement(100, "Viajero del Tiempo", "Logueate a las 3:33 AM exactamente.", true, true, "T")); 

        // Cálculos
        int total = logros.size();
        int unlocked = (int) logros.stream().filter(Achievement::isUnlocked).count();

        // Limpieza de UI
        listGeneral.getChildren().clear();
        listSecrets.getChildren().clear();

        // Renderizado
        int delayIndex = 0;
        for (Achievement a : logros) {
            HBox row = crearFilaLogro(a);
            
            // Lógica de separación (General vs Secretos)
            if (a.isHidden() && !a.isUnlocked()) {
                listSecrets.getChildren().add(row);
            } else if (a.isHidden() && a.isUnlocked()) {
                listSecrets.getChildren().add(row); // Secreto revelado
            } else {
                listGeneral.getChildren().add(row);
            }
            
            // Animación de entrada
            animarEntrada(row, delayIndex * 80); // 80ms de diferencia
            delayIndex++;
        }

        // Actualizar Stats Header
        lblTotalUnlocked.setText(unlocked + " / " + total);
        int pct = total > 0 ? (int) (((double) unlocked / total) * 100) : 0;
        lblCompletionPct.setText(pct + "%");
    }

    /**
     * FABRICA DE FILAS: Crea una barra horizontal elegante para cada logro.
     */
    private HBox crearFilaLogro(Achievement a) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 25, 15, 25));
        
        // --- 1. ESTILOS BASE SEGÚN ESTADO ---
        boolean isUnlocked = a.isUnlocked();
        boolean isSecret = a.isHidden();
        
        String bgColor = isUnlocked ? "rgba(45, 90, 39, 0.2)" : "rgba(0,0,0,0.4)";
        String borderColor = isUnlocked ? "#f7d27a" : (isSecret ? "#a335ee" : "#444");
        double opacity = isUnlocked ? 1.0 : 0.6;

        row.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-border-color: transparent transparent " + borderColor + " transparent;" + // Solo borde inferior sutil
            "-fx-border-width: 0 0 1 0;" +
            "-fx-background-radius: 5;"
        );
        row.setOpacity(0); // Para animar entrada

        // --- 2. ICONO IZQUIERDO ---
        StackPane iconStack = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(borderColor));
        circle.setStrokeWidth(2);
        
        if (isUnlocked) {
            circle.setFill(Color.web(isSecret ? "#a335ee" : "#d4af37"));
            circle.setStroke(Color.web("#ffffff"));
        }

        Label iconLetter = new Label(isSecret && !isUnlocked ? "?" : a.getTitle().substring(0, 1));
        iconLetter.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");
        
        iconStack.getChildren().addAll(circle, iconLetter);

        // --- 3. TEXTOS (TÍTULO Y DESCRIPCIÓN) ---
        VBox textBox = new VBox(5);
        textBox.setAlignment(Pos.CENTER_LEFT);
        
        String titleStr = (isSecret && !isUnlocked) ? "LOGRO SECRETO" : a.getTitle();
        String descStr = (isSecret && !isUnlocked) ? "Sigue jugando para descubrir este misterio..." : a.getDescription();

        Label lblTitle = new Label(titleStr);
        lblTitle.setStyle("-fx-font-family: 'Georgia'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + (isUnlocked ? "#ffffff" : "#888") + ";");
        
        Label lblDesc = new Label(descStr);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-family: 'Verdana'; -fx-font-size: 12px; -fx-text-fill: #aaa;");

        textBox.getChildren().addAll(lblTitle, lblDesc);

        // --- 4. RECOMPENSA/ESTADO (DERECHA) ---
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label lblStatus = new Label(isUnlocked ? "COMPLETADO" : "BLOQUEADO");
        lblStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + (isUnlocked ? "#4ade80" : "#666") + ";");
        
        // Etiqueta de XP (Simulada)
        Label lblXp = new Label(isSecret ? "?? XP" : "+100 XP"); 
        lblXp.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 4; -fx-padding: 2 6; -fx-text-fill: #f7d27a; -fx-font-size: 10px;");

        statusBox.getChildren().addAll(lblStatus, lblXp);

        // --- ENSAMBLAJE ---
        row.getChildren().addAll(iconStack, textBox, spacer, statusBox);

        // --- INTERACTIVIDAD ---
        configurarHover(row, borderColor, isUnlocked);

        return row;
    }

    private void configurarHover(HBox row, String accentColor, boolean isUnlocked) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), row);
        
        row.setOnMouseEntered(e -> {
            // Efecto sutil de elevación
            st.setToX(1.01);
            st.setToY(1.01);
            st.playFromStart();
            
            // Cambiar fondo a algo más claro
            row.setStyle(row.getStyle().replace("-fx-background-color: rgba(0,0,0,0.4);", "-fx-background-color: rgba(255,255,255,0.05);"));
            
            // Añadir borde brillante completo
            if (isUnlocked) {
                row.setEffect(new DropShadow(15, Color.web(accentColor)));
            }
        });

        row.setOnMouseExited(e -> {
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
            row.setStyle(row.getStyle().replace("-fx-background-color: rgba(255,255,255,0.05);", "-fx-background-color: rgba(0,0,0,0.4);"));
            row.setEffect(null);
        });
    }

    private void animarEntrada(Node node, int delayMillis) {
        // Deslizar desde la izquierda
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setFromX(-50);
        tt.setToX(0);
        tt.setDelay(Duration.millis(delayMillis));

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMillis));

        tt.play();
        ft.play();
    }
}