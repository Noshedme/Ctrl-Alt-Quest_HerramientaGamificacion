# Frontend (JavaFX)

Estructura inicial para las pantallas de **splash** (video + barra de carga) y **auth** (login/registro/recuperación), manteniendo estilo RPG/pixel.

## ✨ Nuevas Características (Marzo 2026)

### 📚 Tutorial Interactivo
Se ha agregado un nuevo sistema de tutorial con carrusel interactivo que permite a los usuarios aprender cómo usar la aplicación:

- **Ubicación**: Pestaña "📚 Tutorial" en el menú lateral del home
- **9 Diapositivas** sobre diferentes aspectos de la aplicación
- **Carrusel navegable** con botones Anterior/Siguiente
- **Indicadores visuales** para saltar a cualquier sección
- **Soporte para imágenes** (opcional - funciona sin ellas)

Documentación: [TUTORIAL_QUICK_START.md](../Documents/TUTORIAL_QUICK_START.md)

## Carpetas clave

- `src/main/java/com/ctrlaltquest/ui/app`: arranque de la UI.
- `src/main/java/com/ctrlaltquest/ui/controllers`: controladores JavaFX.
- `src/main/java/com/ctrlaltquest/ui/controllers/views`: **controladores de vistas (nuevo)**
  - `TutorialViewController.java` ← Tutorial interactivo
- `src/main/java/com/ctrlaltquest/ui/navigation`: utilidades de navegación entre escenas.
- `src/main/resources/fxml`: vistas FXML.
- `src/main/resources/fxml/views`: **vistas específicas (nuevo)**
  - `tutorial_view.fxml` ← Vista del tutorial
- `src/main/resources/styles`: estilos CSS (pixel/RPG).
- `src/main/resources/assets`: video, imágenes y fuentes.
  - `assets/images/tutorial/` ← **Imágenes del tutorial (nuevas)**

## Notas

- Coloca el **video de carga** (7–10s) en `src/main/resources/assets/videos/`.
- Sustituye el logo en `assets/images/` por el arte final.
- Define fuentes pixel en `assets/fonts/` y refiérete a ellas desde los CSS.
- **Para el Tutorial**: Agrega imágenes PNG (500x300px) en `assets/images/tutorial/` (opcional)
