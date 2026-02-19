package com.ctrlaltquest.services;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.EventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * EventContextualService - Gestor y planificador de eventos inmersivos.
 * Actúa como el puente entre el EventGenerator (Cerebro) y la Interfaz Gráfica (UI).
 */
public class EventContextualService {
    
    private static EventContextualService instance;
    private final List<EventContextualListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Control de tareas y eventos activos en memoria
    private final Map<Integer, ContextualEventTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<String, ContextualEvent> activeEventsMap = new ConcurrentHashMap<>();
    
    // Rastrea la actividad actual del usuario para generar eventos acordes a ella
    private String currentActivity = "UNKNOWN"; 
    
    private EventContextualService() {}
    
    public static synchronized EventContextualService getInstance() {
        if (instance == null) instance = new EventContextualService();
        return instance;
    }
    
    // ==========================================
    // MANEJO DE LISTENERS (PATRÓN OBSERVER)
    // ==========================================
    
    public void addEventListener(EventContextualListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }
    
    public void removeEventListener(EventContextualListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyEventGenerated(int userId, ContextualEvent event) {
        for (EventContextualListener l : listeners) l.onEventGenerated(userId, event);
    }
    
    private void notifyEventStarted(int userId, ContextualEvent event) {
        for (EventContextualListener l : listeners) l.onEventStarted(userId, event);
    }
    
    public void notifyEventProgress(int userId, String eventId, int currentProgress, int target) {
        ContextualEvent event = activeEventsMap.get(eventId);
        if (event != null) {
            for (EventContextualListener l : listeners) l.onEventProgressUpdated(userId, event, currentProgress, target);
        }
    }

    public void notifyEventCritical(int userId, String eventId) {
        ContextualEvent event = activeEventsMap.get(eventId);
        if (event != null) {
            for (EventContextualListener l : listeners) l.onEventCriticalPhase(userId, event);
        }
    }
    
    private void notifyEventCompleted(int userId, ContextualEvent event, EventContextualListener.CompletionStatus status, int xpReward, int coinReward) {
        for (EventContextualListener l : listeners) l.onEventCompleted(userId, event, status, xpReward, coinReward);
    }
    
    // ==========================================
    // CONTROL DE CONTEXTO
    // ==========================================
    
    /**
     * Actualiza la actividad actual del usuario para que el generador sepa qué evento lanzar.
     */
    public void updateCurrentActivity(String activityName) {
        this.currentActivity = activityName;
    }

    // ==========================================
    // CONTROL DE EVENTOS Y TIMERS
    // ==========================================
    
    public void startEventGenerator(int userId) {
        if (activeTasks.containsKey(userId)) activeTasks.get(userId).cancel = true;
        
        ContextualEventTask task = new ContextualEventTask(userId);
        activeTasks.put(userId, task);
        
        // Se ejecuta cada 3 minutos (180 segundos).
        scheduler.scheduleAtFixedRate(task, 180, 180, TimeUnit.SECONDS); 
        System.out.println("✅ Motor de Eventos Inmersivos Iniciado para el usuario " + userId);
    }
    
    public void stopEventGenerator(int userId) {
        ContextualEventTask task = activeTasks.remove(userId);
        if (task != null) {
            task.cancel = true;
            System.out.println("🛑 Motor de Eventos Detenido.");
        }
    }

    public void changeEventImage(int userId, String eventId, String newImagePath) {
        System.out.println("🎨 Imagen del Boss/Evento actualizada a: " + newImagePath);
    }
    
    /**
     * Genera un evento utilizando el cerebro de EventGenerator para que sea 100% dinámico.
     */
    private ContextualEvent generateDynamicEvent(int userId) {
        // 1. Decidir el tipo de evento basado en la actividad
        EventType type = EventGenerator.generateContextualEvent(userId, currentActivity, 999);
        
        // Si por probabilidad devolvió null, forzamos uno aleatorio (ya que este timer se ejecuta rara vez)
        if (type == null) {
            type = EventType.BREAK_TIME;
        }
        
        // 2. Generar metas y lore
        int target = EventGenerator.generateTarget(type);
        String title = type.displayName;
        String description = EventGenerator.generateDescription(type, target);
        String imagePath = EventGenerator.generateImagePath(type);
        
        // 3. Calcular recompensa XP y Monedas basadas en la dificultad
        int baseXp = type.baseReward + (target / 2);
        int baseCoin = (type.baseReward / 2) + (target / 4);
        
        String eventId = UUID.randomUUID().toString();
        ContextualEvent event = new ContextualEvent(
            eventId, userId, type, title, description, imagePath, baseXp, baseCoin, System.currentTimeMillis()
        );
        
        // 4. Asignar el target al campo correcto del evento
        switch (type) {
            case BREAK_TIME:
            case STRETCH_ROUTINE:
                event.restTimeSeconds = target;
                break;
            case BOSS_ENCOUNTER:
                event.bossMaxHealth = target;
                event.bossHealth = target;
                break;
            default:
                event.targetCount = target;
                break;
        }
        
        // Guardar en memoria activa
        activeEventsMap.put(eventId, event);
        
        return event;
    }
    
    /**
     * Se llama desde la UI cuando el jugador gana, pierde o huye.
     */
    public void completeEvent(int userId, String eventId, boolean success) {
        // Rescatamos el evento de la memoria RAM
        ContextualEvent event = activeEventsMap.remove(eventId);
        
        int xpToGive = 0;
        int coinsToGive = 0;
        EventContextualListener.CompletionStatus status;
        
        if (success) {
            status = EventContextualListener.CompletionStatus.VICTORY;
            // Si el evento existe en memoria, tomamos sus valores calculados
            xpToGive = (event != null) ? event.baseXp : 150;
            coinsToGive = (event != null) ? event.baseCoin : 50;
            
            System.out.println("✨ [EventContextualService] ¡Victoria! Otorgando " + xpToGive + " XP.");
            
            // Otorgar XP a través del Sync Service
            XPSyncService.getInstance().awardXPFromActivity(userId, xpToGive, "contextual_event");
            // Otorgar Monedas
            RewardsService.getInstance().awardCoinsForMission(userId, -1, coinsToGive);
            
            recordEventCompletion(userId, eventId, true, xpToGive);
            
        } else {
            status = EventContextualListener.CompletionStatus.FLED;
            System.out.println("💀 [EventContextualService] Evento Fallido/Huida. Cero recompensas.");
            recordEventCompletion(userId, eventId, false, 0);
        }
        
        // Notificar a todos los observadores (UI, HomeController, etc.)
        notifyEventCompleted(userId, event, status, xpToGive, coinsToGive);
    }
    
    /**
     * Registra el desenlace del evento efímero en la base de datos (Opcional, si tienes tabla events).
     */
    private void recordEventCompletion(int userId, String eventId, boolean success, int xpEarned) {
        String sql = "UPDATE public.events SET handled = true, outcome = ?::jsonb WHERE id::text = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String outcome = "{\"success\": " + success + ", \"xp_earned\": " + xpEarned + ", \"completed_at\": \"" + java.time.Instant.now() + "\"}";
            pstmt.setString(1, outcome);
            pstmt.setString(2, eventId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { 
            // Falla silenciosa permitida para eventos puramente visuales no guardados en DB
        }
    }
    
    // ==========================================
    // CLASES INTERNAS (MODELO LOCAL Y TAREA)
    // ==========================================
    
    private class ContextualEventTask implements Runnable {
        int userId;
        volatile boolean cancel = false;
        
        ContextualEventTask(int userId) { this.userId = userId; }
        
        @Override
        public void run() {
            if (cancel) return;
            try {
                // 1. Genera el evento integrando el contexto real del usuario
                ContextualEvent event = generateDynamicEvent(userId);
                // 2. Notifica para precargar (Ej. Cargar imagen del boss en caché)
                notifyEventGenerated(userId, event);
                // 3. Lanza el evento a la pantalla
                notifyEventStarted(userId, event);
            } catch (Exception e) { 
                System.err.println("⚠️ Error en Task de Eventos: " + e.getMessage());
            }
        }
    }
    
    public static class ContextualEvent {
        public String id;
        public int userId;
        public EventType type;
        public String title;
        public String description;
        public String imagePath; 
        
        public int baseXp;
        public int baseCoin;
        public long generatedAt;
        
        // Propiedades adaptables según el tipo
        public int restTimeSeconds = 30;
        public int targetCount = 100; 
        public int currentProgress = 0;
        public int bossHealth = 100;
        public int bossMaxHealth = 100;
        
        public ContextualEvent(String id, int userId, EventType type, String title, 
                               String description, String imagePath, int baseXp, int baseCoin, long generatedAt) {
            this.id = id;
            this.userId = userId;
            this.type = type;
            this.title = title;
            this.description = description;
            this.imagePath = imagePath;
            this.baseXp = baseXp;
            this.baseCoin = baseCoin;
            this.generatedAt = generatedAt;
        }
    }
}