package com.ctrlaltquest.services;

import com.ctrlaltquest.models.EventType;
import java.util.Random;

/**
 * EventGenerator - Genera eventos contextuales basados en actividad del usuario.
 * Asegura que los eventos sean aleatorios pero consistentes con el tipo de actividad.
 */
public class EventGenerator {
    
    private static final Random random = new Random();
    
    /**
     * Genera un evento contextual basado en la actividad actual del usuario.
     * 
     * @param userId ID del usuario
     * @param currentActivity Actividad actual (ej: "CODING", "BROWSING", "PRODUCTIVITY")
     * @param timeSinceLastEvent Segundos desde el último evento
     * @return EventType generado o null si no hay evento
     */
    public static EventType generateContextualEvent(int userId, String currentActivity, long timeSinceLastEvent) {
        // Solo generar eventos cada 5-15 minutos (300-900 segundos)
        if (timeSinceLastEvent < 300) {
            return null;
        }
        
        // Probabilidad de generar evento: 30-40%
        int probability = random.nextInt(100);
        if (probability > 40) {
            return null;
        }
        
        // Seleccionar evento según la actividad actual
        return selectEventByActivity(currentActivity);
    }
    
    /**
     * Selecciona un evento apropiado según el tipo de actividad.
     * 
     * @param activity Tipo de actividad (CODING, BROWSING, PRODUCTIVITY, etc.)
     * @return EventType contextual
     */
    private static EventType selectEventByActivity(String activity) {
        if (activity == null) {
            return getRandomEventType();
        }
        
        String lower = activity.toLowerCase();
        
        // Actividad de CODING → Reto de escritura o clicks
        if (lower.contains("coding") || lower.contains("vscode") || lower.contains("ide")) {
            int choice = random.nextInt(100);
            if (choice < 50) {
                return EventType.TYPING_CHALLENGE;  // Escritura
            } else if (choice < 85) {
                return EventType.CLICK_RUSH;        // Clicks
            } else {
                return EventType.BOSS_ENCOUNTER;    // Boss
            }
        }
        
        // Actividad de BROWSING → Clicks o boss
        if (lower.contains("browsing") || lower.contains("chrome") || lower.contains("firefox")) {
            int choice = random.nextInt(100);
            if (choice < 60) {
                return EventType.CLICK_RUSH;        // Clicks
            } else {
                return EventType.BOSS_ENCOUNTER;    // Boss
            }
        }
        
        // Actividad de PRODUCTIVITY/OFFICE → Escritura o boss
        if (lower.contains("productivity") || lower.contains("office") || 
            lower.contains("word") || lower.contains("excel")) {
            int choice = random.nextInt(100);
            if (choice < 70) {
                return EventType.TYPING_CHALLENGE;  // Escritura
            } else {
                return EventType.BOSS_ENCOUNTER;    // Boss
            }
        }
        
        // Por defecto: selección aleatoria
        return getRandomEventType();
    }
    
    /**
     * Selecciona un tipo de evento aleatorio.
     * BREAK_TIME tiene menor probabilidad (10%)
     * 
     * @return EventType aleatorio
     */
    private static EventType getRandomEventType() {
        int choice = random.nextInt(100);
        
        if (choice < 35) {
            return EventType.TYPING_CHALLENGE;
        } else if (choice < 70) {
            return EventType.CLICK_RUSH;
        } else if (choice < 95) {
            return EventType.BOSS_ENCOUNTER;
        } else {
            return EventType.BREAK_TIME;
        }
    }
    
    /**
     * Genera el target (objetivo) para un evento según su tipo.
     * 
     * @param eventType Tipo de evento
     * @return Número objetivo
     */
    public static int generateTarget(EventType eventType) {
        switch (eventType) {
            case TYPING_CHALLENGE:
                // 50-200 palabras (aproximadamente 300-1200 caracteres)
                return 50 + random.nextInt(150);
                
            case CLICK_RUSH:
                // 30-100 clicks
                return 30 + random.nextInt(70);
                
            case BOSS_ENCOUNTER:
                // Boss con 50-150 HP
                return 50 + random.nextInt(100);
                
            case BREAK_TIME:
                // 30-60 segundos de descanso
                return 30 + random.nextInt(30);
                
            default:
                return 100;
        }
    }
    
    /**
     * Genera una descripción amigable para el evento.
     * 
     * @param eventType Tipo de evento
     * @param target Objetivo
     * @return Descripción del evento
     */
    public static String generateDescription(EventType eventType, int target) {
        switch (eventType) {
            case TYPING_CHALLENGE:
                return "¡Escribe " + target + " palabras para ganar XP!";
                
            case CLICK_RUSH:
                return "¡Haz " + target + " clicks lo más rápido posible!";
                
            case BOSS_ENCOUNTER:
                return "¡Un Boss apareció! Derrótalo haciendo clicks (Vida: " + target + ")";
                
            case BREAK_TIME:
                return "Es hora de descansar. Aparta los ojos de la pantalla por " + target + " segundos.";
                
            default:
                return "Evento especial";
        }
    }
}
