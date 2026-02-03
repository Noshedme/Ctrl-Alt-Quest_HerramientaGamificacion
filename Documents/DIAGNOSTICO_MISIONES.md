# 🔍 DIAGNÓSTICO: PROBLEMA DE VISUALIZACIÓN DE MISIONES

## ❌ PROBLEMA IDENTIFICADO
Las misiones no se visualizan en la sección de "Crónicas & Encargos" aunque existan registros en la base de datos.

---

## 📋 ANÁLISIS DETALLADO POR COMPONENTE

### 1. **DATABASE (BaseDeDatos_CtrlAltQuest.txt)**
```sql
CREATE TABLE IF NOT EXISTS public.missions (
    id serial NOT NULL,
    user_id integer NOT NULL,
    title character varying(100) NOT NULL,
    category character varying(50),
    difficulty character varying(20),
    xp_reward integer DEFAULT 0,
    coin_reward integer DEFAULT 0,
    trigger_type character varying(50),
    conditions jsonb NOT NULL,
    is_manual boolean DEFAULT false,
    is_daily boolean DEFAULT false,      -- ⚠️ Clave: define tipo DIARIA
    is_weekly boolean DEFAULT false,     -- ⚠️ Clave: define tipo SEMANAL
    progress integer DEFAULT 0,
    completed boolean DEFAULT false,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT missions_pkey PRIMARY KEY (id)
);
```
✅ **Estructura correcta** - La tabla existe con los campos necesarios.

---

### 2. **DAO: MissionsDAO.java**
#### Método: `getMisionesUsuario(int userId)`

```java
String sql = "SELECT id, title, category, difficulty, xp_reward, coin_reward, " +
             "is_daily, is_weekly, progress, completed " +
             "FROM public.missions WHERE user_id = ? ORDER BY completed ASC, created_at DESC";
```

✅ **Query correcta** - Busca misiones por `user_id`.

**Mapeo a Objeto Mission:**
```java
String type = isDaily ? "DIARIA" : (isWeekly ? "SEMANAL" : "CLASE");
```

✅ **Conversión correcta** de `is_daily/is_weekly` a `type`.

---

### 3. **MODEL: Mission.java**

```java
public Mission(int id, String title, String description, String type, 
               int xpReward, int coinReward, double progress, boolean completed) {
    this.type = type;  // Almacena "DIARIA", "SEMANAL" o "CLASE"
}

public String getType() { return type; }
```

✅ **Getters definidos correctamente**.

---

### 4. **CONTROLLER: MissionsViewController.java**

#### Punto de Entrada:
```java
public void setUserId(int userId) {
    this.userId = userId;
    cargarMisionesReales();  // ✅ Se llama automáticamente
}
```

✅ **Correcto** - `setUserId` carga las misiones.

#### Método `cargarMisionesReales()`:
```java
Task<List<Mission>> task = new Task<>() {
    @Override
    protected List<Mission> call() {
        return MissionsDAO.getMisionesUsuario(userId);  // ✅ Llamada correcta
    }
};

task.setOnSucceeded(e -> {
    List<Mission> misiones = task.getValue();
    
    if (misiones == null || misiones.isEmpty()) {  // ⚠️ PROBLEMA AQUÍ
        mostrarMensajeVacio(dailyContainer, "No hay misiones diarias activas.");
        return;
    }
    
    for (Mission m : misiones) {
        HBox tarjeta = crearFilaMision(m);
        
        switch (m.getType()) {
            case "DIARIA" -> dailyContainer.getChildren().add(tarjeta);
            case "SEMANAL" -> weeklyContainer.getChildren().add(tarjeta);
            case "CLASE" -> classContainer.getChildren().add(tarjeta);
        }
    }
});
```

✅ **Lógica correcta** - Las misiones deberían aparecer en sus tabs.

---

### 5. **HomeController.java**

#### Inyección de Datos:
```java
else if (controller instanceof com.ctrlaltquest.ui.controllers.views.MissionsViewController) {
    ((com.ctrlaltquest.ui.controllers.views.MissionsViewController) controller).setUserId(userId);
}
```

✅ **Inyección correcta** - El `userId` se pasa al MissionsViewController.

---

### 6. **FXML: missions_view.fxml**

```xml
<VBox fx:id="dailyContainer" spacing="15" style="-fx-padding: 20 5;"/>
<VBox fx:id="weeklyContainer" spacing="15" style="-fx-padding: 20 5;"/>
<VBox fx:id="classContainer" spacing="15" style="-fx-padding: 20 5;"/>
```

✅ **Contenedores definidos correctamente**.

---

## 🔴 PROBLEMAS POTENCIALES IDENTIFICADOS

### **PROBLEMA #1: Sin Misiones en la Base de Datos**
**Síntoma:** `misiones.isEmpty() == true`

**Causa Probable:** No hay registros en `public.missions` para el usuario actual.

**Verificación:**
```sql
SELECT COUNT(*) FROM public.missions WHERE user_id = <TU_USER_ID>;
```

**Solución:**
- Insertar misiones de prueba en la BD
- Ver sección "SOLUCIONES" abajo

---

### **PROBLEMA #2: user_id Incorrecto en la Tabla `missions`**

**Síntoma:** El usuario tiene misiones en la BD, pero se filtran mal.

**Verificación:**
```sql
SELECT id, title, user_id, is_daily, is_weekly, progress, completed 
FROM public.missions 
WHERE user_id = <TU_USER_ID>;
```

**Solución:**
- Si `user_id = NULL`, eso es el problema
- Las misiones necesitan asociarse explícitamente con el usuario

---

### **PROBLEMA #3: userId = -1 en el Controller**

**Síntoma:** `setUserId()` nunca se llama, o se llama con `-1`.

**Verificación en Código:**
```java
private int userId = -1;

public void setUserId(int userId) {
    this.userId = userId;
    cargarMisionesReales();  // Si userId = -1, retorna sin hacer nada
}

private void cargarMisionesReales() {
    if (userId == -1) return;  // ⚠️ AQUÍ
```

**Solución:**
- Verificar que `HomeController.injectCharacterData()` se llamada SIEMPRE
- Confirmar que `SessionManager.getInstance().getUserId()` devuelve un ID válido

---

### **PROBLEMA #4: Misiones Vacias en BD (progress = 0, completed = false)**

**Síntoma:** Las misiones existen pero tienen tipo indefinido.

**Verificación:**
```sql
SELECT id, is_daily, is_weekly, progress, completed 
FROM public.missions 
WHERE user_id = <TU_USER_ID>;
```

**Problema:** Si tanto `is_daily = false` Y `is_weekly = false`, el tipo será "CLASE" (default).

---

### **PROBLEMA #5: Caché de Vistas en HomeController**

**Verificación:** Cuando carga las misiones por segunda vez:

```java
// Primera carga: ✅ Funciona (setUserId se ejecuta)
// Segunda carga: ⚠️ Vista cacheada, pero injectCharacterData() se llama de nuevo
// Tercera carga: ✅ Debería funcionar (el caché tiene el controlador)
```

**Posible Fallo:** Si el controlador se cachea ANTES de que se llame a `setUserId()`, las misiones nunca cargarán.

---

## ✅ SOLUCIONES RECOMENDADAS

### **SOLUCIÓN #1: Verificar Datos en BD**

```sql
-- 1. Verificar que existen misiones para tu usuario
SELECT id, title, user_id, is_daily, is_weekly, progress, completed, created_at
FROM public.missions 
WHERE user_id = 1  -- Reemplaza con tu user_id real
ORDER BY created_at DESC
LIMIT 10;

-- 2. Si la tabla está vacía, insertar misiones de prueba:
INSERT INTO public.missions 
(user_id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly, progress, completed, conditions, created_at) 
VALUES 
(1, 'Programa 1 Hora', 'Productividad', 'Fácil', 100, 50, true, false, '{"metric": "time_coding", "target": 3600}', false, '{"target": 3600}', NOW()),
(1, 'Escribe 500 Palabras', 'Escritura', 'Medio', 200, 100, false, true, '{"metric": "words_typed", "target": 500}', false, '{"target": 500}', NOW()),
(1, 'Historia: Completa Capítulo 1', 'Historia de Clase', 'Difícil', 500, 250, false, false, '{}', false, '{}', NOW());

-- 3. Verificar resultado:
SELECT * FROM public.missions WHERE user_id = 1;
```

---

### **SOLUCIÓN #2: Verificar Flujo de userId**

**En HomeController.java**, añade logging:

```java
private void injectCharacterData(Object controller) {
    int userId = SessionManager.getInstance().getUserId();
    
    System.out.println("🔍 DEBUG: Inyectando datos a controlador: " + controller.getClass().getSimpleName());
    System.out.println("🔍 DEBUG: userId = " + userId);
    
    if (controller instanceof com.ctrlaltquest.ui.controllers.views.MissionsViewController) {
        System.out.println("✅ MissionsViewController detectado");
        ((com.ctrlaltquest.ui.controllers.views.MissionsViewController) controller).setUserId(userId);
    }
}
```

**En MissionsViewController.java**, añade logging:

```java
public void setUserId(int userId) {
    System.out.println("🔍 DEBUG: MissionsViewController.setUserId(" + userId + ")");
    this.userId = userId;
    cargarMisionesReales();
}

private void cargarMisionesReales() {
    System.out.println("🔍 DEBUG: cargarMisionesReales() - userId = " + userId);
    if (userId == -1) {
        System.out.println("❌ ERROR: userId es -1, abortando carga");
        return;
    }
    
    // ... resto del código ...
}

task.setOnSucceeded(e -> {
    List<Mission> misiones = task.getValue();
    System.out.println("🔍 DEBUG: Misiones cargadas: " + (misiones == null ? "null" : misiones.size()));
    // ... resto del código ...
});
```

---

### **SOLUCIÓN #3: Asegurar que HomeController Inyecta Siempre**

**Problema:** El caché puede estar devolviendo el controlador ANTES de que `setUserId()` se ejecute.

**Solución - Modificar loadView():**

```java
private void loadView(String viewName) {
    try {
        Node nextView = viewCache.get(viewName);
        Object controller = controllerCache.get(viewName);
        
        if (nextView == null) {
            // Nueva vista
            String path = "/fxml/views/" + viewName + ".fxml";
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("⚠️ Vista no encontrada: " + path);
                return; 
            }

            FXMLLoader loader = new FXMLLoader(url);
            nextView = loader.load();
            controller = loader.getController();
            
            viewCache.put(viewName, nextView);
            controllerCache.put(viewName, controller);
        }

        // ✅ SIEMPRE inyectar (aunque sea cacheada)
        if (controller != null) {
            System.out.println("✅ Inyectando datos a: " + viewName);
            injectCharacterData(controller);
        }

        animarCambioDeVista(nextView);
        
    } catch (IOException e) {
        System.err.println("❌ Error navegando a " + viewName + ": " + e.getMessage());
        e.printStackTrace();
    }
}
```

---

### **SOLUCIÓN #4: Revisar mission_progress si se Usa**

Si usas la tabla `mission_progress` para el progreso granular:

```sql
SELECT mp.id, mp.mission_id, mp.metric_key, mp.current_value, mp.target_value
FROM public.mission_progress mp
WHERE mp.user_id = 1
LIMIT 10;
```

**Verificar:** Los registros en `mission_progress` deben coincidir con misiones en `missions`.

---

## 📊 CHECKLIST DE VERIFICACIÓN

- [ ] **BD:** ¿Existen registros en `public.missions` para tu `user_id`?
- [ ] **BD:** ¿Tienen valores correctos en `is_daily` o `is_weekly`?
- [ ] **Backend:** ¿Se ejecuta `HomeController.injectCharacterData()`?
- [ ] **Backend:** ¿Se ejecuta `MissionsViewController.setUserId()`?
- [ ] **Backend:** ¿Devuelve `MissionsDAO.getMisionesUsuario()` resultados?
- [ ] **UI:** ¿Aparecen mensajes de error en la consola?
- [ ] **UI:** ¿Se cargan en el tab correcto (Diarias/Semanales/Clase)?

---

## 🎯 PRÓXIMOS PASOS

1. **Ejecutar queries SQL** para verificar que hay datos en BD
2. **Ejecutar la aplicación con logging** (ejecutar soluciones #2 y #3)
3. **Revisar consola** para errores o mensajes de debug
4. **Aplicar correcciones** según lo encontrado
5. **Probar nuevamente**

---

## 📞 RESUMEN RÁPIDO

**Si NO hay misiones en BD:**
→ Ejecutar el SQL de `SOLUCIÓN #1` para insertar datos de prueba.

**Si hay misiones pero NO se ven:**
→ Activar logging (`SOLUCIONES #2 y #3`) para identificar dónde se corta el flujo.

**Si el uid es -1:**
→ Problema en `SessionManager` o autenticación.

**Si se ven en tabs equivocados:**
→ Revisar `is_daily/is_weekly` en la BD.
