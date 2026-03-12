package com.ctrlaltquest.services;

import com.ctrlaltquest.models.EventType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * EventContextualService — Planificador y gestor de eventos inmersivos.
 *
 * Responsabilidades:
 *  - Generar eventos automáticamente cada N minutos (configurable)
 *  - Notificar a los listeners (HomeController, etc.) via patrón Observer
 *  - Gestionar el ciclo de vida completo de cada evento (inicio → resolución)
 *  - NO abrir modales directamente: delega esa responsabilidad a los listeners
 */
public class EventContextualService {

    // ════════════════════════════════════════════════════════════════════════
    // SINGLETON
    // ════════════════════════════════════════════════════════════════════════

    private static EventContextualService instance;

    private EventContextualService() {}

    public static synchronized EventContextualService getInstance() {
        if (instance == null) instance = new EventContextualService();
        return instance;
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Intervalo en SEGUNDOS entre eventos automáticos.
     * Por defecto: 3 minutos (180 s).
     * Durante desarrollo se puede reducir a 30 s para testear más rápido.
     */
    private static final long EVENT_INTERVAL_SECONDS = 30;

    // ════════════════════════════════════════════════════════════════════════
    // ESTADO INTERNO
    // ════════════════════════════════════════════════════════════════════════

    private final List<EventContextualListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler  = Executors.newScheduledThreadPool(1);

    // userId → future del scheduler (para poder cancelarlo)
    private final Map<Integer, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    // eventId → evento vivo en memoria RAM
    private final Map<String, ContextualEvent> activeEventsMap = new ConcurrentHashMap<>();

    // Actividad actual del usuario (actualizada por ActivityMonitorService cada 2 s)
    private volatile String currentActivity = "UNKNOWN";

    // Guardia: solo un evento activo por usuario a la vez
    private final Map<Integer, Boolean> eventInProgress = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════════════
    // OBSERVER — Listeners
    // ════════════════════════════════════════════════════════════════════════

    public void addEventListener(EventContextualListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeEventListener(EventContextualListener listener) {
        listeners.remove(listener);
    }

    // Notificaciones privadas
    private void notifyEventGenerated(int userId, ContextualEvent event) {
        listeners.forEach(l -> l.onEventGenerated(userId, event));
    }

    private void notifyEventStarted(int userId, ContextualEvent event) {
        listeners.forEach(l -> l.onEventStarted(userId, event));
    }

    /** Llamado desde EventModalController en cada interacción del usuario. */
    public void notifyEventProgress(int userId, String eventId, int current, int target) {
        ContextualEvent event = activeEventsMap.get(eventId);
        if (event != null)
            listeners.forEach(l -> l.onEventProgressUpdated(userId, event, current, target));
    }

    /** Llamado desde EventModalController cuando el boss llega al 20% de vida. */
    public void notifyEventCritical(int userId, String eventId) {
        ContextualEvent event = activeEventsMap.get(eventId);
        if (event != null)
            listeners.forEach(l -> l.onEventCriticalPhase(userId, event));
    }

    private void notifyEventCompleted(int userId, ContextualEvent event,
                                      EventContextualListener.CompletionStatus status,
                                      int xp, int coins) {
        listeners.forEach(l -> l.onEventCompleted(userId, event, status, xp, coins));
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTROL DEL GENERADOR AUTOMÁTICO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Inicia el generador periódico de eventos para un usuario.
     * Si ya había uno activo, lo cancela primero.
     */
    public void startEventGenerator(int userId) {
        stopEventGenerator(userId);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> intentarGenerarEvento(userId),
            EVENT_INTERVAL_SECONDS,   // primer disparo a los N segundos
            EVENT_INTERVAL_SECONDS,   // periodicidad
            TimeUnit.SECONDS
        );

        activeTasks.put(userId, future);
        System.out.println("✅ [EventContextualService] Generador iniciado para usuario " +
                           userId + " (cada " + EVENT_INTERVAL_SECONDS + " s)");
    }

    public void stopEventGenerator(int userId) {
        ScheduledFuture<?> future = activeTasks.remove(userId);
        if (future != null) {
            future.cancel(false);
            System.out.println("🛑 [EventContextualService] Generador detenido para usuario " + userId);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERACIÓN DE EVENTOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Intenta generar un evento si no hay uno activo ya.
     */
    private void intentarGenerarEvento(int userId) {
        // Un solo evento a la vez por usuario
        if (Boolean.TRUE.equals(eventInProgress.get(userId))) {
            System.out.println("⏭️ [EventContextualService] Evento en progreso, saltando generación.");
            return;
        }

        ContextualEvent event = generarEventoDinamico(userId);
        eventInProgress.put(userId, true);

        // Fase 1: precargar
        notifyEventGenerated(userId, event);

        // Fase 2: mostrar (300 ms después para que la UI precargue)
        scheduler.schedule(() -> notifyEventStarted(userId, event), 300, TimeUnit.MILLISECONDS);
    }

    /**
     * Construye el ContextualEvent con EventGenerator.
     */
    private ContextualEvent generarEventoDinamico(int userId) {
        EventType type = EventGenerator.generateContextualEvent(userId, currentActivity, 999);
        if (type == null) type = EventType.BREAK_TIME;

        int target      = EventGenerator.generateTarget(type);
        String title    = type.displayName;
        String desc     = EventGenerator.generateDescription(type, target);
        String imgPath  = EventGenerator.generateImagePath(type);
        int baseXp      = type.baseReward + (target / 2);
        int baseCoin    = (type.baseReward / 2) + (target / 4);

        String eventId = UUID.randomUUID().toString();

        ContextualEvent event = new ContextualEvent(
            eventId, userId, type, title, desc, imgPath, baseXp, baseCoin,
            System.currentTimeMillis()
        );

        // Asignar target según tipo
        switch (type) {
            case BREAK_TIME, STRETCH_ROUTINE -> event.restTimeSeconds = target;
            case BOSS_ENCOUNTER -> { event.bossMaxHealth = target; event.bossHealth = target; }
            default -> event.targetCount = target;
        }

        activeEventsMap.put(eventId, event);
        System.out.println("🎯 [EventContextualService] Evento generado: " + type.displayName +
                           " | Target: " + target + " | ID: " + eventId);
        return event;
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESOLUCIÓN DE EVENTOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Llamado por EventModalController cuando el evento termina (éxito o huida).
     */
    public void completeEvent(int userId, String eventId, boolean success) {
        ContextualEvent event = activeEventsMap.remove(eventId);
        eventInProgress.put(userId, false);

        int xpToGive    = 0;
        int coinsToGive = 0;
        EventContextualListener.CompletionStatus status;

        if (success) {
            status      = EventContextualListener.CompletionStatus.VICTORY;
            xpToGive    = (event != null) ? event.baseXp   : 150;
            coinsToGive = (event != null) ? event.baseCoin  : 50;

            System.out.println("✨ [EventContextualService] VICTORIA — +" + xpToGive + " XP");

            XPSyncService.getInstance().awardXPFromActivity(userId, xpToGive, "contextual_event");
            RewardsService.getInstance().awardCoinsForMission(userId, -1, coinsToGive);
        } else {
            status = EventContextualListener.CompletionStatus.FLED;
            System.out.println("💀 [EventContextualService] Evento fallido/huida.");
        }

        notifyEventCompleted(userId, event, status, xpToGive, coinsToGive);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTEXTO DE ACTIVIDAD
    // ════════════════════════════════════════════════════════════════════════

    /** Actualizado cada 2 s por ActivityMonitorService para que el generador sea contextual. */
    public void updateCurrentActivity(String activityName) {
        this.currentActivity = activityName;
    }

    /**
     * Permite cambiar la imagen de un evento activo en tiempo real.
     * Mantenido por compatibilidad con EventContextualUI y otros componentes.
     */
    public void changeEventImage(int userId, String eventId, String newImagePath) {
        ContextualEvent event = activeEventsMap.get(eventId);
        if (event != null) {
            event.imagePath = newImagePath;
            System.out.println("🎨 [EventContextualService] Imagen actualizada: " + newImagePath);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODELO: ContextualEvent
    // ════════════════════════════════════════════════════════════════════════

    public static class ContextualEvent {
        public final String    id;
        public final int       userId;
        public final EventType type;
        public final String    title;
        public final String    description;
        public String          imagePath;   // mutable: changeEventImage() puede actualizarla
        public final int       baseXp;
        public final int       baseCoin;
        public final long      generatedAt;

        // Campos mutables según tipo
        public int restTimeSeconds = 30;
        public int targetCount     = 100;
        public int currentProgress = 0;
        public int bossHealth      = 100;
        public int bossMaxHealth   = 100;

        public ContextualEvent(String id, int userId, EventType type,
                               String title, String description, String imagePath,
                               int baseXp, int baseCoin, long generatedAt) {
            this.id          = id;
            this.userId      = userId;
            this.type        = type;
            this.title       = title;
            this.description = description;
            this.imagePath   = imagePath;
            this.baseXp      = baseXp;
            this.baseCoin    = baseCoin;
            this.generatedAt = generatedAt;
        }
    }
}