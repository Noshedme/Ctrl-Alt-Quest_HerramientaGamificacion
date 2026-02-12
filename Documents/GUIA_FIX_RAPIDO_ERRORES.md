# ✅ FIX INMEDIATO - ERRORES DE JAVA Y BD

## 🔴 ERRORES QUE VISTE

```
1. Error inicializando mission_progress: 
   ERROR: no hay restricción única o de exclusión que coincida con la especcificación ON CONFLICT

2. Error registrando actividad: 
   ERROR: inserción o actualización en la tabla «app_usage_logs» 
   viola la llave foránea «app_usage_logs_app_id_fkey»
```

---

## ✅ LO QUE HEMOS ARREGLADO

### **Problema #1: mission_progress**
- ❌ Antes: Código usaba `ON CONFLICT (user_id, mission_id, metric_key)` pero la BD tenía otra constraint
- ✅ Ahora: Código actualizado para coincidir con la constraint correcta

**Archivo arreglado:** `MissionsDAO.java` (línea 209)

### **Problema #2: app_usage_logs**
- ❌ Antes: Código guardaba `appName.hashCode()` como app_id (ej: 1999406190), pero ese ID no existía en tabla `apps`
- ✅ Ahora: Código busca el app_id válido y si no existe, lo crea primero

**Archivo arreglado:** `ActivityDAO.java` (línea 59)

---

## 🚀 PASOS A SEGUIR

### **Paso 1: Ejecutar SQL (5 minutos)**

Ejecuta este archivo en pgAdmin:
👉 `FIX_ERRORES_JAVA.sql`

```
1. Abrir pgAdmin 4
2. Query Tool → Copiar contenido de FIX_ERRORES_JAVA.sql
3. Pegar en Query Tool
4. Ejecutar (Ctrl+Enter)
5. Esperar a que termine sin errores
```

### **Paso 2: Recompilar Java (5 minutos)**

```bash
cd c:\Users\Usuario\Desktop\proyecto\Ctrl-Alt-Quest_HerramientaGamificacion\frontend
mvn clean compile
```

Si compila exitosamente, ✅ todo está bien.

### **Paso 3: Probar la aplicación**

- Inicia la aplicación
- Los errores en consola deberían desaparecer
- Si todavía hay errores, reporta el mensaje exacto

---

## 📝 QUÉ CAMBIÓ EN EL CÓDIGO

### **MissionsDAO.java** (línea 209)

**ANTES:**
```java
"ON CONFLICT (user_id, mission_id, metric_key) DO NOTHING";
```

**DESPUÉS:**
```java
"ON CONFLICT (user_id, mission_id, metric_key) DO UPDATE SET " +
"current_value = EXCLUDED.current_value, " +
"progress_percentage = 0.00";
```

---

### **ActivityDAO.java** (línea 59)

**ANTES:**
```java
logStmt.setInt(2, appName != null ? appName.hashCode() : 0);
```

**DESPUÉS:**
```java
// Buscar app_id válido en tabla apps
// Si no existe, crearlo
// Si aún no existe, dejar NULL en lugar de usar hash inválido
Integer appId = null;
if (appName != null && !appName.isEmpty()) {
    // Búsqueda y creación de app si no existe
}
logStmt.setInt(2, appId != null ? appId : null);
```

---

## ✅ CHECKLIST

- [ ] Ejecutaste FIX_ERRORES_JAVA.sql en pgAdmin
- [ ] El script terminó sin errores
- [ ] Recompilaste el proyecto Java
- [ ] Iniciaste la aplicación
- [ ] Los errores en consola desaparecieron

---

## 🆘 SI AÚN HAY ERRORES

Si sigues viendo errores después de estos pasos, reporta:
1. **El mensaje exacto del error** (copiado de la consola)
2. **En qué momento ocurre** (al iniciar, al navegar, etc.)
3. **La línea de la clase Java** (si aparece)

---

**¡Que funcione perfecto! 🚀**
