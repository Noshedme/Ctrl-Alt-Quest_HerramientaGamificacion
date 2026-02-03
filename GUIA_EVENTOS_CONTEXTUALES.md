# Sistema de Eventos Contextuales - Guía de Implementación

## 📋 Resumen

Se ha creado un **sistema completo de eventos contextuales** que aparece de forma aleatoria pero consistente con la actividad del usuario. Los eventos incluyen:

- **TYPING_CHALLENGE**: Retos de escritura (50-200 palabras)
- **CLICK_RUSH**: Tormentas de clicks (30-100 clicks)
- **BOSS_ENCOUNTER**: Encuentros con bosses (50-150 HP)
- **BREAK_TIME**: Descansos forzados (30-60 segundos)

Los eventos se generan cada 5-15 minutos con 30-40% de probabilidad, y su tipo es contextual según la actividad actual del usuario.

---

## 🔧 Archivos Creados

### Modelos
1. **EventType.java** (Enum)
   - Define los 4 tipos de eventos
   - Propiedades: código, nombre, duración, recompensa base

2. **Event.java** (Modelo)
   - Representa un evento individual
   - Propiedades: id, userId, tipo, descripción, progreso, recompensas
   - Método `incrementProgress()` para actualizar avance

### DAOs
3. **EventsDAO.java**
   - `createEvent()` - Crea evento en BD
   - `getEventById()` - Obtiene evento por ID
   - `getActiveEvents()` - Lista eventos activos del usuario
   - `completeEvent()` - Marca evento como completado
   - `getSecondsSinceLastEvent()` - Tiempo desde último evento

### Servicios
4. **EventGenerator.java**
   - `generateContextualEvent()` - Decide si generar evento
   - `selectEventByActivity()` - Selecciona evento según actividad
   - `generateTarget()` - Genera objetivo del evento
   - `generateDescription()` - Crea descripción amigable

5. **EventsService.java** (Singleton)
   - `checkAndGenerateEvent()` - Verifica y genera eventos
   - `generateEvent()` - Crea nuevo evento
   - `updateEventProgress()` - Actualiza progreso
   - `completeEvent()` - Completa evento y otorga recompensas
   - `getActiveEvent()` - Obtiene evento activo del usuario

### Modificaciones
6. **MissionProgressService.java**
   - Agregado `EventsService` como dependencia
   - `processActivityEvent()` ahora llama a `eventsService.checkAndGenerateEvent()`

7. **RewardsService.java**
   - Agregado overload `awardXPForActivity(userId, isProductive, xpAmount)`
   - Permite otorgar XP custom desde eventos

---

## 📊 Flujo de Funcionamiento

```
T=0s: Usuario abre VSCode
T=1s: ActivityMonitorService detecta actividad
     → MissionProgressService.processActivityEvent()
       └─ EventsService.checkAndGenerateEvent()
         ├─ Obtiene segundos desde último evento
         ├─ Si > 300 segundos y random < 40%
         └─ Genera evento contextual

T=2s: Pop-up aparece en UI con evento
     Usuario comienza a interactuar con evento

T=50s: Usuario completa evento
      EventsService.completeEvent()
      ├─ Otorga XP custom
      ├─ Otorga coins custom
      ├─ Verifica logros de eventos
      └─ Registra en BD con outcome
```

---

## 🎯 Lógica de Generación Contextual

### CODING (VSCode, IntelliJ, Eclipse)
- 50% TYPING_CHALLENGE
- 35% CLICK_RUSH
- 15% BOSS_ENCOUNTER

### BROWSING (Chrome, Firefox, Edge)
- 60% CLICK_RUSH
- 40% BOSS_ENCOUNTER

### PRODUCTIVITY (Word, Excel, Office)
- 70% TYPING_CHALLENGE
- 30% BOSS_ENCOUNTER

### Cualquier actividad
- 10% probabilidad de BREAK_TIME

### Limitaciones de frecuencia
- Mínimo 5 minutos entre eventos
- Máximo 15 minutos
- Solo 40% de probabilidad en cada chequeo

---

## 💾 Estructura de BD

### Tabla: events
```
id              | serial
user_id         | integer (FK users)
type            | varchar (TYPING_CHALLENGE, CLICK_RUSH, BOSS_ENCOUNTER, BREAK_TIME)
description     | text
trigger         | jsonb ({"type", "target", "current", "xp_reward", "coin_reward"})
occurred_at     | timestamp
handled         | boolean
outcome         | jsonb ({"completed", "progress"})
```

### Tabla: bosses (ya existe)
```
id              | serial
name            | varchar
description     | text
mechanic        | varchar (CLICK_BASED, TYPING_BASED)
base_hp         | integer
spawn_reason    | varchar
xp_reward       | integer
coin_reward     | integer
difficulty      | varchar
config          | jsonb
```

---

## 🎁 Recompensas por Evento

### TYPING_CHALLENGE
- XP: 30 + (target / 5)
- Coins: 20 + (target / 5)
- Ej: 100 palabras = 50 XP + 40 coins

### CLICK_RUSH
- XP: 25 + (target / 3)
- Coins: 15 + (target / 3)
- Ej: 60 clicks = 45 XP + 35 coins

### BOSS_ENCOUNTER
- XP: 100 + (target / 2)
- Coins: 75 + (target / 2)
- Ej: 100 HP = 150 XP + 125 coins

### BREAK_TIME
- XP: 15 (fijo)
- Coins: 10 (fijo)

---

## 🔌 Integración con UI

### Mostrar evento en Pop-up
```java
// En ActivityViewController o controlador principal
Event activeEvent = EventsService.getInstance().getActiveEvent(userId);
if (activeEvent != null) {
    showEventPopup(activeEvent);  // Tu método para mostrar en UI
}
```

### Actualizar progreso en tiempo real
```java
// En controladores de eventos de mouse/teclado
@FXML
private void onMouseClick() {
    EventsService.getInstance().updateEventProgress(userId, 1);
}

@FXML
private void onKeyTyped() {
    EventsService.getInstance().updateEventProgress(userId, 1);
}
```

### Completar evento manualmente
```java
Event event = EventsService.getInstance().getActiveEvent(userId);
if (event.isCompleted()) {
    EventsService.getInstance().completeEvent(userId, event, true);
}
```

---

## 🧪 Testing

### Setup de BD
```sql
-- En pgAdmin, ejecutar:
1. SISTEMA_MISIONES_TIEMPO_REAL.sql  (si no está ejecutado)
2. SISTEMA_EVENTOS_CONTEXTUALES.sql
```

### Verificar datos
```sql
-- Ver bosses creados
SELECT name, mechanic, base_hp FROM public.bosses;

-- Ver eventos activos
SELECT id, user_id, type, description FROM public.events WHERE handled = false;

-- Ver estadísticas
SELECT user_id, COUNT(*) as total_events FROM public.events GROUP BY user_id;
```

### Pruebas manuales
1. Abre la aplicación
2. Login como usuario 3 (o el usuario que uses)
3. Comienza actividad en VSCode por 5+ minutos
4. Espera a que aparezca pop-up de evento
5. Completa el evento (escribe, haz clicks)
6. Verifica BD: `SELECT * FROM public.events WHERE user_id = 3 ORDER BY occurred_at DESC LIMIT 1;`

---

## 📈 Métricas de Monitoreo

```sql
-- Eventos totales por usuario
SELECT user_id, COUNT(*) as total FROM public.events GROUP BY user_id;

-- Tasa de completación
SELECT user_id, 
       SUM(CASE WHEN outcome->>'completed' = 'true' THEN 1 ELSE 0 END)::float / 
       COUNT(*) * 100 as completion_rate
FROM public.events GROUP BY user_id;

-- XP ganado por eventos
SELECT SUM(CAST(trigger->>'xp_reward' as integer)) as total_xp_from_events
FROM public.events WHERE user_id = 3;
```

---

## ⚙️ Configuración Ajustable

### En EventGenerator.java
```java
// Cambiar probabilidad de evento (línea ~30)
if (probability > 40) {  // 40% = cambiable a 30, 50, 60
    return null;
}

// Cambiar intervalo mínimo entre eventos (línea ~28)
if (timeSinceLastEvent < 300) {  // 300 = 5 minutos, cambiar a 60 (1 min), etc
    return null;
}
```

### En EventsService.java
```java
// Cambiar probabilidades por actividad (método calculateXpReward)
// Ejemplo: TYPING_CHALLENGE XP ahora es 30 + (target/5)
// Cambiar a 50 + (target/5) para más recompensa
```

---

## 🐛 Troubleshooting

### P: Los eventos no aparecen
**R:** Verifica que:
1. La aplicación está corriendo
2. `EventsService.getInstance()` se llama correctamente
3. Revisa logs: busca "🎯 [EventsService] ¡EVENTO APARECE!"

### P: El progreso no se actualiza
**R:** Confirma que:
1. Los listeners de mouse/teclado llamgan a `EventsService.updateEventProgress()`
2. Los eventos no están completados ya

### P: Las recompensas no se otorgan
**R:** Revisa:
1. `RewardsService.getInstance()` está disponible
2. La BD tiene `coin_transactions` y `users` actualizadas

---

## 📌 Próximos Pasos

1. **UI Mejorada**
   - Animaciones al completar evento
   - Progress bar visual
   - Sonidos de evento

2. **Eventos Dinámicos**
   - Eventos especiales por hora
   - Bosses progresivos (más difíciles con cada derrota)
   - Eventos de "Hora Dorada" (2x recompensas)

3. **Leaderboards**
   - Ranking de bosses derrotados
   - Ranking de eventos completados

4. **Social**
   - Eventos coop (2 usuarios vs boss)
   - Desafíos entre amigos

---

## 📝 Notas Finales

- **No invasivo**: Los eventos aparecen máximo cada 5-15 minutos
- **Contextual**: El tipo de evento depende de lo que estés haciendo
- **Consistente**: Los datos se guardan en BD para tracking
- **Recompensador**: Cada evento completado da XP y coins

El sistema está listo para integrase con tu UI. Solo necesitas:
1. Crear PopUp para mostrar eventos
2. Conectar listeners de mouse/teclado
3. Ejecutar SQL de setup

¡Diviértete con los eventos! 🎮
