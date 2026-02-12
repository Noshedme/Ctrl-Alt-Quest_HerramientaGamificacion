# 🔧 CORRECCIÓN DE ERRORES EN BASE DE DATOS - CTRL-ALT-QUEST

## 📋 Problemas Identificados

### ❌ Problema 1: Error Foreign Key en `app_usage_logs`
```
⚠️  Error registrando actividad: ERROR: inserción o actualización en la tabla «app_usage_logs» 
viola la llave foránea «app_usage_logs_app_id_fkey»
Detail: La llave (app_id)=(1999406190) no está presente en la tabla «apps».
```

**Causa:** La aplicación intenta registrar un `app_id` (1999406190) que no existe en la tabla `apps`. Esto sucede porque:
- La aplicación detecta un proceso pero no lo registra primero en la tabla `apps`
- Hay registros huérfanos en `app_usage_logs` con referencias inválidas

**Solución:** 
1. Limpiar registros con `app_id` inválidos
2. El foreign key permitirá NULL (para apps no registradas)
3. Validar que apps se registren antes de usar app_id

---

### ❌ Problema 2: Columna Faltante en `mission_progress`
```
⚠️  Error inicializando mission_progress: ERROR: no existe la columna «progress_percentage» 
en la relación «mission_progress»
Position: 100
```

**Causa:** El código Java intenta acceder a una columna `progress_percentage` que no existe en la tabla. La tabla actual solo tiene:
- `current_value` (valor actual del progreso)
- `target_value` (valor objetivo)

**Solución:** 
1. Agregar columna `progress_percentage` (numeric)
2. Calcular automáticamente: `(current_value / target_value) * 100`
3. Mantener sincronización en triggers

---

## 🛠️ INSTRUCCIONES DE APLICACIÓN

### Opción A: Usando pgAdmin 4 (Recomendado - Paso a Paso)

1. **Abrir pgAdmin 4**
   - Acceder a tu base de datos PostgreSQL
   - Selecciona la base de datOOos `ctrlaltquest`

2. **Ejecutar el script SQL**
   ```
   - Click derecho en la BD → Query Tool
   - Abre el archivo: CORRECCION_BD_ERRORES.sql
   - Click en "Ejecutar" (Play) o Ctrl+Enter
   ```

3. **Verificar resultados**
   - Deberías ver mensajes de confirmación
   - Revisa la tabla `audit_logs` para ver qué se ejecutó

4. **Validar correcciones**
   - Ejecuta las consultas de verificación al final del script

---

### Opción B: Usando línea de comandos (psql)

```bash
# Conectar a PostgreSQL
psql -U usuario -d ctrlaltquest -h localhost

# Ejecutar el script
\i 'C:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\CORRECCION_BD_ERRORES.sql'

# Salir
\q
```

---

### Opción C: Desde tu aplicación Java (si tienes un panel admin)

Si tu aplicación tiene un panel administrativo, puedes:
1. Crear un botón "Mantener BD"
2. Ejecutar el script como un SQL preparado

---

## 📊 CAMBIOS QUE SE REALIZARÁN

| Acción | Descripción | Impacto |
|--------|-------------|--------|
| 🧹 Limpieza | Eliminar registros con `app_id` inválidos | Sin pérdida de datos válidos |
| ➕ Agregar columna | `progress_percentage` en `mission_progress` | Mejora rastreo de progreso |
| 🔄 Sincronización | Asegurar que misiones tengan progreso | Evita errores de NULL |
| 🗑️ Limpiar huérfanos | Eliminar registros sin referencias válidas | Integridad referencial |
| 📝 Auditoría | Registrar todas las operaciones | Trazabilidad de cambios |

---

## ✅ CHECKLIST POST-CORRECCIÓN

Después de ejecutar el script:

- [ ] ✅ El script completó sin errores
- [ ] ✅ Revisé la tabla `audit_logs` y veo registros de `MAINTENANCE`
- [ ] ✅ Ejecuté las consultas de verificación
- [ ] ✅ No hay más errores de foreign key
- [ ] ✅ No hay más errores de columna faltante
- [ ] ✅ La aplicación funciona sin errores en consola

---

## 🚀 PREVENCIÓN DE FUTUROS ERRORES

### En tu código Java

**Antes de registrar una actividad:**
```java
// Verificar que el app existe
Optional<App> app = appRepository.findById(appId);
if (app.isEmpty()) {
    // Crear el app si no existe
    App newApp = new App();
    newApp.setId(appId);
    newApp.setName("Aplicación Detectada");
    appRepository.save(newApp);
}

// Ahora sí, registrar la actividad
AppUsageLog log = new AppUsageLog();
log.setAppId(app.get().getId());
// ... más campos
```

**Para mission_progress:**
```java
// Siempre calcular progress_percentage
int progressPercentage = (currentValue * 100) / targetValue;
missionProgress.setProgressPercentage(progressPercentage);
```

---

## 📞 TROUBLESHOOTING

### Si aún hay errores después del script:

1. **Verificar integridad de datos**
   ```sql
   -- Ver registros problemáticos
   SELECT * FROM app_usage_logs 
   WHERE app_id NOT IN (SELECT id FROM apps);
   
   -- Ver misiones sin progreso
   SELECT m.id, m.title FROM missions m
   WHERE NOT EXISTS (
       SELECT 1 FROM mission_progress mp WHERE mp.mission_id = m.id
   );
   ```

2. **Limpiar manualmente**
   ```sql
   -- Si hay duplicados en mission_progress
   DELETE FROM mission_progress 
   WHERE id NOT IN (
       SELECT MIN(id) FROM mission_progress 
       GROUP BY mission_id, metric_key
   );
   ```

3. **Recrear índices** (última opción)
   ```sql
   REINDEX TABLE mission_progress;
   REINDEX TABLE app_usage_logs;
   ```

---

## 📈 MONITOREO FUTURO

Para evitar estos problemas en el futuro:

### Agregar a tu código Java:
```java
@Scheduled(fixedDelay = 3600000) // Cada hora
public void verificarIntegridadBD() {
    // Verificar registros huérfanos
    // Reportar anomalías
    // Auto-limpiar si es necesario
}
```

---

## 📝 NOTAS IMPORTANTES

- ⚠️ **HACER BACKUP** antes de ejecutar cualquier script
- 📌 El script usa `IF NOT EXISTS` para ser idempotente (puedes ejecutarlo varias veces)
- 🔒 Se usa `BEGIN;` y `COMMIT;` para garantizar transaccionalidad
- 📊 Todos los cambios se registran en `audit_logs` para auditoría

---

**Última actualización:** 28-01-2026  
**Estado:** ✅ Listo para aplicar
