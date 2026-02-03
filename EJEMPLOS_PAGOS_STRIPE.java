// EJEMPLOS DE USO - SISTEMA DE PAGOS CON STRIPE
// Copiar y adaptar según necesites

// ============================================================
// EJEMPLO 1: Procesar un pago desde el controlador
// ============================================================

public class StoreViewController {
    
    @FXML
    private void handleComprarConDineroReal(ActionEvent event) {
        // Obtener datos del usuario
        int userId = SessionManager.getInstance().getUserId();
        Integer deviceId = SessionManager.getInstance().getDeviceId();
        
        // Obtener producto seleccionado
        PaymentProduct product = obtenerProductoSeleccionado();
        
        // Mostrar formulario de pago
        mostrarFormularioPago(userId, product, deviceId);
    }
    
    private void mostrarFormularioPago(int userId, PaymentProduct product, Integer deviceId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment_form.fxml"));
            Parent root = loader.load();
            
            // Obtener controlador
            PaymentFormController controller = loader.getController();
            
            // Inicializar con datos del producto
            controller.inicializar(userId, product, deviceId, () -> {
                System.out.println("✅ Pago exitoso! Refrescando inventario...");
                refrescarInventario();
            });
            
            // Mostrar ventana modal
            Stage stage = new Stage();
            stage.setTitle("Pago Seguro - Stripe");
            stage.setScene(new Scene(root, 600, 700));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("❌ Error abriendo formulario de pago: " + e.getMessage());
        }
    }
}

// ============================================================
// EJEMPLO 2: Usar StripePaymentService directamente
// ============================================================

public class PagoManual {
    
    public static void procesarPagoManualmente() {
        // 1. Crear Payment Intent
        String clientSecret = StripePaymentService.crearPaymentIntent(
            2999,                   // $29.99 en centavos
            "usd",                  // Moneda
            "usuario@example.com",  // Email
            "Juan Pérez",           // Nombre
            "order-uuid-123"        // ID de orden
        );
        
        if (clientSecret == null) {
            System.out.println("❌ Error creando Payment Intent");
            return;
        }
        
        // 2. Crear token de la tarjeta
        String cardToken = StripePaymentService.crearTokenTarjeta(
            "4242424242424242",     // Número de tarjeta
            12,                     // Mes
            25,                     // Año (20xx)
            "123"                   // CVV
        );
        
        if (cardToken == null) {
            System.out.println("❌ Error tokenizando tarjeta");
            return;
        }
        
        // 3. Procesar el pago
        String transactionId = StripePaymentService.procesarPagoConToken(
            clientSecret,
            cardToken
        );
        
        if (transactionId != null) {
            System.out.println("✅ Pago exitoso! ID: " + transactionId);
            
            // 4. Guardar en BD
            PaymentDAO.registrarTransaccion(
                "order-uuid-123",
                "stripe",
                transactionId,
                2999,
                "usd",
                "success",
                "{\"message\":\"pago_exitoso\"}"
            );
            
            // 5. Procesar compra
            PaymentDAO.procesarCompraExitosa("order-uuid-123");
        }
    }
}

// ============================================================
// EJEMPLO 3: Validar una tarjeta ANTES de procesar
// ============================================================

public class ValidarTarjeta {
    
    public static void validarYProcesar() {
        String numeroTarjeta = "4242424242424242";
        int mesVencimiento = 12;
        int anioVencimiento = 25;
        String cvv = "123";
        
        // 1. Validar número (Luhn algorithm)
        if (!StripePaymentService.validarNumeroTarjeta(numeroTarjeta)) {
            System.out.println("❌ Número de tarjeta inválido");
            return;
        }
        
        System.out.println("✅ Número de tarjeta válido");
        
        // 2. Validar vencimiento
        YearMonth ahora = YearMonth.now();
        YearMonth vencimiento = YearMonth.of(2000 + anioVencimiento, mesVencimiento);
        
        if (vencimiento.isBefore(ahora)) {
            System.out.println("❌ Tarjeta vencida");
            return;
        }
        
        System.out.println("✅ Tarjeta no vencida");
        
        // 3. Validar CVV
        if (!cvv.matches("\\d{3,4}")) {
            System.out.println("❌ CVV inválido");
            return;
        }
        
        System.out.println("✅ CVV válido");
        System.out.println("✅ Tarjeta lista para procesar");
    }
}

// ============================================================
// EJEMPLO 4: Verificar estado de un pago
// ============================================================

public class VerificarEstado {
    
    public static void verificarPago(String transactionId) {
        String estado = StripePaymentService.verificarEstadoPago(transactionId);
        
        switch (estado) {
            case "succeeded":
                System.out.println("✅ Pago exitoso");
                // Procesar compra
                break;
            case "failed":
                System.out.println("❌ Pago fallido");
                // Mostrar error al usuario
                break;
            case "pending":
                System.out.println("⏳ Pago pendiente");
                // Reintentar después
                break;
            default:
                System.out.println("❓ Estado desconocido");
        }
    }
}

// ============================================================
// EJEMPLO 5: Procesar reembolso
// ============================================================

public class ProcesarReembolso {
    
    public static void reembolsarAlUsuario(String transactionId, String razon) {
        System.out.println("🔄 Procesando reembolso...");
        
        boolean exitoso = StripePaymentService.reembolsarPago(
            transactionId,
            razon  // "requested_by_customer", "fraud", etc
        );
        
        if (exitoso) {
            System.out.println("✅ Reembolso procesado exitosamente");
            System.out.println("   El dinero será devuelto al cliente en 5-10 días hábiles");
        } else {
            System.out.println("❌ Error procesando reembolso");
        }
    }
}

// ============================================================
// EJEMPLO 6: Obtener ingresos totales de la BD
// ============================================================

public class GenerarReporteIngresos {
    
    public static void reporteIngresos() {
        String sql = """
            SELECT 
                COUNT(*) as total_transacciones,
                SUM(amount_cents) / 100.0 as total_usd,
                AVG(amount_cents) / 100.0 as promedio_usd,
                MIN(created_at) as fecha_primera,
                MAX(created_at) as fecha_ultima
            FROM payment_transactions 
            WHERE status = 'success'
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                int total = rs.getInt("total_transacciones");
                double usd = rs.getDouble("total_usd");
                double promedio = rs.getDouble("promedio_usd");
                
                System.out.println("\n📊 REPORTE DE INGRESOS");
                System.out.println("═".repeat(40));
                System.out.println("Total transacciones: " + total);
                System.out.println("Ingresos totales: $" + String.format("%.2f", usd) + " USD");
                System.out.println("Promedio por transacción: $" + String.format("%.2f", promedio) + " USD");
                System.out.println("═".repeat(40) + "\n");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
}

// ============================================================
// EJEMPLO 7: Obtener saldo actual de Stripe
// ============================================================

public class VerSaldoStripe {
    
    public static void mostrarSaldo() {
        System.out.println("\n💰 SALDO ACTUAL EN STRIPE");
        System.out.println("═".repeat(40));
        StripePaymentService.mostrarSaldoCuenta();
        System.out.println("═".repeat(40) + "\n");
    }
}

// ============================================================
// EJEMPLO 8: Procesamiento asincrónico de pagos
// ============================================================

public class PagoAsincrono {
    
    public static void procesarPagoAsync(String cardNumber, String email, int monto) {
        Task<String> pagoTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Validando tarjeta...");
                updateProgress(10, 100);
                
                // Validar
                if (!StripePaymentService.validarNumeroTarjeta(cardNumber)) {
                    return "ERROR";
                }
                
                updateMessage("Creando Payment Intent...");
                updateProgress(30, 100);
                
                // Crear intent
                String clientSecret = StripePaymentService.crearPaymentIntent(
                    monto, "usd", email, "Cliente", "order-123"
                );
                
                if (clientSecret == null) return "ERROR";
                
                updateMessage("Tokenizando tarjeta...");
                updateProgress(50, 100);
                
                // Tokenizar
                String token = StripePaymentService.crearTokenTarjeta(
                    cardNumber, 12, 25, "123"
                );
                
                if (token == null) return "ERROR";
                
                updateMessage("Procesando pago...");
                updateProgress(80, 100);
                
                // Procesar
                String transactionId = StripePaymentService.procesarPagoConToken(
                    clientSecret, token
                );
                
                updateProgress(100, 100);
                return transactionId != null ? "SUCCESS" : "ERROR";
            }
        };
        
        pagoTask.setOnSucceeded(e -> {
            if ("SUCCESS".equals(pagoTask.getValue())) {
                System.out.println("✅ Pago completado");
            } else {
                System.out.println("❌ Pago fallido");
            }
        });
        
        new Thread(pagoTask).start();
    }
}

// ============================================================
// EJEMPLO 9: Query SQL para análisis de pagos
// ============================================================

/*
SQL ÚTILES PARA MONITOREO:

1. Ver todas las transacciones:
   SELECT * FROM payment_transactions 
   ORDER BY created_at DESC;

2. Ingresos totales:
   SELECT SUM(amount_cents) / 100.0 as total_usd 
   FROM payment_transactions WHERE status = 'success';

3. Transacciones por usuario:
   SELECT u.username, COUNT(*) as transacciones, 
          SUM(pt.amount_cents) / 100.0 as total_usd
   FROM payment_transactions pt
   JOIN payment_orders po ON pt.order_id = po.id
   JOIN users u ON po.user_id = u.id
   WHERE pt.status = 'success'
   GROUP BY u.id, u.username
   ORDER BY total_usd DESC;

4. Producto más vendido:
   SELECT pp.name, COUNT(*) as ventas, 
          SUM(pt.amount_cents) / 100.0 as ingresos
   FROM payment_transactions pt
   JOIN payment_orders po ON pt.order_id = po.id
   JOIN payment_products pp ON po.product_id = pp.id
   WHERE pt.status = 'success'
   GROUP BY pp.id, pp.name
   ORDER BY ingresos DESC;

5. Ingresos por día:
   SELECT DATE(created_at) as fecha, 
          COUNT(*) as transacciones,
          SUM(amount_cents) / 100.0 as ingresos_usd
   FROM payment_transactions
   WHERE status = 'success'
   GROUP BY DATE(created_at)
   ORDER BY fecha DESC;

6. Transacciones fallidas (para análisis):
   SELECT * FROM payment_transactions 
   WHERE status = 'failed' 
   ORDER BY created_at DESC;
*/

// ============================================================
// EJEMPLO 10: Integración completa en un controlador
// ============================================================

public class ControladorPagoCompleto {
    
    private int userId;
    private PaymentProduct producto;
    
    public void comprarProducto(PaymentProduct producto) {
        this.producto = producto;
        this.userId = SessionManager.getInstance().getUserId();
        
        // 1. Crear orden
        String orderUUID = PaymentDAO.crearOrdenPago(userId, producto.getId(), null);
        if (orderUUID == null) {
            mostrarError("No se pudo crear la orden");
            return;
        }
        
        // 2. En background, procesar pago
        Task<Boolean> pagoTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                updateProgress(0, 100);
                updateMessage("Iniciando pago...");
                
                // Simular obtención de datos de tarjeta
                String cardNumber = "4242424242424242";
                String email = "usuario@example.com";
                int monto = producto.getPriceCents();
                
                // Validar
                if (!StripePaymentService.validarNumeroTarjeta(cardNumber)) {
                    return false;
                }
                
                updateProgress(25, 100);
                updateMessage("Creando intención de pago...");
                
                // Crear intent
                String clientSecret = StripePaymentService.crearPaymentIntent(
                    monto, "usd", email, "Usuario", orderUUID
                );
                if (clientSecret == null) return false;
                
                updateProgress(50, 100);
                updateMessage("Procesando pago con Stripe...");
                
                // Tokenizar y procesar
                String token = StripePaymentService.crearTokenTarjeta(
                    cardNumber, 12, 25, "123"
                );
                if (token == null) return false;
                
                String transactionId = StripePaymentService.procesarPagoConToken(
                    clientSecret, token
                );
                if (transactionId == null) return false;
                
                updateProgress(75, 100);
                updateMessage("Registrando transacción...");
                
                // Registrar
                boolean registrado = PaymentDAO.registrarTransaccion(
                    orderUUID, "stripe", transactionId, monto, "usd", "success", null
                );
                if (!registrado) return false;
                
                updateProgress(90, 100);
                updateMessage("Entregando producto...");
                
                // Procesar compra
                return PaymentDAO.procesarCompraExitosa(orderUUID);
            }
        };
        
        pagoTask.setOnSucceeded(e -> {
            if (pagoTask.getValue()) {
                mostrarExito("¡Pago exitoso! Recibiste: " + producto.getName());
                refrescarUI();
            } else {
                mostrarError("El pago falló. Intenta de nuevo.");
            }
        });
        
        pagoTask.setOnFailed(e -> {
            mostrarError("Error: " + pagoTask.getException().getMessage());
        });
        
        new Thread(pagoTask).start();
    }
    
    private void mostrarError(String msg) { /* ... */ }
    private void mostrarExito(String msg) { /* ... */ }
    private void refrescarUI() { /* ... */ }
}

// ============================================================
// FIN DE EJEMPLOS
// ============================================================

/*
NOTAS IMPORTANTES:

1. SIEMPRE valida datos en el servidor (backend)
2. NUNCA guardes números de tarjeta en la BD
3. SIEMPRE usa HTTPS en producción
4. SIEMPRE usa variables de entorno para claves API
5. Prueba primero con sk_test_... antes de sk_live_...
6. Maneja excepciones en todo procesamiento
7. Log todos los errores de pago para debugging
8. Monitorea Stripe Dashboard regularmente
9. Implementa reintentos para fallos temporales
10. Comunica claramente al usuario sobre el estado del pago
*/
