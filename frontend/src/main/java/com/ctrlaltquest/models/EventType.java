package com.ctrlaltquest.models;

/**
 * EventType - Enumeración de tipos de eventos que pueden aparecer.
 */
public enum EventType {
    // Eventos de escritura (basados en actividad de teclado)
    TYPING_CHALLENGE("TYPING_CHALLENGE", "Reto de Escritura", 100, 50),
    
    // Eventos de clicks (basados en movimiento de mouse)
    CLICK_RUSH("CLICK_RUSH", "Tormenta de Clicks", 100, 50),
    
    // Boss fight (combate con jefe)
    BOSS_ENCOUNTER("BOSS_ENCOUNTER", "Encuentro con Jefe", 500, 200),
    
    // Descanso forzado (timer de descanso)
    BREAK_TIME("BREAK_TIME", "Hora del Descanso", 30, 25);
    
    public final String code;
    public final String displayName;
    public final int baseDuration;      // Segundos
    public final int baseReward;        // XP o coins
    
    EventType(String code, String displayName, int baseDuration, int baseReward) {
        this.code = code;
        this.displayName = displayName;
        this.baseDuration = baseDuration;
        this.baseReward = baseReward;
    }
    
    public static EventType fromCode(String code) {
        for (EventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
