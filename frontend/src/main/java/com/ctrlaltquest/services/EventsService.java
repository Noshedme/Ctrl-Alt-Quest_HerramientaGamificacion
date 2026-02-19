package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.EventsDAO;
import com.ctrlaltquest.models.Event;
import com.ctrlaltquest.models.EventType;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EventsService - Motor lógico de eventos basados en la actividad en tiempo real.
 * Gestiona la rareza, progreso, recompensas y el ciclo de vida de los retos.
 */
public class EventsService {
    
    private static EventsService instance;
    
    // Usamos ConcurrentHashMap porque la actividad se reporta cada segundo (evita colisiones de hilos)
    private final Map<Integer, Long> lastEventTimestamp = new ConcurrentHashMap<>();
    private final Map<Integer, Event> activeEvents = new ConcurrentHashMap<>();
    
    private final RewardsService rewardsService = RewardsService.getInstance();
    private final Random random = new Random();
    
    private EventsService() {}
    
    public static synchronized EventsService getInstance() {
        if (instance == null) {
            instance = new EventsService();
        }
        return instance;
    }
    
    /**
     * Evalúa si es el momento adecuado para lanzar un evento sorpresa.
     */
    public void checkAndGenerateEvent(int userId, String currentActivity) {
        // 1. Si el usuario ya está luchando contra un evento, no lo abrumamos con otro
        if (activeEvents.containsKey(userId)) {
            return;
        }
        
        // 2. Verificar cuánto tiempo ha pasado desde su último evento
        long timeSinceLastEvent = EventsDAO.getSecondsSinceLastEvent(userId);
        
        // 3. El Generador decide si lanza algo basado en la actividad actual
        EventType eventType = EventGenerator.generateContextualEvent(userId, currentActivity, timeSinceLastEvent);
        
        if (eventType != null) {
            generateEvent(userId, eventType);
        }
    }
    
    /**
     * Construye el evento con estadísticas dinámicas y posibilidad de que sea ÉPICO.
     */
    private void generateEvent(int userId, EventType eventType) {
        int target = EventGenerator.generateTarget(eventType);
        String imagePath = EventGenerator.generateImagePath(eventType);
        
        // --- SISTEMA DE RAREZA (15% de probabilidad de evento Épico) ---
        boolean isEpic = random.nextInt(100) < 15;
        double multiplier = isEpic ? 2.0 : 1.0;
        
        if (isEpic) {
            target = (int) (target * 1.5); // 50% más difícil
        }
        
        String description = EventGenerator.generateDescription(eventType, target);
        if (isEpic) {
            description = "🌟 [EVENTO ÉPICO] " + description;
        }
        
        // Calcular recompensas y aplicar multiplicador de rareza
        int xpReward = (int) (calculateXpReward(eventType, target) * multiplier);
        int coinReward = (int) (calculateCoinReward(eventType, target) * multiplier);
        
        // Guardar en Base de Datos
        int eventId = EventsDAO.createEvent(userId, eventType, description, target, xpReward, coinReward, imagePath);
        
        if (eventId > 0) {
            Event event = new Event(userId, eventType, description, target, xpReward, coinReward, imagePath);
            event.setId(eventId);
            
            // Guardar en memoria RAM para actualización rápida
            activeEvents.put(userId, event);
            
            // Log Inmersivo
            System.out.println("\n================================================");
            System.out.println("🎯 [ALERTA DE SISTEMA] ¡UN EVENTO HA APARECIDO!");
            if (isEpic) System.out.println("⭐ ¡CUIDADO! ES UNA VARIANTE ÉPICA ⭐");
            System.out.println("   ├─ Tipo: " + eventType.displayName);
            System.out.println("   ├─ Meta: " + target + " acciones");
            System.out.println("   ├─ Botín: " + xpReward + " XP  |  " + coinReward + " Monedas");
            System.out.println("   └─ Misión: " + description);
            System.out.println("================================================\n");
        }
    }
    
    /**
     * Actualiza el progreso de un evento con cada pulsación o segundo registrado.
     */
    public void updateEventProgress(int userId, int progressAmount) {
        Event event = activeEvents.get(userId);
        
        if (event == null || event.isCompleted()) return;
        
        event.incrementProgress(progressAmount);
        
        // Feedback visual en consola solo cada 10% para no spamear
        if (event.getProgress() % 10 == 0 || event.getProgress() == 100) {
            System.out.println("📊 [Batalla en curso] " + event.getType().displayName + " [" + event.getProgress() + "%]");
        }
        
        if (event.isCompleted()) {
            completeEvent(userId, event, true);
        }
    }
    
    /**
     * Finaliza el evento (Victoria o Derrota) y otorga el botín.
     */
    public void completeEvent(int userId, Event event, boolean isSuccess) {
        if (isSuccess) {
            System.out.println("✨ ¡VICTORIA! Has superado el desafío: " + event.getType().displayName);
            System.out.println("   └─ Obtenido: +" + event.getXpReward() + " XP  y  +" + event.getCoinReward() + " Monedas");
            
            rewardsService.awardXPForActivity(userId, true, event.getXpReward());
            rewardsService.awardCoinsForMission(userId, event.getId(), event.getCoinReward());
            
            // Comprobar si esta victoria le dio un logro (ej. "Cazador de Bosses")
            rewardsService.checkAndAwardAchievements(userId);
        } else {
            System.out.println("💀 Derrota o Huida en el evento: " + event.getType().displayName);
        }
        
        // Actualizar en Base de datos y limpiar memoria
        EventsDAO.completeEvent(event.getId(), isSuccess, event.getProgress());
        activeEvents.remove(userId);
    }
    
    /**
     * Obtiene el evento activo actual (Ideal para que la UI sepa qué dibujar).
     */
    public Event getActiveEvent(int userId) {
        return activeEvents.get(userId);
    }
    
    /**
     * Permite al usuario rendirse o cancelar el evento.
     */
    public void cancelEvent(int userId) {
        Event event = activeEvents.remove(userId);
        if (event != null) {
            EventsDAO.completeEvent(event.getId(), false, event.getProgress());
            System.out.println("🏃‍♂️ El usuario ha huido del evento: " + event.getType().displayName);
        }
    }
    
    // ==========================================
    // CÁLCULOS BALANCEADOS DE RECOMPENSAS
    // ==========================================
    
    private int calculateXpReward(EventType type, int target) {
        return switch (type) {
            case TYPING_CHALLENGE -> 50 + (target / 2);     // Escribir da XP moderada
            case CLICK_RUSH -> 30 + (target / 2);           // Clickear da XP rápida
            case BOSS_ENCOUNTER -> 200 + target;            // Los jefes dan muchísima XP
            case BUG_STORM -> 150 + (target * 2);           // Cazar bugs es muy valorado
            case BREAK_TIME -> 40 + (target / 5);           // Descansar da XP base
            case STRETCH_ROUTINE -> 45 + (target / 2);      // Estirar da XP
            case TRIVIA_QUIZ -> 60 + (target * 10);         // Sabiduría da buena XP
            default -> 50;                                  // Seguro contra fallos
        };
    }
    
    private int calculateCoinReward(EventType type, int target) {
        return switch (type) {
            case TYPING_CHALLENGE -> 20 + (target / 5);
            case CLICK_RUSH -> 15 + (target / 5);
            case BOSS_ENCOUNTER -> 100 + (target / 2);      // Jefes sueltan mucho oro
            case BUG_STORM -> 50 + target;                  // Bugs sueltan monedas
            case BREAK_TIME -> 10;                          // Descansar da poco oro
            case STRETCH_ROUTINE -> 15;                     // Recompensa baja en oro
            case TRIVIA_QUIZ -> 25 + (target * 2);          // Recompensa media en oro
            default -> 20;                                  // Seguro contra fallos
        };
    }
}