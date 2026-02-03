# 🔄 FLUJO DETALLADO: CARGA DE MISIONES

## 1️⃣ SECUENCIA DE EJECUCIÓN

```
┌─────────────────────────────────────────────────────────────┐
│                   APLICACIÓN INICIA                          │
│              (User hace login exitosamente)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              LoginController.handleLogin()                   │
│  • Autentica credenciales                                    │
│  • Obtiene user_id = 1 (ejemplo)                             │
│  • Llama a HomeController.initPlayerData(character)          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│        HomeController.initPlayerData(character)              │
│  • Recibe objeto Character con datos del usuario             │
│  • Actualiza HUD (nombre, nivel, monedas, etc.)             │
│  • Guarda datos en variables locales                         │
│  🔍 DEBUG: currentCharacter establecido                       │
└────────────────────────┬────────────────────────────────────┘
                         │
         (Usuario hace click en botón MISIONES)
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           HomeController.showMissions()                      │
│  • Reproduce sonido de click                                 │
│  • Llama a loadView("missions_view")                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│        HomeController.loadView("missions_view")              │
│  1. Busca en caché: viewCache.get("missions_view")           │
│     └─ Si NO existe:                                         │
│        • Carga FXML desde /fxml/views/missions_view.fxml     │
│        • Obtiene controlador MissionsViewController           │
│        • Guarda en caché                                     │
│     └─ Si EXISTE:                                            │
│        • Usa la instancia cacheada                           │
│  2. Llama a injectCharacterData(controller)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│    HomeController.injectCharacterData(controller)            │
│  • Obtiene userId = SessionManager.getInstance()             │
│                    .getUserId()                              │
│  🔍 DEBUG: "userId = 1"                                       │
│  • Verifica instancia: controller instanceof                 │
│    MissionsViewController                                    │
│  ✅ SI → Llama:                                               │
│    controller.setUserId(userId)  // ← AQUÍ SE PASA EL ID     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│   MissionsViewController.setUserId(int userId)               │
│  🔍 DEBUG: "MissionsViewController.setUserId(1)"              │
│  • Guarda: this.userId = 1                                   │
│  • Llama: cargarMisionesReales()                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│   MissionsViewController.cargarMisionesReales()              │
│  🔍 DEBUG: "cargarMisionesReales() - userId = 1"             │
│  • Verifica: if (userId == -1) → return                      │
│  • Limpia contenedores                                       │
│  • Crea background Task (Thread)                             │
│  • Muestra mensaje "Consultando el oráculo..."               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
        ┌────────── EN THREAD SEPARADO ─────────┐
        │                                        │
        ▼                                        │
┌─────────────────────────────────────────────────────────────┐
│    Task.call() → MissionsDAO.getMisionesUsuario(1)           │
│  🔍 DEBUG: "getMisionesUsuario(userId=1)"                     │
│  🔍 DEBUG: "Ejecutando SQL con userId=1"                      │
│                                                              │
│  SQL:  SELECT id, title, category, is_daily, is_weekly...   │
│        FROM public.missions                                  │
│        WHERE user_id = 1                                     │
│                                                              │
│  ResultSet:                                                  │
│  ┌────┬──────────────────────┬──────────┬──────────┐         │
│  │ id │ title                │ is_daily │ is_weekly│         │
│  ├────┼──────────────────────┼──────────┼──────────┤         │
│  │ 1  │ Programa 1 Hora      │ true     │ false    │         │
│  │ 2  │ Lee 3 Artículos      │ true     │ false    │         │
│  │ 3  │ Alcanza 20 Horas     │ false    │ true     │         │
│  │ 4  │ Capítulo 1: Primer.. │ false    │ false    │         │
│  └────┴──────────────────────┴──────────┴──────────┘         │
│                                                              │
│  🔍 DEBUG: "Misión #1: Programa 1 Hora (tipo=DIARIA...)"    │
│  🔍 DEBUG: "Misión #2: Lee 3 Artículos (tipo=DIARIA...)"    │
│  🔍 DEBUG: "Misión #3: Alcanza 20 Horas (tipo=SEMANAL...)"  │
│  🔍 DEBUG: "Misión #4: Capítulo 1 (tipo=CLASE...)"          │
│  🔍 DEBUG: "Total misiones encontradas: 4"                   │
│                                                              │
│  ↓ Retorna List<Mission> con 4 misiones                      │
└─────────────────────────┬──────────────────────────────────┘
        │                                        │
        └────────── FIN THREAD ────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│   Task.setOnSucceeded(e → {...})                             │
│  • Vuelve a hilo JavaFX (UI)                                 │
│  • Obtiene List<Mission> result = task.getValue()           │
│  🔍 DEBUG: "onSucceeded - Misiones recibidas: 4"             │
│  • Verifica: if (misiones != null && !isEmpty)               │
│  ✅ SI → Continúa procesamiento                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│   Para CADA misión en la lista:                              │
│                                                              │
│   Iteración 1: \"Programa 1 Hora\" (type=\"DIARIA\")            │
│   • Crea HBox (tarjeta)                                      │
│   • Mapea datos: título, descripción, recompensas            │
│   🔍 DEBUG: \"Procesando misión: Programa 1 Hora...\"          │
│   • Evalúa switch(m.getType()):                              │
│     └─ case \"DIARIA\" → dailyContainer.add(tarjeta)          │
│     🔍 DEBUG: \"→ Añadida a TAB DIARIAS\"                       │
│   • Anima entrada con fade + translate                       │
│                                                              │
│   Iteración 2: \"Lee 3 Artículos\" (type=\"DIARIA\")            │
│   • (Idem iteración 1)                                       │
│   🔍 DEBUG: \"Procesando misión: Lee 3 Artículos...\"          │
│                                                              │
│   Iteración 3: \"Alcanza 20 Horas\" (type=\"SEMANAL\")          │
│   • Crea HBox                                                │
│   🔍 DEBUG: \"Procesando misión: Alcanza 20 Horas...\"         │
│   • case \"SEMANAL\" → weeklyContainer.add(tarjeta)           │
│   🔍 DEBUG: \"→ Añadida a TAB SEMANALES\"                       │
│                                                              │
│   Iteración 4: \"Capítulo 1\" (type=\"CLASE\")                 │
│   • Crea HBox                                                │
│   🔍 DEBUG: \"Procesando misión: Capítulo 1...\"              │
│   • case \"CLASE\" → classContainer.add(tarjeta)              │
│   🔍 DEBUG: \"→ Añadida a TAB CLASE\"                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  ✅ RESULTADO FINAL                          │
│                                                              │
│  TabPane con 3 tabs visibles:                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ [DIARIAS]  [SEMANALES]  [HISTORIA DE CLASE]        │    │
│  ├─────────────────────────────────────────────────────┤    │
│  │ DIARIAS TAB:                                        │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │ ! │ Programa 1 Hora            │ [EN PROGRESO] │  │    │
│  │  ├──────────────────────────────────────────────┤  │    │
│  │  │ ! │ Lee 3 Artículos            │ [EN PROGRESO] │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                                                     │    │
│  │ SEMANALES TAB:                                      │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │ ! │ Alcanza 20 Horas...         │ [EN PROGRESO] │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                                                     │    │
│  │ HISTORIA DE CLASE TAB:                              │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │ ! │ Capítulo 1: El Primer Código │ [EN PROGRESO] │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ PUNTOS CRÍTICOS DEL FLUJO

### 🔴 Punto 1: SessionManager.getInstance().getUserId()
**En:** `HomeController.injectCharacterData()`
**Verifica:** ¿Devuelve un ID válido (> 0) o -1?

```java
int userId = SessionManager.getInstance().getUserId();
System.out.println("userId = " + userId);  // ¿Qué imprime?
```

**Si userId = -1 → Las misiones nunca cargarán**

---

### 🔴 Punto 2: MissionsDAO.getMisionesUsuario(userId)
**En:** `MissionsDAO.java`
**Verifica:** ¿La query SQL retorna resultados?

```java
SELECT COUNT(*) FROM public.missions WHERE user_id = ?
// ¿Cuántas filas?
```

**Si retorna 0 → No hay misiones en BD**

---

### 🔴 Punto 3: Mapeo de tipos
**En:** `MissionsDAO.getMisionesUsuario()`
**Verifica:** ¿Se calcula correctamente el tipo?

```java
String type = isDaily ? "DIARIA" : (isWeekly ? "SEMANAL" : "CLASE");
```

**Si is_daily=false e is_weekly=false → type="CLASE"** (correcto)

---

### 🔴 Punto 4: injectCharacterData() se ejecuta
**En:** `HomeController.loadView()`
**Verifica:** ¿Se llama después de cargar FXML?

```java
if (controller != null) {
    injectCharacterData(controller);  // ← ¿Se ejecuta?
}
```

**Si no se ejecuta → setUserId() nunca se llama**

---

## 3️⃣ POSIBLES PUNTOS DE RUPTURA

| # | Punto | Síntoma | Causa Probable | Solución |
|---|-------|---------|---|---|
| 1 | SessionManager | userId = -1 | Login fallido o mal guardado | Revisar SessionManager |
| 2 | Query SQL | 0 resultados | BD vacía | Insertar datos con MISIONES_TEST_DATA.sql |
| 3 | Mapeo tipo | Misiones en tab equivocado | is_daily/is_weekly mal | UPDATE en BD |
| 4 | Task thread | No aparecen misiones | Task.setOnSucceeded() no se llama | Revisar excepción en task |
| 5 | UI | Misiones aparecen pero vacías | Datos nulos en Mission.java | Verificar getters |

---

## 4️⃣ MENSAJES DEBUG ESPERADOS

**Cuando TODO funciona correctamente, deberías ver:**

```
🔍 DEBUG [HomeController]: Inyectando datos a controlador: MissionsViewController
🔍 DEBUG [HomeController]: userId = 1
✅ MissionsViewController detectado, inyectando userId
🔍 DEBUG: MissionsViewController.setUserId(1)
🔍 DEBUG: cargarMisionesReales() - userId = 1
🔍 DEBUG: Ejecutando MissionsDAO.getMisionesUsuario(1)
🔍 DEBUG [MissionsDAO]: getMisionesUsuario(userId=1)
🔍 DEBUG [MissionsDAO]: Ejecutando SQL con userId=1
🔍 DEBUG [MissionsDAO]: Misión #1: Programa 1 Hora (tipo=DIARIA, isDaily=true, isWeekly=false)
🔍 DEBUG [MissionsDAO]: Misión #2: Lee 3 Artículos (tipo=DIARIA, isDaily=true, isWeekly=false)
🔍 DEBUG [MissionsDAO]: Total misiones encontradas: 2
🔍 DEBUG: Misiones cargadas: 2
🔍 DEBUG: onSucceeded - Misiones recibidas: 2
✅ Procesando misión: Programa 1 Hora (tipo: DIARIA, progress: 0.5)
   → Añadida a TAB DIARIAS
✅ Procesando misión: Lee 3 Artículos (tipo: DIARIA, progress: 0.75)
   → Añadida a TAB DIARIAS
```

---

## 5️⃣ MENSAJES DE ERROR A BUSCAR

| Error | Ubicación | Significa |
|-------|-----------|-----------|
| `❌ ERROR: userId es -1` | MissionsViewController | No se obtuvo userId |
| `⚠️ ADVERTENCIA: No se encontraron misiones` | MissionsViewController | Query retornó 0 filas |
| `❌ Error cargando misiones: ` | MissionsDAO | Excepción SQL |
| Exception en task | Console | Thread falló |

---

## 6️⃣ RESUMEN VISUAL DEL ESTADO

```
        ESTADO ACTUAL DE TU APP
        ═════════════════════════

┌──────────────────────────────────────────────────┐
│ LoginController     → HomeController             │
│ ✅ CORRECTO          ✅ CORRECTO                  │
│                      injectCharacterData()        │
│                      setUserId() se llama         │
├──────────────────────────────────────────────────┤
│                                                  │
│ MissionsViewController                           │
│ ✅ CORRECTO (con logging ahora)                  │
│ cargarMisionesReales() - OK                      │
│ MissionsDAO.getMisionesUsuario() - OK            │
│                                                  │
├──────────────────────────────────────────────────┤
│                                                  │
│ FXML: missions_view.fxml                         │
│ ✅ CORRECTO - Tabs y contenedores bien definidos │
│                                                  │
├──────────────────────────────────────────────────┤
│                                                  │
│ ❓ BASE DE DATOS                                  │
│ DESCONOCIDO - Necesitas verificar                │
│ ¿Hay datos en public.missions?                   │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

**👉 Siguiente paso:** Ejecuta los pasos en `PASOS_A_SEGUIR.md`
