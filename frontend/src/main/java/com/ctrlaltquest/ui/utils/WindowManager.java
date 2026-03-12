package com.ctrlaltquest.ui.utils;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * WindowManager — Mantiene la ventana principal maximizada en todo momento.
 *
 * Causa raíz del problema:
 *   new Scene(root) sin dimensiones hace que JavaFX calcule el tamaño
 *   a partir del prefWidth/prefHeight del FXML raíz (ej. 1280x720),
 *   lo que provoca que la ventana se "restaure" visualmente aunque
 *   isMaximized() siga devolviendo true internamente.
 *
 * Solución:
 *   Pasar el ancho y alto actuales al constructor de Scene para que
 *   no haya ningún recalculo de tamaño desde el FXML.
 *   setMaximized(true) en Platform.runLater como segunda capa de seguridad.
 */
public class WindowManager {

    private static WindowManager instance;
    private Stage primaryStage;

    private WindowManager() {}

    public static WindowManager getInstance() {
        if (instance == null) instance = new WindowManager();
        return instance;
    }

    // ════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN — llamar una sola vez desde AppLauncher
    // ════════════════════════════════════════════════════════════════════════

    public void initialize(Stage stage) {
        this.primaryStage = stage;
        registrarListenerMaximizado();
    }

    private void registrarListenerMaximizado() {
        if (primaryStage == null) return;

        primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                // El usuario intentó restaurar — volver a maximizar
                Platform.runLater(() -> primaryStage.setMaximized(true));
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // CAMBIO DE ESCENA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Cambia la escena manteniendo el tamaño y estado maximizado.
     *
     * Clave: se pasa el ancho y alto ACTUALES de la ventana al constructor
     * de Scene para que JavaFX no recalcule el tamaño desde el prefWidth
     * del FXML. Luego setMaximized(true) en runLater como segunda garantía.
     */
    public void changeScene(Parent root) {
        if (primaryStage == null)
            throw new IllegalStateException("WindowManager no ha sido inicializado.");

        // Capturar dimensiones ANTES de cambiar la escena
        double currentWidth  = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();

        // Crear escena con las dimensiones actuales — evita el recalculo de prefSize
        Scene newScene = new Scene(root, currentWidth, currentHeight);
        primaryStage.setScene(newScene);

        // Segunda capa: forzar maximizado después del layout pass
        Platform.runLater(() -> primaryStage.setMaximized(true));
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════════════════

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public boolean isMaximized() {
        return primaryStage != null && primaryStage.isMaximized();
    }

    public void forceMaximized() {
        if (primaryStage != null)
            Platform.runLater(() -> primaryStage.setMaximized(true));
    }
}