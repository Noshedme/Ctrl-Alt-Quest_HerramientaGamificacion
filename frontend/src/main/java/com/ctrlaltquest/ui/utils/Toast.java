package com.ctrlaltquest.ui.utils;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Sistema de notificaciones Toast animadas para Ctrl+Alt+Quest.
 *
 * Anti-spam mejorado:
 *  - DUPLICATE_COOLDOWN: mismo mensaje no se repite en X ms
 *  - GLOBAL_COOLDOWN: mínimo tiempo entre cualquier par de toasts
 *  - MAX_VISIBLE_TOASTS: límite de toasts en pantalla a la vez
 */
public class Toast {

    // ── Configuración ────────────────────────────────────────────────────────
    private static final double SCREEN_OFFSET_X    = 20;
    private static final double SCREEN_OFFSET_Y    = 20;
    private static final double TOAST_DURATION     = 4000; // ms que dura visible
    private static final double ANIMATION_DURATION = 400;  // ms de animación entrada/salida
    private static final int    MAX_VISIBLE_TOASTS = 4;    // máximo en pantalla

    /**
     * Cooldown por mensaje duplicado (ms).
     * Un mismo título+mensaje no puede aparecer de nuevo antes de este tiempo.
     */
    private static final long DUPLICATE_COOLDOWN = 5000; // 5 segundos

    /**
     * Cooldown global entre cualquier toast (ms).
     * Evita el spam aunque sean mensajes distintos.
     */
    private static final long GLOBAL_COOLDOWN = 800; // 0.8 segundos

    // ── Estado interno ────────────────────────────────────────────────────────
    private static VBox toastContainer;
    private static final Map<String, Long> recentToasts = new HashMap<>();
    private static long lastToastTime = 0; // Para el cooldown global

    // ── Tipos ─────────────────────────────────────────────────────────────────
    public enum ToastType {
        SUCCESS("#4ade80", "✓"),
        ERROR  ("#ff6b6b", "✗"),
        WARNING("#f59e0b", "⚠"),
        INFO   ("#a855f7", "ⓘ"),
        GOLD   ("#f7d27a", "★");

        public final String color;
        public final String icon;

        ToastType(String color, String icon) {
            this.color = color;
            this.icon  = icon;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ════════════════════════════════════════════════════════════════════════

    public static void initialize(VBox container) {
        toastContainer = container;
        toastContainer.setStyle("-fx-background-color: transparent;");
        toastContainer.setPrefSize(400, 600);
        toastContainer.setSpacing(10);
        toastContainer.setPadding(new Insets(SCREEN_OFFSET_Y, SCREEN_OFFSET_X,
                                             SCREEN_OFFSET_Y, SCREEN_OFFSET_X));
    }

    // ════════════════════════════════════════════════════════════════════════
    // API PÚBLICA
    // ════════════════════════════════════════════════════════════════════════

    public static void success(String title, String message) { show(title, message, ToastType.SUCCESS); }
    public static void error  (String title, String message) { show(title, message, ToastType.ERROR);   }
    public static void warning(String title, String message) { show(title, message, ToastType.WARNING); }
    public static void info   (String title, String message) { show(title, message, ToastType.INFO);    }
    public static void gold   (String title, String message) { show(title, message, ToastType.GOLD);    }

    public static void formError  (String field, String msg) { error("❌ " + field, msg); }
    public static void formSuccess(String title, String msg) { success("✔️ " + title, msg); }
    public static void exception  (String title, Exception e) {
        String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getSimpleName();
        error(title, msg);
    }

    public static void epic(String title, String message) {
        if (!puedeMotrar(title, message)) return;
        VBox toast = createToastUI(title, message, ToastType.GOLD);
        toast.setPrefWidth(450);
        mostrarToast(toast, 6000);
    }

    // ════════════════════════════════════════════════════════════════════════
    // LÓGICA CENTRAL
    // ════════════════════════════════════════════════════════════════════════

    private static void show(String title, String message, ToastType type) {
        if (!puedeMotrar(title, message)) return;
        mostrarToast(createToastUI(title, message, type), TOAST_DURATION);
    }

    /**
     * Verifica si el toast puede mostrarse según los cooldowns.
     * Registra el tiempo si pasa los filtros.
     */
    private static boolean puedeMotrar(String title, String message) {
        if (toastContainer == null) {
            System.err.println("Toast no inicializado. Llamar Toast.initialize() primero.");
            return false;
        }

        long now = System.currentTimeMillis();

        // 1. Cooldown global — evita ráfagas de toasts distintos
        if (now - lastToastTime < GLOBAL_COOLDOWN) return false;

        // 2. Cooldown por mensaje duplicado
        String key = title + "|" + message;
        Long lastTime = recentToasts.get(key);
        if (lastTime != null && now - lastTime < DUPLICATE_COOLDOWN) return false;

        // Pasó los filtros → registrar
        lastToastTime = now;
        recentToasts.put(key, now);

        // Limpiar entradas viejas del mapa para no crecer infinitamente
        if (recentToasts.size() > 50) {
            recentToasts.entrySet().removeIf(e -> now - e.getValue() > DUPLICATE_COOLDOWN * 2);
        }

        return true;
    }

    private static void mostrarToast(VBox toast, double duracionMs) {
        // Límite de toasts visibles: eliminar el más antiguo
        if (toastContainer.getChildren().size() >= MAX_VISIBLE_TOASTS) {
            toastContainer.getChildren().remove(0);
        }

        toastContainer.getChildren().add(toast);
        animateEntry(toast);

        PauseTransition pause = new PauseTransition(Duration.millis(duracionMs));
        pause.setOnFinished(e -> animateExit(toast, toastContainer));
        pause.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // UI DEL TOAST
    // ════════════════════════════════════════════════════════════════════════

    private static VBox createToastUI(String title, String message, ToastType type) {
        VBox toastBox = new VBox();
        toastBox.setStyle(
            "-fx-background-color: " + type.color + "cc;" +
            "-fx-padding: 15;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(type.icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        headerBox.getChildren().addAll(iconLabel, titleLabel);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(320);

        toastBox.getChildren().addAll(headerBox, messageLabel);
        toastBox.setPrefWidth(350);
        toastBox.setMinHeight(80);

        return toastBox;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ANIMACIONES
    // ════════════════════════════════════════════════════════════════════════

    private static void animateEntry(VBox toast) {
        toast.setTranslateX(400);
        toast.setOpacity(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(ANIMATION_DURATION), toast);
        tt.setToX(0); tt.play();

        FadeTransition ft = new FadeTransition(Duration.millis(ANIMATION_DURATION), toast);
        ft.setToValue(1); ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(ANIMATION_DURATION), toast);
        st.setFromX(0.9); st.setFromY(0.9);
        st.setToX(1);     st.setToY(1);
        st.play();
    }

    private static void animateExit(VBox toast, VBox container) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ANIMATION_DURATION), toast);
        tt.setToX(400); tt.play();

        FadeTransition ft = new FadeTransition(Duration.millis(ANIMATION_DURATION), toast);
        ft.setToValue(0);
        ft.setOnFinished(e -> container.getChildren().remove(toast));
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(ANIMATION_DURATION), toast);
        st.setToX(0.9); st.setToY(0.9); st.play();
    }
}