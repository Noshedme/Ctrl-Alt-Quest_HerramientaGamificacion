package com.ctrlaltquest.ui.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;

/**
 * Sistema de notificaciones Toast animadas para Ctrl+Alt+Quest
 * Soporta múltiples tipos: SUCCESS, ERROR, WARNING, INFO, GOLD
 * 
 * Uso fácil:
 * Toast.success("Título", "Mensaje");
 * Toast.error("Error", "Algo salió mal");
 * Toast.warning("Advertencia", "Ten cuidado");
 * Toast.info("Info", "Mensaje informativo");
 * Toast.gold("Épico", "¡Lo lograste!");
 */
public class Toast {
    
    private static final double SCREEN_OFFSET_X = 20;
    private static final double SCREEN_OFFSET_Y = 20;
    private static final double TOAST_DURATION = 4000; // 4 segundos
    private static final double ANIMATION_DURATION = 500; // 0.5 segundos
    
    public enum ToastType {
        SUCCESS("#4ade80", "✓"),
        ERROR("#ff6b6b", "✗"),
        WARNING("#f59e0b", "⚠"),
        INFO("#a855f7", "ⓘ"),
        GOLD("#f7d27a", "★");
        
        public final String color;
        public final String icon;
        
        ToastType(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }
    
    private static VBox toastContainer;
    
    /**
     * Inicializa el contenedor de Toasts (llamar al inicio de la aplicación)
     */
    public static void initialize(VBox container) {
        toastContainer = container;
        toastContainer.setStyle("-fx-background-color: transparent;");
        toastContainer.setPrefSize(400, 600);
        toastContainer.setSpacing(10);
        toastContainer.setPadding(new Insets(SCREEN_OFFSET_Y, SCREEN_OFFSET_X, SCREEN_OFFSET_Y, SCREEN_OFFSET_X));
    }
    
    /**
     * Notificación SUCCESS (Verde) - Operación exitosa
     */
    public static void success(String title, String message) {
        show(title, message, ToastType.SUCCESS);
    }
    
    /**
     * Notificación ERROR (Rojo) - Error o excepción
     */
    public static void error(String title, String message) {
        show(title, message, ToastType.ERROR);
    }
    
    /**
     * Notificación WARNING (Naranja) - Advertencia
     */
    public static void warning(String title, String message) {
        show(title, message, ToastType.WARNING);
    }
    
    /**
     * Notificación INFO (Púrpura) - Información general
     */
    public static void info(String title, String message) {
        show(title, message, ToastType.INFO);
    }
    
    /**
     * Notificación GOLD (Dorado) - Evento épico/especial
     */
    public static void gold(String title, String message) {
        show(title, message, ToastType.GOLD);
    }
    
    /**
     * Muestra un toast con tipo personalizado
     */
    private static void show(String title, String message, ToastType type) {
        if (toastContainer == null) {
            System.err.println("Toast no inicializado. Llamar Toast.initialize() al inicio.");
            return;
        }
        
        // Crear el componente Toast
        VBox toast = createToastUI(title, message, type);
        
        // Agregar al contenedor
        toastContainer.getChildren().add(toast);
        
        // Animación de entrada
        animateEntry(toast);
        
        // Programar desaparición
        PauseTransition pause = new PauseTransition(Duration.millis(TOAST_DURATION));
        pause.setOnFinished(event -> animateExit(toast, toastContainer));
        pause.play();
    }
    
    /**
     * Crea la interfaz visual del Toast
     */
    private static VBox createToastUI(String title, String message, ToastType type) {
        VBox toastBox = new VBox();
        toastBox.getStyleClass().add("toast-container");
        String typeClass = "toast-" + type.name().toLowerCase();
        toastBox.getStyleClass().add(typeClass);
        
        // Icono y título en horizontal
        HBox headerBox = new HBox();
        headerBox.setSpacing(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(type.icon);
        iconLabel.getStyleClass().add("icon");
        iconLabel.setStyle("-fx-font-weight: bold;");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");
        titleLabel.setWrapText(true);
        
        headerBox.getChildren().addAll(iconLabel, titleLabel);
        
        // Mensaje
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(320);
        
        toastBox.getChildren().addAll(headerBox, messageLabel);
        toastBox.setPrefWidth(350);
        toastBox.setMinHeight(80);
        
        return toastBox;
    }
    
    /**
     * Anima la entrada del Toast (desliza desde la derecha con fade)
     */
    private static void animateEntry(VBox toast) {
        // Posición inicial (fuera de pantalla a la derecha)
        toast.setTranslateX(400);
        toast.setOpacity(0);
        
        // Animación de traslación
        TranslateTransition translateTransition = new TranslateTransition(
            Duration.millis(ANIMATION_DURATION), toast
        );
        translateTransition.setToX(0);
        translateTransition.play();
        
        // Animación de opacidad (fade in)
        FadeTransition fadeTransition = new FadeTransition(
            Duration.millis(ANIMATION_DURATION), toast
        );
        fadeTransition.setToValue(1);
        fadeTransition.play();
        
        // Efecto de escala para más dinamismo
        ScaleTransition scaleTransition = new ScaleTransition(
            Duration.millis(ANIMATION_DURATION), toast
        );
        scaleTransition.setFromX(0.9);
        scaleTransition.setFromY(0.9);
        scaleTransition.setToX(1);
        scaleTransition.setToY(1);
        scaleTransition.play();
    }
    
    /**
     * Anima la salida del Toast (desliza a la derecha con fade)
     */
    private static void animateExit(VBox toast, VBox container) {
        TranslateTransition translateTransition = new TranslateTransition(
            Duration.millis(ANIMATION_DURATION), toast
        );
        translateTransition.setToX(400);
        
        FadeTransition fadeTransition = new FadeTransition(
            Duration.millis(ANIMATION_DURATION), toast
        );
        fadeTransition.setToValue(0);
        
        ScaleTransition scaleTransition = new ScaleTransition(
            Duration.millis(ANIMATION_DURATION), toast
        );
        scaleTransition.setToX(0.9);
        scaleTransition.setToY(0.9);
        
        // Al finalizar, remover del contenedor
        fadeTransition.setOnFinished(event -> container.getChildren().remove(toast));
        
        translateTransition.play();
        fadeTransition.play();
        scaleTransition.play();
    }
    
    /**
     * Muestra un error de form (validación)
     * Toast.formError("Campo Obligatorio", "El email es requerido");
     */
    public static void formError(String field, String message) {
        error("❌ " + field, message);
    }
    
    /**
     * Muestra éxito de form
     * Toast.formSuccess("Registro", "Cuenta creada exitosamente");
     */
    public static void formSuccess(String title, String message) {
        success("✔️ " + title, message);
    }
    
    /**
     * Muestra un error tipo Exception (para try/catch)
     * Toast.exception("Error de Base de Datos", exception);
     */
    public static void exception(String title, Exception e) {
        String message = e.getLocalizedMessage() != null ? 
            e.getLocalizedMessage() : e.getClass().getSimpleName();
        error(title, message);
    }
    
    /**
     * Muestra notificación larga (épica)
     */
    public static void epic(String title, String message) {
        if (toastContainer == null) {
            System.err.println("Toast no inicializado. Llamar Toast.initialize() al inicio.");
            return;
        }
        
        VBox toast = createToastUI(title, message, ToastType.GOLD);
        toast.getStyleClass().add("toast-epic");
        toast.setPrefWidth(450);
        
        toastContainer.getChildren().add(toast);
        animateEntry(toast);
        
        // Duración más larga para épicos (6 segundos)
        PauseTransition pause = new PauseTransition(Duration.millis(6000));
        pause.setOnFinished(event -> animateExit(toast, toastContainer));
        pause.play();
    }
}
