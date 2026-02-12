# 📖 Guía de Uso - Sistema de Tienda

## 1. Comprar Items con Coins (Dinero del Juego)

```java
// En StoreViewController o donde se maneje la compra
int userId = sessionUser.getId();  // ID del usuario logueado
int offerId = 5;                   // ID de la oferta seleccionada

// Validar y comprar
boolean exito = StoreDAO.comprarConCoins(userId, offerId);

if (exito) {
    System.out.println("✅ Compra exitosa");
    // Actualizar UI: mostrar mensaje, reducir coins, actualizar inventario
} else {
    System.out.println("❌ No tienes suficientes coins");
}
```

## 2. Procesar Compra con Dinero Real (Stripe)

```java
// En StoreViewController - cuando usuario hace click en "Comprar con Dinero Real"
int userId = sessionUser.getId();
PaymentProduct product = productosDisponibles.get(0);  // El producto seleccionado
Integer deviceId = obtenerIdDispositivo();  // Opcional

// Crear y mostrar formulario de pago
PaymentFormController controller = new PaymentFormController();
controller.inicializar(userId, product, deviceId, () -> {
    System.out.println("Pago completado, recargar UI");
    recargarInventario();
    mostrarNotificacion("¡Compra exitosa!");
});

// El controlador maneja todo el flujo:
// 1. PaymentDAO.crearOrdenPago()
// 2. Validación de tarjeta
// 3. PaymentDAO.registrarTransaccion()
// 4. PaymentDAO.procesarCompraExitosa()
```

## 3. Obtener Inventario del Usuario

```java
int userId = sessionUser.getId();

// Obtener todos los items
List<InventoryItem> inventario = InventoryDAO.obtenerInventario(userId);

// Mostrar en lista
for (InventoryItem item : inventario) {
    System.out.println(item.getItem().getName() + 
                       " x" + item.getQuantity());
    
    if (item.isBoost()) {
        // Mostrar botón "Activar"
    }
}
```

## 4. Activar Boost de XP

```java
int userId = sessionUser.getId();
int itemId = 15;  // Boost de XP que está en inventario

// Verificar que tiene el item
if (InventoryDAO.tieneItem(userId, itemId)) {
    // Activar boost (1 hora = 3600 segundos)
    BoostService.activarBoost(userId, 3600, "Poción de XP");
    
    // Consumir el item (restar 1 de cantidad)
    InventoryDAO.consumirItem(userId, itemId);
    
    System.out.println("⚡ Boost activado por 1 hora");
}
```

## 5. Verificar Boost Activo

```java
int userId = sessionUser.getId();

if (BoostService.tieneBoostActivo(userId)) {
    // Mostrar indicador visual
    Map<String, Object> info = BoostService.obtenerInfoBoost(userId);
    String itemName = (String) info.get("itemName");
    String tiempoRestante = (String) info.get("tiempoRestanteFormato");
    
    System.out.println("🟢 " + itemName + " activo - " + tiempoRestante);
} else {
    System.out.println("⚫ Sin boost activo");
}
```

## 6. Otorgar XP Multiplicado por Boost

```java
// En UserDAO.otorgarRecompensas() cuando se completa una misión
int userId = sessionUser.getId();
int xpBase = 100;
int coinsBase = 50;

// Obtener multiplicador del boost
int multiplicador = BoostService.obtenerMultiplicadorXP(userId);

// Calcular XP final
int xpFinal = xpBase * multiplicador;

// Otorgar recompensas
UserDAO.otorgarRecompensas(userId, xpFinal, coinsBase);

System.out.println("🎉 +"+xpFinal+" XP (x"+multiplicador+" bonus)");
```

## 7. Equipar Item Cosmético

```java
int userId = sessionUser.getId();
int itemId = 20;  // Casco legendario

// Equipar (desequipa otros del mismo tipo automáticamente)
InventoryDAO.equiparItem(userId, itemId);

System.out.println("✨ Casco equipado");
```

## 8. Crear Producto Premium (Admin)

```java
// Crear un pack de coins con dinero real
Map<String, Object> recompensas = new HashMap<>();
recompensas.put("coins", 5000);  // 5000 coins

int productId = PaymentProductDAO.crearProducto(
    "COINS_5000",              // SKU único
    "Pack 5000 Coins",         // Nombre
    "Obten 5000 coins para tu tienda",  // Descripción
    2999,                      // Precio: $29.99
    "USD",                     // Moneda
    "assets/images/coins.png", // Imagen
    "coin_pack",               // Tipo
    recompensas
);

System.out.println("✅ Producto creado: ID " + productId);
```

## 9. Monitorear Estado de Pago

```java
String orderUUID = "550e8400-e29b-41d4-a716-446655440000";

// Verificar estado
String status = PaymentDAO.obtenerEstadoOrden(orderUUID);

switch(status) {
    case "created":
        System.out.println("Orden creada, esperando pago...");
        break;
    case "pending":
        System.out.println("Procesando pago...");
        break;
    case "success":
        System.out.println("✅ Pago completado");
        break;
    case "failed":
        System.out.println("❌ Pago rechazado");
        break;
}
```

## 10. Obtener Productos Premium Disponibles

```java
// Obtener todos los productos activos
List<PaymentProduct> productos = PaymentProductDAO.obtenerProductos();

// Filtrar por tipo
List<PaymentProduct> coinPacks = PaymentProductDAO.obtenerProductosPorTipo("coin_pack");

// Mostrar en tienda
for (PaymentProduct p : coinPacks) {
    System.out.println(p.getName() + 
                       " - " + p.getPriceFormatted() +
                       " (+"+p.getCoinsReward()+" coins)");
}
```

## 11. Integración en StoreViewController

```java
public class StoreViewController {
    
    private int userId;
    
    @FXML
    public void initialize() {
        userId = obtenerUserIdDeSesion();
        cargarOfertas();
        cargarBoosts();
    }
    
    private void cargarOfertas() {
        // Cargar ofertas de moneda del juego
        List<StoreOffer> ofertas = StoreDAO.obtenerOfertas();
        
        for (StoreOffer oferta : ofertas) {
            agregarOfertaAlUI(oferta);
        }
    }
    
    @FXML
    private void comprarConCoins(StoreOffer oferta) {
        if (StoreDAO.comprarConCoins(userId, oferta.getId())) {
            mostrarMensaje("¡Compra exitosa!");
            recargarInventario();
        } else {
            mostrarError("No tienes suficientes coins");
        }
    }
    
    @FXML
    private void comprarConDinero(PaymentProduct producto) {
        PaymentFormController controller = new PaymentFormController();
        controller.inicializar(userId, producto, null, () -> {
            recargarInventario();
            mostrarMensaje("¡Pago exitoso!");
        });
        
        // Mostrar formulario en nueva ventana
        abrirVentanaModal(controller);
    }
    
    private void cargarBoosts() {
        List<InventoryItem> inventory = InventoryDAO.obtenerInventario(userId);
        
        for (InventoryItem item : inventory) {
            if (item.isBoost()) {
                if (BoostService.tieneBoostActivo(userId)) {
                    // Mostrar contador de tiempo
                    Map<String, Object> info = BoostService.obtenerInfoBoost(userId);
                    mostrarTiempoRestante((String) info.get("tiempoRestanteFormato"));
                } else {
                    // Mostrar botón "Activar"
                    agregarBotonActivar(item);
                }
            }
        }
    }
    
    @FXML
    private void activarBoost(InventoryItem boost) {
        BoostService.activarBoost(userId, boost.getItem().getBoostDurationSeconds(), 
                                   boost.getItem().getName());
        InventoryDAO.consumirItem(userId, boost.getItem().getId());
        cargarBoosts();  // Recargar UI
    }
}
```

## 12. Ejemplo Completo de Flujo de Usuario

```java
// Simulación de sesión de usuario comprando en la tienda

public class TiendaFlujoPrueba {
    
    public static void main(String[] args) {
        int userId = 3;
        
        // 1. Obtener dinero actual
        int coinsActuales = 1250;
        System.out.println("💰 Coins disponibles: " + coinsActuales);
        
        // 2. Ver inventario
        List<InventoryItem> inventario = InventoryDAO.obtenerInventario(userId);
        System.out.println("📦 Items en inventario: " + inventario.size());
        
        // 3. Comprar un item con coins
        System.out.println("\n--- COMPRA CON COINS ---");
        if (StoreDAO.comprarConCoins(userId, 1)) {
            System.out.println("✅ Oferta comprada");
            coinsActuales -= 500;
            System.out.println("💰 Coins ahora: " + coinsActuales);
        }
        
        // 4. Obtener boost de inventario
        System.out.println("\n--- ACTIVAR BOOST ---");
        List<InventoryItem> boosts = inventario.stream()
            .filter(InventoryItem::isBoost)
            .collect(Collectors.toList());
        
        if (!boosts.isEmpty()) {
            InventoryItem boost = boosts.get(0);
            BoostService.activarBoost(userId, 3600, boost.getItem().getName());
            System.out.println("⚡ Boost activado por 1 hora");
            InventoryDAO.consumirItem(userId, boost.getItem().getId());
        }
        
        // 5. Completar misión con bonus de XP
        System.out.println("\n--- COMPLETAR MISIÓN CON BOOST ---");
        int xpBase = 100;
        int multiplicador = BoostService.obtenerMultiplicadorXP(userId);
        int xpFinal = xpBase * multiplicador;
        System.out.println("🎉 +"+xpFinal+" XP (x"+multiplicador+")");
        
        // 6. Compra con dinero real
        System.out.println("\n--- COMPRA CON DINERO REAL ---");
        PaymentProduct producto = PaymentProductDAO.obtenerProducto(5);
        PaymentService.procesarCompraPremium(userId, producto, null);
        System.out.println("💳 Orden de pago creada");
        
        System.out.println("\n✅ Flujo completado exitosamente");
    }
}
```

---

**Tips Útiles**:
- Siempre validar que el usuario tiene suficientes coins antes de comprar
- Los boosts usan Timer interno, revisa si necesitas persistencia
- Las transacciones se registran automáticamente en las tablas de auditoría
- Los IDs de items y ofertas vienen de la base de datos
- Stripe es simulado, reemplazar con API real en producción

