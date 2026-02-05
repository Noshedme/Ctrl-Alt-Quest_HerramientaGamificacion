package com.ctrlaltquest.ui.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AppLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Cargar fuentes al inicio
        loadCustomFont("/assets/fonts/pixelcastle/Pixelcastle-Regular.otf");
        loadCustomFont("/assets/fonts/runewood/Runewood.ttf");

        // 2. Cargar el FXML inicial (Splash o Login)
        URL fxmlUrl = getClass().getResource("/fxml/splash.fxml");
        if (fxmlUrl == null) {
            System.err.println("ERROR CRÍTICO: No se encontró /fxml/splash.fxml");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            
            // Creamos la escena. Puedes quitar las dimensiones (960, 540) si quieres
            // que se adapte al contenido, pero dejarlas está bien como tamaño "restaurado".
            Scene scene = new Scene(loader.load()); 
            
            // --- SONIDO DE TECLADO GLOBAL ---
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                SoundManager.playKeyClick();
            });

            stage.setTitle("Ctrl + Alt + Quest");
            stage.setScene(scene);
            
            // Permitir redimensión es obligatorio para que funcione el maximizado
            stage.setResizable(true);
            
            // 3. MOSTRAR Y MAXIMIZAR
            // Importante: show() debe ir ANTES de setMaximized(true) para asegurar
            // que el sistema operativo calcule bien los bordes de la pantalla.
            stage.show();
            stage.setMaximized(true); 
            
        } catch (IOException e) {
            System.err.println("Error al cargar la interfaz inicial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCustomFont(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                Font.loadFont(is, 12);
            } else {
                System.err.println("ADVERTENCIA: Fuente no encontrada en: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Optimización para estabilidad de MediaView
        System.setProperty("video.lib", "gstreamer"); 
        
        launch(args);
    }
}