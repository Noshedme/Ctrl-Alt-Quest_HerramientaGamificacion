package com.ctrlaltquest.ui.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

public class AppLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        var fontUrl = getClass().getResource("/assets/fonts/pixelcastle/pixelcastle.ttf");
        if (fontUrl != null) {
            Font.loadFont(fontUrl.toExternalForm(), 12);
        }
        Font.loadFont(getClass().getResource("/assets/fonts/pixelcastle/pixelcastle.ttf").toExternalForm(), 12);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/splash.fxml"));
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
