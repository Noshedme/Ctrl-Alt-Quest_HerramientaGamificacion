## 🎬 Transiciones Suaves Entre Vistas - Guía de Uso

### 📋 Descripción General
Se ha implementado un sistema completo de transiciones suaves entre vistas usando animaciones JavaFX. Incluye:
- **AnimationManager.java**: Gestor centralizado de todas las animaciones
- **SceneRouter mejorado**: Router con soporte para diferentes tipos de transiciones
- **transitions.css**: Estilos CSS para transiciones visuales adicionales

---

## 🎮 Tipos de Transiciones Disponibles

### 1. **FadeTransition** (Desvanecimiento)
La vista antigua desaparece gradualmente mientras la nueva aparece.

```java
import com.ctrlaltquest.ui.navigation.SceneRouter;

// En un controlador:
public void goToHome() throws IOException {
    new SceneRouter(stage).goTo(
        "/fxml/home.fxml", 
        "Home",
        SceneRouter.TransitionType.FADE
    );
}
```

### 2. **SlideInFromLeft** (Deslizar desde la izquierda)
La nueva vista entra deslizándose desde el lado izquierdo.

```java
public void goToDashboard() throws IOException {
    new SceneRouter(stage).goTo(
        "/fxml/dashboard.fxml", 
        "Dashboard",
        SceneRouter.TransitionType.SLIDE_RIGHT
    );
}
```

### 3. **SlideInFromRight** (Deslizar desde la derecha)
La nueva vista entra deslizándose desde el lado derecho.

```java
public void goBack() throws IOException {
    new SceneRouter(stage).goTo(
        "/fxml/previous.fxml", 
        "Previous",
        SceneRouter.TransitionType.SLIDE_LEFT
    );
}
```

### 4. **ZoomTransition** (Zoom)
La vista entrante se amplía gradualmente mientras la saliente se reduce.

```java
public void goToMissions() throws IOException {
    new SceneRouter(stage).goTo(
        "/fxml/missions.fxml", 
        "Missions",
        SceneRouter.TransitionType.ZOOM
    );
}
```

---

## 🔧 Personalizar Animaciones

### Cambiar Duración de Transiciones

```java
SceneRouter router = new SceneRouter(stage);
router.setTransitionDuration(600); // 600 ms en lugar de 400ms por defecto
router.goToWithDefaultTransition("/fxml/home.fxml", "Home");
```

### Establecer Transición por Defecto

```java
SceneRouter router = new SceneRouter(stage);
router.setDefaultTransition(SceneRouter.TransitionType.SLIDE_LEFT);

// Ahora todas las navegaciones usarán SLIDE_LEFT por defecto
router.goToWithDefaultTransition("/fxml/view1.fxml", "View 1");
router.goToWithDefaultTransition("/fxml/view2.fxml", "View 2");
```

---

## 🎨 Usar AnimationManager Directamente

El gestor de animaciones puede usarse independientemente del SceneRouter:

### Animaciones de Entrada

```java
import com.ctrlaltquest.ui.utils.AnimationManager;

// Fade in simple
AnimationManager.fadeIn(myNode);

// Slide in desde la izquierda con duración personalizada
AnimationManager.slideInFromLeft(myNode, 600);

// Slide in desde el top
AnimationManager.slideInFromTop(myNode);

// Zoom in
AnimationManager.zoomIn(myNode);
```

### Animaciones de Salida

```java
// Fade out simple
AnimationManager.fadeOut(myNode);

// Fade out con callback al terminar
AnimationManager.fadeOut(myNode, 400, () -> {
    System.out.println("Animación terminada!");
});

// Slide out hacia la derecha
AnimationManager.slideOutToRight(myNode, 500);

// Slide out hacia la izquierda
AnimationManager.slideOutToLeft(myNode, 500);

// Zoom out
AnimationManager.zoomOut(myNode);
```

### Animaciones Combinadas

```java
// Cross fade entre dos vistas
AnimationManager.crossFade(oldView, newView, 400);

// Transición de página forward (siguiente)
AnimationManager.pageTransitionNext(oldView, newView, 500);

// Transición de página backward (anterior)
AnimationManager.pageTransitionPrevious(oldView, newView, 500);
```

---

## 📝 Ejemplo Completo en un Controlador

```java
package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.ui.navigation.SceneRouter;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class LoginController {
    
    private Stage stage;
    private SceneRouter router;
    
    @FXML
    private void initialize() {
        // Obtener stage en tiempo de ejecución desde la scene
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
        this.router = new SceneRouter(stage);
        
        // Configurar transiciones por defecto
        this.router.setDefaultTransition(SceneRouter.TransitionType.FADE);
        this.router.setTransitionDuration(500);
    }
    
    @FXML
    private void handleLogin() {
        try {
            // Login exitoso
            router.goTo(
                "/fxml/home.fxml", 
                "Home",
                SceneRouter.TransitionType.SLIDE_LEFT
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            router.goToWithDefaultTransition(
                "/fxml/register.fxml",
                "Register"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

---

## 🎯 Casos de Uso Recomendados

| Escenario | Transición Recomendada |
|-----------|----------------------|
| Cambio a vista relacionada (pestaña, menú) | FADE |
| Navegación forward (siguiente paso) | SLIDE_LEFT |
| Navegación backward (paso anterior) | SLIDE_RIGHT |
| Modal o ventana emergente | ZOOM |
| Transición suave general | FADE |

---

## 💡 Tips y Mejores Prácticas

1. **Consistencia**: Usa el mismo tipo de transición para el mismo flujo de navegación
2. **Duración**: Mantén las transiciones cortas (300-500ms) para no frustrar al usuario
3. **Performance**: Evita transiciones muy complejas en dispositivos lentos
4. **Accesibilidad**: Considera usuarios sensibles a animaciones rápidas
5. **Feedback**: Deshabilita botones durante la transición si es necesario

---

## 📦 Dependencias Necesarias

Todas las transiciones usan las clases estándar de JavaFX:
- `javafx.animation.FadeTransition`
- `javafx.animation.TranslateTransition`
- `javafx.animation.ScaleTransition`
- `javafx.animation.ParallelTransition`

No se requieren dependencias externas.

---

## 🔄 Integración CSS

Puedes aplicar la hoja de estilos de transiciones en tus FXML:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.control.Button?>

<BorderPane xmlns="http://javafx.com/javafx"
            stylesheets="@/styles/shared.css,@/styles/transitions.css">
    <!-- Tu contenido aquí -->
</BorderPane>
```

---

## 🐛 Solución de Problemas

### Las transiciones no funcionan
- Asegúrate de que AnimationManager esté en el classpath correcto
- Verifica que el stage esté correctamente inicializado

### Las transiciones son muy rápidas/lentas
- Ajusta el valor en `setTransitionDuration()`
- Los valores por defecto están en `AnimationManager.DEFAULT_*_DURATION`

### El contenido parpadea durante la transición
- Asegúrate de que la nueva escena se carga antes de que termine la animación
- Usa los callbacks `onFinished` para acciones sincronizadas

---

## ✨ Notas Finales

Este sistema de transiciones es completamente extensible. Puedes:
- Crear nuevos tipos de transiciones personalizadas
- Combinar múltiples transiciones
- Agregar efectos de sonido junto con las transiciones
- Modificar la duración dinámicamente según el contexto

¡Disfruta de las transiciones suaves en tu aplicación! 🎉
