# 📚 Tutorial de Imágenes - Ctrl+Alt+Quest

## Estructura de Carpetas

Las imágenes del tutorial deben colocarse aquí: `src/main/resources/assets/images/tutorial/`

## Imágenes Requeridas

Para que el tutorial funcione correctamente, necesitas agregar las siguientes imágenes PNG:

### 1. **dashboard.png** (500x300)
   - Descripción: Captura del tablero de control principal
   - Muestra: Vista general del dashboard con HUD del jugador

### 2. **missions.png** (500x300)
   - Descripción: Vista de misiones disponibles
   - Muestra: Listado de misiones activas y completadas

### 3. **xp_system.png** (500x300)
   - Descripción: Sistema de ganancia de XP
   - Muestra: Barra de progreso de nivel y XP

### 4. **activity.png** (500x300)
   - Descripción: Monitor de actividad en tiempo real
   - Muestra: Rastreador de ventana activa y productividad

### 5. **store.png** (500x300)
   - Descripción: Tienda de recompensas
   - Muestra: Catálogo de items y skins disponibles

### 6. **achievements.png** (500x300)
   - Descripción: Galería de logros
   - Muestra: Logros desbloqueados y disponibles

### 7. **profile.png** (500x300)
   - Descripción: Panel de perfil
   - Muestra: Datos del personaje y opciones de personalización

### 8. **keybindings.png** (500x300)
   - Descripción: Tabla de atajos de teclado
   - Muestra: Atajos disponibles organizados por categoría

### 9. **tips.png** (500x300)
   - Descripción: Consejos finales motivacionales
   - Muestra: Tips para mejorar el rendimiento

## Cómo Agregar las Imágenes

### Opción 1: Screenshots Personalizados
1. Abre la aplicación Ctrl+Alt+Quest
2. Navega a cada sección (Dashboard, Misiones, etc.)
3. Toma capturas de pantalla (Windows: `Print Screen` o `Snipping Tool`)
4. Redimensiona las imágenes a 500x300 píxeles
5. Guarda como PNG en esta carpeta con los nombres especificados

**Herramientas recomendadas para editar:**
- Paint.NET
- GIMP
- Pixlr
- Online: Canva, Pixlr.com

### Opción 2: Generar Mockups
Si prefieres crear mockups visuales más profesionales:
1. Usa Figma o Adobe XD
2. Crea diseños que representen cada elemento
3. Exporta como PNG 500x300

### Opción 3: Usar Placeholders Temporales
- El sistema incluye un fallback automático si las imágenes no existen
- Mostrará un rectángulo con el nombre de la imagen
- Esto permite que el tutorial funcione mientras agregas las imágenes reales

## Especificaciones Técnicas

- **Formato**: PNG (transparencia soportada)
- **Tamaño recomendado**: 500x300 píxeles
- **Resolución**: 96 DPI mínimo
- **Máximo peso**: 200KB por imagen
- **Fondo recomendado**: Fondo oscuro que combine con el tema

## Estructura Esperada

```
src/main/resources/assets/images/tutorial/
├── dashboard.png
├── missions.png
├── xp_system.png
├── activity.png
├── store.png
├── achievements.png
├── profile.png
├── keybindings.png
├── tips.png
└── README.md (este archivo)
```

## Personalización Futura

Puedes editar el archivo `TutorialViewController.java` para:
- Agregar más diapositivas
- Cambiar descripciones
- Modificar tips rápidos
- Cambiar el orden de las diapositivas

Cada diapositiva se define en el método `loadTutorialSlides()`.

## Notas Importantes

⚠️ **Sin imágenes**: El tutorial seguirá funcionando pero mostrará placeholder rectangulares
✅ **Con imágenes**: La experiencia será mucho más profesional y educativa
📝 **Formato consistente**: Mantén todas las imágenes con las mismas dimensiones

---

¿Necesitas ayuda para crear las imágenes? Consulta la documentación principal del proyecto.
