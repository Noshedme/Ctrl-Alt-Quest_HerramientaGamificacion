package com.ctrlaltquest.ui.utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Gestor centralizado para mantener la ventana principal maximizada en todo momento.
 * Proporciona métodos para cambiar escenas mientras se mantiene el estado maximizado.
 */
public class WindowManager {
    private static WindowManager instance;
    private Stage primaryStage;

    private WindowManager() {
    }

    public static WindowManager getInstance() {
        if (instance == null) {
            instance = new WindowManager();
        }
        return instance;
    }

    /**
     * Inicializa el manager con la ventana principal.
     * Debe ser llamado una vez en AppLauncher.
     */
    public void initialize(Stage stage) {
        this.primaryStage = stage;
        ensureMaximized();
    }

    /**
     * Cambia la escena manteniendo el estado maximizado.
     * Reemplaza la necesidad de hacer directamente stage.setScene()
     */
    public void changeScene(Parent root) {
        if (primaryStage == null) {
            throw new IllegalStateException("WindowManager no ha sido inicializado");
        }

        // Preservar dimensiones actuales si está maximizado
        boolean wasMaximized = primaryStage.isMaximized();
        double prevWidth = primaryStage.getWidth();
        double prevHeight = primaryStage.getHeight();
        double prevX = primaryStage.getX();
        double prevY = primaryStage.getY();

        // Crear nueva escena sin dimensiones fijas
        Scene newScene = new Scene(root);

        // Cambiar escena
        primaryStage.setScene(newScene);

        // Restaurar estado maximizado
        if (wasMaximized) {
            primaryStage.setMaximized(true);
        } else {
            // Si no estaba maximizado, mantener posición anterior
            primaryStage.setWidth(prevWidth);
            primaryStage.setHeight(prevHeight);
            primaryStage.setX(prevX);
            primaryStage.setY(prevY);
        }
    }

    /**
     * Asegura que la ventana esté maximizada y no sea redimensionable.
     */
    private void ensureMaximized() {
        if (primaryStage != null) {
            primaryStage.setResizable(true); // Debe ser true para que setMaximized funcione
            primaryStage.setMaximized(true);

            // Escuchar cambios que intenten restaurar la ventana
            primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    // Si alguien intenta desmaximizar, volver a maximizar
                    primaryStage.setMaximized(true);
                }
            });

            // Prevenir que se cambie el tamaño manualmente por arrastrar bordes
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (primaryStage.isMaximized()) {
                    // Si está maximizado, ignorar cambios de ancho
                    // Esto evita que se redimensione al arrastrar
                }
            });

            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (primaryStage.isMaximized()) {
                    // Si está maximizado, ignorar cambios de alto
                    // Esto evita que se redimensione al arrastrar
                }
            });
        }
    }

    /**
     * Obtiene la ventana principal.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Fuerza la ventana a estar maximizada.
     */
    public void forceMaximized() {
        if (primaryStage != null) {
            primaryStage.setMaximized(true);
        }
    }

    /**
     * Obtiene si la ventana está maximizada.
     */
    public boolean isMaximized() {
        return primaryStage != null && primaryStage.isMaximized();
    }

    /**
     * Calcula el tamaño ideal de una escena basado en la pantalla actual.
     */
    public static double[] getScreenDimensions() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        return new double[]{bounds.getWidth(), bounds.getHeight()};
    }
}
