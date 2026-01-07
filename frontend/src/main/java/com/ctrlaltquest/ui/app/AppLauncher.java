package com.ctrlaltquest.ui.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AppLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Cargar fuente de forma segura (nombre y extensión exactos)
        try (InputStream is = getClass().getResourceAsStream("/assets/fonts/pixelcastle/Pixelcastle-Regular.otf")) {
            if (is != null) {
                Font.loadFont(is, 12);
            } else {
                System.err.println("WARNING: fuente no encontrada en classpath: /assets/fonts/pixelcastle/Pixelcastle-Regular.otf");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Comprobar y cargar FXML de forma segura
        URL fxmlUrl = getClass().getResource("/fxml/splash.fxml");
        if (fxmlUrl == null) {
            System.err.println("ERROR: /fxml/splash.fxml no encontrado en classpath");
            return;
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(loader.load());
        stage.setTitle("Ctrl + Alt + Quest");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
