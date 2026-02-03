# 🛠️ PLAN DE IMPLEMENTACIÓN - SISTEMA DE ACTIVIDADES

## PASO 1: Inicializar mission_progress (CRÍTICO)

### Problema:
Cuando se cargan misiones, no existen registros en `mission_progress`. Sin estos, `MissionsDAO.actualizarProgreso()` no puede actualizar nada.

### Solución:
Crear método en `MissionsDAO` que inicialice `mission_progress` para cada misión.

### Código a agregar en MissionsDAO.java:

```java
/**
 * Inicializa las filas de seguimiento en mission_progress.
 * Se debe llamar cuando se carga una misión por primera vez.
 * 
 * Crea una fila por cada métrica relevante para la misión.
 * Por ejemplo, para "Programa 1 hora", crea:
 *   - metric_key="time_coding", target_value=3600 (1 hora en segundos)
 */
public static void inicializarMisionProgress(int userId, int missionId, String category) {
    // Determinar qué métrica(s) usa esta misión basándose en su categoría
    String metricKey = mapearCategoriaAMetrica(category);
    
    // Obtener el target value basándose en la misión (simplificado)
    long targetValue = obtenerTargetValue(missionId);
    
    String sqlInsert = "INSERT INTO public.mission_progress " +
                       "(user_id, mission_id, metric_key, current_value, target_value, progress_percentage) " +
                       "VALUES (?, ?, ?, 0, ?, 0) " +
                       "ON CONFLICT (user_id, mission_id, metric_key) DO NOTHING";
    
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
        
        pstmt.setInt(1, userId);
        pstmt.setInt(2, missionId);
        pstmt.setString(3, metricKey);
        pstmt.setLong(4, targetValue);
        pstmt.executeUpdate();
        
        System.out.println("✅ mission_progress inicializado: usuario=" + userId + 
                          ", misión=" + missionId + ", métrica=" + metricKey);
        
    } catch (SQLException e) {
        System.err.println("❌ Error inicializando mission_progress: " + e.getMessage());
        e.printStackTrace();
    }
}

/**
 * Mapea la categoría de misión a una clave de métrica.
 */
private static String mapearCategoriaAMetrica(String category) {
    if (category == null) return "app_usage_generic";
    
    String lower = category.toLowerCase();
    if (lower.contains("program") || lower.contains("code") || lower.contains("cód")) {
        return "time_coding";
    } else if (lower.contains("product") || lower.contains("produc")) {
        return "time_productivity";
    } else if (lower.contains("brows") || lower.contains("naveg")) {
        return "time_browsing";
    }
    return "app_usage_generic";
}

/**
 * Obtiene el target value para una misión.
 * Simplificado: asume que el 100% = 3600 segundos (1 hora)
 * Puedes hacerlo más sofisticado leyendo desde BD.
 */
private static long obtenerTargetValue(int missionId) {
    // Por ahora, asumimos 1 hora (3600 segundos) como target estándar
    // En el futuro, podrías tener una columna en missions.target_seconds
    return 3600; // 1 hora
}
```

### Dónde llamarlo:

En **MissionsViewController.java**, después de cargar cada misión:

```java
// En cargarMisionesReales(), después del while (rs.next()):
// Inicializar mission_progress para esta misión si no existe
MissionsDAO.inicializarMisionProgress(userId, missionId, category);
```

O mejor aún, en **HomeController.initPlayerData()** cuando se inicia una sesión:

```java
public void initPlayerData(Character character) {
    // ... código existente ...
    
    // Inicializar mission_progress para todas las misiones del usuario
    List<Mission> missions = MissionsDAO.getMisionesUsuario(userId);
    for (Mission m : missions) {
        MissionsDAO.inicializarMisionProgress(userId, m.getId(), m.getCategory());
    }
    
    // ... resto del código ...
}
```

---

## PASO 2: Registrar coin_transactions (CRÍTICO)

### Problema:
Cuando se otorgan monedas, solo se actualiza `users.coins`, pero no se registra en `coin_transactions`.

### Solución:
Agregar INSERT en `UserDAO.otorgarRecompensas()`.

### Código a modificar en UserDAO.java:

En el método `otorgarRecompensas()`, agregar después de actualizar coins:

```java
// Después de la actualización de usuarios, agregar:

// 5. Log Historial de Monedas
String sqlLogCoins = "INSERT INTO public.coin_transactions " +
                     "(user_id, amount, reason, transaction_type) " +
                     "VALUES (?, ?, 'Misión Completada', 'REWARD')";

try (PreparedStatement pstLogCoins = conn.prepareStatement(sqlLogCoins)) {
    pstLogCoins.setInt(1, userId);
    pstLogCoins.setInt(2, coins);
    pstLogCoins.executeUpdate();
    System.out.println("✅ coin_transaction registrada: usuario=" + userId + 
                      ", coins=" + coins);
} catch (SQLException e) {
    System.err.println("⚠️ Error registrando coin_transaction: " + e.getMessage());
    // No es crítico, continuar
}
```

---

## PASO 3: Registrar actividades en app_usage_logs (RECOMENDADO)

### Problema:
No hay historial de qué aplicaciones usó el usuario.

### Solución:
Extender `ActivityDAO` y llamarlo desde `ActivityMonitorService`.

### Código a agregar en ActivityDAO.java:

```java
/**
 * Registra un tick de actividad en app_usage_logs.
 * Se llama cada 1 segundo desde ActivityMonitorService.
 */
public static void registrarActividad(int userId, String appName, String metricKey) {
    String sql = "INSERT INTO public.app_usage_logs " +
                 "(user_id, app_name, metric_key, duration_seconds, timestamp) " +
                 "VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)";
    
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setInt(1, userId);
        pstmt.setString(2, appName);
        pstmt.setString(3, metricKey);
        pstmt.executeUpdate();
        
    } catch (SQLException e) {
        // Log silencioso para no saturar console
        // System.err.println("Error registrando actividad: " + e.getMessage());
    }
}
```

### Código a modificar en ActivityMonitorService.java:

En el método `reportActivity()`, agregar:

```java
private void reportActivity() {
    String currentApp = getActiveWindowTitle();
    String metricKey = categorizeActivity(currentApp);

    if (metricKey != null && !metricKey.equals("unknown")) {
        // Registrar en BD
        ActivityDAO.registrarActividad(currentUserId, currentApp, metricKey);
        
        // Procesar en GameService
        GameService.getInstance().processActivityEvent(currentUserId, metricKey, 1);
    }
}
```

---

## PASO 4: Mejorar MissionsDAO para manejar mission_progress automáticamente

### Problema:
La inicialización debe ser automática y confiable.

### Solución:
Modificar `actualizarProgreso()` para crear registros faltantes automáticamente.

```java
public static List<Integer> actualizarProgreso(int userId, String metricKey, int amountToAdd) {
    List<Integer> completedMissions = new ArrayList<>();
    
    // 1. Buscar misiones activas que usen esta métrica
    String sqlSelect = "SELECT mp.id, mp.mission_id, mp.current_value, mp.target_value " +
                       "FROM public.mission_progress mp " +
                       "JOIN public.missions m ON mp.mission_id = m.id " +
                       "WHERE mp.user_id = ? AND mp.metric_key = ? AND m.completed = false";
    
    String sqlInsertMissing = "INSERT INTO public.mission_progress " +
                              "(user_id, mission_id, metric_key, current_value, target_value) " +
                              "SELECT ?, m.id, ?, 0, 3600 FROM public.missions m " +
                              "WHERE m.user_id = ? AND m.completed = false " +
                              "AND NOT EXISTS (SELECT 1 FROM public.mission_progress mp " +
                              "               WHERE mp.mission_id = m.id AND mp.metric_key = ?)";

    try (Connection conn = DatabaseConnection.getConnection()) {
        // Crear mission_progress faltantes
        try (PreparedStatement pstInsert = conn.prepareStatement(sqlInsertMissing)) {
            pstInsert.setInt(1, userId);
            pstInsert.setString(2, metricKey);
            pstInsert.setInt(3, userId);
            pstInsert.setString(4, metricKey);
            int inserted = pstInsert.executeUpdate();
            if (inserted > 0) {
                System.out.println("✅ Creados " + inserted + " registros faltantes en mission_progress");
            }
        }
        
        // ... resto del método igual ...
```

---

## RESUMEN DE CAMBIOS

| Archivo | Cambio | Líneas | Prioridad |
|---------|--------|--------|-----------|
| MissionsDAO.java | Agregar `inicializarMisionProgress()` | +40 | 🔴 CRÍTICO |
| UserDAO.java | Agregar `coin_transactions` INSERT | +15 | 🔴 CRÍTICO |
| ActivityDAO.java | Agregar `registrarActividad()` | +20 | 🟡 RECOMENDADO |
| ActivityMonitorService.java | Llamar `registrarActividad()` | +1 | 🟡 RECOMENDADO |
| HomeController.java | Inicializar mission_progress en login | +4 | 🔴 CRÍTICO |

**Total de líneas de código nuevo: ~80 líneas**

---

## EJECUCIÓN RECOMENDADA

### Fase 1 (30 minutos) - Crítico:
1. Implementar `inicializarMisionProgress()` en MissionsDAO
2. Llamarlo desde HomeController.initPlayerData()
3. Agregar coin_transactions a UserDAO
4. **Probar**: Ejecutar app, verificar que mission_progress se popula

### Fase 2 (20 minutos) - Recomendado:
5. Implementar `registrarActividad()` en ActivityDAO
6. Llamarlo desde ActivityMonitorService
7. **Verificar**: app_usage_logs tenga datos

### Fase 3 (Testing):
8. Ejecutar prueba end-to-end:
   - Actividad detectada ✅
   - mission_progress actualizado ✅
   - Misión completa ✅
   - XP otorgado ✅
   - xp_history registrado ✅
   - coin_transactions registrado ✅
   - Level-up si aplica ✅

