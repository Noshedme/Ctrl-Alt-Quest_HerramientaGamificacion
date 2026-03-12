package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.models.EventType;
import com.ctrlaltquest.services.EventContextualService;
import com.ctrlaltquest.services.EventContextualService.ContextualEvent;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

/**
 * EventModalController — Motor visual e interactivo del modal de eventos.
 *
 * Gestiona los 5 tipos de interacción:
 *   - BOSS_ENCOUNTER  → clicks con barra de vida
 *   - CLICK_RUSH      → clicks rápidos
 *   - BUG_STORM       → clicks rápidos (variante)
 *   - TYPING_CHALLENGE → escritura de palabras
 *   - BREAK_TIME / STRETCH_ROUTINE → timer pasivo
 *   - TRIVIA_QUIZ     → preguntas con opciones
 */
public class EventModalController {

    // ── Estructura general ────────────────────────────────────────────────────
    @FXML private VBox rootContainer;
    @FXML private HBox headerBox;
    @FXML private Label lblEventIcon;
    @FXML private Label lblEventType;
    @FXML private Label lblEventTitle;
    @FXML private Label lblTimer;
    @FXML private Label lblDescription;
    @FXML private Label lblRewards;
    @FXML private Label lblProgressLabel;
    @FXML private Label lblProgressValue;
    @FXML private ProgressBar progressBar;
    @FXML private ImageView imgEvent;
    @FXML private Region imageGlow;

    // ── Boss ─────────────────────────────────────────────────────────────────
    @FXML private VBox bossHealthSection;
    @FXML private ProgressBar bossHealthBar;
    @FXML private Label lblBossHP;
    @FXML private Label lblDamageFloat;

    // ── Panel Click (Boss / ClickRush / BugStorm) ────────────────────────────
    @FXML private VBox panelClick;
    @FXML private Label lblClickInstruction;
    @FXML private StackPane clickZone;
    @FXML private Label lblClickIcon;
    @FXML private Label lblClickCount;
    @FXML private Label lblClickCombo;

    // ── Panel Typing ─────────────────────────────────────────────────────────
    @FXML private VBox panelTyping;
    @FXML private TextArea txtTarget;
    @FXML private TextField txtInput;
    @FXML private Label lblTypingFeedback;

    // ── Panel Pasivo (Break / Stretch) ────────────────────────────────────────
    @FXML private VBox panelPassive;
    @FXML private Label lblPassiveTitle;
    @FXML private Label lblPassiveInstruction;
    @FXML private ProgressIndicator passiveProgress;
    @FXML private Label lblPassiveCountdown;
    @FXML private Button btnPassiveDone;

    // ── Panel Trivia ─────────────────────────────────────────────────────────
    @FXML private VBox panelTrivia;
    @FXML private Label lblTriviaProgress;
    @FXML private Label lblTriviaQuestion;
    @FXML private VBox triviaOptionsContainer;
    @FXML private Label lblTriviaFeedback;

    // ── Estado interno ────────────────────────────────────────────────────────
    private ContextualEvent currentEvent;
    private Stage stage;
    private int userId;

    private Timeline countdownTimeline;
    private Timeline passiveTimeline;
    private int secondsRemaining;

    // Click
    private int clickCount = 0;
    private int comboCount = 0;
    private long lastClickTime = 0;

    // Typing
    private List<String> wordsToType = new ArrayList<>();

    // Trivia
    private List<TriviaQuestion> currentQuestions;
    private int triviaIndex = 0;
    private int triviaCorrect = 0;

    // Pasivo
    private int passiveTotalSeconds;
    private boolean passiveCompleted = false;

    // ════════════════════════════════════════════════════════════════════════
    // PUNTO DE ENTRADA PÚBLICO
    // ════════════════════════════════════════════════════════════════════════

    public void setEvento(int userId, ContextualEvent event, Stage stage) {
        this.userId = userId;
        this.currentEvent = event;
        this.stage = stage;
        configurarUI();

        // Los eventos pasivos (Break / Stretch) NO usan timer global:
        // tienen su propio passiveTimeline interno.
        // El timer global solo aplica a eventos de interacción activa.
        boolean esEventoPasivo = (event.type == EventType.BREAK_TIME ||
                                  event.type == EventType.STRETCH_ROUTINE);
        if (!esEventoPasivo) {
            iniciarTimerGlobal();
        } else {
            lblTimer.setText("--:--");
            lblTimer.setStyle("-fx-text-fill: #555; -fx-font-size: 26px; -fx-font-weight: 900;");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE UI SEGÚN TIPO
    // ════════════════════════════════════════════════════════════════════════

    private void configurarUI() {
        EventType type = currentEvent.type;
        String color = type.themeColor;

        // Cabecera
        lblEventIcon.setText(type.icon);
        lblEventType.setText(type.displayName.toUpperCase());
        lblEventTitle.setText(currentEvent.title);
        lblDescription.setText(currentEvent.description);
        lblRewards.setText("🏆  +" + currentEvent.baseXp + " XP   |   +" + currentEvent.baseCoin + " 💰");

        // Color temático en borde y barra de progreso
        rootContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(8,4,18,0.99), rgba(15,8,30,0.99));" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 2; -fx-background-radius: 20; -fx-border-radius: 20;");
        headerBox.setStyle(
            "-fx-padding: 20 28 18 28;" +
            "-fx-background-color: linear-gradient(to right, " + hexToRgba(color, 0.30) + ", transparent);" +
            "-fx-background-radius: 20 20 0 0;" +
            "-fx-border-color: transparent transparent " + hexToRgba(color, 0.35) + " transparent;" +
            "-fx-border-width: 1;");
        progressBar.setStyle(
            "-fx-accent: " + color + "; -fx-control-inner-background: rgba(10,5,25,0.7);" +
            "-fx-background-radius: 6; -fx-border-radius: 6;");
        imageGlow.setStyle(
            "-fx-background-color: radial-gradient(radius 60%, " + hexToRgba(color, 0.45) + ", transparent);" +
            "-fx-background-radius: 110;");

        // Timer
        secondsRemaining = type.baseDuration;
        actualizarDisplayTimer(secondsRemaining);

        // Imagen
        cargarImagenEvento(currentEvent.imagePath);

        // Panel según tipo
        ocultarTodosLosPaneles();
        switch (type) {
            case BOSS_ENCOUNTER            -> configurarPanelBoss();
            case CLICK_RUSH, BUG_STORM     -> configurarPanelClick();
            case TYPING_CHALLENGE          -> configurarPanelTyping();
            case BREAK_TIME, STRETCH_ROUTINE -> configurarPanelPasivo();
            case TRIVIA_QUIZ               -> configurarPanelTrivia();
        }

        animarEntrada();
    }

    // ─── Boss ─────────────────────────────────────────────────────────────────

    private void configurarPanelBoss() {
        mostrarPanel(panelClick);
        bossHealthSection.setVisible(true);
        bossHealthSection.setManaged(true);

        int hp = currentEvent.bossMaxHealth;
        currentEvent.bossHealth = hp;
        lblBossHP.setText(hp + " / " + hp);
        bossHealthBar.setProgress(1.0);

        lblClickInstruction.setText("¡HAZ CLIC PARA ATACAR! Los combos rápidos hacen más daño.");
        lblClickIcon.setText("⚔️");
        lblClickCount.setText("0 GOLPES");
        lblProgressLabel.setText("DAÑO CAUSADO");
        lblProgressValue.setText("0 / " + hp);
    }

    // ─── Click Rush / Bug Storm ───────────────────────────────────────────────

    private void configurarPanelClick() {
        mostrarPanel(panelClick);
        lblClickInstruction.setText(
            currentEvent.type == EventType.BUG_STORM
                ? "¡APLASTA TODOS LOS BUGS! Cada clic elimina uno."
                : "¡FRENESÍ! Haz clic tan rápido como puedas.");
        lblClickIcon.setText(currentEvent.type == EventType.BUG_STORM ? "🐛" : "🖱️");
        lblClickCount.setText("0 CLICKS");
        lblProgressLabel.setText("CLICKS REALIZADOS");
        lblProgressValue.setText("0 / " + currentEvent.targetCount);
    }

    // ─── Typing ───────────────────────────────────────────────────────────────

    private void configurarPanelTyping() {
        mostrarPanel(panelTyping);
        int cantidad = Math.min(currentEvent.targetCount, 8);
        wordsToType = generarPalabras(cantidad);
        txtTarget.setText(String.join("  ·  ", wordsToType));
        txtInput.clear();
        lblTypingFeedback.setText("");
        lblProgressLabel.setText("PALABRAS ESCRITAS");
        lblProgressValue.setText("0 / " + wordsToType.size());
        Platform.runLater(() -> txtInput.requestFocus());
    }

    // ─── Pasivo ───────────────────────────────────────────────────────────────

    private void configurarPanelPasivo() {
        mostrarPanel(panelPassive);
        passiveTotalSeconds = currentEvent.restTimeSeconds;
        passiveCompleted = false;
        lblPassiveCountdown.setText(String.valueOf(passiveTotalSeconds));
        passiveProgress.setProgress(1.0);
        btnPassiveDone.setDisable(true);

        boolean esStretch = currentEvent.type == EventType.STRETCH_ROUTINE;
        String color = esStretch ? "#06b6d4" : "#10b981";

        lblPassiveTitle.setText(esStretch ? "🧘 PAUSA ACTIVA" : "🔥 SANTUARIO DE DESCANSO");
        lblPassiveInstruction.setText(esStretch
            ? "Levántate de tu silla y estira tu cuerpo durante " + passiveTotalSeconds + " segundos.\nTu espalda y cuello te lo agradecerán."
            : "Aléjate de la pantalla y cierra los ojos durante " + passiveTotalSeconds + " segundos.\nDeja que tus ojos y mente descansen.");
        passiveProgress.setStyle("-fx-progress-color: " + color + "; -fx-accent: " + color + ";");
        lblPassiveCountdown.setStyle(
            "-fx-text-fill: " + color + "; -fx-font-size: 36px; -fx-font-weight: 900;");
        btnPassiveDone.setStyle(
            "-fx-font-size: 14px; -fx-padding: 12 30;" +
            "-fx-background-color: " + hexToRgba(color, 0.3) + ";" +
            "-fx-border-color: " + color + "; -fx-border-radius: 12;" +
            "-fx-text-fill: " + color + "; -fx-border-width: 2; -fx-cursor: hand; -fx-font-weight: 900;");

        lblProgressLabel.setText("TIEMPO RESTANTE");
        lblProgressValue.setText(passiveTotalSeconds + " seg");

        iniciarTimerPasivo();
    }

    // ─── Trivia ───────────────────────────────────────────────────────────────

    private void configurarPanelTrivia() {
        mostrarPanel(panelTrivia);
        List<TriviaQuestion> banco = new ArrayList<>(TRIVIA_BANK);
        Collections.shuffle(banco);
        int total = Math.min(currentEvent.targetCount, banco.size());
        currentQuestions = new ArrayList<>(banco.subList(0, total));
        triviaIndex = 0;
        triviaCorrect = 0;
        lblProgressLabel.setText("PREGUNTAS RESPONDIDAS");
        lblProgressValue.setText("0 / " + currentQuestions.size());
        mostrarPreguntaTrivia();
    }

    // ════════════════════════════════════════════════════════════════════════
    // HANDLERS DE INTERACCIÓN
    // ════════════════════════════════════════════════════════════════════════

    // ─── Click ────────────────────────────────────────────────────────────────

    @FXML
    public void registrarClick(MouseEvent e) {
        clickCount++;
        long now = System.currentTimeMillis();
        comboCount = (now - lastClickTime < 350) ? comboCount + 1 : 1;
        lastClickTime = now;

        int dano = 1 + (comboCount / 5);
        int current, target;

        if (currentEvent.type == EventType.BOSS_ENCOUNTER) {
            actualizarBossHP(dano);
            target  = currentEvent.bossMaxHealth;
            current = target - currentEvent.bossHealth;
        } else {
            target  = currentEvent.targetCount;
            current = clickCount;
        }

        double progress = Math.min(1.0, (double) current / target);
        actualizarProgreso(progress, current, target);

        lblClickCount.setText(clickCount +
            (currentEvent.type == EventType.BOSS_ENCOUNTER ? " GOLPES" : " CLICKS"));
        lblClickCombo.setText(comboCount >= 3 ? "🔥 COMBO x" + comboCount + "!" : "");

        animarClickZone();
        if (progress >= 1.0) completarEventoExito();
    }

    private void actualizarBossHP(int dano) {
        currentEvent.bossHealth = Math.max(0, currentEvent.bossHealth - dano);
        double ratio = (double) currentEvent.bossHealth / currentEvent.bossMaxHealth;
        bossHealthBar.setProgress(ratio);
        lblBossHP.setText(currentEvent.bossHealth + " / " + currentEvent.bossMaxHealth);

        // Daño flotante
        lblDamageFloat.setText("-" + dano + " HP");
        lblDamageFloat.setOpacity(1.0);
        FadeTransition ft = new FadeTransition(Duration.millis(700), lblDamageFloat);
        ft.setFromValue(1.0); ft.setToValue(0.0); ft.play();

        // Fase crítica (< 20% HP)
        if (ratio > 0.0 && ratio <= 0.20) {
            bossHealthBar.setStyle(
                "-fx-accent: #ff8c00; -fx-control-inner-background: rgba(10,5,25,0.7);" +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
            EventContextualService.getInstance().notifyEventCritical(userId, currentEvent.id);
        }
    }

    // ─── Typing ───────────────────────────────────────────────────────────────

    @FXML
    public void onTypingKeyReleased() {
        String input = txtInput.getText().trim();
        String[] parts = input.split("\\s+");

        int correctas = 0;
        for (int i = 0; i < Math.min(parts.length, wordsToType.size()); i++) {
            if (parts[i].equalsIgnoreCase(wordsToType.get(i))) correctas++;
        }

        int total = wordsToType.size();
        double progress = Math.min(1.0, (double) correctas / total);
        actualizarProgreso(progress, correctas, total);

        if (correctas == total && total > 0) {
            lblTypingFeedback.setText("✓ ¡PERFECTO! Todas las palabras escritas.");
            lblTypingFeedback.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;");
            completarEventoExito();
        } else if (correctas > 0) {
            lblTypingFeedback.setText("✓ " + correctas + " palabra(s) correcta(s)...");
            lblTypingFeedback.setStyle("-fx-text-fill: #4ecdc4; -fx-font-size: 12px;");
        } else {
            lblTypingFeedback.setText("");
        }
    }

    // ─── Pasivo ───────────────────────────────────────────────────────────────

    private void iniciarTimerPasivo() {
        final int[] rem = {passiveTotalSeconds};
        passiveTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            rem[0]--;
            double ratio = (double) rem[0] / passiveTotalSeconds;
            passiveProgress.setProgress(ratio);
            lblPassiveCountdown.setText(String.valueOf(rem[0]));
            lblProgressValue.setText(rem[0] + " seg");
            actualizarProgreso(1.0 - ratio, passiveTotalSeconds - rem[0], passiveTotalSeconds);

            if (rem[0] <= 0) {
                passiveTimeline.stop();
                passiveCompleted = true;
                btnPassiveDone.setDisable(false);
                lblPassiveInstruction.setText("✅ ¡Tiempo completado! Pulsa el botón para reclamar tu recompensa.");
                lblPassiveInstruction.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px;");
            }
        }));
        passiveTimeline.setCycleCount(passiveTotalSeconds);
        passiveTimeline.play();
    }

    @FXML
    public void completarEventoPasivo() {
        if (!passiveCompleted) return;
        if (passiveTimeline != null) passiveTimeline.stop();
        completarEventoExito();
    }

    // ─── Trivia ───────────────────────────────────────────────────────────────

    private void mostrarPreguntaTrivia() {
        if (triviaIndex >= currentQuestions.size()) {
            completarEventoExito();
            return;
        }
        TriviaQuestion q = currentQuestions.get(triviaIndex);
        lblTriviaQuestion.setText(q.question);
        lblTriviaProgress.setText((triviaIndex + 1) + " / " + currentQuestions.size());
        lblTriviaFeedback.setText("");
        triviaOptionsContainer.getChildren().clear();

        List<String> opciones = new ArrayList<>(q.wrongAnswers);
        opciones.add(q.correctAnswer);
        Collections.shuffle(opciones);

        for (String opcion : opciones) {
            Button btn = new Button(opcion);
            btn.setMaxWidth(Double.MAX_VALUE);
            aplicarEstiloOpcionTrivia(btn, false, false);
            btn.setOnMouseEntered(e -> aplicarEstiloOpcionTrivia(btn, false, true));
            btn.setOnMouseExited(e -> aplicarEstiloOpcionTrivia(btn, false, false));
            boolean esCorrecta = opcion.equals(q.correctAnswer);
            btn.setOnAction(ev -> responderTrivia(btn, esCorrecta));
            triviaOptionsContainer.getChildren().add(btn);
        }
    }

    private void aplicarEstiloOpcionTrivia(Button btn, boolean correcto, boolean hover) {
        if (correcto) {
            btn.setStyle(
                "-fx-font-size: 13px; -fx-padding: 10 18; -fx-cursor: hand;" +
                "-fx-background-color: rgba(74,222,128,0.3); -fx-border-color: #4ade80;" +
                "-fx-border-radius: 10; -fx-text-fill: #ffffff; -fx-border-width: 2; -fx-background-radius: 10;");
        } else if (hover) {
            btn.setStyle(
                "-fx-font-size: 13px; -fx-padding: 10 18; -fx-cursor: hand;" +
                "-fx-background-color: rgba(244,114,182,0.25); -fx-border-color: #f472b6;" +
                "-fx-border-radius: 10; -fx-text-fill: #ffffff; -fx-border-width: 2; -fx-background-radius: 10;");
        } else {
            btn.setStyle(
                "-fx-font-size: 13px; -fx-padding: 10 18; -fx-cursor: hand;" +
                "-fx-background-color: rgba(244,114,182,0.10); -fx-border-color: rgba(244,114,182,0.35);" +
                "-fx-border-radius: 10; -fx-text-fill: #e0e0e0; -fx-border-width: 1.5; -fx-background-radius: 10;");
        }
    }

    private void responderTrivia(Button btnSel, boolean esCorrecta) {
        triviaOptionsContainer.getChildren().forEach(n -> n.setDisable(true));

        if (esCorrecta) {
            triviaCorrect++;
            aplicarEstiloOpcionTrivia(btnSel, true, false);
            lblTriviaFeedback.setText("✓ ¡CORRECTO! Puntos: " + triviaCorrect);
            lblTriviaFeedback.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            btnSel.setStyle(btnSel.getStyle() +
                "-fx-background-color: rgba(255,71,87,0.3); -fx-border-color: #ff4757;");
            lblTriviaFeedback.setText("✗ Incorrecto. Respuesta: " + currentQuestions.get(triviaIndex).correctAnswer);
            lblTriviaFeedback.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
        }

        triviaIndex++;
        actualizarProgreso(
            (double) triviaIndex / currentQuestions.size(),
            triviaIndex, currentQuestions.size());

        new Timeline(new KeyFrame(Duration.millis(1300), e -> mostrarPreguntaTrivia())).play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TIMER GLOBAL
    // ════════════════════════════════════════════════════════════════════════

    private void iniciarTimerGlobal() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            actualizarDisplayTimer(secondsRemaining);

            if (secondsRemaining <= 10) {
                lblTimer.setStyle(
                    "-fx-text-fill: #ff4757; -fx-font-size: 28px; -fx-font-weight: 900;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(255,71,87,0.9), 18, 0, 0, 0);");
                ScaleTransition pulse = new ScaleTransition(Duration.millis(400), lblTimer);
                pulse.setFromX(1.0); pulse.setToX(1.20);
                pulse.setAutoReverse(true); pulse.setCycleCount(2); pulse.play();
            }

            if (secondsRemaining <= 0) {
                countdownTimeline.stop();
                timeoutEvento();
            }
        }));
        countdownTimeline.setCycleCount(secondsRemaining);
        countdownTimeline.play();
    }

    private void actualizarDisplayTimer(int secs) {
        lblTimer.setText(String.format("%02d:%02d", secs / 60, secs % 60));
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESOLUCIÓN
    // ════════════════════════════════════════════════════════════════════════

    private void completarEventoExito() {
        limpiarTimers();
        EventContextualService.getInstance().completeEvent(userId, currentEvent.id, true);
        Toast.success("🏆 ¡VICTORIA!", "+" + currentEvent.baseXp + " XP  |  +" + currentEvent.baseCoin + " 💰");
        cerrarConAnimacion();
    }

    @FXML
    public void huirDelEvento() {
        limpiarTimers();
        EventContextualService.getInstance().completeEvent(userId, currentEvent.id, false);
        Toast.error("🏃 Huiste", "Evento abandonado. Sin recompensa.");
        cerrarConAnimacion();
    }

    private void timeoutEvento() {
        limpiarTimers();
        EventContextualService.getInstance().completeEvent(userId, currentEvent.id, false);
        Platform.runLater(() -> Toast.error("⏰ Tiempo agotado", "Sin recompensa."));
        cerrarConAnimacion();
    }

    private void cerrarConAnimacion() {
        Platform.runLater(() -> {
            FadeTransition fade = new FadeTransition(Duration.millis(300), rootContainer);
            ScaleTransition scale = new ScaleTransition(Duration.millis(300), rootContainer);
            fade.setToValue(0.0);
            scale.setToX(0.90); scale.setToY(0.90);
            fade.setOnFinished(e -> { if (stage != null) stage.close(); });
            fade.play(); scale.play();
        });
    }

    private void limpiarTimers() {
        if (countdownTimeline != null) countdownTimeline.stop();
        if (passiveTimeline != null) passiveTimeline.stop();
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES DE UI
    // ════════════════════════════════════════════════════════════════════════

    private void ocultarTodosLosPaneles() {
        for (VBox p : List.of(panelClick, panelTyping, panelPassive, panelTrivia)) {
            p.setVisible(false); p.setManaged(false);
        }
        bossHealthSection.setVisible(false);
        bossHealthSection.setManaged(false);
    }

    private void mostrarPanel(VBox panel) {
        panel.setVisible(true); panel.setManaged(true);
    }

    private void actualizarProgreso(double ratio, int current, int total) {
        progressBar.setProgress(Math.min(1.0, ratio));
        lblProgressValue.setText(current + " / " + total);
        EventContextualService.getInstance()
            .notifyEventProgress(userId, currentEvent.id, current, total);
    }

    private void cargarImagenEvento(String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) return;
            URL url = getClass().getResource(imagePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), 200, 200, true, true, true);
                img.progressProperty().addListener((obs, old, prog) -> {
                    if (prog.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> imgEvent.setImage(img));
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    private void animarEntrada() {
        rootContainer.setScaleX(0.82); rootContainer.setScaleY(0.82);
        rootContainer.setOpacity(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(380), rootContainer);
        scale.setToX(1.0); scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(380), rootContainer);
        fade.setToValue(1.0);

        scale.play(); fade.play();
    }

    private void animarClickZone() {
        if (clickZone == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(70), clickZone);
        st.setToX(0.92); st.setToY(0.92);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    private String hexToRgba(String hex, double alpha) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERADORES DE CONTENIDO
    // ════════════════════════════════════════════════════════════════════════

    private static final List<String> WORD_BANK = List.of(
        "algoritmo", "función", "variable", "clase", "objeto",
        "interfaz", "herencia", "polimorfismo", "encapsulamiento", "abstracción",
        "compilador", "depurador", "repositorio", "commit", "branch",
        "recursión", "iteración", "puntero", "proceso", "hilo",
        "socket", "protocolo", "latencia", "servidor", "cliente"
    );

    private List<String> generarPalabras(int count) {
        List<String> shuffled = new ArrayList<>(WORD_BANK);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // BANCO DE TRIVIA
    // ════════════════════════════════════════════════════════════════════════

    private record TriviaQuestion(String question, String correctAnswer, List<String> wrongAnswers) {}

    private static final List<TriviaQuestion> TRIVIA_BANK = List.of(
        new TriviaQuestion("¿Qué significa 'RAM'?",
            "Random Access Memory",
            List.of("Read And Modify", "Remote Access Module", "Runtime Application Manager")),
        new TriviaQuestion("¿Qué comando de Git guarda cambios localmente?",
            "git commit",
            List.of("git push", "git save", "git merge")),
        new TriviaQuestion("¿Qué es un 'bug' en programación?",
            "Un error en el código",
            List.of("Una librería externa", "Un tipo de variable", "Un framework de testing")),
        new TriviaQuestion("¿Qué protocolo usa HTTPS?",
            "TLS/SSL",
            List.of("FTP", "UDP", "SMTP")),
        new TriviaQuestion("¿Qué hace el operador '==' en Java con objetos?",
            "Compara referencias de memoria",
            List.of("Compara el contenido", "Lanza una excepción", "Asigna un valor")),
        new TriviaQuestion("¿Cuántos bits tiene un byte?",
            "8",
            List.of("4", "16", "32")),
        new TriviaQuestion("¿Qué es la recursión?",
            "Una función que se llama a sí misma",
            List.of("Un bucle for avanzado", "Una herramienta de debug", "Un patrón de diseño")),
        new TriviaQuestion("¿Qué significa 'API'?",
            "Application Programming Interface",
            List.of("Automated Program Integration", "Advanced Protocol Index", "Application Process Instance")),
        new TriviaQuestion("¿Qué es el 'stack overflow' en ejecución?",
            "Desbordamiento de la pila de llamadas",
            List.of("Error de memoria heap", "Falla del compilador", "Error de red")),
        new TriviaQuestion("¿Qué es un 'commit' en Git?",
            "Un punto de guardado del historial",
            List.of("Una rama nueva", "Un servidor remoto", "Un conflicto de fusión")),
        new TriviaQuestion("¿Cuál es la complejidad de buscar en un HashMap?",
            "O(1) en promedio",
            List.of("O(n)", "O(log n)", "O(n²)")),
        new TriviaQuestion("¿Qué es el patrón Singleton?",
            "Una clase con una única instancia global",
            List.of("Un objeto inmutable", "Una interfaz funcional", "Un tipo de colección"))
    );
}