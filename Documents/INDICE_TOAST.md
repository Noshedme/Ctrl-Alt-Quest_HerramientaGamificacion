# 🎯 ÍNDICE DE TOAST NOTIFICATIONS

## 📋 Ruta de Inicio Recomendada

1. **Comienza por aquí** → `GUIA_RAPIDA_TOAST.md` ⚡ (5 minutos)
2. **Implementa el setup** → Agrega los 3 pasos
3. **Revisa ejemplos** → `ExampleControllerWithToast.java` 💻
4. **Para casos avanzados** → `GUIA_TOAST_NOTIFICATIONS.md` 📚
5. **Referencia rápida** → `TOAST_SYSTEM_COMPLETADO.md` 📖

---

## 📁 Archivos Incluidos

### 🎨 ESTILOS

#### `frontend/src/main/resources/styles/toast.css`
- 250+ líneas de CSS
- 5 tipos de toasts pre-diseñados
- 6 animaciones suaves
- Variantes: small, large, epic
- **Importar en scene:** `scene.getStylesheets().add("/styles/toast.css")`

---

### ☕ CÓDIGO JAVA

#### `Toast.java` (Principal)
```java
// Métodos públicos:
Toast.success(title, message)      // ✓ Verde
Toast.error(title, message)        // ✗ Rojo
Toast.warning(title, message)      // ⚠ Naranja
Toast.info(title, message)         // ⓘ Púrpura
Toast.gold(title, message)         // ★ Dorado
Toast.epic(title, message)         // ⭐ Épico (6s)
Toast.exception(title, exception)  // Automático
Toast.formError(field, message)    // Formulario
Toast.formSuccess(title, message)  // Formulario exitoso

// Inicializar (una sola vez):
Toast.initialize(toastContainer)
```

#### `ToastHelper.java` (Utilidades)
```java
// VALIDACIONES:
validateEmail(email)
validatePassword(password)
validateNotEmpty(value, fieldName)
validateRange(value, min, max, fieldName)

// MANEJADORES DE ERROR:
handleDatabaseError(exception)
handleNetworkError(exception)
handlePaymentError(exception)
handleDatabaseError(exception)

// OPERACIONES ESPECIALES:
epicEvent(title, description)
operationSuccess(operation, item, details)
loadingStarted(activity)
loadingComplete(dataType, count)
syncComplete(dataType)
deleteSuccess(type, name)
deleteError(exception, type)
```

#### `ExampleControllerWithToast.java` (Ejemplos)
- 10 ejemplos completos
- Casos reales: Login, Registro, CRUD, Pagos
- Patrones copiables de try/catch
- Validaciones de formulario
- Operaciones asincrónicas

---

### 📚 DOCUMENTACIÓN

#### `GUIA_RAPIDA_TOAST.md` ⚡ **EMPIEZA AQUÍ**
- 3 pasos de setup
- Casos de uso comunes (copiar-pegar)
- Checklist de implementación
- Patrones recomendados
- Solución de errores comunes
- **Tiempo:** 5-10 minutos

#### `GUIA_TOAST_NOTIFICATIONS.md` 📖 **DOCUMENTACIÓN COMPLETA**
- Setup detallado
- 30+ ejemplos de código
- Casos de uso por módulo (Auth, Misiones, Tienda, etc.)
- Personalización avanzada
- Posicionamiento y duración
- **Tiempo:** 20-30 minutos

#### `TOAST_SYSTEM_COMPLETADO.md` 📋 **REFERENCIA**
- Resumen de qué se agregó
- Flujo de implementación visual
- Estadísticas del sistema
- Patrones de uso
- Testing checklist
- Referencia rápida de métodos

---

## 🚀 Implementación Rápida (3 Pasos)

### 1️⃣ FXML - Crear contenedor
```xml
<VBox fx:id="toastContainer" maxWidth="400" maxHeight="600"/>
```

### 2️⃣ Java - Inicializar
```java
@FXML private VBox toastContainer;

public void initialize(...) {
    Toast.initialize(toastContainer);
}
```

### 3️⃣ CSS - Importar estilos
```java
scene.getStylesheets().add("/styles/toast.css");
```

✅ **¡Listo!**

---

## 💻 Uso Básico

### Éxito
```java
Toast.success("¡Éxito!", "Operación completada");
```

### Error
```java
try {
    operacion();
} catch (Exception e) {
    Toast.exception("Error", e);
}
```

### Validación
```java
if (!ToastHelper.validateEmail(email)) return;
```

---

## 🎓 Ejemplos por Caso de Uso

### 🔐 Login/Registro
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 500-550
ExampleControllerWithToast.java → handleLogin(), handleRegister()
```

### 👤 Actualizar Perfil
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 600-650
ExampleControllerWithToast.java → handleActualizarPerfil()
```

### ⚔️ Misiones
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 700-750
ExampleControllerWithToast.java → handleCompletarMisión()
```

### 🛍️ Tienda & Pagos
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 750-800
ExampleControllerWithToast.java → handleComprarItem()
```

### 📊 Cargar Datos Async
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 850-900
ExampleControllerWithToast.java → handleCargarDatos()
```

### 💾 Base de Datos
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 600-650
ExampleControllerWithToast.java → handleActualizarPerfil()
ToastHelper.java → handleDatabaseError()
```

### 🌐 Errores de Red
```
GUIA_TOAST_NOTIFICATIONS.md → Línea 900-950
ExampleControllerWithToast.java → handleCargarDatos()
ToastHelper.java → handleNetworkError()
```

---

## 🎨 Paleta de Colores

```
┌─────────────────────────────────────────────────────┐
│  Tipo      │ Color    │ Código   │ Icono │ Uso     │
├─────────────────────────────────────────────────────┤
│ SUCCESS    │ Verde    │ #4ade80  │  ✓   │ Éxito   │
│ ERROR      │ Rojo     │ #ff6b6b  │  ✗   │ Error   │
│ WARNING    │ Naranja  │ #f59e0b  │  ⚠   │ Aviso   │
│ INFO       │ Púrpura  │ #a855f7  │  ⓘ   │ Info    │
│ GOLD       │ Dorado   │ #f7d27a  │  ★   │ Épico   │
└─────────────────────────────────────────────────────┘
```

---

## ⏱️ Duración Automática

| Tipo | Duración | Comportamiento |
|------|----------|---|
| **Normal** | 4 segundos | Aparece, espera, desaparece |
| **Epic** | 6 segundos | Aparece, espera más, desaparece |

---

## 🔗 Estructura de Carpetas

```
Ctrl-Alt-Quest/
│
├── 📁 frontend/
│   └── src/main/
│       ├── java/com/ctrlaltquest/ui/utils/
│       │   ├── 📄 Toast.java ✨ NUEVO
│       │   ├── 📄 ToastHelper.java ✨ NUEVO
│       │   └── 📄 ExampleControllerWithToast.java ✨ NUEVO
│       │
│       └── resources/styles/
│           └── 📄 toast.css ✨ NUEVO
│
├── 📁 Documents/
│   ├── 📄 GUIA_RAPIDA_TOAST.md ✨ NUEVO (EMPIEZA AQUÍ!)
│   ├── 📄 GUIA_TOAST_NOTIFICATIONS.md ✨ NUEVO
│   ├── 📄 TOAST_SYSTEM_COMPLETADO.md ✨ NUEVO
│   └── 📄 INDICE_TOAST.md ✨ NUEVO (este archivo)
```

---

## ✨ Características Principales

✅ **5 tipos predefinidos** - SUCCESS, ERROR, WARNING, INFO, GOLD
✅ **Animaciones suaves** - Entrada, salida, bounce, pulse
✅ **Validaciones integradas** - Email, password, rango, no-vacío
✅ **Manejadores especiales** - Base de datos, red, pagos
✅ **Ejemplos completos** - 10 casos reales de uso
✅ **Documentación extensiva** - 3 guías + ejemplos
✅ **Fácil integración** - 3 pasos de setup
✅ **Thread-safe** - Compatible con operaciones async
✅ **Consistencia visual** - Tema Dorado & Púrpura
✅ **Extensible** - CSS y Java fáciles de customizar

---

## 📊 Estadísticas

| Métrica | Valor |
|---------|-------|
| Tipos de notificación | 5 |
| Métodos en Toast | 9 |
| Métodos en ToastHelper | 20+ |
| Ejemplos de código | 30+ |
| Líneas CSS | 250+ |
| Líneas Java | 400+ |
| Documentación | 3 guías |
| Animaciones | 6 |
| Tiempo de setup | 5 minutos |

---

## 🎯 Próximos Pasos

### Paso 1: Leer guía rápida (5 min)
→ `GUIA_RAPIDA_TOAST.md`

### Paso 2: Implementar setup (5 min)
→ Crear contenedor FXML
→ Inicializar en controlador
→ Importar CSS

### Paso 3: Agregar a try/catch (10 min)
→ `Toast.success()` en éxito
→ `Toast.exception()` en catch

### Paso 4: Agregar validaciones (10 min)
→ `ToastHelper.validateEmail()`
→ `ToastHelper.validatePassword()`
→ etc.

### Paso 5: Revisar ejemplos (20 min)
→ `ExampleControllerWithToast.java`
→ Copiar patrones a tus controladores

### Paso 6: Implementar en módulos (30 min+)
→ Login/Register
→ Formularios
→ CRUD (Create, Read, Update, Delete)
→ Misiones
→ Tienda
→ Eventos

**Total estimado:** 1-2 horas para implementación completa

---

## 🔍 Búsqueda Rápida

### Necesito... 🔎

**Empezar desde cero**
→ `GUIA_RAPIDA_TOAST.md` (3 pasos)

**Un ejemplo de login**
→ `ExampleControllerWithToast.java` (línea 40)

**Validar email**
→ `ToastHelper.validateEmail()` (línea 100)

**Manejo de BD**
→ `ToastHelper.handleDatabaseError()` (línea 50)

**Operación async**
→ `ExampleControllerWithToast.java` (línea 200)

**Evento épico**
→ `Toast.epic()` o `ToastHelper.epicEvent()`

**Más detalles**
→ `GUIA_TOAST_NOTIFICATIONS.md` (documentación completa)

---

## 🚀 Comandos Comunes

### Mostrar éxito
```java
Toast.success("Título", "Mensaje");
```

### Mostrar error
```java
Toast.error("Título", "Mensaje");
```

### Mostrar con excepción
```java
Toast.exception("Título", exception);
```

### Validar email
```java
if (!ToastHelper.validateEmail(email)) return;
```

### Manejo BD
```java
catch (SQLException e) {
    ToastHelper.handleDatabaseError(e);
}
```

### Operación exitosa
```java
ToastHelper.operationSuccess("Compra", "Espada", "100 monedas");
```

### Evento épico
```java
Toast.epic("⭐ LOGRO", "¡Desbloqueado!");
```

---

## 💡 Tips Importantes

✅ Inicializa Toast **una sola vez** en el controlador principal
✅ Usa `Platform.runLater()` para toasts desde threads
✅ Las validaciones **muestran toast automáticamente**
✅ Los toasts desaparecen **automáticamente**
✅ Soporta múltiples mensajes **sin solapamiento**
✅ CSS es **totalmente customizable**
✅ Colores usan **tu tema actual**

---

## 🎓 Recursos

| Recurso | Contenido | Tiempo |
|---------|----------|--------|
| GUIA_RAPIDA_TOAST.md | Setup + Básicos | 5 min |
| GUIA_TOAST_NOTIFICATIONS.md | Completo + 30 ejemplos | 30 min |
| TOAST_SYSTEM_COMPLETADO.md | Referencia + Patrones | 15 min |
| ExampleControllerWithToast.java | 10 ejemplos reales | 20 min |
| toast.css | Estilos + Animaciones | Referencia |

---

## 📞 Referencia de Métodos

### Toast (Principal)
- `success(title, message)`
- `error(title, message)`
- `warning(title, message)`
- `info(title, message)`
- `gold(title, message)`
- `epic(title, message)`
- `exception(title, exception)`
- `formError(field, message)`
- `formSuccess(title, message)`
- `initialize(container)`

### ToastHelper (Utilidades)
- **Validar:** validateEmail, validatePassword, validateNotEmpty, validateRange
- **BD:** handleDatabaseError, deleteSuccess, deleteError
- **Red:** handleNetworkError, syncError, syncComplete
- **Pagos:** handlePaymentError
- **UI:** operationSuccess, loadingStarted, loadingComplete
- **Login:** epicEvent, verificationFailed

---

## ✅ Checklist Final

- [ ] Leí `GUIA_RAPIDA_TOAST.md`
- [ ] Agregué VBox en FXML
- [ ] Inicialicé Toast en controlador
- [ ] Importé CSS
- [ ] Probé Toast.success()
- [ ] Probé Toast.error()
- [ ] Agregué en try/catch
- [ ] Probé validaciones
- [ ] Revié ExampleControllerWithToast.java
- [ ] Implementé en mis controladores
- [ ] Probé todas las variantes
- [ ] Listo para usar en producción ✅

---

**¡Sistema de Toast Notifications completamente configurado y listo para usar!** 🎉

**Última actualización:** 2 de Marzo de 2026
**Versión:** 1.0
**Estado:** ✅ Producción
