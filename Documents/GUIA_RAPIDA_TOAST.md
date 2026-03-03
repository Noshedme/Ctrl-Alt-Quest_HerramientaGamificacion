# 🚀 Guía Rápida de Toast Notifications - 3 Pasos

## ⚡ Setup Rápido (2 minutos)

### Paso 1: Crear el contenedor en tu FXML principal

En tu `home.fxml` o controlador principal, agrega un VBox transparente en la parte superior:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>

<BorderPane fx:id="rootPane" xmlns="http://javafx.com/javafx" 
            xmlns:fx="http://javafx.com/fxml"
            fx:controller="com.ctrlaltquest.ui.controllers.HomeController">
    
    <!-- CONTENEDOR DE TOASTS (Ponerlo primero) -->
    <VBox fx:id="toastContainer" maxWidth="400" maxHeight="600"/>
    
    <!-- REST DEL CONTENIDO -->
    <center>
        <!-- Tu contenido aquí -->
    </center>
</BorderPane>
```

### Paso 2: Inicializar Toast en el controlador

```java
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import com.ctrlaltquest.ui.utils.Toast;

public class HomeController {
    
    @FXML private VBox toastContainer;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicializar Toast ✅
        Toast.initialize(toastContainer);
    }
}
```

### Paso 3: Importar los estilos CSS

En tu `MainApp.java` o donde cargas las scenes:

```java
scene.getStylesheets().add(
    getClass().getResource("/styles/toast.css").toExternalForm()
);
```

✅ **¡Listo!** Ya puedes usar Toast en cualquier lugar

---

## 💻 Uso Básico (Copiar y Pegar)

### Éxito
```java
Toast.success("¡Éxito!", "Operación completada");
```

### Error
```java
Toast.error("Error", "Algo salió mal");
```

### Advertencia
```java
Toast.warning("Cuidado", "Verifica esto");
```

### Información
```java
Toast.info("Nota", "Para tu información");
```

### Especial/Épico
```java
Toast.gold("🏆 LOGRO", "¡Ganaste algo especial!");
```

---

## 🎯 Casos de Uso Comunes

### Try/Catch Simple

```java
try {
    usuario = usuarioDAO.obtenerPorID(id);
    Toast.success("Cargado", "Usuario encontrado");
} catch (SQLException e) {
    Toast.error("Error de BD", "No se pudo cargar el usuario");
}
```

### Validación de Formulario

```java
if (emailField.getText().isEmpty()) {
    Toast.formError("Email", "El email es requerido");
    return;
}
```

### Click en Botón

```java
guardarButton.setOnAction(e -> {
    try {
        guardar();
        Toast.success("✓ Guardado", "Los cambios se guardaron");
    } catch (Exception ex) {
        Toast.exception("Error", ex);
    }
});
```

### Operación Larga

```java
button.setOnAction(e -> {
    Toast.info("Cargando...", "Por favor espera");
    
    new Thread(() -> {
        try {
            List<Dato> datos = cargarDatos();
            Platform.runLater(() -> {
                Toast.success("Datos", "Se cargaron " + datos.size() + " items");
            });
        } catch (Exception ex) {
            Platform.runLater(() -> Toast.exception("Error", ex));
        }
    }).start();
});
```

### Compra/Pago

```java
if (jugador.getDinero() < precio) {
    Toast.warning("Dinero Insuficiente", 
        "Te faltan " + (precio - jugador.getDinero()) + " monedas");
    return;
}

realizar_compra();
Toast.gold("🛍️ Compra", "¡Obtuviste: Espada de Fuego!");
```

---

## 🔥 Atajos Útiles

### Para excepciones en BD:
```java
} catch (SQLException e) {
    Toast.exception("Error BD", e);  // Muestra el error exacto
}
```

### Para validación:
```java
ToastHelper.validateEmail(email);        // Valida y muestra toast
ToastHelper.validateNotEmpty(texto, "Campo");  // Valida y muestra toast
```

### Para pago/dinero:
```java
ToastHelper.handlePaymentError(exception);  // Detecta tipo de error automáticamente
```

### Para carga de datos:
```java
ToastHelper.loadingStarted("Descargando...");
// ... código ...
ToastHelper.loadingComplete("registros", "150");
```

### Para evento épico:
```java
ToastHelper.epicEvent("MISIÓN COMPLETADA", "Ganaste 500 XP");
```

---

## 📋 Checklist de Implementación

- [ ] Crear VBox `toastContainer` en FXML principal
- [ ] Inicializar Toast en el controlador: `Toast.initialize(toastContainer)`
- [ ] Importar CSS `toast.css` en la scene
- [ ] Importar clase Toast en controladores: `import com.ctrlaltquest.ui.utils.Toast;`
- [ ] Agregar Toast.success/error/warning en tus try/catch
- [ ] Probar que aparecen las notificaciones
- [ ] Verificar que se cierren automáticamente después de 4 segundos
- [ ] Disfrutar de notificaciones hermosas 🎉

---

## 🎨 Colores Predefinidos

| Tipo | Color | Uso |
|------|-------|-----|
| **SUCCESS** | Verde `#4ade80` | Operaciones exitosas ✓ |
| **ERROR** | Rojo `#ff6b6b` | Errores ✗ |
| **WARNING** | Naranja `#f59e0b` | Advertencias ⚠ |
| **INFO** | Púrpura `#a855f7` | Información ⓘ |
| **GOLD** | Dorado `#f7d27a` | Eventos épicos ★ |

---

## 🚨 Errores Comunes

### "Toast not initialized"
```
❌ No inicializaste Toast.initialize(container)
✅ Solución: Agregar Toast.initialize(toastContainer) en initialize()
```

### No aparecen los Toasts
```
❌ El contenedor no está en la scene
✅ Solución: Verificar que toastContainer esté agregado al BorderPane/Pane
```

### CSS no se aplica
```
❌ No importaste el CSS
✅ Solución: Agregar scene.getStylesheets().add("/styles/toast.css")
```

---

## 🎓 Patrones Recomendados

### Patrón 1: Validación + Operación
```java
if (!ToastHelper.validateEmail(email)) return;
if (!ToastHelper.validateNotEmpty(password, "Contraseña")) return;

try {
    login(email, password);
    Toast.success("Bienvenido", "Login exitoso");
} catch (Exception e) {
    Toast.exception("Error de Login", e);
}
```

### Patrón 2: Operación Async
```java
new Thread(() -> {
    try {
        List<?> data = loadFromServer();
        Platform.runLater(() -> {
            Toast.success("Datos", "Cargados: " + data.size());
        });
    } catch (Exception e) {
        Platform.runLater(() -> Toast.exception("Error", e));
    }
}).start();
```

### Patrón 3: Operación CRUD
```java
try {
    switch (operation) {
        case "create": dao.insert(obj); Toast.success("Creado", "Nuevo registro");
        case "update": dao.update(obj); Toast.success("Actualizado", "Cambios guardados");
        case "delete": dao.delete(obj); Toast.success("Eliminado", "Registro removido");
    }
} catch (Exception e) {
    ToastHelper.handleDatabaseError(e);
}
```

---

## 📚 Archivos Agregados

✅ `toast.css` - Estilos y animaciones
✅ `Toast.java` - Clase principal
✅ `ToastHelper.java` - Utilidades
✅ `ExampleControllerWithToast.java` - 10 ejemplos reales
✅ `GUIA_TOAST_NOTIFICATIONS.md` - Documentación completa
✅ `GUIA_RAPIDA_TOAST.md` - Esta guía

---

## 🎮 Demo Interactivo

Prueba agregando esto a un botón para ver los Toasts en acción:

```java
demoButton.setOnAction(e -> {
    Toast.success("¡Hola!", "Este es un toast SUCCESS");
    Toast.error("Error", "Este es un toast ERROR");
    Toast.warning("Cuidado", "Este es un toast WARNING");
    Toast.info("Info", "Este es un toast INFO");
    Toast.gold("¡ÉPICO!", "Este es un toast GOLD");
});
```

---

## ✨ Próximos Pasos

1. **Implementar en Login/Registro** - Validaciones de formulario
2. **Implementar en BD** - Errores de conexión y CRUD
3. **Implementar en Misiones** - Eventos épicos
4. **Implementar en Tienda** - Compras y pagos
5. **Customizar colores** (opcional) - Ajustar a tu tema

---

¡**Listo!** Si necesitas ayuda, revisa `GUIA_TOAST_NOTIFICATIONS.md` para ejemplos avanzados 🚀
