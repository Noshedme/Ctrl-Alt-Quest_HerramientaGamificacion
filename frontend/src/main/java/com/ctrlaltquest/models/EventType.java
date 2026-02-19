package com.ctrlaltquest.models;

/**
 * EventType - Enumeración de tipos de eventos dinámicos que pueden aparecer.
 * Ahora incluye metadatos visuales y lógicos para que la UI reaccione automáticamente.
 */
public enum EventType {
    
    // ⌨️ Eventos de escritura (basados en velocidad de teclado)
    TYPING_CHALLENGE("TYPING_CHALLENGE", "Reto de Escritura", 100, 50, "#3B82F6", "⌨️", InteractionType.KEYBOARD),
    
    // 🖱️ Eventos de clicks rápidos (basados en agilidad del ratón)
    CLICK_RUSH("CLICK_RUSH", "Tormenta de Clicks", 100, 50, "#F59E0B", "🖱️", InteractionType.MOUSE),
    
    // ⚔️ Boss fight (Combate pesado, alta recompensa, requiere todo)
    BOSS_ENCOUNTER("BOSS_ENCOUNTER", "Encuentro con Jefe", 300, 200, "#EF4444", "⚔️", InteractionType.MIXED),
    
    // 🐛 Invasión de Bugs (ideal para cuando está programando)
    BUG_STORM("BUG_STORM", "Lluvia de Bugs", 150, 100, "#9C27B0", "🐛", InteractionType.MOUSE),
    
    // 🔥 Descanso forzado o recomendado (Recuperación)
    BREAK_TIME("BREAK_TIME", "Santuario de Descanso", 30, 25, "#10B981", "🔥", InteractionType.PASSIVE),
    
    // 🧘 NUEVO: Pausa Activa (Estiramientos, salud física)
    STRETCH_ROUTINE("STRETCH_ROUTINE", "Pausa Activa", 60, 40, "#06B6D4", "🧘", InteractionType.PASSIVE),
    
    // 🧠 NUEVO: Prueba mental rápida (Para romper la monotonía)
    TRIVIA_QUIZ("TRIVIA_QUIZ", "Prueba de Sabiduría", 45, 60, "#F472B6", "🧠", InteractionType.KEYBOARD);
    
    // Atributos de la enumeración
    public final String code;
    public final String displayName;
    public final int baseDuration;      // Segundos límite base para completarlo
    public final int baseReward;        // XP base otorgada al triunfar
    
    // --- NUEVOS ATRIBUTOS VISUALES Y LÓGICOS ---
    public final String themeColor;     // Color en formato Hexadecimal para inyectar en JavaFX CSS
    public final String icon;           // Emoji o ícono representativo para la UI
    public final InteractionType requiredInteraction;

    /**
     * Sub-enumeración para clasificar qué periférico/acción demanda el evento.
     * Útil para que el sistema decida qué evento lanzar si, por ejemplo, detecta
     * que el usuario está usando mucho el teclado o mucho el ratón.
     */
    public enum InteractionType {
        KEYBOARD,   // Requiere escribir (Ej. Reto de mecanografía)
        MOUSE,      // Requiere hacer clicks (Ej. Tormenta de clicks)
        MIXED,      // Requiere ambos (Ej. Jefes complejos)
        PASSIVE     // No requiere tocar el PC (Ej. Descansos, estiramientos)
    }
    
    // Constructor
    EventType(String code, String displayName, int baseDuration, int baseReward, 
              String themeColor, String icon, InteractionType requiredInteraction) {
        this.code = code;
        this.displayName = displayName;
        this.baseDuration = baseDuration;
        this.baseReward = baseReward;
        this.themeColor = themeColor;
        this.icon = icon;
        this.requiredInteraction = requiredInteraction;
    }
    
    /**
     * Convierte un código String de la Base de Datos al objeto EventType correspondiente.
     */
    public static EventType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        
        for (EventType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        System.err.println("⚠️ Tipo de evento desconocido en la BD: " + code);
        return null; 
    }
}