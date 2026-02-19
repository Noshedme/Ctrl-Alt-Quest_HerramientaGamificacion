package com.ctrlaltquest.ui.utils;

import com.ctrlaltquest.services.EventContextualListener;
import com.ctrlaltquest.services.EventContextualService;
import java.io.File;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * EventContextualUI - Gestor visual Inmersivo de Eventos.
 * Maneja Jefes, Bugs, Misiones y Descansos con imágenes dinámicas, animaciones y colores temáticos.
 */
public class EventContextualUI implements EventContextualListener {
    
    private Stage eventStage;
    private EventContextualService.ContextualEvent currentEvent;
    
    // Variables para el control de la fase crítica visual (parpadeo rojo)
    private Timeline criticalAlertTimeline;
    private boolean isCriticalPhase = false;
    
    public EventContextualUI() {
        EventContextualService.getInstance().addEventListener(this);
        System.out.println("✅ EventContextualUI registrado como listener");
    }
    
    // ==========================================
    // 1. DESCANSO (BREAK_TIME / STRETCH_ROUTINE)
    // ==========================================
    
    private void showRestBreakDialog(EventContextualService.ContextualEvent event) {
        Platform.runLater(() -> {
            try {
                String colorHex = event.type.themeColor;
                setupBaseStage(event.type.icon + " " + event.title, 400, 450);
                
                VBox root = createBaseContainer();
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, " + colorHex + "22); -fx-padding: 30;");
                
                ImageView imgView = createEventImage(event.imagePath, 150);
                Label titleLabel = createTitleLabel(event.type.icon + " " + event.title, colorHex);
                Label descLabel = createDescLabel(event.description);
                
                Label timerLabel = new Label(String.valueOf(event.restTimeSeconds));
                timerLabel.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 72; -fx-font-weight: bold;");
                Label secondsLabel = new Label("segundos de recuperación");
                secondsLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 14;");
                
                Button skipButton = createStyledButton("Huir del Santuario (Saltar)", "#5a2d2d");
                skipButton.setOnAction(e -> {
                    eventStage.close();
                    EventContextualService.getInstance().completeEvent(event.userId, event.id, false);
                });
                
                root.getChildren().addAll(imgView, titleLabel, descLabel, timerLabel, secondsLabel, skipButton);
                showStage(root);
                
                startRestBreakTimer(event, timerLabel, event.restTimeSeconds, eventStage);
            } catch (Exception e) {
                System.err.println("❌ Error en UI (Rest Break): " + e.getMessage());
            }
        });
    }
    
    private void startRestBreakTimer(EventContextualService.ContextualEvent event, Label timerLabel, int seconds, Stage stage) {
        Timeline timeline = new Timeline();
        final int[] remaining = {seconds};
        
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            timerLabel.setText(String.valueOf(remaining[0]));
            if (remaining[0] == 0) {
                timeline.stop();
                stage.close();
                EventContextualService.getInstance().completeEvent(event.userId, event.id, true);
            }
        }));
        timeline.setCycleCount(seconds);
        timeline.play();
    }
    
    // ==========================================
    // 2. MISIONES RÁPIDAS Y BUGS (CLICK_RUSH / BUG_STORM / TYPING)
    // ==========================================
    
    private void showQuickMissionDialog(EventContextualService.ContextualEvent event) {
        Platform.runLater(() -> {
            try {
                String colorHex = event.type.themeColor;
                setupBaseStage(event.type.icon + " " + event.title, 500, 450);
                
                VBox root = createBaseContainer();
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, " + colorHex + "33); -fx-padding: 30;");
                
                ImageView imgView = createEventImage(event.imagePath, 120);
                Label titleLabel = createTitleLabel(event.type.icon + " " + event.title, colorHex);
                Label descLabel = createDescLabel(event.description);
                
                ProgressBar progressBar = new ProgressBar(0);
                progressBar.setPrefWidth(400);
                progressBar.setStyle("-fx-accent: " + colorHex + "; -fx-control-inner-background: #2a2a3e;");
                
                Label progressLabel = new Label("Progreso: 0/" + event.targetCount);
                progressLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 14; -fx-font-weight: bold;");
                
                // Simulación de interacción rápida
                Timeline progressTimeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
                    event.currentProgress++;
                    double progress = (double) event.currentProgress / event.targetCount;
                    progressBar.setProgress(Math.min(progress, 1.0));
                    progressLabel.setText("Progreso: " + event.currentProgress + "/" + event.targetCount);
                    
                    if (event.currentProgress >= event.targetCount) {
                        eventStage.close();
                        EventContextualService.getInstance().completeEvent(event.userId, event.id, true);
                    }
                }));
                progressTimeline.setCycleCount(event.targetCount);
                progressTimeline.play();
                
                Button cancelButton = createStyledButton("Rendirse", "#5a2d2d");
                cancelButton.setOnAction(e -> {
                    progressTimeline.stop();
                    eventStage.close();
                    EventContextualService.getInstance().completeEvent(event.userId, event.id, false);
                });
                
                root.getChildren().addAll(imgView, titleLabel, descLabel, progressBar, progressLabel, cancelButton);
                showStage(root);
            } catch (Exception e) {
                System.err.println("❌ Error en UI (Quick Mission): " + e.getMessage());
            }
        });
    }
    
    // ==========================================
    // 3. BATALLA DE JEFE (BOSS_BATTLE)
    // ==========================================
    
    private void showBossBattleDialog(EventContextualService.ContextualEvent event) {
        Platform.runLater(() -> {
            try {
                String colorHex = event.type.themeColor;
                setupBaseStage(event.type.icon + " ALERTA DE JEFE", 550, 550);
                
                // Root principal y capa roja para el estado crítico
                StackPane mainLayer = new StackPane();
                VBox root = createBaseContainer();
                
                // Capa de alerta visual (Fondo rojo intermitente)
                VBox criticalOverlay = new VBox();
                criticalOverlay.setStyle("-fx-background-color: rgba(255, 0, 0, 0.2);");
                criticalOverlay.setOpacity(0); // Oculta al principio
                criticalOverlay.setMouseTransparent(true); // Evita que bloquee los clicks
                
                // Contenedor de la imagen del Jefe con botón de editar encima
                StackPane bossImageContainer = new StackPane();
                ImageView bossImageView = createEventImage(event.imagePath, 200);
                
                Button btnEditImage = new Button("⚙️");
                btnEditImage.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14;");
                StackPane.setAlignment(btnEditImage, Pos.TOP_RIGHT);
                
                btnEditImage.setOnAction(e -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Seleccionar Nueva Apariencia del Jefe");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
                    File file = fc.showOpenDialog(eventStage);
                    if (file != null) {
                        Image newImg = new Image(file.toURI().toString());
                        bossImageView.setImage(newImg);
                        EventContextualService.getInstance().changeEventImage(event.userId, event.id, file.getAbsolutePath());
                    }
                });
                
                bossImageContainer.getChildren().addAll(bossImageView, btnEditImage);
                
                Label titleLabel = createTitleLabel(event.title, colorHex);
                Label descLabel = createDescLabel(event.description);
                
                Label healthLabel = new Label("HP: " + event.bossHealth + "/" + event.bossMaxHealth);
                healthLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 16; -fx-font-weight: bold;");
                
                ProgressBar bossHealthBar = new ProgressBar(1.0);
                bossHealthBar.setPrefWidth(450);
                bossHealthBar.setPrefHeight(20);
                bossHealthBar.setStyle("-fx-accent: " + colorHex + "; -fx-control-inner-background: #331111;");
                
                Button attackButton = createStyledButton("⚔️ ATACAR!", "#a12323");
                attackButton.setPrefWidth(200);
                attackButton.setPrefHeight(50);
                
                attackButton.setOnAction(e -> {
                    int damage = 15;
                    event.bossHealth -= damage;
                    int hp = Math.max(0, event.bossHealth);
                    
                    // Animación de daño al boss
                    ScaleTransition st = new ScaleTransition(Duration.millis(50), bossImageView);
                    st.setByX(-0.1); st.setByY(-0.1);
                    st.setAutoReverse(true); st.setCycleCount(2); st.play();
                    
                    // Número de daño flotante
                    showFloatingDamage(bossImageContainer, damage);
                    
                    // Actualizar UI
                    healthLabel.setText("HP: " + hp + "/" + event.bossMaxHealth);
                    double healthPercent = (double) hp / event.bossMaxHealth;
                    bossHealthBar.setProgress(healthPercent);
                    
                    // Disparar Fase Crítica si la vida baja del 30%
                    if (healthPercent <= 0.3 && !isCriticalPhase) {
                        isCriticalPhase = true;
                        startCriticalAlert(criticalOverlay);
                    }
                    
                    if (event.bossHealth <= 0) {
                        attackButton.setDisable(true);
                        stopCriticalAlert();
                        showVictorySequence(root, event);
                    }
                });
                
                Button fleeButton = createStyledButton("Huir...", "#444");
                fleeButton.setOnAction(e -> {
                    stopCriticalAlert();
                    eventStage.close();
                    EventContextualService.getInstance().completeEvent(event.userId, event.id, false);
                });
                
                HBox actionBox = new HBox(20, attackButton, fleeButton);
                actionBox.setAlignment(Pos.CENTER);
                
                root.getChildren().addAll(bossImageContainer, titleLabel, descLabel, healthLabel, bossHealthBar, actionBox);
                
                mainLayer.getChildren().addAll(root, criticalOverlay);
                
                Scene scene = new Scene(mainLayer);
                eventStage.setScene(scene);
                
                // Entrada dramática
                mainLayer.setOpacity(0);
                mainLayer.setTranslateY(20);
                FadeTransition ft = new FadeTransition(Duration.millis(400), mainLayer);
                ft.setToValue(1.0); ft.play();
                
                eventStage.show();
                
            } catch (Exception e) {
                System.err.println("❌ Error mostrando BOSS_BATTLE: " + e.getMessage());
            }
        });
    }
    
    // --- EFECTOS ESPECIALES Y UTILIDADES DE JUEGO ---
    
    /**
     * Muestra un número rojo flotando hacia arriba cuando atacas al jefe.
     */
    private void showFloatingDamage(StackPane container, int damage) {
        Label dmgLabel = new Label("-" + damage);
        dmgLabel.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 24));
        dmgLabel.setTextFill(Color.RED);
        
        // Posición aleatoria cerca del centro
        dmgLabel.setTranslateX((Math.random() - 0.5) * 60);
        dmgLabel.setTranslateY((Math.random() - 0.5) * 60);
        
        container.getChildren().add(dmgLabel);
        
        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1), dmgLabel);
        moveUp.setByY(-50);
        
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), dmgLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        moveUp.setOnFinished(e -> container.getChildren().remove(dmgLabel));
        
        moveUp.play();
        fadeOut.play();
    }
    
    /**
     * Hace parpadear una capa roja sobre la pantalla para indicar peligro (Poca vida del Boss o poco tiempo).
     */
    private void startCriticalAlert(VBox overlay) {
        criticalAlertTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(overlay.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(500), new javafx.animation.KeyValue(overlay.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(1000), new javafx.animation.KeyValue(overlay.opacityProperty(), 0))
        );
        criticalAlertTimeline.setCycleCount(Timeline.INDEFINITE);
        criticalAlertTimeline.play();
    }
    
    private void stopCriticalAlert() {
        if (criticalAlertTimeline != null) {
            criticalAlertTimeline.stop();
        }
    }
    
    private void showVictorySequence(VBox parent, EventContextualService.ContextualEvent event) {
        Label victoryLabel = new Label("🎉 ¡JEFE DERROTADO! 🎉");
        victoryLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 28; -fx-font-weight: bold;");
        
        FadeTransition fade = new FadeTransition(Duration.seconds(1), victoryLabel);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        
        parent.getChildren().add(victoryLabel);
        
        new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            eventStage.close();
            EventContextualService.getInstance().completeEvent(event.userId, event.id, true);
        })).play();
    }
    
    // ==========================================
    // CONSTRUCTORES UI BASE
    // ==========================================
    
    private void setupBaseStage(String title, int width, int height) {
        eventStage = new Stage();
        eventStage.initStyle(StageStyle.DECORATED);
        eventStage.initModality(Modality.APPLICATION_MODAL);
        eventStage.setTitle(title);
        eventStage.setWidth(width);
        eventStage.setHeight(height);
        eventStage.setResizable(false);
        eventStage.setAlwaysOnTop(true);
        isCriticalPhase = false; // Resetear bandera crítica
    }
    
    private VBox createBaseContainer() {
        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #0f0f1a); -fx-padding: 30;");
        root.setAlignment(Pos.CENTER);
        return root;
    }
    
    private Label createTitleLabel(String text, String colorHex) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 24; -fx-font-weight: bold;");
        return lbl;
    }
    
    private Label createDescLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 15;");
        lbl.setTextAlignment(TextAlignment.CENTER);
        lbl.setWrapText(true);
        return lbl;
    }
    
    private Button createStyledButton(String text, String bgColor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-font-size: 14; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5;");
        return btn;
    }
    
    private ImageView createEventImage(String path, int size) {
        ImageView imgView = new ImageView();
        try {
            if (path != null && !path.isEmpty()) {
                Image img;
                if (path.startsWith("http") || path.startsWith("file:")) {
                    img = new Image(path);
                } else {
                    img = new Image(getClass().getResource(path).toExternalForm());
                }
                imgView.setImage(img);
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo cargar la imagen: " + path);
        }
        imgView.setFitWidth(size);
        imgView.setFitHeight(size);
        imgView.setPreserveRatio(true);
        return imgView;
    }
    
    private void showStage(VBox root) {
        Scene scene = new Scene(root);
        eventStage.setScene(scene);
        
        root.setOpacity(0);
        root.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(400), root);
        ft.setToValue(1.0); ft.play();
        
        eventStage.show();
    }
    
    // ==========================================
    // IMPLEMENTACIÓN DEL LISTENER 
    // ==========================================
    
    @Override
    public void onEventGenerated(int userId, EventContextualService.ContextualEvent event) {
        this.currentEvent = event;
        System.out.println("📢 [EventContextualUI] Precargando evento: " + event.title);
    }
    
    @Override
    public void onEventStarted(int userId, EventContextualService.ContextualEvent event) {
        System.out.println("🎮 [EventContextualUI] Invocando Interfaz para: " + event.type.displayName);
        
        switch (event.type) {
            case BREAK_TIME:
            case STRETCH_ROUTINE:
                showRestBreakDialog(event);
                break;
            case CLICK_RUSH:
            case BUG_STORM:
            case TYPING_CHALLENGE:
            case TRIVIA_QUIZ:
                showQuickMissionDialog(event);
                break;
            case BOSS_ENCOUNTER:
                showBossBattleDialog(event);
                break;
        }
    }

    @Override
    public void onEventProgressUpdated(int userId, EventContextualService.ContextualEvent event, int currentProgress, int target) {
        // En un futuro, si extraemos la barra de progreso fuera del popup, la actualizaríamos aquí.
    }

    @Override
    public void onEventCriticalPhase(int userId, EventContextualService.ContextualEvent event) {
        // En un futuro, si el servicio nos avisa externamente de que queda poco tiempo,
        // podríamos desencadenar el parpadeo de la pantalla desde aquí.
    }
    
    @Override
    public void onEventCompleted(int userId, EventContextualService.ContextualEvent event, CompletionStatus status, int xpReward, int coinReward) {
        stopCriticalAlert(); // Asegurarnos de detener animaciones infinitas
        String result = (status == CompletionStatus.VICTORY) ? "✅ SUPERADO" : "❌ " + status.name();
        System.out.println("🏁 " + result + " | Evento: " + event.title + " | Botín: +" + xpReward + " XP, +" + coinReward + " Oro");
    }
}