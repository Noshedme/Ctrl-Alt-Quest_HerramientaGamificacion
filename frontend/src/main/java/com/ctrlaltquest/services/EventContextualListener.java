package com.ctrlaltquest.services;

/**
 * EventContextualListener — Interfaz Observer para el ciclo de vida de eventos inmersivos.
 *
 * Implementada por: HomeController (abre el modal), y cualquier otro componente
 * que necesite reaccionar a los eventos contextuales.
 */
public interface EventContextualListener {

    /**
     * Estados posibles al resolver un evento.
     */
    enum CompletionStatus {
        VICTORY,  // El usuario alcanzó la meta
        DEFEAT,   // El usuario falló explícitamente
        FLED,     // El usuario cerró/huyó
        TIMEOUT   // Se agotó el tiempo límite
    }

    /**
     * 1. PREPARACIÓN — El evento fue creado pero aún no es visible.
     *    Ideal para precargar imágenes pesadas (bosses) sin lag.
     */
    void onEventGenerated(int userId, EventContextualService.ContextualEvent event);

    /**
     * 2. INICIO — El evento debe mostrarse en pantalla ahora.
     *    HomeController usa este método para abrir el modal.
     */
    void onEventStarted(int userId, EventContextualService.ContextualEvent event);

    /**
     * 3. PROGRESO — El usuario realizó una acción (click, palabra, etc.).
     *    Permite animar barras de vida o mostrar daño flotante.
     */
    void onEventProgressUpdated(int userId, EventContextualService.ContextualEvent event,
                                int currentProgress, int target);

    /**
     * 4. FASE CRÍTICA — El boss tiene < 20% de vida o quedan < 10 s.
     *    Permite cambiar música, parpadear en rojo, etc.
     */
    void onEventCriticalPhase(int userId, EventContextualService.ContextualEvent event);

    /**
     * 5. RESOLUCIÓN — El evento terminó. Cierra el modal y reparte botín.
     *
     * @param status     Resultado final del evento
     * @param xpReward   XP otorgada (0 si no fue victoria)
     * @param coinReward Monedas otorgadas (0 si no fue victoria)
     */
    void onEventCompleted(int userId, EventContextualService.ContextualEvent event,
                          CompletionStatus status, int xpReward, int coinReward);
}