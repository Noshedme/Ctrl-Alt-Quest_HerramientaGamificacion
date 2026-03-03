# 🎬 Sistema de Toast Notifications - GUÍA VISUAL

## 🎯 Flujo Completo del Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         APLICACIÓN CTRL+ALT+QUEST                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    VISTA (FXML)                                   │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │  VBox fx:id="toastContainer" (DONDE APARECEN LOS TOASTS!)  │  │  │
│  │  │                                                             │  │  │
│  │  │  ┌─────────────────────────────────┐                      │  │  │
│  │  │  │  ✓ ¡Éxito!                      │ ← Toast animado     │  │  │
│  │  │  │  Operación completada           │                     │  │  │
│  │  │  └─────────────────────────────────┘                      │  │  │
│  │  │                                                             │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  │                                                                    │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │  FORMULARIOS, BOTONES, CAMPOS DE ENTRADA                   │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  │                                                                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                   CONTROLADOR (Java)                              │  │
│  │                                                                    │  │
│  │  1. INICIALIZAR (al cargar la aplicación)                        │  │
│  │     ├─ @Override initialize() {                                  │  │
│  │     └─   Toast.initialize(toastContainer); ✓                    │  │
│  │                                                                    │  │
│  │  2. USAR EN TRY/CATCH (en operaciones)                           │  │
│  │     ├─ try {                                                      │  │
│  │     │   operacion();                                              │  │
│  │     │   Toast.success("OK", "Listo!"); ✓ ÉXITO                  │  │
│  │     ├─ } catch (Exception e) {                                    │  │
│  │     │   Toast.exception("Error", e);  ✗ ERROR                   │  │
│  │     └─ }                                                          │  │
│  │                                                                    │  │
│  │  3. VALIDACIONES                                                  │  │
│  │     ├─ if (!ToastHelper.validateEmail(email)) ⚠ AVISO           │  │
│  │     └─   return;                                                  │  │
│  │                                                                    │  │
│  │  4. EVENTOS ESPECIALES                                            │  │
│  │     └─ Toast.epic("⭐ LOGRO", "¡Desbloqueado!"); ★ ÉPICO        │  │
│  │                                                                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                   ESTILOS CSS (toast.css)                         │  │
│  │                                                                    │  │
│  │  ✓ Success (Verde #4ade80)   - Operaciones exitosas            │  │
│  │  ✗ Error (Rojo #ff6b6b)      - Errores y excepciones           │  │
│  │  ⚠ Warning (Naranja #f59e0b) - Advertencias                    │  │
│  │  ⓘ Info (Púrpura #a855f7)   - Información general             │  │
│  │  ★ Gold (Dorado #f7d27a)    - Eventos épicos                   │  │
│  │                                                                    │  │
│  │  ANIMACIONES:                                                      │  │
│  │  • Entrada: Desliza + Fade (500ms)                              │  │
│  │  • Permanencia: 4 segundos (6 segundos épico)                    │  │
│  │  • Salida: Desliza + Fade (500ms)                               │  │
│  │                                                                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Ciclo de Vida de un Toast

```
TIEMPO          ESTADO              VISUAL
═════════════════════════════════════════════════════════════════

    0ms    CREACIÓN
           ↓
           Se llama: Toast.success("Hola", "Mundo")
           ↓
           Se crea componente VBox

    100ms  ANIMACIÓN ENTRADA ⬇️
           Opacidad:  0 → 1
           PosX:    +400 → 0
           Escala:  0.9 → 1.0

    500ms  VISIBLE ✓
           ┌──────────────────┐
           │ ✓ Hola           │
           │ Mundo            │
           └──────────────────┘

    500ms  |||||||||||||||||||||||||||||  (duración: 4000-6000ms)
   -4500ms |||||||||||||||||||||||||||||

    5000ms ANIMACIÓN SALIDA ⬇️
           Opacidad:  1 → 0
           PosX:      0 → +400
           Escala:   1.0 → 0.9

    5500ms DESTRUCCIÓN
           Se elimina del DOM
           Fin
```

---

## 🎨 Vista de Colores y Animación

### Paleta de Colores

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  ÉXITO          │  ✓ ✓ ✓  Verde #4ade80           │
│                 │  Operaciones exitosas              │
│                 ├─────────────────────────────────┤
│                                                     │
│  ERROR          │  ✗ ✗ ✗  Rojo #ff6b6b            │
│                 │  Errores y excepciones            │
│                 ├─────────────────────────────────┤
│                                                     │
│  WARNING        │  ⚠ ⚠ ⚠  Naranja #f59e0b        │
│                 │  Advertencias y validaciones      │
│                 ├─────────────────────────────────┤
│                                                     │
│  INFO           │  ⓘ ⓘ ⓘ  Púrpura #a855f7       │
│                 │  Información y notificaciones    │
│                 ├─────────────────────────────────┤
│                                                     │
│  ESPECIAL/ÉPICO │  ★ ★ ★  Dorado #f7d27a        │
│                 │  Logros y eventos épicos         │
│                 │  (duración aumentada a 6s)       │
│                 │                                   │
└─────────────────────────────────────────────────────┘
```

### Animación de Entrada

```
ANTES (Opacidad 0%)              DURANTE (Opacidad 50%)           DESPUÉS (Opacidad 100%)
┌─────────────────────┐          ┌─────────────────────┐          ┌─────────────────────┐
│░░░░░░░░░░░░░░░░░░░░│          │ · · · · · · · · · · │          │ ┌─────────────────┐ │
│░░░░░░Toast░░░░░░░░│          │ · · · Toast · · · · │          │ │ ✓ ¡Éxito!      │ │
│░░░░░░░░░░░░░░░░░░░░│          │ · · · · · · · · · · │          │ │ Completado ✓    │ │
│░░░░░░░░░░░░░░░░░░░░│          │ · · · · · · · · · · │          │ └─────────────────┘ │
└─────────────────────┘          └─────────────────────┘          └─────────────────────┘
    0ms (Inicio)                     250ms (Mitad)                   500ms (Final)
    Traslación: +400px              Traslación: +200px             Traslación: 0px
    Escala: 0.9                     Escala: 0.95                   Escala: 1.0
```

---

## 🔄 Flujo de Uso en Controlador

```
┌──────────────────────────────────────────────────────────────┐
│                  CONTROLADOR                                 │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  MÉTODO: handleLogin()                                       │
│  │                                                            │
│  ├─→ 1️⃣ VALIDACIÓN                                            │
│  │   if (!ToastHelper.validateEmail(email))                  │
│  │     └─ Toast muestra: "❌ Email Inválido"                │
│  │        Return (no continúa)                               │
│  │                                                            │
│  ├─→ 2️⃣ TRY/CATCH                                             │
│  │   try {                                                    │
│  │     usuario = autenticar(email, password)                │
│  │                                                            │
│  │     ├─→ 3️⃣ ÉXITO                                           │
│  │     │   Toast.success("✓ Bienvenido", "Login OK")        │
│  │     │   cambiarAPantallaPrincipal()                      │
│  │     │                                                     │
│  │   } catch (SQLException e) {                              │
│  │     │                                                     │
│  │     ├─→ 4️⃣ ERROR BD                                        │
│  │     │   ToastHelper.handleDatabaseError(e)               │
│  │     │   Toast muestra: "❌ Error de BD"                  │
│  │     │                                                     │
│  │   } catch (Exception e) {                                │
│  │     │                                                     │
│  │     └─→ 5️⃣ ERROR GENÉRICO                                 │
│  │         Toast.exception("Error", e)                     │
│  │         Toast muestra: "❌ Error: [mensaje]"            │
│  │                                                          │
│  └─→ FIN                                                     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Estructura de Métodos

```
Toast (Clase Principal)
├─ initialize(container) ........... Inicializa el sistema (una sola vez)
│
├─ SUCCESS ......................... Operación exitosa
│  └─ success(title, message)
│  └─ formSuccess(title, message)
│
├─ ERROR ........................... Error o excepción
│  ├─ error(title, message)
│  ├─ exception(title, exception)
│  └─ formError(field, message)
│
├─ WARNING ......................... Advertencia
│  └─ warning(title, message)
│
├─ INFO ............................ Información
│  └─ info(title, message)
│
├─ SPECIAL ......................... Eventos épicos
│  ├─ gold(title, message)
│  └─ epic(title, message)
│
└─ INTERNOS ........................ Animaciones y control
   ├─ animateEntry(toast)
   ├─ animateExit(toast)
   └─ createToastUI(title, message, type)


ToastHelper (Clase Utilidades)
├─ VALIDACIONES .................... Validan y muestran toast
│  ├─ validateEmail(email)
│  ├─ validatePassword(password)
│  ├─ validateNotEmpty(value, fieldName)
│  └─ validateRange(value, min, max, fieldName)
│
├─ MANEJADORES DE ERROR ............ Detectan tipo de error
│  ├─ handleDatabaseError(exception)
│  ├─ handleNetworkError(exception)
│  └─ handlePaymentError(exception)
│
├─ OPERACIONES ..................... Para operaciones comunes
│  ├─ operationSuccess(operation, item, details)
│  ├─ loadingStarted(activity)
│  ├─ loadingComplete(dataType, count)
│  └─ syncComplete(dataType)
│
└─ EVENTOS ......................... Para eventos especiales
   ├─ epicEvent(title, description)
   ├─ deleteSuccess(type, name)
   └─ verificationFailed(reason)
```

---

## 🔢 Números Clave del Sistema

```
┌────────────────────────────────┐
│  DURACIÓN                       │
├────────────────────────────────┤
│  Animación entrada     500 ms   │
│  Animación salida      500 ms   │
│  Permanencia normal    4000 ms  │
│  Permanencia épico     6000 ms  │
└────────────────────────────────┘

┌────────────────────────────────┐
│  DIMENSIONES                    │
├────────────────────────────────┤
│  Ancho normal          350 px   │
│  Ancho grande          450 px   │
│  Ancho pequeño         250 px   │
│  Alto mínimo           80 px    │
│  Padding               20 px    │
└────────────────────────────────┘

┌────────────────────────────────┐
│  DESPLAZAMIENTO                 │
├────────────────────────────────┤
│  X entrada/salida      400 px   │
│  Y (stack)             10 px    │
│  Offset desde borde:           │
│    Superior            20 px    │
│    Derecha             20 px    │
└────────────────────────────────┘
```

---

## 🎭 Ejemplos de Estados

### ✅ Toast SUCCESS

```
┌─────────────────────────────────┐
│                                 │
│  ✓ Operación Completada         │ ← Título (Verde #4ade80)
│  Los cambios se guardaron...     │ ← Mensaje (Blanco)
│                                 │
│  Borde: Verde #4ade80           │
│  Fondo: Transparente + Verde    │
│  Efecto: Brillo dorado al hover │
│                                 │
└─────────────────────────────────┘
```

### ❌ Toast ERROR

```
┌─────────────────────────────────┐
│                                 │
│  ✗ Error de Conexión            │ ← Título (Rojo #ff6b6b)
│  No se pudo conectar a la BD     │ ← Mensaje (Blanco)
│                                 │
│  Borde: Rojo #ff6b6b            │
│  Fondo: Transparente + Rojo     │
│  Efecto: Brillo rojo al hover   │
│                                 │
└─────────────────────────────────┘
```

### ⭐ Toast EPIC

```
┌──────────────────────────────────────────┐
│                                          │
│  ★ ¡LOGRO DESBLOQUEADO!                 │ ← Título (Dorado #f7d27a)
│  Completaste 10 misiones en 1 hora       │ ← Mensaje (Púrpura claro)
│                                          │
│  Borde: Dorado #f7d27a                  │
│  Fondo: Gradiente dorado                │
│  Efecto: Pulso continuo                 │
│  Duración: 6 segundos (vs 4 normales)   │
│                                          │
└──────────────────────────────────────────┘
```

---

## 🔌 Integración en Proyecto

```
Mi Aplicación Ctrl+Alt+Quest
│
├── 🎨 FRONTEND
│   ├── 📁 Recursos
│   │   ├── styles/
│   │   │   ├── shared.css (existente)
│   │   │   ├── alerts.css (existente)
│   │   │   └── toast.css ✨ NUEVO
│   │   └── fxml/
│   │       └── home.fxml (AGREGAR: <VBox fx:id="toastContainer"/>)
│   │
│   └── 🏗️ Código Java
│       ├── ui/controllers/
│       │   ├── HomeController.java (AGREGAR: Toast.initialize())
│       │   ├── LoginController.java (USAR: Toast.success/error)
│       │   ├── MisionController.java (USAR: Toast.epic)
│       │   └── TiendaController.java (USAR: Toast.gold)
│       │
│       └── ui/utils/
│           ├── Toast.java ✨ NUEVO
│           ├── ToastHelper.java ✨ NUEVO
│           └── ExampleControllerWithToast.java ✨ NUEVO
│
├── 📖 DOCUMENTACIÓN
│   ├── INDICE_TOAST.md ✨ NUEVO
│   ├── GUIA_RAPIDA_TOAST.md ✨ NUEVO
│   ├── GUIA_TOAST_NOTIFICATIONS.md ✨ NUEVO
│   └── TOAST_SYSTEM_COMPLETADO.md ✨ NUEVO
│
└── 🗄️ BASE DE DATOS
    └── (Sin cambios)
```

---

## 🎯 Checklists Rápidos

### ✅ Setup Inicial (5 minutos)

- [ ] Crear VBox `toastContainer` en FXML
- [ ] Inyectar con `@FXML`
- [ ] Llamar `Toast.initialize()` en `initialize()`
- [ ] Importar CSS: `scene.getStylesheets().add("/styles/toast.css")`
- [ ] Compilar sin errores

### ✅ Primer Toast (2 minutos)

```java
btnTest.setOnAction(e -> Toast.success("¡Hola!", "Mi primer toast"));
```

- [ ] Hacer click en botón
- [ ] Ver Toast apareciendo desde la derecha
- [ ] Ver Toast desapareciendo después de 4 segundos
- [ ] Ver animaciones suaves

### ✅ Try/Catch (5 minutos)

- [ ] Agregar `Toast.success()` en path exitoso
- [ ] Agregar `Toast.exception()` en catch
- [ ] Probar operación exitosa
- [ ] Probar operación fallida

### ✅ Validaciones (5 minutos)

- [ ] Usar `ToastHelper.validateEmail()`
- [ ] Ver que muestra error automáticamente
- [ ] No continúa si validación falla
- [ ] Validación exitosa permite continuar

---

## 📚 Documentación Incluida

```
GUIA_RAPIDA_TOAST.md
├─ ¿Cómo empezar? (3 pasos rápidos)
├─ Ejemplos copiar-pegar
├─ Casos comunes
└─ Solución de errores

GUIA_TOAST_NOTIFICATIONS.md
├─ Guía completa
├─ 30+ ejemplos de código
├─ Por módulo (Login, Misiones, Tienda, etc.)
├─ Personalización avanzada
└─ Troubleshooting

TOAST_SYSTEM_COMPLETADO.md
├─ Qué se agregó
├─ Flujo de implementación
├─ Estadísticas del sistema
├─ Patrones recomendados
└─ Testing checklist

INDICE_TOAST.md (este)
├─ Ruta de inicio
├─ Estructura de carpetas
├─ Búsqueda rápida
└─ Referencia de métodos
```

---

## 🚀 Próximas Acciones

1. ✅ Leer `GUIA_RAPIDA_TOAST.md` (5 min)
2. ✅ Implementar setup (5 min)
3. ✅ Agregar primer Toast (2 min)
4. ✅ Probar animaciones (2 min)
5. ✅ Agregar en try/catch (10 min)
6. ✅ Revisar `ExampleControllerWithToast.java` (20 min)
7. ✅ Implementar en tus controladores (30+ min)

**Total:** 1-2 horas para implementación completa

---

## ✨ ¡Sistema Listo!

```
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║  ✅ Sistema de Toast Notifications Completado             ║
║                                                            ║
║  ✨ 5 tipos predefinidos                                  ║
║  ⚡ Animaciones suaves y dinámicas                        ║
║  🎨 Consistente con tu tema Dorado & Púrpura             ║
║  📚 Documentación extensiva                               ║
║  💻 20+ ejemplos de código                                ║
║  🔧 Fácil de integrar en try/catch                        ║
║  🚀 Listo para producción                                 ║
║                                                            ║
║  PRÓXIMO PASO: Lee GUIA_RAPIDA_TOAST.md                 ║
║                                                            ║
╚════════════════════════════════════════════════════════════╝
```

---

**Versión:** 1.0 Completo  
**Fecha:** 2 de Marzo de 2026  
**Estado:** ✅ Producción Lista  
**Documentación:** ✅ Completa
