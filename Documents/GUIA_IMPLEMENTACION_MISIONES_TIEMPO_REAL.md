# 🎮 SISTEMA DE MISIONES EN TIEMPO REAL - Guía Completa de Implementación

## 📋 Resumen de Cambios

Se ha implementado un **sistema integrado de progresión automática** donde:

✅ **La actividad del usuario (VSCode, navegación, etc.) actualiza misiones en tiempo real**  
✅ **Al completar misiones, se otorgan XP y monedas automáticamente**  
✅ **Los logros se desbloquean cuando se cumplen sus condiciones**  
✅ **Cada segundo de actividad productiva = +1 XP (configurable)**  

---

## 🔧 Nuevos Componentes Creados

### 1. **RewardsService** 
**Ubicación:** `com.ctrlaltquest.services.RewardsService`

**Responsabilidades:**
- Otorgar XP automáticamente por actividad productiva
- Otorgar monedas por misiones completadas
- Manejar level-ups y bonificaciones
- Desbloquear logros y otorgar sus recompensas
- Verificar condiciones de logros

**Métodos principales:**
```java
// Otorgar XP cada segundo (si es productivo)
int xpAwarded = RewardsService.getInstance().awardXPForActivity(userId, isProductive);

// Otorgar monedas por misión
RewardsService.getInstance().awardCoinsForMission(userId, missionId, coins);

// Desbloquear logro específico
boolean unlocked = RewardsService.getInstance().awardAchievement(userId, achievementId);

// Verificar y desbloquear todos los logros aplicables
RewardsService.getInstance().checkAndAwardAchievements(userId);
```

---

### 2. **MissionProgressService**
**Ubicación:** `com.ctrlaltquest.services.MissionProgressService`

**Responsabilidades:**
- Procesar eventos de actividad
- Actualizar misiones basadas en tiempo (segundos en app)
- Actualizar misiones basadas en contadores (apps únicas usadas)
- Completar misiones automáticamente cuando alcanzan 100%
- Integrar con RewardsService para otorgar recompensas

**Métodos principales:**
```java
// Se llama cada segundo desde ActivityMonitorService
MissionProgressService.getInstance().processActivityEvent(
    userId,           // ID del usuario
    "VSCode",         // Nombre de la app detectada
    true              // ¿Es productiva?
);

// Esto internamente:
// 1. Categoriza la app (CODING, BROWSING, OFFICE, etc.)
// 2. Incrementa misiones de tiempo en esa categoría
// 3. Actualiza contadores (apps únicas, categorías usadas)
// 4. Otorga XP si es productivo
// 5. Verifica y desbloquea logros
```

---

### 3. **AchievementsDAO**
**Ubicación:** `com.ctrlaltquest.dao.AchievementsDAO`

**Responsabilidades:**
- Obtener logros del usuario
- Verificar si un usuario tiene un logro
- Calcular progreso hacia logros
- Contar logros totales desbloqueados

**Métodos principales:**
```java
// Obtener todos los logros (desbloqueados + bloqueados)
List<Achievement> achievements = AchievementsDAO.getAllAvailableAchievements(userId);

// Verificar si tiene un logro
boolean has = AchievementsDAO.hasAchievement(userId, achievementId);

// Obtener progreso hacia un logro (0-100%)
int progress = AchievementsDAO.getAchievementProgress(userId, achievementId);

// Contar logros totales
int total = AchievementsDAO.getTotalUnlockedAchievements(userId);
```

---

## 🔌 Integración con Componentes Existentes

### ActivityMonitorService (MODIFICADO)
```java
// En el método reportActivity(), ahora se llama:
MissionProgressService missionService = MissionProgressService.getInstance();
missionService.processActivityEvent(currentUserId, currentApp, isProductive);
```

**Flujo:**
1. Detecta app actual cada 1 segundo
2. Llama a `ActivityDAO.registrarActividad()` (existente)
3. **NUEVO:** Llama a `MissionProgressService.processActivityEvent()`
4. Que a su vez llama a `RewardsService` para XP y logros

---

## 📊 Flujo de Datos en Tiempo Real

```
┌─────────────────────────────────────────────────────────────────┐
│ CADA SEGUNDO (Mientras el usuario está trabajando)              │
└─────────────────────────────────────────────────────────────────┘

1. ActivityMonitorService.reportActivity()
   └─→ Detecta "Visual Studio Code" con título activo

2. ActivityDAO.registrarActividad()
   └─→ Inserta en app_usage_logs

3. MissionProgressService.processActivityEvent()
   ├─→ Categoriza app como "CODING"
   ├─→ actualiza mission_progress +1 segundo
   │  └─→ UPDATE mission_progress SET current_value = current_value + 1
   │
   ├─→ Verifica si misión completada (current_value >= target_value)
   │  └─→ Si sí: llama completeMission()
   │
   └─→ RewardsService.awardXPForActivity(userId, true)
      └─→ UPDATE users SET current_xp = current_xp + 1

4. Cada 10 segundos productivos acumulados:
   ├─→ RewardsService verifica level-up
   │  └─→ Si nuevo nivel: UPDATE users SET level = level + 1
   │     └─→ Bonus: +50 monedas automáticas
   │
   └─→ RewardsService.checkAndAwardAchievements()
      └─→ Verifica condiciones de todos los logros no desbloqueados
      └─→ Si se cumplen: INSERT INTO user_achievements
         └─→ Otorga recompensas (XP + monedas del logro)

┌─────────────────────────────────────────────────────────────────┐
│ CUANDO SE COMPLETA UNA MISIÓN                                   │
└─────────────────────────────────────────────────────────────────┘

1. MissionProgressService detecta current_value >= target_value

2. completeMission() es llamado:
   ├─→ UPDATE missions SET completed = true, progress = 100
   │
   ├─→ SELECT xp_reward, coin_reward FROM missions WHERE id = ?
   │
   ├─→ RewardsService.awardCoinsForMission()
   │  └─→ INSERT INTO coin_transactions
   │     └─→ UPDATE users SET coins = coins + coinReward
   │
   ├─→ UPDATE users SET current_xp = current_xp + xpReward
   │
   └─→ RewardsService.checkAndAwardAchievements()
      └─→ Verifica "Completar 1 misión", "Completar 10", etc.

┌─────────────────────────────────────────────────────────────────┐
│ CUANDO SE DESBLOQUEA UN LOGRO                                   │
└─────────────────────────────────────────────────────────────────┘

1. RewardsService.awardAchievement(userId, achievementId)

2. INSERT INTO user_achievements (user_id, achievement_id)

3. SELECT xp_reward, coin_reward FROM achievements WHERE id = ?

4. Otorga bonificaciones:
   ├─→ UPDATE users SET current_xp = current_xp + xp_reward
   └─→ INSERT INTO coin_transactions (para coin_reward)

5. Verificar level-up nuevamente
   └─→ Si nuevo nivel: +50 monedas bonus
```

---

## 🚀 Guía de Implementación

### PASO 1: Ejecutar Script SQL de Setup

1. Abrir **pgAdmin 4**
2. Query Tool → Copiar contenido de `SISTEMA_MISIONES_TIEMPO_REAL.sql`
3. Ejecutar (Ctrl+Enter)
4. Esperar confirmación: "SETUP COMPLETADO"

**Esto crea:**
- 8 misiones de ejemplo variadas
- 18 logros categorizados
- Datos de audit de prueba

### PASO 2: Recompilar Java

```bash
cd c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend
mvn clean compile
```

**Esto compila los 3 nuevos servicios:**
- RewardsService.java
- MissionProgressService.java  
- AchievementsDAO.java

### PASO 3: Ejecutar Aplicación

1. Iniciar la app (login normal)
2. Esperar a que cargue HomeController
3. El monitoreo se inicia automáticamente

---

## 🧪 Prueba del Sistema en Tiempo Real

### Verificación Básica (Sin cambios en DB)

1. **Verificar que el monitoreo inició:**
   - Consola debe mostrar: `✅ Sesión BD iniciada: ID X`
   - Seguido de: `✅ Se inicializaron 8 misiones para usuario 1`

2. **Abrir VSCode**
   - Esperar 2 segundos
   - Consola debe mostrar progresión de misiones

3. **Verificar cada 10 segundos:**
   ```sql
   SELECT current_value, progress_percentage 
   FROM mission_progress 
   WHERE user_id = 1 
   ORDER BY mission_id DESC LIMIT 1;
   ```
   - El `current_value` debe incrementar de 1 en 1

### Verificación Avanzada (Forzar Completación)

1. **Completar una misión manualmente** (para testing):
   ```sql
   -- Buscar ID de misión incompleta
   SELECT id, title FROM missions 
   WHERE user_id = 1 AND completed = false LIMIT 1;
   
   -- Completarla:
   UPDATE mission_progress 
   SET current_value = target_value 
   WHERE mission_id = 123;  -- Reemplazar 123 por ID real
   ```

2. **En el siguiente tick del monitor (~1 segundo):**
   - Consola mostrará: `✅ [MissionProgressService] Misión Completada: Coder por 1 Hora | +XP: 50 | +Monedas: 100`
   - Base de datos actualizará: `missions.completed = true`
   - Usuario recibirá: +50 XP + 100 monedas

3. **Verificar recompensas otorgadas:**
   ```sql
   SELECT username, level, current_xp, total_xp, coins 
   FROM users WHERE id = 1;
   ```

### Verificación de Logros

1. **Ver logros desbloqueados:**
   ```sql
   SELECT a.name, ua.unlocked_at 
   FROM user_achievements ua
   JOIN achievements a ON ua.achievement_id = a.id
   WHERE ua.user_id = 1
   ORDER BY ua.unlocked_at DESC;
   ```

2. **Ver transacciones de monedas:**
   ```sql
   SELECT reason, amount, created_at 
   FROM coin_transactions
   WHERE user_id = 1
   ORDER BY created_at DESC;
   ```

---

## ⚙️ Configuración y Personalización

### Cambiar XP otorgado por segundo

**Archivo:** `RewardsService.java` - Línea ~60
```java
// Cambiar de 1 a X
int xpAwarded = 1;  // ← Modificar aquí

// Ejemplo: 10 XP por segundo
int xpAwarded = 10;
```

### Cambiar XP requerido para level-up

**Archivo:** `RewardsService.java` - Línea ~98
```java
// Cambiar de nivel * 100
int xpRequiredForNext = (currentLevel + 1) * 100;

// Ejemplo: nivel * 200
int xpRequiredForNext = (currentLevel + 1) * 200;
```

### Agregar nuevas categorías de apps

**Archivo:** `MissionProgressService.java` - Método `categorizeApp()`
```java
private String categorizeApp(String appName, boolean isProductive) {
    // Agregar nuevo caso:
    if (lower.contains("myapp")) {
        return "MY_CATEGORY";
    }
    // ...
}
```

---

## 📋 Checklist de Validación

- [ ] Script SQL ejecutado sin errores
- [ ] Maven compile ejecutado exitosamente  
- [ ] Aplicación inicia sin excepciones
- [ ] Console muestra: "Sesión BD iniciada"
- [ ] Console muestra: "Se inicializaron X misiones"
- [ ] Abrir VSCode → console actualiza misiones
- [ ] Cada 10 segundos → XP incrementa en UI
- [ ] Completar misión manualmente → recompensas otorgadas
- [ ] Verificar en BD que misiones tienen `progress_percentage` incrementando
- [ ] Verificar en BD que `users.total_xp` incrementa
- [ ] Verificar en BD que logros se desbloquean automáticamente

---

## 🐛 Troubleshooting

### Consola muestra error: "ON CONFLICT no coincide"
- Solución: Asegurarse de haber ejecutado `FIX_ERRORES_JAVA.sql` primero

### Las misiones no avanzan
- Verificar: ¿Está VSCode en foco? (debe detectar nombre en ventana activa)
- Verificar: Console debe mostrar app name cada 1 segundo
- Verificar: Conexión a BD activa

### XP no se otorga
- Verificar: RewardsService.getInstance() está inicializado
- Verificar: isProductive retorna true para VSCode

### Logros no se desbloquean
- Verificar: Usuario tiene suficientes misiones completadas o XP acumulado
- Verificar: condition JSON en tabla achievements es válida

---

## 📈 Métricas a Monitorear

Ejecutar regularmente:

```sql
-- Dashboard de usuario
SELECT 
    u.username,
    u.level,
    u.current_xp,
    u.total_xp,
    u.coins,
    COUNT(DISTINCT m.id) as total_missions,
    COUNT(DISTINCT CASE WHEN m.completed THEN m.id END) as completed_missions,
    COUNT(DISTINCT ua.achievement_id) as achievements_unlocked
FROM users u
LEFT JOIN missions m ON u.id = m.user_id
LEFT JOIN user_achievements ua ON u.id = ua.user_id
WHERE u.id = 1
GROUP BY u.id, u.username, u.level, u.current_xp, u.total_xp, u.coins;
```

---

## 🎯 Próximas Mejoras Sugeridas

1. **UI en Tiempo Real:** Actualizar visualmente misiones mientras progresan
2. **Notificaciones:** Pop-ups cuando se completa misión o desbloquea logro
3. **Eventos:** Eventos especiales (doblar XP por N minutos, etc.)
4. **Streaks:** Sistema de rachas diarias/semanales
5. **Leaderboard:** Comparar progreso con otros usuarios

---

**✅ Sistema Listo para Pruebas**

Cualquier pregunta o problema, revisar los logs de consola primero. El sistema es completamente observable en tiempo real.
