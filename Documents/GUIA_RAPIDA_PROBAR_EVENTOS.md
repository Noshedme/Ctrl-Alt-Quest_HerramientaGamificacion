# 🚀 GUÍA RÁPIDA - PROBAR EVENTOS VISUALES AHORA

## 30 Segundos: Test Rápido

```bash
# 1. Clonar código corregido
cd "c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend"

# 2. Compilar (20 segundos)
mvn compile

# 3. Ejecutar
mvn javafx:run
```

**Espera**: Evento debería aparecer en consola + VENTANA en pantalla en ~3 minutos

---

## 30 Segundos: Test INSTANTÁNEO (Recomendado)

### Paso 1: Editar Tiempo de Evento (1 minuto)

Abre archivo:
```
c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend\src\main\java\com\ctrlaltquest\services\EventContextualService.java
```

**Línea 113**, busca esto:
```java
scheduler.scheduleAtFixedRate(task, 180, 180, TimeUnit.SECONDS);
```

**Cambia a:**
```java
scheduler.scheduleAtFixedRate(task, 1, 20, TimeUnit.SECONDS);
// Primer evento en 1 segundo, cada 20 segundos después
```

**Guarda archivo** (Ctrl+S)

### Paso 2: Compilar (30 segundos)
```bash
cd "c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend"
mvn clean compile
```

**Espera que muestre**: `BUILD SUCCESS`

### Paso 3: Ejecutar (10 segundos)
```bash
mvn javafx:run
```

**Espera la app, luego:**
1. Login con tu usuario
2. Selecciona personaje
3. ✅ **¡VENTANA DEBE APARECER EN 1-2 SEGUNDOS!**

---

## 🎯 QUÉ VAS A VER

### En Consola:
```
✅ EventContextualUI registrado como listener
⚡ ActivityMonitor: Iniciado para usuario XXX
✅ Generador de eventos iniciado para usuario XXX (cada 3 minutos)

[1 segundo después...]

🎲 Evento generado para usuario XXX: ⏰ ¡Tómate un Descanso!
📢 [EventContextualUI] Evento generado: ⏰ ¡Tómate un Descanso!
🎮 [EventContextualUI] onEventStarted() - Mostrando: REST_BREAK
✅ REST_BREAK dialog mostrado en pantalla         ← ¡AQUÍ ES IMPORTANTE!
```

### En Pantalla:
```
┌─────────────────────────────────────┐
│ ⏰ Tómate un Descanso              │
├─────────────────────────────────────┤
│                                     │
│ Has trabajado mucho...              │
│                                     │
│              1                      │
│          (timer)                    │
│                                     │
│   [Saltar Descanso]                │
│                                     │
└─────────────────────────────────────┘
```
**Esto es una ventana REAL que flota sobre tu pantalla**

---

## ✅ CHECKLIST DE FUNCIONAMIENTO

Marca lo que VES:

- [ ] Consola muestra logs de evento generado
- [ ] Consola muestra "dialog mostrado en pantalla"
- [ ] Ventana emergente aparece en tu pantalla
- [ ] Ventana está ENCIMA de otras ventanas
- [ ] Ventana tiene título (⏰ Tómate un Descanso)
- [ ] Timer cuenta regresiva funciona
- [ ] Botón "Saltar" está clickeable
- [ ] XP se actualiza en la UI después de completar
- [ ] Siguiente evento aparece en ~20 segundos (si usas config rápida)

**Si todo está marcado ✅**: ¡FUNCIONANDO PERFECTO!

---

## 🎮 TIPOS DE EVENTOS (Aparecerán aleatoriamente)

### Evento 1: REST_BREAK ⏰
- Aparece automáticamente
- Timer de 30 segundos
- Opción para saltar
- **XP**: +50 si esperas, 0 si saltas

### Evento 2: QUICK_MISSION ⚡
- Diferentes misiones aleatorias
- Barra de progreso automática
- Se completa sola
- **XP**: +75

### Evento 3: BOSS_BATTLE ⚔️
- Batalla contra boss
- Botón ATACAR para damaging
- 5 clicks = Victoria
- **XP**: +100

---

## 📊 VALIDACIÓN EN CONSOLA

**Después de completar evento deberías ver:**
```
✅ COMPLETADO | Evento: ⏰ ¡Tómate un Descanso! | XP: +50
```

O:
```
❌ FALLIDO | Evento: ⚡ Misión Rápida | XP: +0
```

**EN LA BARRA DE XP DE LA APP:**
- Debe actualizar inmediatamente
- El número sube
- Si sube el XP suficiente para level: animación + sonido

---

## 🆘 SI NO APARECE NADA

### Problema A: No ves logs de evento
**Causas posibles:**
1. No esperaste 1 segundo (si usas config rápida)
2. No editaste la línea 113
3. No compilaste después de editar

**Solución:**
```bash
# 1. Verificar que editaste línea 113 (abre archivo)
# 2. Recompilar:
mvn clean compile

# 3. Ejecutar nuevamente:
mvn javafx:run

# 4. Espera 1 segundo después de seleccionar personaje
```

### Problema B: Ves logs pero NO ves ventana
**Causas posibles:**
1. Ventana aparece atrás de otra aplicación
2. Ventana muy pequeña en esquina
3. Issue de visualización de JavaFX

**Solución:**
1. Minimiza app de Ctrl+Alt+Quest
2. Mira atrás en otras ventanas
3. Si app está maximizada, búsca pequeña ventana flotante
4. Si nada funciona, revisar consola para excepciones

### Problema C: Excepción en consola
**Solución:**
```bash
# Busca líneas con "ERROR" o "Exception"
# Copia el error y envía para debugging

# Mientras tanto, intenta:
mvn clean compile -X
```

---

## 🎯 CONFIRMACIÓN VISUAL

Cuando todo funciona, deberías ver:

```
┌──────────────────────────────────────────┐
│                                          │
│  [Ventana flotante de evento]            │
│     Aparece sobre todo                   │
│     Es una ventana REAL de JavaFX        │
│     Se puede interactuar                 │
│     Tiene bordes, título, botones        │
│                                          │
└──────────────────────────────────────────┘
```

**NO** es:
- ❌ Un mensaje en consola
- ❌ Un popup de texto
- ❌ Una notificación del SO
- ❌ Un diálogo escondido

**SÍ** es:
- ✅ Una ventana JavaFX real
- ✅ Que aparece ENCIMA de otras apps
- ✅ Que se ve claramente en pantalla
- ✅ Con la que puedes interactuar

---

## 📱 DEMOSTRACIÓN

1. **Abre Chrome o Firefox**
2. **Ejecuta mvn javafx:run**
3. **Login en Ctrl+Alt+Quest**
4. **Selecciona personaje**
5. **Espera 1 segundo**
6. **¡VENTANA DE EVENTO DEBE APARECER SOBRE EL NAVEGADOR!**

Si no aparece sobre el navegador, puede estar detrás. Minimiza app Ctrl+Alt+Quest para ver.

---

## ⏰ TIEMPOS ESPERADOS

| Acción | Tiempo |
|--------|--------|
| Login + Personaje | < 5 seg |
| Hasta primer evento (rápido) | 1-2 seg |
| Hasta primer evento (normal) | 3 min |
| Duración REST_BREAK | 30 seg |
| Duración QUICK_MISSION | 5-10 seg (auto) |
| Duración BOSS_BATTLE | 5 seg (5 clicks) |
| Intervalo entre eventos (rápido) | 20 seg |
| Intervalo entre eventos (normal) | 3 min |

---

## 💾 ARCHIVO IMPORTANTE

Si necesitas cambiar tiempo de eventos nuevamente:

**Archivo**: `EventContextualService.java`  
**Línea**: 113 - Busca `scheduleAtFixedRate`

```java
// NORMAL (cada 3 minutos):
scheduler.scheduleAtFixedRate(task, 180, 180, TimeUnit.SECONDS);

// RÁPIDO (cada 20 segundos):
scheduler.scheduleAtFixedRate(task, 1, 20, TimeUnit.SECONDS);

// ULTRA RÁPIDO (cada 5 segundos):
scheduler.scheduleAtFixedRate(task, 1, 5, TimeUnit.SECONDS);
```

**Después de cambiar:**
```bash
mvn clean compile && mvn javafx:run
```

---

**¡Listo para probar!** 🎮

Ejecuta: `mvn javafx:run`

La ventana de evento debería aparecer en tu pantalla en los próximos 1-3 minutos (o 1 segundo si editaste la línea 113).

---

**Si funciona**: Marca ✅ arriba  
**Si no funciona**: Incluye logs de consola para debugging
