package com.ctrlaltquest.services;

/**
 * EventContextualListener - Interfaz avanzada para escuchar eventos contextuales inmersivos.
 * Implementa el patrón Observer para notificar a la UI sobre el ciclo de vida completo
 * de los eventos interactivos (Bosses, misiones rápidas, descansos, etc.).
 */
public interface EventContextualListener {
    
    /**
     * Enumeración para definir exactamente CÓMO terminó un evento.
     * Permite a la UI mostrar diferentes animaciones, sonidos o mensajes.
     */
    enum CompletionStatus {
        VICTORY,    // El usuario alcanzó la meta o derrotó al jefe.
        DEFEAT,     // El usuario falló el evento interactivo explícitamente.
        FLED,       // El usuario cerró la ventana o presionó "Huir/Rendirse".
        TIMEOUT     // Se acabó el tiempo límite antes de completar el objetivo.
    }

    /**
     * 1. FASE DE PREPARACIÓN
     * Se llama cuando un evento es creado lógicamente pero AÚN NO se muestra.
     * Ideal para que la UI pre-cargue en caché imágenes pesadas (Bosses) o sonidos 
     * antes de abrir la ventana, evitando "tirones" o lag visual.
     * * @param userId ID del usuario afectado
     * @param event Datos del evento generado
     */
    void onEventGenerated(int userId, EventContextualService.ContextualEvent event);
    
    /**
     * 2. FASE DE INICIO
     * Se llama en el momento exacto en que el evento debe saltar a la pantalla.
     * Desencadena la apertura de popups, carga la imagen del boss, inicia los timers.
     */
    void onEventStarted(int userId, EventContextualService.ContextualEvent event);

    /**
     * 3. FASE DE INTERACCIÓN (NUEVO)
     * Se dispara cada vez que el usuario hace un progreso (un click al boss, una palabra escrita).
     * Permite a la UI animar la barra de vida, hacer temblar la imagen del jefe, o 
     * mostrar números de daño (ej: "-15 HP") flotando en la pantalla.
     * * @param currentProgress Daño causado o progreso actual
     * @param target HP total del Boss o meta final
     */
    void onEventProgressUpdated(int userId, EventContextualService.ContextualEvent event, int currentProgress, int target);

    /**
     * 4. FASE CRÍTICA (NUEVO)
     * Se llama cuando el evento entra en un estado de "Tensión" o "Peligro".
     * Ejemplo: Al boss le queda < 20% de HP (Se enfurece), o quedan < 5 segundos en el timer.
     * Permite a la UI parpadear en rojo, reproducir un latido de corazón o cambiar la música.
     */
    void onEventCriticalPhase(int userId, EventContextualService.ContextualEvent event);
    
    /**
     * 5. FASE DE RESOLUCIÓN
     * Se llama cuando el ciclo de vida del evento concluye, cierra la ventana y 
     * reparte el botín.
     * * @param status El estado final (Victoria, Huida, Timeout)
     * @param xpReward Experiencia final obtenida (0 si falló)
     * @param coinReward Monedas de oro obtenidas (0 si falló)
     */
    void onEventCompleted(int userId, EventContextualService.ContextualEvent event, CompletionStatus status, int xpReward, int coinReward);
}