# 🎯 Sistema de Toast Notifications Animadas

## ✨ Características Principales

✅ **5 tipos de notificaciones pre-diseñadas:**
- **SUCCESS** (Verde `#4ade80`) - Operaciones exitosas ✓
- **ERROR** (Rojo `#ff6b6b`) - Errores y excepciones ✗
- **WARNING** (Naranja `#f59e0b`) - Advertencias ⚠
- **INFO** (Púrpura `#a855f7`) - Información general ⓘ
- **GOLD** (Dorado `#f7d27a`) - Eventos épicos ★

✅ **Animaciones suaves:**
- Entrada: Desliza desde la derecha con fade in y escala
- Salida: Desliza a la derecha con fade out
- Duración automática: 4 segundos (6 segundos para épicos)
- Sin intervención del usuario

✅ **Consistencia visual:**
- Colores coordinados con el tema Ctrl+Alt+Quest
- Bordes dorados semi-transparentes
- Sombras dinámicas
- Efectos visuales al hover

---

## 🚀 Instalación y Setup

### Paso 1: Importar el CSS

En tu archivo FXML principal o en el controlador, agrega:

```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    // Cargar el CSS de Toasts
    String toastCss = getClass().getResource("/styles/toast.css").toExternalForm();
    
    // Inicializar el contenedor de Toasts
    VBox toastContainer = new VBox();
    toastContainer.setPrefSize(400, 600);
    
    // Agregarlo a tu scene root como overlay
    YourRootPane.getChildren().add(toastContainer);
    
    // Inicializar el sistema de Toast
    Toast.initialize(toastContainer);
}
```

### Paso 2: Agregar a tu Scene Root

En FXML (ejemplo):

```xml
<BorderPane fx:id="rootPane">
    <VBox fx:id="toastContainer" maxWidth="400" maxHeight="600"
          style="-fx-background-color: transparent;" />
    <!-- Resto de tu contenido -->
</BorderPane>
```

En Java:

```java
VBox toastContainer = new VBox();
rootPane.setTop(toastContainer); // O el layout que uses
Toast.initialize(toastContainer);
```

---

## 💻 Ejemplos de Uso

### 1️⃣ Uso Básico (Operaciones Exitosas)

```java
// Cuando una operación finaliza correctamente
Toast.success("¡Éxito!", "Usuario registrado correctamente");
Toast.success("Guardado", "Misión completada exitosamente");
Toast.success("Compra", "Artículo adquirido de la tienda");
```

### 2️⃣ Manejo de Errores con Try/Catch

```java
try {
    // Código que puede fallar
    usuario = usuarioDAO.obtenerPorID(id);
} catch (SQLException e) {
    Toast.error("Error de Base de Datos", 
        "No se pudo cargar el usuario: " + e.getMessage());
}

// Alternativa más corta:
try {
    procesarPago();
} catch (Exception e) {
    Toast.exception("Error de Pago", e);
}
```

### 3️⃣ Validaciones de Formulario

```java
if (emailField.getText().isEmpty()) {
    Toast.formError("Email Requerido", 
        "Por favor ingresa tu correo electrónico");
    return;
}

// Después de validaciones exitosas:
if (registrarCuenta(email, password)) {
    Toast.formSuccess("Registro", 
        "¡Cuenta creada exitosamente!");
}
```

### 4️⃣ Advertencias

```java
if (dineroInsuficiente) {
    Toast.warning("Dinero Insuficiente", 
        "No tienes suficientes monedas para esta compra");
}

Toast.warning("Conexión Lenta", 
    "La conexión parece lenta, por favor espera...");
```

### 5️⃣ Información General

```java
Toast.info("Nuevo Evento", "Un evento contextual ha sido detectado");
Toast.info("Misión Disponible", "Tienes una nueva misión en tu zona");
```

### 6️⃣ Eventos Épicos (Especiales)

```java
// Logros o eventos especiales
Toast.gold("🏆 LOGRO DESBLOQUEADO", "¡Has alcanzado el nivel 10!");
Toast.epic("⭐ MISIÓN ÉPICA", "Completaste una misión de dificultad extrema");
```

### 7️⃣ Carga de Datos

```java
Thread loadDataThread = new Thread(() -> {
    try {
        List<Usuario> usuarios = usuarioDAO.obtenerTodos();
        Platform.runLater(() -> {
            Toast.success("Datos Cargados", 
                "Se cargaron " + usuarios.size() + " registros");
        });
    } catch (Exception e) {
        Toast.exception("Error al Cargar", e);
    }
});
loadDataThread.start();
```

---

## 📋 Casos de Uso Complejos

### Login/Registro

```java
loginButton.setOnAction(e -> {
    try {
        if (!validarCampos()) {
            Toast.formError("Campos Vacíos", 
                "Email y contraseña son requeridos");
            return;
        }
        
        Usuario usuario = autenticar(email, password);
        
        if (usuario != null) {
            Toast.success("Bienvenido", "¡Hola " + usuario.getNombre() + "!");
            // Cambiar pantalla...
        } else {
            Toast.error("Error de Login", 
                "Email o contraseña incorrectos");
        }
    } catch (NetworkException ex) {
        Toast.exception("Error de Conexión", ex);
    }
});
```

### Sistema de Misiones

```java
completarMision(mision);

if (mision.esEpica()) {
    Toast.epic("⚔️ MISIÓN ÉPICA COMPLETADA", 
        "Ganaste " + mision.getRecompensa() + " monedas de oro");
} else {
    Toast.success("Misión Completada", 
        "+" + mision.getRecompensa() + " XP");
}
```

### Sistema de Tienda

```java
comprarArticulo.setOnAction(e -> {
    try {
        if (jugador.getDinero() < articulo.getPrecio()) {
            Toast.warning("Dinero Insuficiente", 
                "Te faltan " + (articulo.getPrecio() - jugador.getDinero()) 
                + " monedas");
            return;
        }
        
        jugador.comprar(articulo);
        Toast.gold("🛍️ Compra Realizada", 
            "Obtuviste: " + articulo.getNombre());
        actualizarInventario();
        
    } catch (TransactionException ex) {
        Toast.error("Error de Transacción", ex.getMessage());
    }
});
```

### Guardado de Datos

```java
guardarButton.setOnAction(e -> {
    try {
        usuario.setNombre(nombreField.getText());
        usuario.setEmail(emailField.getText());
        
        usuarioDAO.actualizar(usuario);
        
        Toast.success("Guardado", "Cambios guardados exitosamente");
        
    } catch (ValidationException ve) {
        Toast.formError("Validación Fallida", ve.getMessage());
    } catch (SQLException se) {
        Toast.exception("Error de Base de Datos", se);
    }
});
```

### Carga Asincrónica

```java
cargarDatosButton.setOnAction(e -> {
    Toast.info("Cargando", "Por favor espera...");
    
    new Thread(() -> {
        try {
            List<Dato> datos = APIClient.obtenerDatos();
            
            Platform.runLater(() -> {
                mostrarDatos(datos);
                Toast.success("Datos Listos", 
                    "Se cargaron " + datos.size() + " elementos");
            });
            
        } catch (IOException io) {
            Platform.runLater(() -> 
                Toast.exception("Error de Red", io)
            );
        }
    }).start();
});
```

---

## 🎨 Personalización

### Toast de Tamaño Grande

```java
VBox largeToast = new VBox();
largeToast.getStyleClass().addAll("toast-container", "toast-success", "toast-large");
// Agregar labels...
```

### Toast de Tamaño Pequeño

```java
VBox smallToast = new VBox();
smallToast.getStyleClass().addAll("toast-container", "toast-info", "toast-small");
```

### Toast Épico con Pulso

```java
Toast.epic("⭐ VICTORIA", "¡Ganaste la batalla!");
// Automáticamente tendrá animación de pulso y duración de 6 segundos
```

---

## 📱 Posicionamiento

Por defecto, los Toasts aparecen en la esquina superior derecha (20px de offset).

Para cambiar la posición, modifica en Toast.java:

```java
private static final double SCREEN_OFFSET_X = 20;   // Derecha
private static final double SCREEN_OFFSET_Y = 20;   // Arriba
```

O en shared.css ajusta el padding del contenedor.

---

## ⏱️ Duración

- **Toasts normales**: 4 segundos
- **Toasts épicos**: 6 segundos
- **Personalizados**: Modifica `TOAST_DURATION` en Toast.java

---

## 🎯 Recomendaciones

✅ **Sí usa Toast para:**
- Confirmaciones de acciones
- Errores no críticos
- Mensajes informativos cortos
- Validaciones de formulario
- Excepciones en try/catch

❌ **No uses Toast para:**
- Errores críticos (usa Dialog)
- Mensajes muy largos (usa Alerts)
- Solicitudes de confirmación (usa ConfirmDialog)

---

## 🔧 Integración en Controllers Existentes

```java
import com.ctrlaltquest.ui.utils.Toast;

public class MisionesController implements Initializable {
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Ya debe estar inicializado en la aplicación principal
    }
    
    public void completarMision() {
        try {
            mision.marcarCompleta();
            Toast.success("Misión Completada", 
                "¡Ganaste " + mision.getRecompensa() + " puntos!");
        } catch (Exception e) {
            Toast.exception("Error al Completar", e);
        }
    }
    
    public void rechazarMision() {
        Toast.warning("Misión Rechazada", 
            "Podrás aceptarla más tarde");
    }
}
```

---

## 🚨 Solución de Problemas

### "Toast no inicializado"
**Solución**: Asegúrate de llamar `Toast.initialize(toastContainer)` en tu controlador principal

### Los Toasts no se ven
**Solución**: Verifica que el contenedor esté agregado correctamente al Scene Graph y que sea transparent

### CSS no se carga
**Solución**: Asegúrate de importar `/styles/toast.css` en el Scene

```java
scene.getStylesheets().add(getClass().getResource("/styles/toast.css").toExternalForm());
```

---

## 📊 Ejemplo Completo de Integración

```java
public class MainApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Cargar FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
        BorderPane root = loader.load();
        
        // Crear contenedor de Toasts
        VBox toastContainer = new VBox();
        toastContainer.setStyle("-fx-background-color: transparent;");
        toastContainer.setPrefSize(400, 600);
        
        // Agregarlo al root
        root.setTop(toastContainer);
        
        // Inicializar Toast
        Toast.initialize(toastContainer);
        
        // Crear scene
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            getClass().getResource("/styles/shared.css").toExternalForm()
        );
        scene.getStylesheets().add(
            getClass().getResource("/styles/toast.css").toExternalForm()
        );
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
```

¡Listo! Ahora tienes un sistema completo de Toast Notifications 🚀
