# Frontend (JavaFX)

Estructura inicial para las pantallas de **splash** (video + barra de carga) y **auth** (login/registro/recuperación), manteniendo estilo RPG/pixel.

## Carpetas clave

- `src/main/java/com/ctrlaltquest/ui/app`: arranque de la UI.
- `src/main/java/com/ctrlaltquest/ui/controllers`: controladores JavaFX.
- `src/main/java/com/ctrlaltquest/ui/navigation`: utilidades de navegación entre escenas.
- `src/main/resources/fxml`: vistas FXML.
- `src/main/resources/styles`: estilos CSS (pixel/RPG).
- `src/main/resources/assets`: video, imágenes y fuentes.

## Notas

- Coloca el **video de carga** (7–10s) en `src/main/resources/assets/videos/`.
- Sustituye el logo en `assets/images/` por el arte final.
- Define fuentes pixel en `assets/fonts/` y refiérete a ellas desde los CSS.
