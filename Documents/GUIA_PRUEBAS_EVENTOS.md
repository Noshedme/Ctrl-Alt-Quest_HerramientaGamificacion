# 🧪 GUÍA RÁPIDA DE PRUEBAS - EVENTOS CONTEXTUALES

## Validación Paso a Paso

### 1️⃣ COMPILACIÓN ✅
```bash
cd c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend
mvn clean compile
```
**Esperado**: `BUILD SUCCESS`

### 2️⃣ EJECUCIÓN 🚀
```bash
mvn javafx:run
```

### 3️⃣ LOGIN 🔐
- Usuario: [Tu usuario]
- Contraseña: [Tu contraseña]
- Seleccionar personaje

### 4️⃣ VERIFICAR INICIALIZACIÓN EN CONSOLA

**Debe ver estos logs**:
```
✅ Personaje cargado: [NombrePersonaje] (Nivel X)
✅ Sesión BD iniciada: ID [sessionId]
✅ Misiones globales inicializadas
✅ HomeController registrado como XP Observer
✅ EventContextualUI inicializado
✅ Monitoreo de actividad iniciado
✅ Generador de eventos iniciado para usuario [userId] (cada 3 minutos)
```

Si NO ve estos logs → Revisar consola para errores

---

## 🎲 PRUEBAS DE EVENTOS

### Opción A: Esperar 3 minutos
- Simplemente esperar a que el evento aparezca naturalmente

### Opción B: Acelerar el tiempo (RECOMENDADO para pruebas)
1. Abrir: [EventContextualService.java](../../frontend/src/main/java/com/ctrlaltquest/services/EventContextualService.java#L113)
2. Línea 113, cambiar:
   ```java
   // DE:
   scheduler.scheduleAtFixedRate(task, 180, 180, TimeUnit.SECONDS);
   
   // A:
   scheduler.scheduleAtFixedRate(task, 1, 20, TimeUnit.SECONDS); // Primer evento en 1 seg, cada 20 seg
   ```
3. Recompilar: `mvn compile`
4. Ejecutar: `mvn javafx:run`

---

## ✨ VALIDACIÓN DE EVENTOS

### REST_BREAK ⏰
**Esperado**:
- [ ] Ventana con título "⏰ Tómate un Descanso"
- [ ] Cuadro con timer contando 30 → 0
- [ ] Dos botones: timer o "Saltar Descanso"
- [ ] Sonido de suceso
- [ ] XP ganado: +50
- [ ] Console: `✅ Evento completado: [id] | XP: +50`

### QUICK_MISSION ⚡
**Esperado**:
- [ ] Ventana con título "⚡ Misión Rápida"
- [ ] Descripción: "Escribe 100 palabras" (u otra tarea)
- [ ] Barra de progreso visible
- [ ] Auto-completa la misión
- [ ] Sonido de suceso
- [ ] XP ganado: +75
- [ ] Console: `✅ Evento completado: [id] | XP: +75`

### BOSS_BATTLE ⚔️
**Esperado**:
- [ ] Ventana con título "⚔️ Boss Apareció!"
- [ ] Nombre del boss: "Procrastinación Boss" (u otro)
- [ ] Barra de salud: "100/100"
- [ ] Botón "ATACAR" (fondo rojo)
- [ ] Al hacer click: Salud disminuye (-20 por click)
- [ ] Al llegar a 0: "🎉 ¡VICTORIA!"
- [ ] Sonido de victoria
- [ ] XP ganado: +100
- [ ] Console: `✅ Evento completado: [id] | XP: +100`

---

## 🔍 VALIDACIÓN EN BD

### Conectarse a PostgreSQL
```bash
psql -U [usuario] -d ctrlaltquest
```

### Verificar tabla de eventos
```sql
SELECT 
    id,
    user_id,
    type,
    created_at,
    handled,
    outcome
FROM public.events
ORDER BY created_at DESC
LIMIT 10;
```

**Esperado**:
- Eventos registrados con tipos: REST_BREAK, QUICK_MISSION, BOSS_BATTLE
- Columna `outcome` contiene: `{"success": true, "xp_earned": XX, "completed_at": "..."}`
- Eventos más recientes primero

### Verificar historial de XP
```sql
SELECT 
    xp_history.user_id,
    xp_history.xp_amount,
    xp_history.activity_type,
    xp_history.created_at
FROM public.xp_history
WHERE activity_type = 'contextual_event'
ORDER BY created_at DESC
LIMIT 10;
```

**Esperado**:
- Registros con `activity_type = 'contextual_event'`
- `xp_amount` = 50, 75, o 100 según tipo evento
- Timestamps recientes

---

## 📊 VALIDACIÓN DE SINCRONIZACIÓN

### En la UI de HomeController
- [ ] Barra de XP se actualiza en tiempo real
- [ ] Número de XP actualizado inmediatamente
- [ ] Si XP alcanza nuevo nivel: animación y sonido
- [ ] Label de nivel parpadea brevemente

### En la BD
- Comprobar que cada evento aparece en BD dentro de 1 segundo de completarse
- Comprobar que XP otorgado coincide (50/75/100)

---

## 🚨 TROUBLESHOOTING

### Error: "EventContextualUI no se encuentra"
```
Solution: Verificar que el archivo existe en:
src/main/java/com/ctrlaltquest/ui/utils/EventContextualUI.java
```

### Error: "EventContextualListener no compilado"
```
Solution: Ejecutar:
mvn clean compile -X
```

### Console: "⚠️ Generador de eventos detenido..."
```
Significa que el usuario cerró sesión o cambió de personaje
Esto es normal - eventos se reanudarán en siguiente login
```

### No aparecen eventos cada 3 minutos
```
Solución 1: Verificar que ActivityMonitorService.startMonitoring() se ejecutó
Solución 2: Verificar en consola que dice "Generador de eventos iniciado"
Solución 3: Cambiar tiempo a 1 segundos en EventContextualService.java línea 113
```

### BD no muestra eventos
```
Solución 1: Verificar conexión a BD en DatabaseConnection
Solución 2: Verificar tabla "public.events" existe en BD
Solución 3: Revisar logs de compilación para excepciones en recordEventCompletion()
```

---

## ✅ CHECKLIST FINAL

- [ ] Compilación: BUILD SUCCESS
- [ ] App inicia sin errores
- [ ] HomeController inicializa sin excepciones
- [ ] Console muestra "Generador de eventos iniciado"
- [ ] Primer evento aparece (dentro de tiempo configurado)
- [ ] Evento muestra diálogo correcto (REST/QUEST/BOSS)
- [ ] XP se actualiza al completar evento
- [ ] Sonidos reproducen correctamente
- [ ] BD registra evento en tabla "public.events"
- [ ] Barra de XP en UI sincronizada con BD

---

## 🎮 FUNCIONAMIENTO ESPERADO

```
Timeline de Ejecución:
├─ T=0s: Usuario selecciona personaje
├─ T=0s: HomeController.initialize() ejecuta
├─ T=0s: ActivityMonitorService.startMonitoring(userId)
├─ T=0s: EventContextualService.startEventGenerator(userId)
├─ T≈1s: Primer evento generado (con timeout = 1s)
├─ T≈1s: EventContextualUI abre diálogo
├─ T=1-30s: Usuario interactúa con evento
├─ T≈30s: XPSyncService otorga XP
├─ T≈30s: BD registra evento
├─ T≈30s: HomeController actualiza UI
├─ T>21s: Siguiente evento generado
└─ ... Repite cada 20s (o 3 minutos en producción)
```

---

**Para preguntas o problemas, revisar EVENTOS_CONTEXTUALES_IMPLEMENTADOS.md**
