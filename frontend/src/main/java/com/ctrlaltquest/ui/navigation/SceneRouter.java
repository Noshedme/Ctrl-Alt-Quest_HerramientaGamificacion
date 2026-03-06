package com.ctrlaltquest.ui.navigation;

import java.io.IOException;

import com.ctrlaltquest.ui.utils.AnimationManager;
import com.ctrlaltquest.ui.utils.WindowManager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Router mejorado para navegación entre escenas con transiciones suaves
 */
public class SceneRouter {
    private final Stage stage;
    
    // Tipo de transición por defecto
    public enum TransitionType {
        FADE,           // Desvanecimiento simple
        SLIDE_LEFT,     // Desliza salida a derecha, entrada desde izquierda
        SLIDE_RIGHT,    // Desliza salida a izquierda, entrada desde derecha
        ZOOM,           // Zoom in/out
        NONE            // Sin transiciones
    }
    
    private TransitionType defaultTransition = TransitionType.FADE;
    private double transitionDuration = 400; // milisegundos

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Navega a una nueva escena sin transiciones (método original)
     */
    public void goTo(String fxmlPath, String title) throws IOException {
        goTo(fxmlPath, title, TransitionType.NONE);
    }
    
    /**
     * Navega a una nueva escena con tipo de transición especificado
     */
    public void goTo(String fxmlPath, String title, TransitionType transitionType) throws IOException {
        Parent oldRoot = stage.getScene() != null ? stage.getScene().getRoot() : null;
        Parent newRoot = new FXMLLoader(getClass().getResource(fxmlPath)).load();
        
        applyTransition(oldRoot, newRoot, transitionType);
        
        stage.setTitle(title);
        // Usar WindowManager para cambiar escena y mantener maximizado
        WindowManager.getInstance().changeScene(newRoot);
        stage.show();
    }
    
    /**
     * Navega a una nueva escena con la transición por defecto
     */
    public void goToWithDefaultTransition(String fxmlPath, String title) throws IOException {
        goTo(fxmlPath, title, defaultTransition);
    }
    
    /**
     * Establece el tipo de transición por defecto
     */
    public void setDefaultTransition(TransitionType transition) {
        this.defaultTransition = transition;
    }
    
    /**
     * Establece la duración de las transiciones
     */
    public void setTransitionDuration(double durationMs) {
        this.transitionDuration = durationMs;
    }
    
    /**
     * Aplica la transición apropiada entre escenas antiguas y nuevas
     */
    private void applyTransition(Parent oldRoot, Parent newRoot, TransitionType type) {
        if (type == TransitionType.NONE || oldRoot == null) {
            return;
        }
        
        switch (type) {
            case FADE:
                AnimationManager.fadeOut(oldRoot, transitionDuration);
                AnimationManager.fadeIn(newRoot, transitionDuration);
                break;
                
            case SLIDE_LEFT:
                AnimationManager.slideOutToRight(oldRoot, transitionDuration);
                AnimationManager.slideInFromRight(newRoot, transitionDuration);
                break;
                
            case SLIDE_RIGHT:
                AnimationManager.slideOutToLeft(oldRoot, transitionDuration);
                AnimationManager.slideInFromLeft(newRoot, transitionDuration);
                break;
                
            case ZOOM:
                AnimationManager.zoomOut(oldRoot, transitionDuration);
                AnimationManager.zoomIn(newRoot, transitionDuration);
                break;
                
            default:
                break;
        }
    }
}
