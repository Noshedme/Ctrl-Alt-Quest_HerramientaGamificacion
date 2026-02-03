# 🗺️ Diagrama Visual del Sistema de Tienda

## Arquitectura General

```
┌─────────────────────────────────────────────────────────────┐
│                    USUARIO (StoreViewController)            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │ Comprar con COINS    │      │ Comprar con DINERO   │    │
│  │ (Moneda del Juego)   │      │ (Tarjeta de Crédito) │    │
│  └──────────┬───────────┘      └──────────┬───────────┘    │
│             │                             │                │
└─────────────┼─────────────────────────────┼────────────────┘
              │                             │
              ▼                             ▼
         ┌─────────────┐          ┌──────────────────┐
         │  StoreDAO   │          │ PaymentFormCtlr  │
         │             │          │                  │
         │ comprarCoin │          │ validarFormulario│
         │    Coins()  │          │  procesarPago()  │
         └──────┬──────┘          └────────┬─────────┘
                │                         │
                ▼                         ▼
        ┌──────────────┐        ┌──────────────────┐
        │  UserDAO     │        │  PaymentDAO      │
        │              │        │                  │
        │ actualizar   │        │ crearOrdenPago()│
        │   coins      │        │ registrarTxn()  │
        └──────┬───────┘        │procesarCompra() │
               │                └────────┬────────┘
               │                         │
               │                         ▼
               │                  ┌─────────────────┐
               │                  │ PaymentProduct  │
               │                  │     DAO         │
               │                  │  obtenerProduct │
               │                  └────────┬────────┘
               │                           │
               │                           ▼
               │                  ┌─────────────────┐
               │                  │  user_inventory │
               │                  │  (tabla BD)     │
               │                  │                 │
               │                  │  Agregar items  │
               └──────────┬────────┴─────────────────┘
                          │
                          ▼
                  ┌─────────────────┐
                  │  InventoryDAO   │
                  │                 │
                  │ obtenerInventario
                  │ equiparItem()   │
                  │ consumirItem()  │
                  └────────┬────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │ BoostService     │
                  │                  │
                  │ activarBoost()   │
                  │ multiplicador()  │
                  └──────────────────┘
```

---

## Flujo de Compra con Coins (Detallado)

```
USUARIO COMPRA CON COINS
│
├─ 1️⃣ SelectionEvent: Oferta Seleccionada
│  │
│  └─ offerId = 5, userId = 3
│
├─ 2️⃣ StoreDAO.comprarConCoins(3, 5)
│  │
│  ├─ 🔍 Verificar oferta existe
│  │  └─ SELECT * FROM store_offers WHERE id = 5
│  │
│  ├─ 💰 Obtener precio
│  │  └─ price = 500 coins
│  │
│  ├─ 👤 Verificar usuario tiene coins
│  │  └─ SELECT coins FROM users WHERE id = 3 FOR UPDATE
│  │  └─ coins = 1250 ✅ (>= 500)
│  │
│  ├─ ➖ Restar coins (ATÓMICO)
│  │  └─ UPDATE users SET coins = 750 WHERE id = 3
│  │
│  ├─ 📝 Registrar transacción
│  │  └─ INSERT INTO coin_transactions (user_id, amount, reason)
│  │     VALUES (3, -500, 'Compra Oferta Store')
│  │
│  ├─ 📦 Agregar items a inventario
│  │  └─ INSERT INTO user_inventory (user_id, item_id, quantity)
│  │     ON CONFLICT(user_id, item_id)
│  │     DO UPDATE SET quantity = quantity + 1
│  │
│  └─ ✅ COMMIT (Transacción)
│
└─ 3️⃣ UI Actualizada
   ├─ Mostrar "¡Compra Exitosa!"
   ├─ Reducir coins mostrados: 1250 → 750
   └─ Recargar inventario
```

---

## Flujo de Compra Premium con Stripe (Detallado)

```
USUARIO COMPRA CON DINERO REAL
│
├─ 1️⃣ ClickEvent: Producto Premium Seleccionado
│  │
│  └─ productId = 5 (Pack 5000 Coins), userId = 3
│
├─ 2️⃣ PaymentFormController.inicializar()
│  │
│  ├─ Crear orden pendiente
│  │  └─ orderUUID = PaymentDAO.crearOrdenPago(3, 5, null)
│  │     INSERT payment_orders (user_id, product_id, status='created')
│  │     RETURNING uuid → "a1b2c3d4-e5f6-..."
│  │
│  └─ Mostrar formulario de pago en modal
│
├─ 3️⃣ Usuario ingresa datos de tarjeta
│  │
│  ├─ Nombre: "Juan García"
│  ├─ Email: "juan@gmail.com"
│  ├─ Tarjeta: "4532 1234 5678 9010"
│  ├─ Vencimiento: "12/25"
│  └─ CVV: "123"
│
├─ 4️⃣ PaymentFormController.validarFormulario()
│  │
│  ├─ ✓ Nombre no vacío
│  ├─ ✓ Email contiene @
│  ├─ ✓ Tarjeta 16 dígitos numéricos
│  ├─ ✓ Vencimiento MM/YY
│  ├─ ✓ CVV 3-4 dígitos
│  │
│  └─ ✅ Todas las validaciones pasan
│
├─ 5️⃣ procesarPago()
│  │
│  ├─ 🟢 PaymentDAO.registrarTransaccion()
│  │  │
│  │  └─ INSERT INTO payment_transactions (
│  │        order_id, provider='stripe',
│  │        provider_tx_id='ch_...',
│  │        amount_cents=2999,
│  │        currency='USD',
│  │        status='success'
│  │     )
│  │
│  ├─ 💳 [En producción: Enviar a Stripe API]
│  │  │
│  │  └─ return { status: 'success', tx_id: 'ch_...' }
│  │
│  ├─ 🟡 PaymentDAO.procesarCompraExitosa()
│  │  │
│  │  ├─ BEGIN TRANSACTION
│  │  │
│  │  ├─ 1. SELECT recompensas del producto
│  │  │     coins_reward = 5000
│  │  │
│  │  ├─ 2. UPDATE users SET coins = coins + 5000
│  │  │     WHERE id = 3
│  │  │     (1250 + 5000 = 6250)
│  │  │
│  │  ├─ 3. INSERT INTO coin_transactions
│  │  │     (user_id=3, amount=5000, reason='Compra Premium')
│  │  │
│  │  ├─ 4. INSERT INTO user_inventory (items)
│  │  │     (si tiene items incluidos)
│  │  │
│  │  ├─ 5. UPDATE payment_orders
│  │  │     SET status='success' WHERE uuid='a1b2c3d4...'
│  │  │
│  │  └─ COMMIT TRANSACTION
│  │
│  └─ ✅ Compra completada
│
├─ 6️⃣ mostrarExito()
│  │
│  ├─ Alert: "¡Pago realizado exitosamente!"
│  └─ "Has recibido 5000 💰 coins"
│
└─ 7️⃣ UI Actualizada
   ├─ Cerrar formulario de pago
   ├─ Actualizar balance: 1250 → 6250
   ├─ Recargar inventario
   └─ Mostrar notificación
```

---

## Flujo de Boost de XP (Detallado)

```
USUARIO ACTIVA BOOST
│
├─ 1️⃣ ClickEvent: Botón "Activar Boost"
│  │
│  └─ itemId = 42 (Poción XP), userId = 3
│
├─ 2️⃣ Verificar que tiene el item
│  │
│  └─ InventoryDAO.tieneItem(3, 42)
│     SELECT 1 FROM user_inventory
│     WHERE user_id=3 AND item_id=42 AND quantity>0
│     → true ✅
│
├─ 3️⃣ BoostService.activarBoost()
│  │
│  ├─ durationSeconds = 3600 (1 hora)
│  │
│  ├─ Crear Timer interno
│  │  └─ Timer.schedule(() -> {
│  │       activeBoosts.remove(3)  // Remover después de 1h
│  │     }, 3600000 ms)
│  │
│  └─ activeBoosts.put(3, BoostInfo)
│     ├─ userId = 3
│     ├─ itemName = "Poción XP"
│     └─ tiempoFin = System.currentTimeMillis() + 3600000
│
├─ 4️⃣ Consumir el item
│  │
│  └─ InventoryDAO.consumirItem(3, 42)
│     UPDATE user_inventory
│     SET quantity = quantity - 1
│     WHERE user_id=3 AND item_id=42
│     (cantidad: 5 → 4)
│
├─ 5️⃣ UI Actualizada
│  │
│  ├─ Mostrar: "⚡ Poción XP activada - 59m 59s"
│  └─ Actualizar inventario: 5 → 4
│
├─ 6️⃣ Usuario completa misión
│  │
│  └─ Gana 100 XP
│
├─ 7️⃣ GameService.completarMision()
│  │
│  └─ UserDAO.otorgarRecompensas(3, 100, 50)
│     │
│     ├─ Obtener multiplicador
│     │  └─ multiplicador = BoostService.obtenerMultiplicadorXP(3)
│     │     ├─ Verifica si tieneBoostActivo(3) → true
│     │     └─ return 2
│     │
│     ├─ Calcular XP final
│     │  └─ xpFinal = 100 * 2 = 200 XP
│     │
│     └─ Otorgar recompensas
│        ├─ UPDATE users SET current_xp = current_xp + 200
│        ├─ INSERT xp_history
│        └─ Verificar nivel up
│
├─ 8️⃣ UI Muestra resultado
│  │
│  └─ "🎉 +200 XP (x2 BOOST!)"
│
├─ 9️⃣ [1 hora después...]
│  │
│  └─ Timer expira
│     ├─ activeBoosts.remove(3)
│     ├─ UI: "⚫ Boost expirado"
│     └─ Sistema listo para nuevo boost
│
└─ ✅ Siguiente XP sin multiplicador
   └─ Gana 100 XP → 100 XP (x1)
```

---

## Estados de una Orden de Pago

```
payment_orders.status

'created'
  ▼
[Esperando pago de usuario]
  ▼
'pending'
  ▼
[Procesando en Stripe/banco]
  ▼
'success' ✅
  ▼
[Compra completada, items entregados]

O

'failed' ❌
  ▼
[Pago rechazado, sin items]
```

---

## Arquitectura de Datos

```
USUARIOS
│
├─ coins (saldo actual)
├─ total_xp (experiencia total)
├─ current_xp (XP en nivel actual)
└─ level (nivel actual)
    │
    ├─────┬──────────────┬────────────────┐
    │     │              │                │
    ▼     ▼              ▼                ▼
[user_inventory] [coin_transactions] [xp_history] [payment_orders]
│                │                    │             │
├─ item_id       ├─ amount            ├─ amount     ├─ product_id
├─ quantity      ├─ reason            ├─ reason     ├─ status
├─ equipped      ├─ ref_id            ├─ ref_id     └─ order_uuid
└─ acquired_at   └─ created_at        └─ created_at
                                              │
                                              └─[payment_transactions]
                                                  ├─ provider (stripe)
                                                  ├─ provider_tx_id
                                                  ├─ amount_cents
                                                  └─ status

[items tabla]
├─ id
├─ name
├─ type (BOOST_XP, HELMET, CHEST, etc.)
├─ price_coins
├─ rarity
├─ boost_duration_seconds
└─ boost_multiplier
```

---

## Mapa de Métodos y Llamadas

```
StoreViewController
├─ comprarConCoins()
│  └─ StoreDAO.comprarConCoins()
│     ├─ UserDAO (verificar coins)
│     ├─ coin_transactions (registrar)
│     └─ user_inventory (agregar items)
│
├─ comprarConDinero()
│  └─ PaymentFormController.inicializar()
│     ├─ PaymentDAO.crearOrdenPago()
│     │  └─ payment_orders (INSERT)
│     │
│     ├─ [Validación de tarjeta]
│     │
│     ├─ PaymentDAO.registrarTransaccion()
│     │  └─ payment_transactions (INSERT)
│     │
│     └─ PaymentDAO.procesarCompraExitosa()
│        ├─ users (actualizar coins)
│        ├─ coin_transactions (registrar)
│        ├─ user_inventory (agregar items)
│        └─ payment_orders (actualizar status)
│
└─ activarBoost()
   ├─ BoostService.activarBoost()
   │  ├─ Timer interno
   │  └─ activeBoosts map
   │
   └─ InventoryDAO.consumirItem()
      └─ user_inventory (UPDATE quantity)
```

---

## Validaciones Cascada

```
COMPRA CON COINS
└─ StoreDAO.comprarConCoins()
   ├─ ✓ Usuario existe
   ├─ ✓ Oferta existe y activa
   ├─ ✓ Coins disponibles >= precio
   ├─ ✓ Items agregados sin duplicados
   └─ ✓ Transacción atómica

COMPRA CON DINERO REAL
├─ PaymentFormController.validarFormulario()
│  ├─ ✓ Nombre no vacío
│  ├─ ✓ Email válido
│  ├─ ✓ Tarjeta 13-19 dígitos
│  ├─ ✓ Vencimiento MM/YY
│  ├─ ✓ CVV 3-4 dígitos
│  └─ ✓ Todos numéricos
│
└─ PaymentDAO.procesarCompraExitosa()
   ├─ ✓ Orden existe
   ├─ ✓ Usuario existe
   ├─ ✓ Producto existe
   ├─ ✓ Recompensas válidas
   └─ ✓ Transacción atómica

BOOST
├─ ✓ Usuario existe
├─ ✓ Item existe
├─ ✓ Usuario tiene item
├─ ✓ Item es consumible
├─ ✓ Duración válida
└─ ✓ Un solo boost activo
```

---

**Diagrama Creado**: Enero 27, 2026
**Arquitectura**: DAO + Service + Controller + UI
**Estado**: ✅ Completo

