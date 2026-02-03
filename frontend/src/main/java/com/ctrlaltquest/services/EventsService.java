package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.EventsDAO;
import com.ctrlaltquest.models.Event;
import com.ctrlaltquest.models.EventType;
import java.util.HashMap;
import java.util.Map;

/**
 * EventsService - Servicio que gestiona la generación y seguimiento de eventos contextuales.
 * Los eventos aparecen de forma aleatoria pero consistente con la actividad del usuario.
 */
public class EventsService {
    
    private static EventsService instance;
    private final Map<Integer, Long> lastEventTimestamp = new HashMap<>();
    private final Map<Integer, Event> activeEvents = new HashMap<>();
    private final RewardsService rewardsService = RewardsService.getInstance();
    
    private EventsService() {}
    
    public static synchronized EventsService getInstance() {
        if (instance == null) {
            instance = new EventsService();
        }
        return instance;
    }
    
    /**
     * 🎯 Procesa actividad y decide si generar un evento contextual.
     * Se llama periódicamente desde MissionProgressService.
     * 
     * @param userId ID del usuario
     * @param currentActivity Actividad actual (ej: "CODING", "BROWSING")
     */
    public void checkAndGenerateEvent(int userId, String currentActivity) {
        // Evitar múltiples eventos simultáneos
        if (activeEvents.containsKey(userId)) {
            return;
        }
        
        // Obtener tiempo desde el último evento
        long timeSinceLastEvent = EventsDAO.getSecondsSinceLastEvent(userId);
        
        // Generar evento contextual (si es tiempo)
        EventType eventType = EventGenerator.generateContextualEvent(userId, currentActivity, timeSinceLastEvent);
        
        if (eventType != null) {
            generateEvent(userId, eventType);
        }
    }
    
    /**
     * Genera un nuevo evento y lo muestra en la UI.
     * 
     * @param userId ID del usuario
     * @param eventType Tipo de evento a generar
     */
    private void generateEvent(int userId, EventType eventType) {
        // 1. Generar parámetros del evento
        int target = EventGenerator.generateTarget(eventType);
        String description = EventGenerator.generateDescription(eventType, target);
        
        // Calcular recompensas según tipo
        int xpReward = calculateXpReward(eventType, target);
        int coinReward = calculateCoinReward(eventType, target);
        
        // 2. Crear evento en BD
        int eventId = EventsDAO.createEvent(userId, eventType, description, target, xpReward, coinReward);
        
        if (eventId > 0) {
            // 3. Guardar en memoria para seguimiento
            Event event = new Event(userId, eventType, description, target, xpReward, coinReward);
            event.setId(eventId);
            activeEvents.put(userId, event);
            
            // 4. Mostrar en UI (aquí iría la llamada al controller para mostrar popup)
            System.out.println("🎯 [EventsService] ¡EVENTO APARECE!");
            System.out.println("   ├─ Tipo: " + eventType.displayName);
            System.out.println("   ├─ Meta: " + target);
            System.out.println("   ├─ Recompensa: " + xpReward + " XP + " + coinReward + " coins");
            System.out.println("   └─ " + description);
        }
    }
    
    /**
     * Actualiza el progreso de un evento activo.
     * Se llama cuando el usuario realiza acciones (clicks, escritura, etc.)
     * 
     * @param userId ID del usuario
     * @param progressAmount Cantidad a incrementar en el evento
     */
    public void updateEventProgress(int userId, int progressAmount) {
        Event event = activeEvents.get(userId);
        if (event == null || event.isCompleted()) {
            return;
        }
        
        // Incrementar progreso
        event.incrementProgress(progressAmount);
        
        // Mostrar progreso
        System.out.println("📊 [Event Progress] " + event.getType().displayName + 
                          " [" + event.getProgress() + "%] - " + event.getCurrent() + "/" + event.getTarget());
        
        // Si se completó
        if (event.isCompleted()) {
            completeEvent(userId, event, true);
        }
    }
    
    /**
     * Completa un evento (exitosamente o por timeout).
     * 
     * @param userId ID del usuario
     * @param event Evento completado
     * @param isSuccess ¿Se completó exitosamente?
     */
    public void completeEvent(int userId, Event event, boolean isSuccess) {
        System.out.println("✅ [EventsService] Evento completado: " + event.getType().displayName);
        
        if (isSuccess) {
            // Otorgar recompensas
            System.out.println("   ├─ XP: +" + event.getXpReward());
            System.out.println("   ├─ Coins: +" + event.getCoinReward());
            
            rewardsService.awardXPForActivity(userId, true, event.getXpReward());
            rewardsService.awardCoinsForMission(userId, event.getId(), event.getCoinReward());
            
            // Verificar si desbloqueó algún logro
            rewardsService.checkAndAwardAchievements(userId);
        } else {
            System.out.println("   └─ Evento expirado sin completar");
        }
        
        // Registrar en BD
        EventsDAO.completeEvent(event.getId(), isSuccess, event.getProgress());
        
        // Remover de eventos activos
        activeEvents.remove(userId);
    }
    
    /**
     * Obtiene el evento activo del usuario (si existe).
     * 
     * @param userId ID del usuario
     * @return Event o null
     */
    public Event getActiveEvent(int userId) {
        return activeEvents.get(userId);
    }
    
    /**
     * Cancela un evento activo.
     * 
     * @param userId ID del usuario
     */
    public void cancelEvent(int userId) {
        Event event = activeEvents.remove(userId);
        if (event != null) {
            EventsDAO.completeEvent(event.getId(), false, event.getProgress());
            System.out.println("❌ Evento cancelado: " + event.getType().displayName);
        }
    }
    
    /**
     * Calcula la recompensa XP según el tipo y dificultad del evento.
     */
    private int calculateXpReward(EventType eventType, int target) {
        switch (eventType) {
            case TYPING_CHALLENGE:
                return 30 + (target / 5);  // Más palabras = más XP
                
            case CLICK_RUSH:
                return 25 + (target / 3);  // Más clicks = más XP
                
            case BOSS_ENCOUNTER:
                return 100 + (target / 2); // Boss = mucho XP
                
            case BREAK_TIME:
                return 15;                 // Descanso = poco XP
                
            default:
                return 50;
        }
    }
    
    /**
     * Calcula la recompensa de monedas según el tipo y dificultad del evento.
     */
    private int calculateCoinReward(EventType eventType, int target) {
        switch (eventType) {
            case TYPING_CHALLENGE:
                return 20 + (target / 5);
                
            case CLICK_RUSH:
                return 15 + (target / 3);
                
            case BOSS_ENCOUNTER:
                return 75 + (target / 2);
                
            case BREAK_TIME:
                return 10;
                
            default:
                return 30;
        }
    }
    
    /**
     * Obtiene estadísticas de eventos del usuario.
     */
    public String getEventStats(int userId) {
        return EventsDAO.getEventStats(userId);
    }
}
