# 🛡️ RECOMENDACIONES PARA CÓDIGO JAVA - SINCRONIZACIÓN CON BD

## Contexto
Los errores que observaste se deben a que el código Java intenta insertar datos sin validar que las referencias existan en la base de datos. Esto causa violaciones de foreign keys.

---

## 1. 🔴 PROBLEMA: app_usage_logs con app_id inválido

### Error Original:
```
ERROR: inserción o actualización en la tabla «app_usage_logs» 
viola la llave foránea «app_usage_logs_app_id_fkey»
Detail: La llave (app_id)=(1999406190) no está presente en la tabla «apps».
```

### 📝 Solución en Java

**Ubicación esperada:** Clase que registra actividades (probablemente `ActivityService` o `AppMonitorService`)

#### ANTES (❌ Incorrecto):
```java
public void logAppUsage(int appId, LocalDateTime startTime, LocalDateTime endTime) {
    AppUsageLog log = new AppUsageLog();
    log.setSessionId(currentSessionId);
    log.setAppId(appId);  // ❌ Sin validar que existe
    log.setStartTime(startTime);
    log.setEndTime(endTime);
    appUsageLogRepository.save(log);
}
```

#### DESPUÉS (✅ Correcto):
```java
public void logAppUsage(int appId, LocalDateTime startTime, LocalDateTime endTime) {
    // 1️⃣ Validar que el app existe
    Optional<App> existingApp = appRepository.findById(appId);
    
    if (existingApp.isEmpty()) {
        // Si no existe, crear un registro genérico
        App newApp = new App();
        newApp.setId(appId);
        newApp.setName("Aplicación Detectada - " + appId);
        newApp.setCategory("UNKNOWN");
        newApp.setIsProductive(false);
        appRepository.save(newApp);
        
        // Log para debugging
        logger.info("✅ App registrada automáticamente: {} (ID: {})", newApp.getName(), appId);
    }
    
    // 2️⃣ Ahora sí, registrar la actividad
    try {
        AppUsageLog log = new AppUsageLog();
        log.setSessionId(currentSessionId);
        log.setAppId(appId);
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        
        // Calcular duración
        if (endTime != null && startTime != null) {
            long durationMs = ChronoUnit.MILLIS.between(startTime, endTime);
            log.setDuration(Duration.ofMillis(durationMs));
        }
        
        appUsageLogRepository.save(log);
        logger.info("✅ Actividad registrada para app: {}", appId);
        
    } catch (Exception e) {
        logger.error("❌ Error registrando actividad para app {}: {}", appId, e.getMessage());
        // No lanzar excepción para no romper el flujo principal
    }
}
```

---

## 2. 🔴 PROBLEMA: mission_progress sin progress_percentage

### Error Original:
```
⚠️  Error inicializando mission_progress: 
ERROR: no existe la columna «progress_percentage» en la relación «mission_progress»
```

### 📝 Solución en Java

**Ubicación esperada:** Clase que inicializa misiones (probablemente `MissionService` o `MissionProgressService`)

#### ANTES (❌ Incorrecto):
```java
public void createMissionProgress(int missionId, int userId, 
                                  long targetValue) {
    MissionProgress progress = new MissionProgress();
    progress.setMissionId(missionId);
    progress.setUserId(userId);
    progress.setMetricKey("completion_progress");
    progress.setCurrentValue(0);
    progress.setTargetValue(targetValue);
    // ❌ No configura progress_percentage
    
    missionProgressRepository.save(progress);
}
```

#### DESPUÉS (✅ Correcto):
```java
public void createMissionProgress(int missionId, int userId, 
                                  long targetValue) {
    // 1️⃣ Validar que la misión existe
    Mission mission = missionRepository.findById(missionId)
        .orElseThrow(() -> new IllegalArgumentException("Misión no encontrada: " + missionId));
    
    // 2️⃣ Verificar que el usuario existe
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    
    try {
        // 3️⃣ Crear o actualizar progreso
        MissionProgress progress = new MissionProgress();
        progress.setMissionId(missionId);
        progress.setUserId(userId);
        progress.setMetricKey("completion_progress");
        progress.setCurrentValue(0);
        progress.setTargetValue(targetValue);
        
        // ✅ CRÍTICO: Calcular progress_percentage
        progress.setProgressPercentage(0.0);  // 0% al inicio
        
        // Establecer timestamp
        progress.setLastUpdated(LocalDateTime.now());
        
        missionProgressRepository.save(progress);
        logger.info("✅ Progreso de misión creado: usuario={}, misionId={}", userId, missionId);
        
    } catch (DataAccessException e) {
        logger.error("❌ Error al crear progreso de misión: {}", e.getMessage());
        throw new RuntimeException("Error sincronizando progreso", e);
    }
}
```

---

## 3. 🔧 ACTUALIZAR PROGRESO CORRECTAMENTE

### Entity Java (MissionProgress)

```java
@Entity
@Table(name = "mission_progress")
public class MissionProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "mission_id", nullable = false)
    private Integer missionId;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "metric_key", nullable = false)
    private String metricKey;
    
    @Column(name = "current_value")
    private Long currentValue = 0L;
    
    @Column(name = "target_value")
    private Long targetValue = 0L;
    
    // ✅ NUEVO: Campo progress_percentage
    @Column(name = "progress_percentage", columnDefinition = "numeric(5,2)")
    private Double progressPercentage = 0.0;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    // ✅ Método auxiliar para actualizar progreso
    public void updateProgress(long newValue) {
        this.currentValue = newValue;
        calculateProgressPercentage();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // ✅ Calcula automáticamente el porcentaje
    public void calculateProgressPercentage() {
        if (targetValue != null && targetValue > 0) {
            this.progressPercentage = (currentValue.doubleValue() / targetValue.doubleValue()) * 100.0;
            // Limitar a 100%
            if (this.progressPercentage > 100.0) {
                this.progressPercentage = 100.0;
            }
        } else {
            this.progressPercentage = 0.0;
        }
    }
    
    // Getters y Setters
    public Integer getId() { return id; }
    public Integer getMissionId() { return missionId; }
    public Integer getUserId() { return userId; }
    public String getMetricKey() { return metricKey; }
    public Long getCurrentValue() { return currentValue; }
    public Long getTargetValue() { return targetValue; }
    public Double getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public void setCurrentValue(Long currentValue) { 
        this.currentValue = currentValue;
        calculateProgressPercentage();
    }
    public void setTargetValue(Long targetValue) { 
        this.targetValue = targetValue;
        calculateProgressPercentage();
    }
    public void setProgressPercentage(Double progressPercentage) { 
        this.progressPercentage = progressPercentage; 
    }
    public void setLastUpdated(LocalDateTime lastUpdated) { 
        this.lastUpdated = lastUpdated; 
    }
}
```

---

## 4. 💾 REPOSITORY CON VALIDACIÓN

```java
@Repository
public interface MissionProgressRepository extends JpaRepository<MissionProgress, Integer> {
    
    Optional<MissionProgress> findByMissionIdAndMetricKey(Integer missionId, String metricKey);
    
    List<MissionProgress> findByUserId(Integer userId);
    
    List<MissionProgress> findByMissionId(Integer missionId);
}
```

---

## 5. 🔄 SERVICE CON SINCRONIZACIÓN COMPLETA

```java
@Service
@Slf4j
public class MissionProgressService {
    
    @Autowired
    private MissionProgressRepository progressRepository;
    
    @Autowired
    private MissionRepository missionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Actualizar progreso de una misión con validación completa
     */
    public MissionProgress updateMissionProgress(int userId, int missionId, 
                                                 long currentValue) {
        try {
            // 1️⃣ Validar usuario
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));
            
            // 2️⃣ Validar misión
            Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException("Misión no encontrada: " + missionId));
            
            // 3️⃣ Obtener o crear progreso
            MissionProgress progress = progressRepository
                .findByMissionIdAndMetricKey(missionId, "completion_progress")
                .orElseGet(() -> {
                    MissionProgress newProgress = new MissionProgress();
                    newProgress.setMissionId(missionId);
                    newProgress.setUserId(userId);
                    newProgress.setMetricKey("completion_progress");
                    newProgress.setTargetValue(100L);  // Por defecto
                    return newProgress;
                });
            
            // 4️⃣ Actualizar valor
            progress.updateProgress(currentValue);
            
            // 5️⃣ Guardar cambios
            MissionProgress saved = progressRepository.save(progress);
            
            log.info("✅ Progreso actualizado: usuario={}, misión={}, progreso={}%", 
                     userId, missionId, saved.getProgressPercentage());
            
            return saved;
            
        } catch (DataAccessException e) {
            log.error("❌ Error en base de datos al actualizar progreso: {}", e.getMessage());
            throw new RuntimeException("Error sincronizando progreso con BD", e);
        } catch (Exception e) {
            log.error("❌ Error inesperado al actualizar progreso: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar progreso", e);
        }
    }
    
    /**
     * Crear progreso para nueva misión
     */
    public MissionProgress createProgressForMission(int userId, int missionId, long targetValue) {
        try {
            // Validaciones
            userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
            missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException("Misión no encontrada"));
            
            MissionProgress progress = new MissionProgress();
            progress.setMissionId(missionId);
            progress.setUserId(userId);
            progress.setMetricKey("completion_progress");
            progress.setCurrentValue(0L);
            progress.setTargetValue(targetValue);
            progress.setProgressPercentage(0.0);
            progress.setLastUpdated(LocalDateTime.now());
            
            MissionProgress saved = progressRepository.save(progress);
            log.info("✅ Nuevo progreso de misión creado");
            return saved;
            
        } catch (Exception e) {
            log.error("❌ Error creando progreso de misión: {}", e.getMessage());
            throw new RuntimeException("Error creando progreso", e);
        }
    }
    
    /**
     * Limpiar datos huérfanos (ejecutar periódicamente)
     */
    @Scheduled(fixedDelay = 3600000)  // Cada hora
    public void cleanupOrphanedData() {
        try {
            log.info("🧹 Iniciando limpieza de datos huérfanos...");
            
            // Encontrar progreso sin misión válida
            List<MissionProgress> allProgress = progressRepository.findAll();
            List<MissionProgress> toDelete = new ArrayList<>();
            
            for (MissionProgress progress : allProgress) {
                if (!missionRepository.existsById(progress.getMissionId())) {
                    toDelete.add(progress);
                    log.warn("⚠️ Eliminando progreso huérfano: misionId={}", progress.getMissionId());
                }
            }
            
            if (!toDelete.isEmpty()) {
                progressRepository.deleteAll(toDelete);
                log.info("✅ {} registros huérfanos eliminados", toDelete.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Error en limpieza de datos: {}", e.getMessage());
        }
    }
}
```

---

## 6. 🛡️ EXCEPTION HANDLERS

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        log.error("❌ Usuario no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Usuario no encontrado", e.getMessage()));
    }
    
    @ExceptionHandler(MissionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissionNotFound(MissionNotFoundException e) {
        log.error("❌ Misión no encontrada: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Misión no encontrada", e.getMessage()));
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e) {
        log.error("❌ Error de acceso a datos: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Error de base de datos", "Intenta de nuevo más tarde"));
    }
}
```

---

## 7. 📋 CHECKLIST DE IMPLEMENTACIÓN

- [ ] ✅ Agregue `progress_percentage` a Entity `MissionProgress`
- [ ] ✅ Actualicé todos los métodos que crean `MissionProgress`
- [ ] ✅ Agregué validación de usuario en `createMissionProgress()`
- [ ] ✅ Agregué validación de misión en `createMissionProgress()`
- [ ] ✅ Implementé `calculateProgressPercentage()` en Entity
- [ ] ✅ Agregué `updateProgress()` helper method
- [ ] ✅ Configuré `@Transactional` en services
- [ ] ✅ Agregué logs detallados para debugging
- [ ] ✅ Implementé `cleanupOrphanedData()` scheduler
- [ ] ✅ Agregué Global Exception Handler
- [ ] ✅ Ejecuté el script SQL de corrección

---

## 8. 🧪 TESTING

```java
@SpringBootTest
class MissionProgressServiceTest {
    
    @Autowired
    private MissionProgressService service;
    
    @Autowired
    private MissionProgressRepository repository;
    
    @Test
    void testCreateProgressWithValidation() {
        // Dado un usuario y misión válidos
        // Cuando creo un progreso
        // Entonces progress_percentage debe ser 0.0
        
        MissionProgress progress = service.createProgressForMission(1, 1, 100L);
        
        assertNotNull(progress);
        assertEquals(0.0, progress.getProgressPercentage());
    }
    
    @Test
    void testUpdateProgressCalculation() {
        // Dado un progreso existente
        // Cuando actualizo currentValue a 50 con targetValue 100
        // Entonces progress_percentage debe ser 50.0
        
        MissionProgress progress = service.updateMissionProgress(1, 1, 50L);
        
        assertEquals(50.0, progress.getProgressPercentage());
    }
}
```

---

## ✅ Resumen

| Problema | Solución | Archivo |
|----------|----------|---------|
| app_id inválido | Validar que app existe, si no crear | Service layer |
| progress_percentage faltante | Agregar columna a Entity y calcular | MissionProgress.java |
| Falta sincronización | Agregar triggers en BD | ESQUEMA_BD_ACTUALIZADO.sql |
| Datos huérfanos | Implementar cleanup scheduler | MissionProgressService.java |

