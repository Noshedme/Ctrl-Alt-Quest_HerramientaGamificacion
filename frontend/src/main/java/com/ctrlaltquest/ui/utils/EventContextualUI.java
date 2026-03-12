package com.ctrlaltquest.ui.utils;

import com.ctrlaltquest.services.EventContextualListener;
import com.ctrlaltquest.services.EventContextualService;

/**
 * EventContextualUI — Listener auxiliar de eventos contextuales.
 *
 * NOTA: Esta clase ya NO abre ventanas ni modales propios.
 * La responsabilidad de mostrar el modal fue movida a HomeController,
 * que usa EventModalController + event_modal.fxml para una UI completa y funcional.
 *
 * EventContextualUI se mantiene como listener pasivo para:
 *  - Logging de ciclo de vida de eventos
 *  - Posibles efectos de capa global en el futuro (ej. overlay en el HUD)
 */
public class EventContextualUI implements EventContextualListener {

    public EventContextualUI() {
        EventContextualService.getInstance().addEventListener(this);
        System.out.println("✅ [EventContextualUI] Registrado como listener auxiliar.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // IMPLEMENTACIÓN DEL LISTENER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Fase 1 — El evento fue creado pero aún no es visible.
     * Aquí se podría precargar assets en caché si fuera necesario.
     */
    @Override
    public void onEventGenerated(int userId, EventContextualService.ContextualEvent event) {
        System.out.println("📢 [EventContextualUI] Evento precargado: " + event.title +
                           " | Tipo: " + event.type.displayName);
    }

    /**
     * Fase 2 — El evento debe mostrarse.
     * HomeController es quien abre el modal; aquí solo registramos el inicio.
     */
    @Override
    public void onEventStarted(int userId, EventContextualService.ContextualEvent event) {
        System.out.println("🎮 [EventContextualUI] Evento iniciado: " + event.type.displayName +
                           " | Target: " + obtenerTarget(event));
    }

    /**
     * Fase 3 — El usuario realizó una acción (clic, palabra, etc.).
     * El progreso ya se muestra dentro del modal; este método es informativo.
     */
    @Override
    public void onEventProgressUpdated(int userId, EventContextualService.ContextualEvent event,
                                       int currentProgress, int target) {
        // Silencioso para no spam en consola por cada clic
    }

    /**
     * Fase 4 — El boss tiene poca vida o queda poco tiempo.
     * El modal ya maneja el efecto visual; aquí podríamos añadir efectos de capa global.
     */
    @Override
    public void onEventCriticalPhase(int userId, EventContextualService.ContextualEvent event) {
        System.out.println("⚠️ [EventContextualUI] FASE CRÍTICA en: " + event.title);
    }

    /**
     * Fase 5 — El evento terminó.
     * Registra el resultado en consola.
     */
    @Override
    public void onEventCompleted(int userId, EventContextualService.ContextualEvent event,
                                 CompletionStatus status, int xpReward, int coinReward) {
        String resultado = switch (status) {
            case VICTORY -> "✅ VICTORIA";
            case DEFEAT  -> "💀 DERROTA";
            case FLED    -> "🏃 HUIDA";
            case TIMEOUT -> "⏰ TIEMPO AGOTADO";
        };
        System.out.println("🏁 [EventContextualUI] " + resultado +
                           " | Evento: " + (event != null ? event.title : "desconocido") +
                           " | Botín: +" + xpReward + " XP, +" + coinReward + " 💰");
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDAD
    // ════════════════════════════════════════════════════════════════════════

    private String obtenerTarget(EventContextualService.ContextualEvent event) {
        return switch (event.type) {
            case BOSS_ENCOUNTER              -> event.bossMaxHealth + " HP";
            case BREAK_TIME, STRETCH_ROUTINE -> event.restTimeSeconds + " seg";
            default                          -> event.targetCount + " acciones";
        };
    }
}