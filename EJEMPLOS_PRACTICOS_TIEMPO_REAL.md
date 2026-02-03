# 🎮 EJEMPLOS PRÁCTICOS - Sistema en Tiempo Real

## Escenario 1: Usuario Abre VSCode y Trabaja 1 Hora

### Estado Inicial
```sql
-- Usuario y misiones antes de empezar
SELECT username, level, current_xp, total_xp, coins FROM users WHERE id = 1;
-- username: "jugador1", level: 1, current_xp: 0, total_xp: 0, coins: 500

SELECT title, progress, completed FROM missions WHERE user_id = 1 LIMIT 3;
-- "Coder por 1 Hora", progress: 0, completed: false
-- "Coder por 5 Horas", progress: 0, completed: false
-- "1 Hora Productiva", progress: 0, completed: false
```

### Timeline de Eventos (Cada segundo mientras está en VSCode)

**T = 0 segundos (Abre VSCode)**
```
Console Output:
✅ Sesión BD iniciada: ID 42
🔄 Inicializando mission_progress para misiones activas...
✅ Se inicializaron 8 misiones para usuario 1
⚡ ActivityMonitor: Iniciado para usuario 1
```

**T = 1 segundo (Primer tick)**
```
Console Output:
✨ [RewardsService] XP Otorgado: +1 XP | Total: 1
📝 [ActivityMonitorService] VSCode detectado (productivo)

BD Changes:
mission_progress: current_value 0 → 1, progress_percentage 0 → 0.03
users: current_xp 0 → 1, total_xp 0 → 1
app_usage_logs: INSERT log de 1 segundo en VSCode
```

**T = 10 segundos**
```
Console Output (Cada tick):
✨ [RewardsService] XP Otorgado: +1 XP | Total: 10

BD Changes:
mission_progress: current_value 10, progress_percentage 0.28%
users: current_xp 10, total_xp 10
```

**T = 3600 segundos (1 Hora Exacta)**
```
Console Output:
✨ [RewardsService] XP Otorgado: +1 XP | Total: 3600
✅ [MissionProgressService] Misión Completada: Coder por 1 Hora | +XP: 50 | +Monedas: 100
💰 [RewardsService] Monedas Otorgadas: +100 | Misión #1
  └─ 🏆 [RewardsService] Logro Desbloqueado: ID #1 (Primer Código)
    └─ ✨ XP Bonus del Logro: +50
    └─ 💰 Coin Bonus del Logro: +100

BD Changes:
missions: SET completed = true, progress = 100, completed_at = NOW() WHERE id = 1
mission_progress: current_value 3600, progress_percentage 100.00
users: 
  - current_xp: 3610 (3600 + 50 XP bonus del logro)
  - total_xp: 3610
  - coins: 700 (500 inicial + 100 misión + 100 logro)
coin_transactions: 
  - INSERT "Misión completada" +100
  - INSERT "Logro desbloqueado" +100
user_achievements: INSERT (user_id: 1, achievement_id: 1, unlocked_at: NOW())
```

### Estado Final (Después de 1 Hora)
```sql
SELECT username, level, current_xp, total_xp, coins FROM users WHERE id = 1;
-- username: "jugador1", level: 1, current_xp: 3610, total_xp: 3610, coins: 700

SELECT COUNT(*) as missions_completed FROM missions WHERE user_id = 1 AND completed = true;
-- missions_completed: 1

SELECT COUNT(*) as achievements FROM user_achievements WHERE user_id = 1;
-- achievements: 1

SELECT total_earned FROM (
    SELECT SUM(amount) as total_earned FROM coin_transactions WHERE user_id = 1
) t;
-- total_earned: 200
```

---

## Escenario 2: Usuario Usa Múltiples Apps (Browsing + Coding)

### Timeline de Eventos

**T = 0 (Abre Chrome)**
```
Categoría: BROWSING
metric_key: time_browsing

Console: 📝 [ActivityMonitorService] Chrome detectado (no productivo)
```

**T = 300 seg (5 minutos en Chrome)**
```
mission_progress: time_browsing → current_value 300, progress_percentage 8.33%

Console: ✨ [RewardsService] XP Otorgado: +1 XP (no, porque no es productivo)
         No se otorga XP en navegación general

BD: Sin cambios en XP (solo en actividad_logs y mission_progress)
```

**T = 300 seg (Abre VSCode)**
```
Switch a CODING
metric_key: time_coding

Console: 📝 [ActivityMonitorService] VSCode detectado (productivo)
         ✨ [RewardsService] XP Otorgado: +1 XP | Total: X
```

**T = 600 seg (10 minutos en VSCode)**
```
Ambas categorías se registran:
mission_progress: 
  - time_browsing: 300 segundos (no progresa más, cambió de app)
  - time_coding: 300 segundos
  - categories_used: 2 categorías detectadas

Console updates:
✨ Cada segundo +1 XP por VSCode
💡 Si hay misión "Explorador Versátil" (3 categorías): aún falta 1

BD: categories_used incrementa cuando detecta 2+ apps
    Puede completar misión "apps_used" si usó suficientes apps
```

---

## Escenario 3: Level Up y Bonificaciones en Cascada

### Contexto
- Usuario está en nivel 1
- XP requerido para nivel 2 = (1+1) * 100 = 200 XP
- Ya tiene: 150 XP

### Usuario Abre VSCode

**T = 0**
```
Situación: current_xp = 150, level = 1
```

**T = 50 segundos (50 XP ganados)**
```
Console Output (en tick 40):
✨ [RewardsService] XP Otorgado: +1 XP | Total: 190
```

**T = 51 segundos (XP = 201)**
```
Console Output:
✨ [RewardsService] XP Otorgado: +1 XP | Total: 201
⬆️ [RewardsService] ¡NIVEL SUBIDO! Nuevo nivel: 2
💰 [RewardsService] Monedas Otorgadas: +50 | Misión #-1
```

**BD Changes:**
```sql
users: 
  - level: 1 → 2
  - current_xp: 200 (se reinicia o continúa según diseño)
  - total_xp: 401 (sigue incrementando)
  - coins: +50 bonus

coin_transactions: INSERT "Level up bonus" +50

-- Verificar:
SELECT username, level, current_xp, total_xp, coins FROM users WHERE id = 1;
-- "jugador1", 2, 1 (o 201), 401, 550
```

---

## Escenario 4: Desbloquear Logros por Hito

### Usuario Lleva 7 Horas de Programación Acumuladas

```sql
-- Estado actual:
SELECT total_time_coding, total_xp FROM user_stats WHERE user_id = 1;
-- total_time_coding: 25200 (7 horas)
-- total_xp: 10500
```

### RewardsService.checkAndAwardAchievements() es llamado

```
Verifica todas las condiciones de logros no desbloqueados:

1. ¿"Primer Código" (1 hora de coding)? 
   ✅ CUMPLE → Ya está desbloqueado (anterior)

2. ¿"Programador Dedicado" (10 horas de coding)?
   ✅ CUMPLE → Desbloquear ahora
   
3. ¿"Marathonista de Código" (8 horas consecutivas)?
   ❌ NO CUMPLE (necesita en una sesión continua)

4. ¿"Cazador de Misiones" (10 misiones completadas)?
   ❌ NO CUMPLE (solo tiene 3 completadas)

5. ¿"Erudito" (1000 XP)?
   ✅ CUMPLE → Desbloquear ahora
```

**Console Output:**
```
🏆 [RewardsService] Logro Desbloqueado: ID #2 (Programador Dedicado)
  └─ ✨ XP Bonus del Logro: +200
  └─ 💰 Coin Bonus del Logro: +500

🏆 [RewardsService] Logro Desbloqueado: ID #12 (Erudito)
  └─ ✨ XP Bonus del Logro: +0
  └─ 💰 Coin Bonus del Logro: +500
```

**BD Changes:**
```sql
user_achievements: INSERT 2 filas
  - (user_id: 1, achievement_id: 2, unlocked_at: NOW())
  - (user_id: 1, achievement_id: 12, unlocked_at: NOW())

coin_transactions: INSERT 2 filas
  - "Logro Programador Dedicado" +500
  - "Logro Erudito" +500

users: 
  - total_xp: 10900 (10500 + 200 + 0)
  - coins: +1000 (500+500)
```

---

## Escenario 5: Verificación en Base de Datos

### Ver Progreso de Misiones en Tiempo Real

```sql
-- Cada 5 segundos ejecutar:
SELECT 
    mp.metric_key,
    mp.current_value,
    mp.target_value,
    mp.progress_percentage,
    m.title,
    m.completed
FROM mission_progress mp
JOIN missions m ON mp.mission_id = m.id
WHERE mp.user_id = 1 AND m.completed = false
ORDER BY mp.progress_percentage DESC;

-- Output esperado (mientras está en VSCode):
┌──────────────────────┬───────────────┬──────────────┬──────────────────┬─────────────────┬───────────┐
│ metric_key           │ current_value │ target_value │ progress_percent │ title            │ completed │
├──────────────────────┼───────────────┼──────────────┼──────────────────┼──────────────────┼───────────┤
│ time_coding          │ 47            │ 3600         │ 1.31             │ Coder por 1 Hora │ false     │
│ time_productivity    │ 8             │ 3600         │ 0.22             │ 1 Hora Productiva│ false     │
│ apps_used            │ 1             │ 5            │ 20.00            │ Multitarea Expert│ false     │
└──────────────────────┴───────────────┴──────────────┴──────────────────┴──────────────────┴───────────┘

-- El current_value incrementa 1 por segundo (mientras está en VSCode)
```

### Ver XP en Tiempo Real

```sql
SELECT username, level, current_xp, total_xp, coins FROM users WHERE id = 1;

-- Output después de 47 segundos en VSCode:
┌──────────┬───────┬────────────┬───────────┬───────┐
│ username │ level │ current_xp │ total_xp  │ coins │
├──────────┼───────┼────────────┼───────────┼───────┤
│ jugador1 │ 1     │ 47         │ 47        │ 500   │
└──────────┴───────┴────────────┴───────────┴───────┘

-- El current_xp incrementa 1 por segundo (solo en actividad productiva)
```

### Ver Logros Desbloqueados

```sql
SELECT 
    a.name,
    a.xp_reward,
    a.coin_reward,
    ua.unlocked_at,
    DATE_PART('seconds', NOW() - ua.unlocked_at) as hace_segundos
FROM user_achievements ua
JOIN achievements a ON ua.achievement_id = a.id
WHERE ua.user_id = 1
ORDER BY ua.unlocked_at DESC
LIMIT 5;

-- Output (después de 1 hora de gaming):
┌──────────────────────┬────────────┬──────────────┬─────────────────────┬───────────────┐
│ name                 │ xp_reward  │ coin_reward  │ unlocked_at         │ hace_segundos │
├──────────────────────┼────────────┼──────────────┼─────────────────────┼───────────────┤
│ Logro Reciente       │ 100        │ 200          │ 2025-01-28 14:25:00 │ 5             │
│ Primer Código        │ 50         │ 100          │ 2025-01-28 14:20:00 │ 300           │
└──────────────────────┴────────────┴──────────────┴─────────────────────┴───────────────┘
```

---

## Consultas Útiles para Debug

### Ver Toda la Actividad de Hoy

```sql
SELECT 
    DATE_TRUNC('hour', aul.start_time) as hora,
    a.name as app_name,
    COUNT(*) as ticks,
    SUM(EXTRACT(EPOCH FROM aul.duration)) as segundos_totales
FROM app_usage_logs aul
LEFT JOIN apps a ON aul.app_id = a.id
JOIN activity_sessions act ON aul.session_id = act.id
WHERE act.user_id = 1 AND DATE(aul.start_time) = CURRENT_DATE
GROUP BY DATE_TRUNC('hour', aul.start_time), a.name
ORDER BY DATE_TRUNC('hour', aul.start_time) DESC;
```

### Ver Progreso General del Usuario

```sql
SELECT 
    u.username,
    u.level,
    u.total_xp,
    u.coins,
    COUNT(DISTINCT m.id) as total_missions,
    SUM(CASE WHEN m.completed THEN 1 ELSE 0 END) as missions_completed,
    COUNT(DISTINCT ua.achievement_id) as achievements_unlocked,
    SUM(ct.amount) as total_coins_earned
FROM users u
LEFT JOIN missions m ON u.id = m.user_id
LEFT JOIN user_achievements ua ON u.id = ua.user_id  
LEFT JOIN coin_transactions ct ON u.id = ct.user_id
WHERE u.id = 1
GROUP BY u.id, u.username, u.level, u.total_xp, u.coins;
```

### Ver Últimas 10 Transacciones

```sql
SELECT reason, amount, created_at, 
       ROW_NUMBER() OVER (ORDER BY created_at DESC) as posicion
FROM coin_transactions
WHERE user_id = 1
LIMIT 10;
```

---

## 📊 Resumen de Formulas

| Evento | Cálculo | Resultado |
|--------|---------|-----------|
| 1 seg actividad productiva | +1 XP | total_xp incrementa |
| 1 seg actividad no productiva | +0 XP | sin cambios |
| Misión completada | XP = misión.xp_reward | UPDATE users.current_xp |
| Level up | XP requerido = (nivel+1)*100 | bonus +50 monedas |
| Logro desbloqueado | Bonus = logro.xp_reward + logro.coin_reward | INSERT user_achievements |
| Misión + Logro mismo tick | Se suman recompensas | Ambas se aplican |

---

**💡 Tip:** Combina estas queries para crear un dashboard en tiempo real que actualices cada 5 segundos mientras la app está en ejecución.
