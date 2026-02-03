package com.ctrlaltquest.ui.app;

import com.ctrlaltquest.ui.utils.SoundManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AppLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Cargar fuentes al inicio para que estén disponibles en el Splash y Login
        loadCustomFont("/assets/fonts/pixelcastle/Pixelcastle-Regular.otf");
        loadCustomFont("/assets/fonts/runewood/Runewood.ttf");

        // 2. Cargar el FXML inicial
        URL fxmlUrl = getClass().getResource("/fxml/splash.fxml");
        if (fxmlUrl == null) {
            System.err.println("ERROR CRÍTICO: No se encontró /fxml/splash.fxml");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load(), 960, 540);
            
            // --- SONIDO DE TECLADO GLOBAL ---
            // Este filtro captura cualquier tecla presionada en la ventana y reproduce el sonido
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                SoundManager.playKeyClick();
            });

            stage.setTitle("Ctrl + Alt + Quest");
            stage.setScene(scene);
            
            // Estética de Splash: Sin redimensión y centrado
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();
            
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