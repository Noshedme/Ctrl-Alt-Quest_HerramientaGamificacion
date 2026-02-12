# 🎯 GUÍA RÁPIDA - FIX ERRORES BD EN 5 PASOS

## TL;DR (Too Long; Didn't Read)

Tu aplicación tiene 2 errores en la base de datos:
1. ❌ **app_usage_logs** intenta guardar un `app_id` que no existe en tabla `apps`
2. ❌ **mission_progress** busca una columna `progress_percentage` que no existe

**Solución:** Ejecuta 3 archivos SQL que creé para ti.

---

## ⚡ FIX EN 5 MINUTOS

### Paso 1: Backup (1 min)
```bash
# Windows PowerShell
pg_dump -U usuario -d ctrlaltquest > backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql

# Linux/Mac
pg_dump -U usuario -d ctrlaltquest > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Paso 2: Ejecutar Script Principal (2 min)
```
1. Abrir pgAdmin 4
2. Right click en base de datos → Query Tool
3. Abrir archivo: CORRECCION_BD_ERRORES.sql
4. Click en Play/Ejecutar
5. Esperar a que termine (sin errores)
```

### Paso 3: Ejecutar Script de Schema (1 min)
```
1. pgAdmin 4 → Query Tool
2. Abrir archivo: ESQUEMA_BD_ACTUALIZADO.sql
3. Ejecutar
```

### Paso 4: Verificar (1 min)
```
1. pgAdmin 4 → Query Tool
2. Abrir archivo: VERIFICACION_RAPIDA_BD.sql
3. Ejecutar
4. Ver resultados (deben estar todos ✅ BIEN)
```

### Paso 5: Actualizar Java (hacerlo después)
- Leer: **RECOMENDACIONES_CODIGO_JAVA.md**
- Aplicar cambios a Entity `MissionProgress`
- Actualizar Service layer
- Compilar y testear

---

## 📂 ARCHIVOS CREADOS (En tu carpeta)

| # | Archivo | Acción | Urgencia |
|---|---------|--------|----------|
| 1 | `CORRECCION_BD_ERRORES.sql` | Ejecutar en BD | 🔴 AHORA |
| 2 | `ESQUEMA_BD_ACTUALIZADO.sql` | Ejecutar triggers | 🔴 AHORA |
| 3 | `VERIFICACION_RAPIDA_BD.sql` | Verificar que funciona | 🟠 Luego |
| 4 | `GUIA_CORRECCION_BD.md` | Leer instrucciones detalladas | 📖 Referencia |
| 5 | `RECOMENDACIONES_CODIGO_JAVA.md` | Actualizar código Java | 🟠 Después |
| 6 | `RESUMEN_ERRORES_Y_SOLUCIONES.md` | Entender problemas | 📖 Referencia |

---

## 🔧 SÍNTOMAS ANTES vs DESPUÉS

### ANTES (Ahora mismo ❌)
```
Console: 🔍 DEBUG [HomeController]: userId = 3

⚠️  Error registrando actividad: ERROR: inserción o actualización 
en la tabla «app_usage_logs» viola la llave foránea

⚠️  Error inicializando mission_progress: ERROR: no existe 
la columna «progress_percentage»
```

### DESPUÉS (Después del fix ✅)
```
Console: 🔍 DEBUG [HomeController]: userId = 3

✅ Actividad registrada para app: 1999406190
✅ Progreso de misión creado: usuario=3, misionId=5
✅ Progreso actualizado: usuario=3, misión=5, progreso=25%
```

---

## 🎯 CHECKLIST RÁPIDO

### Antes de ejecutar cualquier script:
- [ ] Hiciste BACKUP de la BD
- [ ] Cierras la aplicación Java
- [ ] Tienes acceso a pgAdmin 4
- [ ] Tienes username y password de PostgreSQL

### Después de ejecutar CORRECCION_BD_ERRORES.sql:
- [ ] Script completó SIN ERRORES
- [ ] Puedes ver registros en tabla `audit_logs`
- [ ] No hay errores de foreign key

### Después de ejecutar ESQUEMA_BD_ACTUALIZADO.sql:
- [ ] Script completó SIN ERRORES
- [ ] Columna `progress_percentage` existe (verificar con \d mission_progress)
- [ ] Triggers se crearon

### Después de ejecutar VERIFICACION_RAPIDA_BD.sql:
- [ ] Todos los checks retornan ✅ BIEN
- [ ] `issues_found` = 0 en todos los casos

### Después de cambios en Java:
- [ ] Entity MissionProgress tiene `progress_percentage`
- [ ] Service valida usuario y misión
- [ ] Código compila sin errores
- [ ] Tests pasan
- [ ] Aplicación corre sin errores en consola

---

## 🚨 SI ALGO SALE MAL

| Problema | Solución |
|----------|----------|
| Script da error de sintaxis | Verificar que copiaste el archivo completo |
| Error de permisos | Usar user `postgres` o superuser |
| Columna no aparece | Ejecutar ESQUEMA_BD_ACTUALIZADO.sql |
| Aún hay errores de foreign key | Eliminar registros inválidos manualmente |
| Java no compila | Actualizar Entity según RECOMENDACIONES_CODIGO_JAVA.md |

---

## 📊 CAMBIOS PRINCIPALES

### En Base de Datos:
```sql
-- AGREGADO: Columna en mission_progress
ALTER TABLE mission_progress 
ADD COLUMN progress_percentage numeric(5, 2) DEFAULT 0.00;

-- AGREGADO: Triggers automáticos
CREATE TRIGGER trigger_update_progress_percentage_insert
BEFORE INSERT ON mission_progress
FOR EACH ROW
EXECUTE FUNCTION update_progress_percentage();
```

### En Java Entity:
```java
@Column(name = "progress_percentage")
private Double progressPercentage = 0.0;

public void calculateProgressPercentage() {
    if (targetValue > 0) {
        this.progressPercentage = (currentValue.doubleValue() / targetValue.doubleValue()) * 100.0;
    }
}
```

### En Java Service:
```java
// VALIDAR que app existe
Optional<App> app = appRepository.findById(appId);
if (app.isEmpty()) {
    // CREAR si no existe
    App newApp = new App();
    newApp.setId(appId);
    appRepository.save(newApp);
}
```

---

## 💾 ORDEN DE EJECUCIÓN

```
1. BACKUP
   ↓
2. CORRECCION_BD_ERRORES.sql (limpia datos)
   ↓
3. ESQUEMA_BD_ACTUALIZADO.sql (agrega triggers)
   ↓
4. VERIFICACION_RAPIDA_BD.sql (valida todo)
   ↓
5. Actualizar código Java
   ↓
6. Compilar y testear
   ↓
7. ✅ DONE
```

---

## 📞 PREGUNTAS COMUNES

**P: ¿Perderé datos al ejecutar el script?**  
R: No. Solo se eliminan registros inválidos (app_id que no existen).

**P: ¿Tengo que parar la aplicación?**  
R: Sí, es recomendable mientras ejecutas los scripts.

**P: ¿Cuánto tiempo tarda?**  
R: 5-10 minutos para todo.

**P: ¿Qué pasa después?**  
R: Tu aplicación funcionará sin esos errores. Lee RECOMENDACIONES_CODIGO_JAVA.md para mejorar el código.

**P: ¿Esto es temporal?**  
R: No. Son cambios permanentes en la estructura de BD. Pero también necesitas cambios en Java para evitar que se repita.

---

## 🎓 QUÉ APRENDER

1. **Validación de Foreign Keys**: Siempre valida que existan referencias antes de guardar
2. **Triggers en PostgreSQL**: Pueden sincronizar datos automáticamente
3. **Entity-DB Sync**: Código Java y BD deben estar sincronizados
4. **Error Handling**: Maneja errores de BD correctamente

---

## 📝 PRÓXIMOS PASOS (Después del Fix)

1. ✅ Ejecutar scripts SQL (Hoy)
2. 📖 Leer RECOMENDACIONES_CODIGO_JAVA.md (Mañana)
3. 💻 Implementar cambios en Java (Esta semana)
4. 🧪 Testing completo (Antes de producción)
5. 📊 Monitoreo (Después del deploy)

---

**¿Listo? Empieza con el Paso 1: BACKUP**

---

*Creado: 28-01-2026*  
*Para: Ctrl-Alt-Quest Gamification Tool*  
*Status: Ready to Deploy* ✅
