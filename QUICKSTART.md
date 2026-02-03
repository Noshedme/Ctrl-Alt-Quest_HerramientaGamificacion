# ⚡ QUICKSTART - LO QUE DEBES HACER AHORA

## 🎯 En 5 Minutos

### 1. Compilar el Proyecto
```bash
cd c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend
mvn clean package
```

**Resultado esperado**: `BUILD SUCCESS`

### 2. Asegúrate de que PostgreSQL está corriendo
```bash
# Verificar que la BD está conectada
# (la aplicación lo hará automáticamente)
```

### 3. Ejecutar la Aplicación
```bash
java -jar target/CtrlAltQuest.jar
```

**O desde IDE**: Run la clase principal (probablemente `Application.java` o similar)

### 4. Hacer Login
- Ingresa credenciales válidas
- El sistema **automáticamente**:
  - ✅ Inicia sesión en BD
  - ✅ Crea mission_progress
  - ✅ Inicia monitoreo de actividades

### 5. Ver Misiones
- Navega a la sección "Crónicas & Encargos"
- Deberías ver las misiones cargadas

### 6. Generar Actividad
- Abre Visual Studio Code o tu IDE favorito
- Escribe código o simplemente mantén abierto
- **Cada segundo**:
  - ✅ Sistema detecta actividad
  - ✅ Misión progresa automáticamente
  - ✅ Si completa → XP + Monedas automáticas

---

## 🔧 Si Algo Falla

### "No veo misiones"
1. Verificar que `public.missions` tiene datos:
```sql
SELECT COUNT(*) FROM public.missions WHERE user_id = 3;
```

2. Si está vacía, ejecutar:
```sql
-- Ver el archivo MISIONES_TEST_DATA.sql
-- Y ejecutar el script SQL
```

### "Mission_progress no se crea"
1. Revisar consola para errores
2. Verificar que la tabla existe:
```sql
SELECT EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_name = 'mission_progress');
```

### "Las misiones no avanzan"
1. Verificar que AS IDE está abierto (VS Code, IntelliJ, etc)
2. Revisar que la ventana está activa (foco del mouse)
3. Esperar 10+ segundos
4. Consultar:
```sql
SELECT current_value, target_value FROM public.mission_progress 
WHERE user_id = 3 LIMIT 1;
```

### "No tengo XP después de completar misión"
1. Verificar que la misión llegó a 100%:
```sql
SELECT progress FROM public.missions WHERE id = (SELECT mission_id FROM public.mission_progress WHERE user_id = 3 LIMIT 1);
```

2. Si está en 100%, verificar que se otorgó:
```sql
SELECT * FROM public.xp_history WHERE user_id = 3 ORDER BY created_at DESC LIMIT 1;
```

---

## 📊 Verificación Completa (2 minutos)

Para verificar que TODO funciona:

```sql
-- 1. ¿Se crea mission_progress?
SELECT COUNT(*) FROM public.mission_progress WHERE user_id = 3;

-- 2. ¿Se registran actividades?
SELECT COUNT(*) FROM public.app_usage_logs WHERE user_id = 3;

-- 3. ¿Se registran monedas?
SELECT COUNT(*) FROM public.coin_transactions WHERE user_id = 3;

-- 4. ¿Se registra XP?
SELECT COUNT(*) FROM public.xp_history WHERE user_id = 3;

-- 5. ¿Usuario tiene datos?
SELECT coins, level, total_xp FROM public.users WHERE id = 3;
```

**Si todos retornan valores > 0, TODO FUNCIONA ✅**

---

## 📚 Documentación (Si necesitas entender en detalle)

| Documento | Cuándo leer |
|-----------|------------|
| **SISTEMA_COMPLETADO.md** | Para entender qué se implementó |
| **GUIA_PRUEBA_SISTEMA.md** | Para pruebas detalladas (6 pruebas) |
| **ANALISIS_SISTEMA_COMPLETO.md** | Para entender flujo completo |
| **CAMBIOS_IMPLEMENTADOS.md** | Para ver código exacto de cada cambio |

---

## ✅ Checklist de Cambios Implementados

- [x] MissionsDAO - Inicializar mission_progress
- [x] UserDAO - Registrar coin_transactions
- [x] ActivityDAO - Registrar actividades
- [x] ActivityMonitorService - Integración de logging
- [x] HomeController - Inicializar en login
- [x] Compilación exitosa
- [x] Documentación completa

---

## 🎮 Cómo Funciona Ahora (Resumen)

```
LOGIN
  ↓
AUTOMÁTICAMENTE se crean misiones
  ↓
CADA SEGUNDO se detecta actividad
  ↓
MISIÓN PROGRESA automáticamente
  ↓
CUANDO LLEGA A 100%
  ↓
XP + MONEDAS otorgadas
  ↓
SI HAY LEVEL-UP
  ↓
NOTIFICACIÓN ESPECIAL + SONIDO
```

---

## 💡 Tips

1. **Para testing rápido**: Reduce el target de una misión a 10 segundos:
```sql
UPDATE public.mission_progress SET target_value = 10 WHERE user_id = 3 LIMIT 1;
```

2. **Para ver logs**: Abre consola/terminal y busca messages como:
   - `✅ mission_progress inicializado`
   - `✅ coin_transaction registrada`

3. **Para resetear todo**:
```sql
DELETE FROM public.mission_progress WHERE user_id = 3;
DELETE FROM public.coin_transactions WHERE user_id = 3;
DELETE FROM public.app_usage_logs WHERE user_id = 3;
```

4. **Para ver progreso en tiempo real**:
```sql
SELECT current_value, target_value, ROUND(100.0 * current_value / target_value, 1) as percent
FROM public.mission_progress WHERE user_id = 3 LIMIT 1;
```

---

## 🚀 Ya Está Listo

No hay más pasos. El sistema está **100% implementado y compilado**.

Solo necesitas:
1. ✅ Compilar (mvn clean package)
2. ✅ Ejecutar (java -jar ...)
3. ✅ ¡A jugar! 🎮

---

**¡Disfruta tu sistema de gamificación completamente funcional!**

