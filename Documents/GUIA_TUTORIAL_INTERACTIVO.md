# 📚 Tutorial Interactivo - Guía de Implementación

## ✅ Cambios Realizados

Se ha agregado una nueva pestaña de **Tutorial Interactivo** a la aplicación Ctrl+Alt+Quest con un sistema de carrusel que permite a los usuarios aprender cómo usar la aplicación de forma visual e interactiva.

### 1. Nuevos Archivos Creados

#### **Frontend**
```
src/main/resources/fxml/views/tutorial_view.fxml
├── Vista FXML del tutorial
├── Carrusel con controles de navegación
└── Sistema de indicadores de página

src/main/java/com/ctrlaltquest/ui/controllers/views/TutorialViewController.java
├── Controlador del tutorial
├── Gestión del carrusel de diapositivas
├── Carga y visualización de imágenes
└── Clase interna TutorialSlide

src/main/resources/assets/images/tutorial/README.md
└── Guía para agregar imágenes al tutorial
```

### 2. Archivos Modificados

**home.fxml**
- Se agregó un nuevo botón en el menú lateral: `📚 Tutorial`
- Ubicado entre "🏆 Trofeos" y el separador final
- Con acción `onAction="#showTutorial"`

**HomeController.java**
- Se agregó el método `@FXML private void showTutorial()`
- Se agregó inyección de datos para TutorialViewController en `injectCharacterData()`
- Mantiene consistencia con el patrón de navegación existente

## 🎨 Características del Tutorial

### Carrusel Interactivo
- **9 diapositivas** sobre diferentes aspectos de la aplicación
- **Navegación bidireccional**: Botones "Anterior" y "Siguiente"
- **Indicadores visuales**: Puntos seleccionables para ir a cualquier diapositiva
- **Animaciones suaves**: Transiciones fade in/out entre diapositivas

### Contenido por Defecto
1. **Bienvenida** - Introducción al sistema
2. **Misiones** - Sistema de tareas diarias
3. **XP** - Ganancia de experiencia
4. **Rastreador** - Monitor de actividad
5. **Tienda** - Compra de recompensas
6. **Logros** - Galería de trofeos
7. **Perfil** - Personalización del personaje
8. **Atajos** - Controles del teclado
9. **Consejos Finales** - Motivación y mejores prácticas

### Elementos de Interfaz
```
┌─────────────────────────────────────┐
│  📚 TUTORIAL INTERACTIVO            │
│  Aprende a dominar Ctrl+Alt+Quest   │
├─────────────────────────────────────┤
│                                     │
│      ┌───────────────────────┐      │
│      │   [IMAGEN DE LA      │      │
│      │   DIAPOSITIVA]       │      │
│      │                       │      │
│      │   Descripción de la   │      │
│      │   funcionalidad...    │      │
│      └───────────────────────┘      │
│                                     │
├─────────────────────────────────────┤
│  ◀ ANTERIOR  [● ○ ○ ○ ○]  SIGUIENTE ▶│
│                                     │
├─────────────────────────────────────┤
│  💡 CONSEJO RÁPIDO                  │
│  Usa el tutorial para familiarizarte│
│  con el sistema...                  │
└─────────────────────────────────────┘
```

## 🖼️ Cómo Agregar Imágenes

### Requisitos
- **Ubicación**: `src/main/resources/assets/images/tutorial/`
- **Formato**: PNG
- **Dimensiones**: 500x300 píxeles
- **Tamaño**: Máximo 200KB por imagen

### Pasos para Agregar

1. **Captura Automática** (Recomendado)
   - Navega a cada sección en la aplicación
   - Toma una captura (Windows: `Print Screen` o `Snipping Tool`)
   - Redimensiona a 500x300 con Paint, GIMP, etc.
   - Guarda como PNG en `assets/images/tutorial/`

2. **Nombres de Archivo Esperados**
   ```
   dashboard.png      - Tablero principal
   missions.png       - Sistema de misiones
   xp_system.png      - Ganancia de XP
   activity.png       - Monitor de actividad
   store.png          - Tienda de recompensas
   achievements.png   - Logros desbloqueados
   profile.png        - Perfil del personaje
   keybindings.png    - Atajos de teclado
   tips.png           - Consejos finales
   ```

## 🔧 Cómo Personalizar el Tutorial

### Editar Diapositivas
Abre `TutorialViewController.java` y modifica el método `loadTutorialSlides()`:

```java
// Agregar una nueva diapositiva
slides.add(new TutorialSlide(
    "Título de la Diapositiva",
    "Descripción detallada de la funcionalidad...",
    "nombre_imagen",  // Sin extensión .png
    "Tip rápido para mostrar"
));
```

### Cambiar Descripción de una Diapositiva
```java
// Diapositiva 3: Busca y modifica el constructor
slides.add(new TutorialSlide(
    "⭐ Ganancia de Experiencia",  // Cambiar aquí
    "Tu nueva descripción aquí",   // Cambiar aquí
    "xp_system",
    "Tu nuevo consejo aquí"        // Cambiar aquí
));
```

### Agregar Más Diapositivas
```java
// Al final de loadTutorialSlides(), antes de crearIndicadores()
slides.add(new TutorialSlide(
    "🆕 Nueva Sección",
    "Descripción de la nueva funcionalidad...",
    "nueva_imagen",
    "Nuevo consejo"
));
```

## 🎯 Navegación Desde el Menú

**Acceso al Tutorial:**
1. Inicia la aplicación
2. Haz clic en el botón **"📚 Tutorial"** en el menú lateral izquierdo
3. Se mostrará la primera diapositiva
4. Usa los botones o los puntos para navegar

**Navegación:**
- **Botón "◀ ANTERIOR"**: Diapositiva anterior
- **Botón "SIGUIENTE ▶"**: Siguiente diapositiva
- **Puntos (• ○ ○)**: Click en cualquier punto para ir directamente a esa diapositiva
- **Etiqueta**: Muestra "Diapositiva X/9"

## 📊 Comportamiento Sin Imágenes

Si las imágenes no están disponibles:
- El tutorial sigue funcionando completamente
- Muestra un **placeholder rectangular** en lugar de la imagen
- El placeholder contiene el nombre del archivo esperado
- Permite que el usuario siga aprendiendo mientras agrega las imágenes

**Placeholder visible:**
```
┌─────────────────────┐
│                     │
│      📸             │
│  dashboard          │
│                     │
└─────────────────────┘
```

## 🎨 Personalización de Estilos

### Cambiar Colores
Los estilos están definidos en `tutorial_view.fxml` y `TutorialViewController.java`:

```java
// Ejemplo: Cambiar color de los puntos activos
dot.setStyle("-fx-background-color: #FF6B6B;"); // Rojo
```

### Cambiar Duración de Animaciones
En `TutorialViewController.java`:
```java
// Cambiar duración del fade (por defecto 300ms)
FadeTransition fadeIn = new FadeTransition(Duration.millis(500), currentSlideNode);
```

## 📝 Cosas a Recordar

✅ **Funciona sin imágenes** - Puedes compilar y probar sin agregarlas
✅ **Carrusel totalmente funcional** - Navegación completa disponible
✅ **Fácil de personalizar** - Edita diapositivas en el código
✅ **Consistente con el diseño** - Sigue la paleta de colores de la app
✅ **Accesible** - Tooltips y descripciones clara

⚠️ **Nota importante**: Las imágenes mejoran significativamente la experiencia, así que se recomienda agregarlas cuando sea posible.

## 🔄 Integración con el Sistema Existente

El tutorial se integra perfectamente con:
- ✅ Sistema de caché de vistas
- ✅ Inyección de dependencias
- ✅ Animaciones y transiciones del sistema
- ✅ Toast notifications
- ✅ Sonidos de clic (SoundManager)

## 📚 Estructura de Clases

### TutorialViewController
```java
public class TutorialViewController {
    @FXML VBox slideContainer;
    @FXML HBox dotsContainer;
    @FXML Label pageLabel;
    @FXML Label quickTip;
    
    List<TutorialSlide> slides;
    int currentSlideIndex;
    
    void loadTutorialSlides()      // Carga todas las diapositivas
    void nextSlide()                // Siguiente
    void previousSlide()            // Anterior
    void showSlide(int index)       // Mostrar índice específico
    void crearDiapositiva()         // Crear vista de diapositiva
    void actualizarIndicadores()    // Actualizar puntos
}

// Clase interna
class TutorialSlide {
    String title;           // Título
    String description;     // Descripción
    String imageName;       // Nombre del archivo de imagen
    String quickTip;        // Consejo rápido
}
```

## 🐛 Solución de Problemas

| Problema | Solución |
|----------|----------|
| Tutorial no aparece en el menú | Verifica que el botón está en home.fxml |
| Las imágenes no se cargan | Coloca los PNG en `assets/images/tutorial/` |
| El tutorial está en blanco | Verifica que TutorialViewController.java compila sin errores |
| Botones de navegación no funcionan | Verifica que `nextSlide()` y `previousSlide()` tienen `@FXML` |

## 🚀 Próximos Pasos (Opcional)

- [ ] Agregar imágenes a todas las diapositivas
- [ ] Crear videos cortos para cada sección
- [ ] Agregar cuestionarios interactivos
- [ ] Implementar un sistema de progreso del tutorial
- [ ] Agregar animaciones más complejas al carrusel

---

**Última actualización**: Marzo 2026
**Versión**: 1.0
**Estado**: ✅ Completo y Funcional
