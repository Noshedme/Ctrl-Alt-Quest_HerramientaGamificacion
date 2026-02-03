# 🧪 GUÍA DE PRUEBA - SISTEMA DE ACTIVIDADES

## Pre-Requisitos
- ✅ Aplicación compilada (ejecutar `mvn clean package`)
- ✅ BD PostgreSQL iniciada
- ✅ Misiones creadas en `public.missions` (usar script de test data si es necesario)

---

## PRUEBA 1: Inicialización de mission_progress

### Objetivo
Verificar que al hacer login, se crean automáticamente registros en `mission_progress`.

### Pasos:
1. **Abre pgAdmin o herramienta SQL**
   - Consulta ANTES de login:
   ```sql
   SELECT COUNT(*) as total_registros FROM public.mission_progress WHERE user_id = 3;
   ```
   - **Resultado esperado**: 0

2. **Ejecuta la aplicación y haz login** con usuario ID 3

3. **Inmediatamente después**, en BD consulta:
   ```sql
   SELECT COUNT(*) as total_registros FROM public.mission_progress WHERE user_id = 3;
   ```
   - **Resultado esperado**: > 0 (debe haber creado registros para cada misión activa)

4. **Verifica los detalles**:
   ```sql
   SELECT mission_id, metric_key, target_value, current_value 
   FROM public.mission_progress 
   WHERE user_id = 3 
   LIMIT 5;
   ```
   - **Debe mostrar**: misiones con metric_key ("time_coding", "time_productivity", etc.)

### ✅ Prueba exitosa si:
- mission_progress tiene registros después de login
- Cada misión activa tiene al menos una fila en mission_progress

---

## PRUEBA 2: Monitoreo y Registro de Actividades

### Objetivo
Verificar que cada segundo de actividad se registra en BD.

### Pasos:
1. **Abre la consola** (ver output de la aplicación)
   - Debe ver messages como:
   ```
   ⚡ ActivityMonitor: Iniciado para usuario 3
   ✅ coin_transaction registrada: usuario=3, coins=...
   ```

2. **Abre VS Code** (o cualquier IDE) durante 10 segundos

3. **Consulta app_usage_logs en BD**:
   ```sql
   SELECT COUNT(*) as total_ticks 
   FROM public.app_usage_logs 
   WHERE user_id = 3 AND metric_key = 'time_coding';
   ```
   - **Resultado esperado**: ~10 (uno por segundo)

4. **Verifica detalles de registros**:
   ```sql
   SELECT app_name, metric_key, timestamp 
   FROM public.app_usage_logs 
   WHERE user_id = 3 
   ORDER BY timestamp DESC 
   LIMIT 10;
   ```
   - **Debe mostrar**: "Visual Studio Code", "time_coding", timestamp reciente

### ✅ Prueba exitosa si:
- app_usage_logs tiene registros
- metric_key es correcto para cada aplicación
- timestamps son recientes

---

## PRUEBA 3: Actualización de mission_progress

### Objetivo
Verificar que la actividad detectada actualiza el progreso de misiones.

### Pasos:
1. **Anota el valor actual de una misión**:
   ```sql
   SELECT current_value, target_value 
   FROM public.mission_progress 
   WHERE user_id = 3 AND metric_key = 'time_coding' 
   LIMIT 1;
   ```
   - **Anota**: current_value (ej: 0)

2. **Usa una herramienta de programación** (VS Code, IDE, etc.) durante 30 segundos

3. **Consulta el valor nuevamente**:
   ```sql
   SELECT current_value, target_value 
   FROM public.mission_progress 
   WHERE user_id = 3 AND metric_key = 'time_coding' 
   LIMIT 1;
   ```
   - **Resultado esperado**: current_value ≈ 30 (incrementó en ~1 segundo)

4. **Verifica el porcentaje en missions**:
   ```sql
   SELECT title, progress 
   FROM public.missions 
   WHERE user_id = 3 AND completed = false 
   LIMIT 1;
   ```
   - **Debe mostrar**: progress entre 0-100% (calculado automáticamente)

### ✅ Prueba exitosa si:
- current_value incrementa con la actividad
- mission_progress y missions.progress están sincronizados

---

## PRUEBA 4: Recompensas y coin_transactions

### Objetivo
Verificar que al completar misiones, se otorgan monedas y se registran.

### Pasos:
1. **Edita una misión para hacerla trivial** (opcional, para testing):
   ```sql
   UPDATE public.missions 
   SET xp_reward = 50, coin_reward = 20 
   WHERE id = (SELECT mission_id FROM public.mission_progress WHERE user_id = 3 LIMIT 1);
   ```

2. **Reduce el target de una misión** para completarla rápido:
   ```sql
   UPDATE public.mission_progress 
   SET target_value = 5 
   WHERE user_id = 3 AND metric_key = 'time_coding' 
   LIMIT 1;
   ```

3. **Usa IDE durante 10 segundos** para completar la misión

4. **Verifica coin_transactions**:
   ```sql
   SELECT user_id, amount, reason, timestamp 
   FROM public.coin_transactions 
   WHERE user_id = 3 
   ORDER BY timestamp DESC 
   LIMIT 1;
   ```
   - **Resultado esperado**: coins = 20, reason = 'Misión Completada'

5. **Verifica xp_history**:
   ```sql
   SELECT user_id, amount, reason, timestamp 
   FROM public.xp_history 
   WHERE user_id = 3 
   ORDER BY timestamp DESC 
   LIMIT 1;
   ```
   - **Resultado esperado**: amount = 50, reason = 'Misión Completada'

6. **Verifica users actualizado**:
   ```sql
   SELECT coins, total_xp, current_xp, level 
   FROM public.users 
   WHERE id = 3;
   ```
   - **Resultado esperado**: coins aumentó en 20, total_xp aumentó en 50

### ✅ Prueba exitosa si:
- coin_transactions tiene registros
- xp_history tiene registros
- users.coins y users.total_xp aumentaron

---

## PRUEBA 5: Level-up

### Objetivo
Verificar que cuando se acumula suficiente XP, ocurre un level-up.

### Pasos:
1. **Anota nivel actual**:
   ```sql
   SELECT level, current_xp, total_xp 
   FROM public.users 
   WHERE id = 3;
   ```
   - **Anota**: level, current_xp

2. **Otorga XP suficiente para subir** (asumiendo que necesita nivel * 1000):
   ```sql
   -- Si está en nivel 1, necesita 1000 XP para nivel 2
   UPDATE public.users 
   SET current_xp = 980 
   WHERE id = 3;
   ```

3. **Completa una misión que otorgue 50 XP**:
   ```sql
   UPDATE public.mission_progress 
   SET target_value = 1 
   WHERE user_id = 3 
   LIMIT 1;
   ```

4. **Usa IDE durante 5 segundos** para trigger misión completada

5. **Verifica en consola**: Debe ver mensaje:
   ```
   🎉 ¡SUBIDA DE NIVEL! Nuevo nivel: 2
   ```

6. **Verifica BD**:
   ```sql
   SELECT level, current_xp, total_xp 
   FROM public.users 
   WHERE id = 3;
   ```
   - **Resultado esperado**: level = 2, current_xp menor a 1000, total_xp aumentó

7. **En UI**: Debe haber popup "¡LEVEL UP!" con sonido

### ✅ Prueba exitosa si:
- Level aumentó en consola
- BD muestra nuevo level
- UI mostró notificación

---

## PRUEBA 6: Flujo Completo End-to-End

### Objetivo
Ejecutar ciclo completo: actividad → misión → recompensa → level-up

### Pasos:
1. **Prepara datos de test**:
   ```sql
   -- Reset para testing limpio
   DELETE FROM public.mission_progress WHERE user_id = 3;
   
   -- Crear misión de test
   INSERT INTO public.missions 
   (user_id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly)
   VALUES 
   (3, 'Test: Programar 10 Segundos', 'programming', 'easy', 100, 50, true, false);
   
   -- Obtener ID de misión creada
   SELECT id FROM public.missions WHERE title = 'Test: Programar 10 Segundos';
   ```

2. **Reinicia la aplicación** (para que reinicialice mission_progress)

3. **Edita el mission_progress creado**:
   ```sql
   UPDATE public.mission_progress 
   SET target_value = 10 
   WHERE user_id = 3 AND metric_key = 'time_coding';
   ```

4. **Abre VS Code durante 15 segundos**

5. **Verifica la cadena completa**:
   ```sql
   -- Misión completada
   SELECT completed, progress FROM public.missions WHERE title = 'Test: Programar 10 Segundos';
   
   -- Recompensas registradas
   SELECT * FROM public.xp_history WHERE user_id = 3 ORDER BY created_at DESC LIMIT 1;
   SELECT * FROM public.coin_transactions WHERE user_id = 3 ORDER BY created_at DESC LIMIT 1;
   
   -- Usuario actualizado
   SELECT coins, total_xp, current_xp, level FROM public.users WHERE id = 3;
   ```

### ✅ Prueba exitosa si:
- Misión marcada como completed = true
- xp_history tiene registro de recompensa
- coin_transactions tiene registro de monedas
- users.coins y users.total_xp incrementaron

---

## 🔧 Debugging si algo falla

### Si mission_progress no se crea:
```sql
-- Verificar logs de console
-- Buscar: "✅ Se inicializaron X misiones para usuario 3"

-- Verificar que existan misiones
SELECT * FROM public.missions WHERE user_id = 3 AND completed = false;

-- Verificar que no haya errores en BD
-- Revisar permisos de tabla mission_progress
```

### Si app_usage_logs no tiene registros:
```sql
-- Verificar en consola que ActivityMonitor inició:
-- "⚡ ActivityMonitor: Iniciado para usuario 3"

-- Si hay error de SQL, verificar que app_usage_logs exista:
SELECT EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_schema = 'public' 
               AND table_name = 'app_usage_logs');

-- Si falta tabla, crearla:
CREATE TABLE public.app_usage_logs (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES public.users(id),
    app_name VARCHAR(255),
    metric_key VARCHAR(50),
    duration_seconds INT DEFAULT 1,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Si coin_transactions no se registra:
```sql
-- Verificar en consola:
-- "✅ coin_transaction registrada: usuario=3, coins=50"

-- Si tabla falta, crearla:
CREATE TABLE public.coin_transactions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES public.users(id),
    amount INT NOT NULL,
    reason VARCHAR(255),
    transaction_type VARCHAR(50) DEFAULT 'REWARD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📊 Resumen de Verificación

| Prueba | Esperado | Resultado | ✅/❌ |
|--------|----------|-----------|-------|
| mission_progress se crea | > 0 registros | | |
| app_usage_logs registra | ~10 por min | | |
| mission_progress actualiza | current_value aumenta | | |
| coin_transactions se registra | Registros en BD | | |
| xp_history se registra | Registros en BD | | |
| level-up ocurre | mensaje en consola | | |
| Completo: actividad → recompensa | Todos los anteriores | | |

---

## Conclusión

Si todas las 6 pruebas pasan ✅, el sistema está completamente funcional y listo para jugar.

**Tiempo de pruebas**: ~15 minutos
**Complejidad**: Baja (es principalmente consultas SQL)

