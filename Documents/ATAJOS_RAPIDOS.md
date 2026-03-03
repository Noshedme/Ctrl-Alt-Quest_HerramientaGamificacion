# ⌨️ ATAJOS DE TECLADO - GUÍA RÁPIDA DE USO

## 🎯 Los 9 Atajos Principales

| Atajo | Función |
|-------|---------|
| **Alt + M** | 🔊 Mutear música |
| **Alt + S** | 🎵 Mutear sonidos |
| **Ctrl + M** | 🔇 Mutear TODO |
| **Espacio** | ⏸️ Pausar/Reanudar video |
| **Alt + V** | ⏯️ Pausar/Reanudar video (alt) |
| **F11** | 🖥️ Pantalla completa |
| **Ctrl + K** | 📋 Ver todos los atajos |
| **Ctrl + H** | ❓ Mostrar ayuda |
| **Ctrl + Q** | ❌ Salir |

---

## 🚀 Cómo Usar

### Como Usuario Final
1. Presiona cualquier combinación de teclas listada arriba en **cualquier parte de la aplicación**
2. La acción se ejecutlará inmediatamente
3. Mira la consola para confirmar la acción (mejorías visuales próximamente)

### Ver Todos los Atajos Disponibles
**Opción A:** Presiona `Ctrl + K`
**Opción B:** Ve a Configuración → "📋 Ver todos los atajos disponibles"

---

## 📍 Ubicación en la UI

### En Configuración (Settings)
Se agregó una nueva sección con:
- ✅ Los 6 atajos principales visible en el editor
- 🔑 Combinaciones de teclas resaltadas en verde
- 📖 Descripciones breves de cada atajo
- 📋 Botón para ver todos los atajos

### Nueva Ventana Modal
- Presiona `Ctrl+K` o el botón en Configuración
- Se abre ventana con lista completa de atajos
- Cada atajo en forma de tarjeta visual
- Tema oscuro consistente con la aplicación

---

## 📦 Archivos Nuevos Creados

```
frontend/
├── src/main/java/com/ctrlaltquest/ui/
│   ├── utils/
│   │   ├── KeyBindings.java          ← Definiciones de atajos
│   │   └── KeyBindingManager.java    ← Gestor de atajos
│   ├── controllers/views/
│   │   └── KeyBindingsViewController.java ← Controlador de vista
│   └── examples/
│       └── KeyBindingsExamples.java  ← 7 ejemplos prácticos
│
├── src/main/resources/
│   ├── fxml/views/
│   │   └── keybindings_view.fxml     ← Vista de atajos
│   └── styles/
│       └── keybindings.css           ← Estilos de vista
│
└── Documents/
    ├── GUIA_ATAJOS_TECLADO.md        ← Guía completa
    └── IMPLEMENTACION_ATAJOS_TECLADO.md ← Resumen técnico
```

---

## 💡 Tips

### Muteando Música
```
Presiona Alt + M
→ Música se desactiva
→ Se actualiza automáticamente en Configuración
```

### Pausando Video
```
Presiona Espacio (o Alt + V)
→ Video se pausa
→ Presiona nuevamente para reanudar
```

### Pantalla Completa
```
Presiona F11
→ Aplicación entra en pantalla completa
→ Presiona F11 nuevamente para salir
```

---

## 📖 Más Información

Para documentación completa, leer:
- **GUIA_ATAJOS_TECLADO.md** - Documentación detallada
- **IMPLEMENTACION_ATAJOS_TECLADO.md** - Resumen técnico
- **KeyBindingsExamples.java** - Ejemplos de código

---

## ✅ Estado

- ✅ 9 atajos implementados
- ✅ Funcionan globalmente en toda la aplicación
- ✅ Integrados en Configuración
- ✅ Ventana modal de ayuda
- ✅ Documentación completa
- ✅ Ejemplos de uso

---

**¡Comienza a usar los atajos ahora mismo!** ⌨️🎮
