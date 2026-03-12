package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.EventsDAO;
import com.ctrlaltquest.models.Event;
import com.ctrlaltquest.models.EventType;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EventsService — Motor lógico para eventos del modelo Event (BD).
 *
 * NOTA: Este servicio gestiona los eventos guardados en la base de datos (tabla events),
 * a diferencia de EventContextualService que gestiona eventos efímeros en RAM.
 * Ambos coexisten: EventsService para historial y persistencia,
 * EventContextualService para la experiencia visual en tiempo real.
 */
public class EventsService {

    private static EventsService instance;

    private final Map<Integer, Long>  lastEventTimestamp = new ConcurrentHashMap<>();
    private final Map<Integer, Event> activeEvents       = new ConcurrentHashMap<>();
    private final RewardsService      rewardsService     = RewardsService.getInstance();
    private final Random              random             = new Random();

    private EventsService() {}

    public static synchronized EventsService getInstance() {
        if (instance == null) instance = new EventsService();
        return instance;
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Evalúa si corresponde lanzar un evento sorpresa basado en el tiempo y actividad.
     * Llamado desde GameService o ActivityMonitorService si se desea persistencia en BD.
     */
    public void checkAndGenerateEvent(int userId, String currentActivity) {
        if (activeEvents.containsKey(userId)) return;

        long timeSinceLastEvent = EventsDAO.getSecondsSinceLastEvent(userId);
        EventType eventType = EventGenerator.generateContextualEvent(
            userId, currentActivity, timeSinceLastEvent);

        if (eventType != null) generateEvent(userId, eventType);
    }

    private void generateEvent(int userId, EventType eventType) {
        int    target    = EventGenerator.generateTarget(eventType);
        String imagePath = EventGenerator.generateImagePath(eventType);

        // Sistema de rareza (15% épico)
        boolean isEpic     = random.nextInt(100) < 15;
        double  multiplier = isEpic ? 2.0 : 1.0;
        if (isEpic) target = (int) (target * 1.5);

        String description = EventGenerator.generateDescription(eventType, target);
        if (isEpic) description = "🌟 [EVENTO ÉPICO] " + description;

        int xpReward   = (int) (calculateXpReward(eventType, target)   * multiplier);
        int coinReward = (int) (calculateCoinReward(eventType, target)  * multiplier);

        int eventId = EventsDAO.createEvent(userId, eventType, description, target,
                                            xpReward, coinReward, imagePath);
        if (eventId > 0) {
            Event event = new Event(userId, eventType, description, target,
                                    xpReward, coinReward, imagePath);
            event.setId(eventId);
            activeEvents.put(userId, event);

            System.out.println("\n================================================");
            System.out.println("🎯 ¡EVENTO GENERADO!");
            if (isEpic) System.out.println("⭐ ¡VARIANTE ÉPICA!");
            System.out.println("   ├─ Tipo:  " + eventType.displayName);
            System.out.println("   ├─ Meta:  " + target);
            System.out.println("   ├─ Botín: " + xpReward + " XP  |  " + coinReward + " Monedas");
            System.out.println("   └─ Desc:  " + description);
            System.out.println("================================================\n");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROGRESO Y RESOLUCIÓN
    // ════════════════════════════════════════════════════════════════════════

    public void updateEventProgress(int userId, int amount) {
        Event event = activeEvents.get(userId);
        if (event == null || event.isCompleted()) return;

        event.incrementProgress(amount);
        if (event.getProgress() % 10 == 0 || event.getProgress() == 100)
            System.out.println("📊 [Evento] " + event.getType().displayName +
                               " [" + event.getProgress() + "%]");

        if (event.isCompleted()) completeEvent(userId, event, true);
    }

    public void completeEvent(int userId, Event event, boolean isSuccess) {
        if (isSuccess) {
            System.out.println("✨ VICTORIA: " + event.getType().displayName);
            rewardsService.awardXPForActivity(userId, true, event.getXpReward());
            rewardsService.awardCoinsForMission(userId, event.getId(), event.getCoinReward());
            rewardsService.checkAndAwardAchievements(userId);
        } else {
            System.out.println("💀 Derrota en: " + event.getType().displayName);
        }
        EventsDAO.completeEvent(event.getId(), isSuccess, event.getProgress());
        activeEvents.remove(userId);
    }

    public Event getActiveEvent(int userId) { return activeEvents.get(userId); }

    public void cancelEvent(int userId) {
        Event event = activeEvents.remove(userId);
        if (event != null) {
            EventsDAO.completeEvent(event.getId(), false, event.getProgress());
            System.out.println("🏃 Huida del evento: " + event.getType().displayName);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CÁLCULO DE RECOMPENSAS
    // ════════════════════════════════════════════════════════════════════════

    private int calculateXpReward(EventType type, int target) {
        return switch (type) {
            case TYPING_CHALLENGE -> 50 + (target * 8);
            case CLICK_RUSH       -> 30 + (target / 2);
            case BOSS_ENCOUNTER   -> 200 + target;
            case BUG_STORM        -> 150 + (target * 2);
            case BREAK_TIME       -> 40 + (target / 5);
            case STRETCH_ROUTINE  -> 45 + (target / 2);
            case TRIVIA_QUIZ      -> 60 + (target * 10);
        };
    }

    private int calculateCoinReward(EventType type, int target) {
        return switch (type) {
            case TYPING_CHALLENGE -> 20 + (target * 3);
            case CLICK_RUSH       -> 15 + (target / 5);
            case BOSS_ENCOUNTER   -> 100 + (target / 2);
            case BUG_STORM        -> 50 + target;
            case BREAK_TIME       -> 10;
            case STRETCH_ROUTINE  -> 15;
            case TRIVIA_QUIZ      -> 25 + (target * 2);
        };
    }
}