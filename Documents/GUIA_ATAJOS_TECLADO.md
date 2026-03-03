# ⌨️ ATAJOS DE TECLADO GLOBALES - GUÍA COMPLETA

## 📋 Resumen de Implementación

Se ha implementado un sistema completo de **atajos de teclado globales** que funcionan en cualquier parte de la aplicación. Los atajos están integrados a nivel de aplicación y no requieren que el usuario esté en una vista específica.

---

## 🎯 Atajos Implementados

### 🔊 Controles de Audio

| Atajo | Función | Descripción |
|-------|---------|-------------|
| **Alt + M** | Mutear Música | Pausa/Reanuda la música de fondo |
| **Alt + S** | Mutear Sonidos | Pausa/Reanuda todos los efectos de sonido |
| **Ctrl + M** | Mutear Todo | Pausa/Reanuda toda el audio del sistema |

### 🎬 Controles de Video

| Atajo | Función | Descripción |
|-------|---------|-------------|
| **Espacio** | Pausar Video | Pausa/Reanuda el video de fondo actual |
| **Alt + V** | Pausar Video (Alt) | Forma alternativa de pausar/reanudar video |

### 🖥️ Controles de Aplicación

| Atajo | Función | Descripción |
|-------|---------|-------------|
| **F11** | Pantalla Completa | Activa/Desactiva pantalla completa |
| **Ctrl + H** | Mostrar Ayuda | Abre el menú de ayuda (futuro) |
| **Ctrl + K** | Mostrar Atajos | Muestra todos los atajos disponibles |
| **Ctrl + Q** | Salir | Cierra la aplicación |

---

## 📁 Archivos Implementados

### 1. **KeyBindings.java**
**Ubicación**: `src/main/java/com/ctrlaltquest/ui/utils/KeyBindings.java`

Define todas las combinaciones de teclas y sus descripciones de forma centralizada.

```java
// Ejemplo de uso
public static final KeyCombination TOGGLE_MUSIC = 
    new KeyCombination(KeyCode.M, KeyCombination.ModifierValue.DOWN, ...);
```

**Características**:
- Constantes para todas las combinaciones de teclas
- Array `ALL_BINDINGS` con información descriptiva
- Clase interna `KeyBindingInfo` para metadata

### 2. **KeyBindingManager.java**
**Ubicación**: `src/main/java/com/ctrlaltquest/ui/utils/KeyBindingManager.java`

Singleton que maneja la lógica de ejecución de atajos globales.

```java
KeyBindingManager manager = KeyBindingManager.getInstance();
manager.initializeKeyBindings(stage, scene);
manager.enableVideoControl(() -> pauseVideo());
```

**Características**:
- Patrón Singleton
- Event filter global en la Scene
- Registro de acciones personalizadas
- Callback para control de video dinámico

### 3. **KeyBindingsViewController.java**
**Ubicación**: `src/main/java/com/ctrlaltquest/ui/controllers/views/KeyBindingsViewController.java`

Controlador para la ventana modal que muestra todos los atajos disponibles.

**Características**:
- Carga dinámica de todos los atajos
- Tarjetas visuales para cada atajo
- Diseño responsivo

### 4. **keybindings_view.fxml**
**Ubicación**: `src/main/resources/fxml/views/keybindings_view.fxml`

Vista FXML que muestra todos los atajos disponibles en una ventana modal.

### 5. **keybindings.css**
**Ubicación**: `src/main/resources/styles/keybindings.css`

Estilos CSS para la vista de atajos (tema oscuro compatible).

### 6. **settings.fxml** (Modificado)
**Ubicación**: `src/main/resources/fxml/settings.fxml`

Se agreg   ó una nueva sección que muestra los atajos principales y un botón para ver todos.

### 7. **SettingsController.java** (Modificado)
**Ubicación**: `src/main/java/com/ctrlaltquest/ui/controllers/SettingsController.java`

Se agregó el método `handleShowKeybindings()` que abre la ventana de atajos.

### 8. **AppLauncher.java** (Modificado)
**Ubicación**: `src/main/java/com/ctrlaltquest/ui/app/AppLauncher.java`

Se inicializa el `KeyBindingManager` en el método `start()`.

---

## 🚀 Cómo Usar

### Para el Usuario Final

1. **Ver todos los atajos disponibles**:
   - Presionar `Ctrl + K` en cualquier momento
   - O ir a Configuración → "📋 Ver todos los atajos disponibles"

2. **Usar los atajos**:
   - Presionar la combinación en cualquier pantalla
   - El sistema ejecutará la acción de inmediato
   - Verás un mensaje en consola confirmando el atajo

### Para Desarrolladores

#### Inicializar Atajos (Ya implementado en AppLauncher)

```java
KeyBindingManager keyBindingManager = KeyBindingManager.getInstance();
keyBindingManager.initializeKeyBindings(stage, scene);
```

#### Agregar Nuevos Atajos

```java
KeyBindingManager manager = KeyBindingManager.getInstance();

// Registrar nueva acción
manager.registerAction("MY_CUSTOM_ACTION", () -> {
    System.out.println("Acción personalizada ejecutada!");
});
```

#### Habilitar Control de Video

```java
// En HomeController o cualquier vista que reproduzca videos
KeyBindingManager manager = KeyBindingManager.getInstance();
manager.enableVideoControl(() -> {
    // Toggle video on/off
    videoPlayer.play();
    videoPlayer.pause();
});

// Cuando la vista se cierre
manager.disableVideoControl();
```

---

## 🧪 Casos de Uso

### Caso 1: Usuario Quiere Mutear Todo
```
Usuario presiona: Ctrl + M
↓
KeyBindingManager captura la tecla
↓
Ejecuta: toggleAllAudio()
↓
Actualiza: SettingsController.isMusicEnabled, isTypingSoundEnabled
↓
Sincroniza SoundManager
↓
Muestra notificación en consola: "Audio Global: DESACTIVADO 🔇"
```

### Caso 2: Usuario en HomeController Quiere Pausar Video
```
Usuario presiona: Espacio
↓
KeyBindingManager captura la tecla
↓
HomeController registró callback: enableVideoControl(() -> pauseVideo())
↓
Ejecuta callback
↓
Video se pausa/reanuda
↓
Muestra notificación
```

### Caso 3: Usuario Quiere Ver Todos los Atajos
```
Opción A: Presiona Ctrl + K
   ↓
   KeyBindingManager muestra lista en consola
   
Opción B: Va a Configuración → Botón "Ver todos los atajos"
   ↓
   Abre ventana modal KeyBindingsView
   ↓
   Muestra todas las combinaciones con descripción
```

---

## 🔧 Personalización

### Cambiar una Acción Existente

```java
KeyBindingManager manager = KeyBindingManager.getInstance();

// Sobrescribir acción
manager.registerAction("TOGGLE_MUSIC", () -> {
    // Tu lógica personalizada
});
```

### Agregar Nueva Combinación de Teclas

1. Agregar constante en `KeyBindings.java`:
```java
public static final KeyCombination MY_KEYBINDING = 
    new KeyCombination(KeyCode.X, KeyCombination.ModifierValue.DOWN, ...);
```

2. Agregar al array `ALL_BINDINGS`:
```java
new KeyBindingInfo(
    "Mi Acción",
    "Ctrl + X",
    "Descripción de qué hace",
    MY_KEYBINDING
)
```

3. Manejar en `KeyBindingManager.handleKeyPress()`:
```java
case X:
    if (ctrl && !alt && !shift) {
        executeAction("MY_ACTION");
        event.consume();
    }
    break;
```

---

## 💡 Tips de Implementación

### Para HomeController (Control de Video)

```java
public class HomeController {
    
    @Override
    public void initialize() {
        // ...
        // Habilitar control de video por teclado
        KeyBindingManager.getInstance().enableVideoControl(() -> {
            toggleVideoPlayback();
        });
    }
    
    private void toggleVideoPlayback() {
        if (videoNode.getMediaPlayer().getStatus() == MediaPlayer.Status.PLAYING) {
            videoNode.getMediaPlayer().pause();
        } else {
            videoNode.getMediaPlayer().play();
        }
    }
    
    @Override
    public void onViewHidden() {
        // Deshabilitar control de video cuando salimos de esta vista
        KeyBindingManager.getInstance().disableVideoControl();
    }
}
```

### Para Otros Controladores

```java
public class MiController {
    
    @FXML
    public void initialize() {
        // Puedes registrar acciones personalizadas si lo necesitas
        KeyBindingManager manager = KeyBindingManager.getInstance();
        
        // Ejemplo: registrar acción para Ctrl+M solo en esta vista
        manager.registerAction("MI_ACCION_ESPECIAL", () -> {
            // Tu lógica
        });
    }
}
```

---

## 🎨 Interfaz de Usuario

### Vista de Atajos en Configuración

La ventana de configuración ahora muestra:
- 📍 Sección "ATAJOS DE TECLADO" separada
- 🔑 Los 6 atajos principales en una caja destacada
- 📋 Botón para ver TODOS los atajos disponibles

### Ventana Modal de Atajos

Al presionar Ctrl+K o Click en botón:
- ✅ Se abre ventana modal con lista completa
- 📋 Cada atajo en una tarjeta visual
- 🎨 Tema oscuro (Dracula) consistente con la app
- 🖱️ Botón para cerrar

---

## ⚙️ Arquitectura Técnica

```
AppLauncher
    ↓
    Inicializa KeyBindingManager
    ↓
    KeyBindingManager.initializeKeyBindings(stage, scene)
    ↓
    Agrega EventFilter a Scene
    ↓
    KeyEvent → handleKeyPress() → executeAction()
    ↓
    Ejecuta Runnable registrada
    ↓
    Actualiza estado en SettingsController o ejecuta callback
```

---

## 🐛 Solución de Problemas

### Los atajos no funcionan
- ✅ Verifica que AppLauncher inicialice KeyBindingManager
- ✅ Comprueba que la Scene esté activa
- ✅ Busca mensajes en consola

### El video no pausa con Espacio
- ✅ Verifica que el controlador haya llamado `enableVideoControl()`
- ✅ El callback debe estar correctamente registrado
- ✅ Revisa que el MediaPlayer esté accesible

### Atajo no aparece en lista
- ✅ Agrega a `ALL_BINDINGS` en KeyBindings.java
- ✅ Incluye descripción clara

---

## 📚 Documentación Relacionada

- `KeyBindings.java` - Definiciones de atajos
- `KeyBindingManager.java` - Lógica de ejecución
- `keybindings_view.fxml` - Vista de atajos
- `SettingsController.java` - Integración en configuración

---

## ✅ Checklist de Integración

- [x] KeyBindingManager creado como Singleton
- [x] Atajos de audio implementados (Alt+M, Alt+S, Ctrl+M)
- [x] Atajos de video implementados (Espacio, Alt+V)
- [x] Atajos de aplicación implementados (F11, Ctrl+K, Ctrl+Q)
- [x] Vista de atajos en FXML
- [x] Integración en SettingsController
- [x] AppLauncher modificado para inicializar
- [x] Documentación completa

---

## 🎉 Conclusión

El sistema de atajos de teclado está completamente implementado y listo para usar. Los atajos funcionan globalmente en toda la aplicación, mejorando significativamente la experiencia del usuario al permitir control rápido sin usar el ratón.

**Próximas mejoras sugeridas**:
- Implementar notificaciones visuales en pantalla (Toast)
- Agregar sonido de confirmación de atajo
- Permitir personalización de atajos por usuario
- Guardar preferencias de atajos

¡Comienza a usar los atajos hoy mismo! 🚀
