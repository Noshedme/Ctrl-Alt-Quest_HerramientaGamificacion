# 🛒 SISTEMA DE PAGOS REAL CON STRIPE

## ✅ COMPLETADO: Integración de Pagos REALES

Tu sistema de tienda ahora tiene **integración REAL con Stripe** para procesar pagos de tarjeta de crédito/débito con transferencia directa a tu cuenta bancaria.

---

## 🚀 REQUISITOS PREVIOS

### 1. **Crear Cuenta en Stripe**
- Ve a: https://dashboard.stripe.com/register
- Registra tu cuenta de negocio
- Completa la verificación de identidad

### 2. **Obtener Claves API**
- Ve a: https://dashboard.stripe.com/apikeys
- Copia tu **Secret Key** (comienza con `sk_live_` o `sk_test_`)
  - `sk_test_...` = Modo Testing (sin dinero real)
  - `sk_live_...` = Modo Producción (dinero REAL)

### 3. **Conectar Cuenta Bancaria**
- Ve a: https://dashboard.stripe.com/settings/payouts
- Agrega tu información bancaria
- Stripe transferirá automáticamente los fondos a tu cuenta

---

## 🔧 CONFIGURACIÓN EN LA APLICACIÓN

### **Opción A: Variables de Entorno (RECOMENDADO)**

#### En Windows:
```bash
# Abrir Variables de Entorno
setx STRIPE_SECRET_KEY "sk_test_tu_clave_aqui"

# Reiniciar la aplicación
```

#### En Linux/Mac:
```bash
export STRIPE_SECRET_KEY="sk_test_tu_clave_aqui"
# O agregar a ~/.bashrc o ~/.bash_profile
```

### **Opción B: Archivo de Configuración**
Si prefieres no usar variables de entorno, puedes crear `.env`:

```bash
STRIPE_SECRET_KEY=sk_test_tu_clave_aqui
```

---

## 💳 CÓMO USAR PAGOS REALES

### 1. **Usuario Abre la Tienda**
```java
// El usuario ve productos con precio en USD
Producto: "Pack de 1000 Coins"
Precio: $29.99 USD
```

### 2. **Usuario Selecciona "Comprar con Dinero Real"**
```
┌─────────────────────────────────┐
│  Formulario de Pago Seguro      │
├─────────────────────────────────┤
│  Nombre: ____________________   │
│  Email:  ____________________   │
│  Tarjeta: _____________________ │
│  Vence: MM/YY | CVV: ___        │
│                                 │
│  [💰 Pagar Ahora] [Cancelar]   │
└─────────────────────────────────┘
```

### 3. **Procesamiento con Stripe**
El flujo interno:

```
Usuario → Valida Datos
        ↓
Crea Orden en BD (payment_orders)
        ↓
Tokeniza Tarjeta con Stripe
        ↓
Crea Payment Intent en Stripe
        ↓
Procesa Pago REAL ← Dinero va a tu cuenta
        ↓
Registra Transacción en BD
        ↓
Otorga Coins/Items al Inventario
        ↓
✅ Pago Exitoso - Usuario recibe producto
```

### 4. **Confirmación**
```
✅ ¡Pago Exitoso!

• Producto: Pack de 1000 Coins
• Monto: $29.99 USD
• Estado: Pagado
• El dinero se transfirió a tu cuenta bancaria
• El producto fue agregado a tu inventario
```

---

## 📊 MONITOREAR TRANSACCIONES

### **En Stripe Dashboard**
1. Ve a: https://dashboard.stripe.com/payments
2. Verás todas las transacciones en TIEMPO REAL
3. Puedes ver:
   - Monto pagado
   - Email del cliente
   - Método de pago
   - Fecha/Hora
   - Estado

### **En tu Base de Datos**
```sql
-- Ver todas las transacciones
SELECT * FROM payment_transactions WHERE provider = 'stripe';

-- Ver órdenes completadas
SELECT * FROM payment_orders WHERE status = 'success';

-- Ver ingresos totales
SELECT SUM(amount_cents) / 100.0 as total_usd FROM payment_transactions WHERE status = 'success';
```

---

## 🧪 MODO TESTING vs PRODUCCIÓN

### **Modo Testing** (Recomendado para pruebas)
- Secret Key: `sk_test_...`
- NO usa dinero real
- Usa tarjetas de prueba de Stripe:

```
Tarjetas de Prueba:
┌──────────────────┬──────┬──────┐
│ Número           │ Vence│ CVC  │
├──────────────────┼──────┼──────┤
│ 4242 4242 4242 42│ 12/25│ 123  │ ✅ Pago exitoso
│ 4000 0000 0000 002│12/25│ 123  │ ❌ Tarjeta declinada
│ 4000 0025 0000 003│12/25│ 123  │ ⚠️  3D Secure requerido
└──────────────────┴──────┴──────┘
```

### **Modo Producción** (Dinero REAL)
- Secret Key: `sk_live_...`
- CADA transacción es dinero real
- Se transferirá a tu cuenta bancaria
- No uses tarjetas de prueba

---

## 🔒 SEGURIDAD

### **Lo que está asegurado:**
✅ Números de tarjeta NUNCA se guardan en tu base de datos
✅ Tokenización segura con Stripe
✅ Cumple PCI DSS (standard de industria)
✅ Encriptación TLS/SSL
✅ 3D Secure para transacciones de alto riesgo

### **Buenas Prácticas:**
1. **Nunca** hardcodees la clave API en el código
2. **Siempre** usa variables de entorno
3. **Usa** `sk_live_...` solo cuando estés listo para producción
4. **Monitorea** el dashboard de Stripe regularmente
5. **Ten** un plan de reembolsos establecido

---

## 💰 COMISIONES DE STRIPE

- **Pago con Tarjeta**: 2.9% + $0.30 por transacción
- **Transferencias Bancarias**: Gratis (1-2 días hábiles)

**Ejemplo:**
```
Cliente paga: $29.99
Comisión Stripe: $0.87 + $0.30 = $1.17
Tú recibes: $28.82
```

---

## 📝 FUNCIONES DISPONIBLES

### **StripePaymentService.java**

```java
// Crear Payment Intent
String clientSecret = StripePaymentService.crearPaymentIntent(
    2999,           // Monto en centavos ($29.99)
    "usd",          // Moneda
    "user@email.com", // Email del cliente
    "Juan Pérez",   // Nombre del cliente
    orderUUID       // ID de orden
);

// Procesar pago
String transactionId = StripePaymentService.procesarPagoConToken(
    clientSecret,   // Del cliente
    cardToken       // Token tokenizado
);

// Verificar estado
String status = StripePaymentService.verificarEstadoPago(transactionId);
// Retorna: "succeeded", "failed", "pending"

// Reembolsar
boolean exitoso = StripePaymentService.reembolsarPago(
    transactionId,
    "requested_by_customer"
);

// Ver saldo
StripePaymentService.mostrarSaldoCuenta();
```

---

## 🐛 TROUBLESHOOTING

### **Error: "STRIPE_SECRET_KEY not configured"**
→ Agrega la variable de entorno y reinicia la aplicación

### **Error: "Invalid token"**
→ Verifica que:
  - El número de tarjeta es válido
  - El vencimiento no está pasado
  - El CVV es correcto (3 o 4 dígitos)

### **Error: "Declined"**
→ Posibles razones:
  - Fondos insuficientes
  - Tarjeta reportada como robada
  - Restricciones de la tarjeta
  - En modo testing: usa `4242 4242 4242 4242`

### **Transacción no aparece en Stripe Dashboard**
→ Verifica:
  - Estás usando la clave correcta (`sk_live_` vs `sk_test_`)
  - La transacción fue exitosa (status = "succeeded")
  - Espera a que se sincronice (usualmente < 1 minuto)

---

## 📚 ESTRUCTURA DE DATOS

### **payment_orders** (Tabla Principal)
```sql
CREATE TABLE payment_orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER,
    product_id INTEGER,
    order_uuid UUID UNIQUE,
    status VARCHAR(30),      -- created, success, failed
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### **payment_transactions** (Registro de Transacciones)
```sql
CREATE TABLE payment_transactions (
    id SERIAL PRIMARY KEY,
    order_id INTEGER,
    provider VARCHAR(40),        -- "stripe"
    provider_tx_id VARCHAR(120), -- ID de Stripe
    amount_cents INTEGER,        -- En centavos
    currency VARCHAR(10),        -- "usd"
    status VARCHAR(30),          -- "success", "failed"
    raw_payload JSONB,           -- Respuesta de Stripe
    created_at TIMESTAMP
);
```

### **payment_product_rewards** (Qué recibe el usuario)
```sql
CREATE TABLE payment_product_rewards (
    product_id INTEGER,
    item_id INTEGER,
    coins_amount INTEGER,  -- Coins que recibe
    quantity INTEGER       -- Cantidad de items
);
```

---

## 🎯 FLUJO COMPLETO DE EJEMPLO

### **Scenario: Usuario compra 1000 Coins por $29.99**

```
1. USUARIO ABRE TIENDA
   ↓
2. VE OFERTA: "Pack 1000 Coins - $29.99"
   ↓
3. PRESIONA "COMPRAR CON DINERO REAL"
   ↓
4. SE ABRE FORMULARIO DE PAGO:
   - Nombre: Juan Pérez
   - Email: juan@example.com
   - Tarjeta: 4242 4242 4242 4242
   - Vence: 12/25
   - CVV: 123
   ↓
5. PRESIONA "PAGAR AHORA"
   ↓
6. BACKEND:
   a) Crea orden → order_uuid = "abc-123..."
   b) Tokeniza tarjeta con Stripe
   c) Crea Payment Intent de $29.99
   d) Procesa pago → stripe_charge_id = "ch_1Abc..."
   e) Guarda en payment_transactions (status: success)
   f) Agrega 1000 coins a user_inventory
   g) Registra transacción en coin_transactions
   ↓
7. STRIPE:
   - Recibe el pago
   - Deduce comisión (2.9% + $0.30 = $1.17)
   - Usuario recibe: $28.82 en su cuenta bancaria
   ↓
8. USUARIO VE:
   ✅ ¡Pago Exitoso!
   • Recibiste 1000 Coins
   • Se descargaron $29.99 de tu tarjeta
   ↓
9. STREAMER (TÚ) VES:
   - Dashboard Stripe: +$28.82
   - BD: Nueva transacción registrada
   - Email de Stripe: Confirmación de pago
```

---

## 🚀 PRÓXIMOS PASOS

### Opcional: Stripe Webhooks
Para sincronización automática de pagos confirmados:

```java
@PostMapping("/webhook/stripe")
public ResponseEntity<String> handleStripeWebhook(@RequestBody String body) {
    // Verifica firma de Stripe
    // Procesa evento de pago
    // Actualiza base de datos automáticamente
    return ResponseEntity.ok("ok");
}
```

### Opcional: Reembolsos Automáticos
```java
// Si usuario abre ticket de soporte:
StripePaymentService.reembolsarPago(transactionId, "requested_by_customer");
```

---

## 📞 SOPORTE

- **Stripe Docs**: https://stripe.com/docs
- **Dashboard Stripe**: https://dashboard.stripe.com
- **Support Stripe**: https://support.stripe.com
- **Código Ejemplo**: Ver `PaymentFormController.java`

---

## ✨ RESUMEN

Tu sistema de tienda ahora:
- ✅ Procesa pagos REALES con Stripe
- ✅ Transfiere dinero a tu cuenta bancaria
- ✅ Registra todas las transacciones en BD
- ✅ Otorga coins/items automáticamente
- ✅ Cumple estándares de seguridad PCI
- ✅ Muestra transacciones en Stripe Dashboard
- ✅ Soporta reembolsos y disputas

**¡Estás listo para monetizar tu aplicación!** 💰
