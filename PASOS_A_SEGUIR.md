# 📋 ACCIONES A REALIZAR - SOLUCIÓN MISIONES NO VISIBLES

## ✅ LO QUE YA HEMOS HECHO

He revisado profundamente tu código y encontré que **la lógica está correcta**, pero necesitamos verificar:

### 1. **Cambios Implementados (Código)**
✅ Añadido logging DEBUG a:
- `MissionsViewController.java` → Ahora imprime cuándo se llama y qué datos recibe
- `HomeController.java` → Imprime cuándo inyecta userId
- `MissionsDAO.java` → Imprime cuántas misiones encuentra en BD

### 2. **Documentación Generada**
✅ Creados:
- `DIAGNOSTICO_MISIONES.md` → Análisis detallado de cada componente
- `MISIONES_TEST_DATA.sql` → Script SQL con datos de prueba

---

## 🎯 PRÓXIMOS PASOS (TÚ DEBES HACER ESTO)

### **PASO 1: Verificar Datos en Base de Datos**

Abre tu cliente SQL (pgAdmin, DBeaver, etc.) y ejecuta:

```sql
-- Primero, ve cuál es tu USER_ID
SELECT id, username, email FROM public.users WHERE username = '<TU_USUARIO>';
```

Anota el `id` que obtengas. Luego:

```sql
-- Verifica si tienes misiones
SELECT COUNT(*) as total_misiones 
FROM public.missions 
WHERE user_id = <REEMPLAZA_CON_TU_ID>;
```

**Posibles Resultados:**

| Resultado | Significado | Acción |
|-----------|-----------|--------|
| `0` | No hay misiones | Ejecutar `MISIONES_TEST_DATA.sql` (paso 3) |
| `> 0` | Sí hay misiones | Continuar con Paso 2 |
| Error de conexión | BD no accesible | Verificar conexión PostgreSQL |

---

### **PASO 2: Ejecutar la Aplicación con Logging**

1. Abre tu IDE (IntelliJ, Eclipse, VS Code)
2. **Compila el proyecto:**
   ```bash
   mvn clean compile
   ```
3. **Ejecuta la aplicación**
4. **Haz login** y **navega a Misiones**
5. **Abre la consola** y busca mensajes como:
   ```
   🔍 DEBUG: MissionsViewController.setUserId(1)
   🔍 DEBUG [MissionsDAO]: getMisionesUsuario(userId=1)
   🔍 DEBUG [MissionsDAO]: Total misiones encontradas: 5
   ✅ Procesando misión: Programa 1 Hora...
   ```

**Analiza los mensajes:**

| Mensaje Visto | Significado | Solución |
|---------------|-----------|----------|
| No aparecen mensajes de DEBUG | `setUserId()` nunca se llama | Revisar `SessionManager.getInstance().getUserId()` |
| `Total misiones encontradas: 0` | BD vacía | Ver Paso 3 |
| `Total misiones encontradas: 5` pero no aparecen | Problema en UI | Ver Paso 4 |
| Error SQL | Problema de conexión | Ver credentials BD |

---

### **PASO 3: Insertar Datos de Prueba**

Si tu BD está vacía:

1. Abre `MISIONES_TEST_DATA.sql` (está en tu proyecto)
2. **Reemplaza `<USER_ID>` con tu ID real** (el que obtuviste en Paso 1)
3. Selecciona la sección "2. INSERTAR MISIONES DIARIAS"
4. **Ejecuta en tu cliente SQL**
5. Ejecuta la sección "5. VERIFICAR DATOS INSERTADOS" para confirmar

**Resultado esperado:**
```
id │ user_id │ title                         │ is_daily │ is_weekly │ progress
───┼─────────┼───────────────────────────────┼──────────┼───────────┼──────────
 1 │       1 │ Programa 1 Hora...            │ true     │ false     │      50
 2 │       1 │ Lee 3 Artículos Técnicos      │ true     │ false     │      75
 3 │       1 │ Escribe 500 Palabras          │ true     │ false     │     100
 4 │       1 │ Alcanza 20 Horas...           │ false    │ true      │      60
...
```

---

### **PASO 4: Verificar Mapeo Correcto**

Si los datos existen en BD pero **aún NO ves las misiones en UI**:

**Revisar que el tipo de misión sea correcto:**

```sql
SELECT id, title, is_daily, is_weekly, progress
FROM public.missions 
WHERE user_id = <TU_ID>;
```

**Importante:**
- `is_daily = true` → Aparece en tab "DIARIAS"
- `is_weekly = true` → Aparece en tab "SEMANALES"
- `is_daily = false Y is_weekly = false` → Aparece en tab "HISTORIA DE CLASE"

**Si falta algo en tu BD:**
```sql
UPDATE public.missions 
SET is_daily = true, is_weekly = false 
WHERE id = <ID_MISION> AND user_id = <TU_ID>;
```

---

### **PASO 5: Revisar Consola para Errores**

En la consola de tu IDE, busca líneas que empiecen con:
- ❌ `ERROR`
- ⚠️ `ADVERTENCIA`
- 🔍 `DEBUG`

**Si ves errores**, cópialos y pégalos aquí para análisis profundo.

---

## 🔧 COMPONENTES VERIFICADOS

| Componente | Estado | Notas |
|-----------|--------|-------|
| **models/Mission.java** | ✅ OK | Getters correctos |
| **dao/MissionsDAO.java** | ✅ OK + DEBUG | Query SQL correcta, logging añadido |
| **ui/controllers/views/MissionsViewController.java** | ✅ OK + DEBUG | Lógica correcta, logging añadido |
| **ui/controllers/HomeController.java** | ✅ OK + DEBUG | Inyección correcta, logging añadido |
| **fxml/views/missions_view.fxml** | ✅ OK | Contenedores definidos correctamente |
| **Database (missions table)** | ❓ VERIFICAR | Necesitas confirmar datos |

---

## 📞 CHECKLIST FINAL

- [ ] **BD:** Ejecuté `SELECT * FROM public.missions WHERE user_id = ?` y obtuve resultados
- [ ] **BD:** Mis misiones tienen `is_daily=true` O `is_weekly=true` O ambas `false`
- [ ] **Código:** Compilé el proyecto sin errores (`mvn clean compile`)
- [ ] **App:** Ejecuté la aplicación y vi mensajes DEBUG en consola
- [ ] **UI:** Las misiones ahora aparecen en los tabs correctos
- [ ] **Datos:** Las misiones muestran el progreso correcto
- [ ] **Botones:** Puedo hacer click en "RECLAMAR" en misiones completadas

---

## 🆘 SI AÚN NO FUNCIONA

Ejecuta esto en tu consola SQL y **copia la salida completa** para que podamos analizar:

```sql
-- Información del usuario
SELECT id, username, email, level, current_xp, coins 
FROM public.users 
WHERE id = <TU_ID>;

-- Información de misiones
SELECT id, title, user_id, is_daily, is_weekly, progress, completed, created_at
FROM public.missions 
WHERE user_id = <TU_ID>
LIMIT 10;

-- Información de progreso (si aplica)
SELECT mp.id, mp.mission_id, mp.metric_key, mp.current_value, mp.target_value
FROM public.mission_progress mp
WHERE mp.user_id = <TU_ID>
LIMIT 5;
```

---

## 📖 RESUMEN DE FLUJO

El flujo **debería ser:**

```
HomeController.initPlayerData()
    ↓
showMissions() (click en botón)
    ↓
loadView("missions_view")
    ↓
injectCharacterData(MissionsViewController)
    ↓ 🔍 DEBUG: inyectando datos
    ↓
MissionsViewController.setUserId(userId)
    ↓ 🔍 DEBUG: setUserId(1)
    ↓
cargarMisionesReales()
    ↓ 🔍 DEBUG: cargarMisionesReales() - userId = 1
    ↓
MissionsDAO.getMisionesUsuario(userId)
    ↓ 🔍 DEBUG: Ejecutando SQL con userId=1
    ↓ 🔍 DEBUG: Total misiones encontradas: 5
    ↓
Mostrar tarjetas en TabPane
    ↓ ✅ Procesando misión: Programa 1 Hora (tipo: DIARIA)
    ↓
✅ VISIBLE EN UI
```

Si se corta en algún punto, ese es el problema.

---

## 📧 PRÓXIMO CONTACTO

Después de ejecutar los pasos 1-3:
1. **Si ves las misiones:** ¡Perfecto! La solución funcionó
2. **Si NO ves las misiones:** Comparte conmigo:
   - Salida de la consola Java (mensajes DEBUG)
   - Salida de la query SQL
   - Usuario/contraseña para revisar directamente (si es posible)

---

**¡A RESOLVER ESTO! 🎮**
