package com.ctrlaltquest.ui.examples;

import java.io.IOException;

import com.ctrlaltquest.ui.navigation.SceneRouter;
import com.ctrlaltquest.ui.navigation.SceneRouter.TransitionType;
import com.ctrlaltquest.ui.utils.AnimationManager;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Ejemplos de uso de transiciones suaves en la aplicación
 * Este archivo sirve como referencia para implementadores
 */
public class TransitionExamples {
    
    // ============ EJEMPLO 1: NAVEGACIÓN BÁSICA EN UN CONTROLADOR ============
    
    /**
     * Ejemplo de cómo usar SceneRouter en un controlador de login
     */
    public static void ejemplo_LoginNavigation(Stage stage) throws IOException {
        SceneRouter router = new SceneRouter(stage);
        
        // El usuario ha iniciado sesión exitosamente
        // Navegar al home con transición de desvanecimiento
        router.goTo(
            "/fxml/home.fxml",
            "Ctrl + Alt + Quest - Home",
            TransitionType.FADE
        );
    }
    
    
    // ============ EJEMPLO 2: NAVEGACIÓN CON TRANSICIÓN PERSONALIZADA ============
    
    /**
     * Ejemplo de navegación forward con slide personalizado
     */
    public static void ejemplo_NavigateForward(Stage stage) throws IOException {
        SceneRouter router = new SceneRouter(stage);
        
        // Navegar a la siguiente pantalla con deslizamiento a la izquierda
        router.goTo(
            "/fxml/missions.fxml",
            "Missions",
            TransitionType.SLIDE_LEFT  // Sale a la izquierda, entra desde la derecha
        );
    }
    
    
    // ============ EJEMPLO 3: NAVEGACIÓN BACKWARD CON TRANSICIÓN INVERSA ============
    
    /**
     * Ejemplo de navegación back con slide o opuesto
     */
    public static void ejemplo_NavigateBack(Stage stage) throws IOException {
        SceneRouter router = new SceneRouter(stage);
        
        // Navegar a la pantalla anterior con deslizamiento a la derecha
        router.goTo(
            "/fxml/home.fxml",
            "Home",
            TransitionType.SLIDE_RIGHT  // Sale a la derecha, entra desde la izquierda
        );
    }
    
    
    // ============ EJEMPLO 4: USAR TRANSICIÓN POR DEFECTO ============
    
    /**
     * Ejemplo de establecer una transición por defecto para toda la app
     */
    public static void ejemplo_SetDefaultTransition(Stage stage) throws IOException {
        SceneRouter router = new SceneRouter(stage);
        
        // Establecer FADE como transición por defecto
        router.setDefaultTransition(TransitionType.FADE);
        
        // Ahora todas las navegaciones usan FADE automáticamente
        router.goToWithDefaultTransition("/fxml/dashboard.fxml", "Dashboard");
    }
    
    
    // ============ EJEMPLO 5: PERSONALIZAR DURACIÓN ============
    
    /**
     * Ejemplo de cambiar la duración de las transiciones
     */
    public static void ejemplo_CustomDuration(Stage stage) throws IOException {
        SceneRouter router = new SceneRouter(stage);
        
        // Hacer transiciones más lentas (600ms en lugar de 400ms)
        router.setTransitionDuration(600);
        
        // Usar transición en zoom con duración personalizada
        router.goTo(
            "/fxml/store.fxml",
            "Store",
            TransitionType.ZOOM
        );
    }
    
    
    // ============ EJEMPLO 6: USAR ANIMATION MANAGER DIRECTAMENTE ============
    
    /**
     * Ejemplo de usar AnimationManager en un controlador para animar elementos
     */
    public static void ejemplo_DirectAnimationManager(VBox contentPanel) {
        // Animar entrada de un panel
        contentPanel.setOpacity(0);
        AnimationManager.fadeIn(contentPanel, 500);
        
        // Alternativa: slide in desde arriba
        contentPanel.setTranslateY(-50);
        AnimationManager.slideInFromTop(contentPanel, 400);
        
        // Alternativa: zoom in
        contentPanel.setScaleX(0.8);
        contentPanel.setScaleY(0.8);
        AnimationManager.zoomIn(contentPanel, 300);
    }
    
    
    // ============ EJEMPLO 7: ANIMAR SALIDA CON CALLBACK ============
    
    /**
     * Ejemplo de animar un elemento y ejecutar código cuando termine
     */
    public static void ejemplo_AnimationWithCallback(Node button) {
        // Animar desvanecimiento y luego hacer algo
        AnimationManager.fadeOut(button, 500, () -> {
            System.out.println("Animación completada!");
            // Aquí puedes actualizar el UI, cambiar de vista, etc.
            button.setVisible(false);
        });
    }
    
    
    // ============ EJEMPLO 8: TRANSICIÓN COMBINADA ============
    
    /**
     * Ejemplo de transición que mueve dos elementos simultáneamente
     */
    public static void ejemplo_CombinedTransition(Node oldView, Node newView) {
        // Slide left + fade out para oldView, slide in + fade in para newView
        AnimationManager.pageTransitionNext(oldView, newView, 500);
    }
    
    
    // ============ EJEMPLO 9: FLUJO DE NAVEGACIÓN COMPLETO ============
    
    /**
     * Ejemplo de un flujo completo de navegación en una aplicación
     */
    public static void ejemplo_CompleteFlow(Stage stage) throws IOException {
        SceneRouter router = new SceneRouter(stage);
        
        // Configuración inicial
        router.setDefaultTransition(TransitionType.FADE);
        router.setTransitionDuration(400);
        
        // Flujo 1: Login -> Home
        if (userIsLoggedIn()) {
            router.goTo("/fxml/home.fxml", "Home", TransitionType.FADE);
        }
        
        // Flujo 2: Home -> Missions (forward)
        // router.goTo("/fxml/missions.fxml", "Missions", TransitionType.SLIDE_LEFT);
        
        // Flujo 3: Missions -> Home (backward)
        // router.goTo("/fxml/home.fxml", "Home", TransitionType.SLIDE_RIGHT);
        
        // Flujo 4: Home -> Store (modal/special)
        // router.goTo("/fxml/store.fxml", "Store", TransitionType.ZOOM);
    }
    
    
    // ============ EJEMPLO 10: ANIMACIONES EN LISTA DINÁMICAMENTE ============
    
    /**
     * Ejemplo de animar elementos de una lista cuando se cargan
     */
    public static void ejemplo_AnimateListItems(java.util.List<Node> listItems) {
        for (int i = 0; i < listItems.size(); i++) {
            Node item = listItems.get(i);
            
            // Delay progresivo para cada item
            javafx.animation.Timeline delay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(i * 100),
                    e -> AnimationManager.slideInFromLeft(item, 300)
                )
            );
            delay.play();
        }
    }
    
    
    // ============ UTILIDADES ============
    
    private static boolean userIsLoggedIn() {
        // Aquí va tu lógica de verificación de login
        return true;
    }
    
    
    // ============ INTEGRACIÓN EN UN CONTROLADOR REAL ============
    
    /**
     * Plantilla para integrar en un controlador existente
     */
    public static class ControllerTemplate {
        
        private Stage stage;
        private SceneRouter router;
        
        // En el initialize() del controlador
        public void setupNavigation(Stage stage) {
            this.stage = stage;
            this.router = new SceneRouter(stage);
            
            // Configurar preferencias de transiciones
            this.router.setDefaultTransition(TransitionType.FADE);
            this.router.setTransitionDuration(400);
        }
        
        // En un manejador de evento (como onAction de un botón)
        public void onNavigationButtonClick(String fxmlPath, String title) throws IOException {
            // Opción 1: Usar transición específica
            router.goTo(fxmlPath, title, TransitionType.SLIDE_LEFT);
            
            // Opción 2: Usar transición por defecto
            // router.goToWithDefaultTransition(fxmlPath, title);
        }
        
        // Para regresar
        public void onBackButtonClick(String fxmlPath, String title) throws IOException {
            router.goTo(fxmlPath, title, TransitionType.SLIDE_RIGHT);
        }
        
        // Para acciones especiales
        public void onSpecialActionClick(String fxmlPath, String title) throws IOException {
            router.goTo(fxmlPath, title, TransitionType.ZOOM);
        }
    }
}
