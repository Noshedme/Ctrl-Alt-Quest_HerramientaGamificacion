package com.ctrlaltquest.ui.navigation;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneRouter {
    private final Stage stage;

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    public void goTo(String fxmlPath, String title) throws IOException {
        Parent root = new FXMLLoader(getClass().getResource(fxmlPath)).load();
        Scene scene = new Scene(root);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
