package com.ctrlaltquest.services;

import com.ctrlaltquest.models.EventType;
import java.util.Random;

/**
 * EventGenerator - Genera eventos contextuales inmersivos basados en actividad.
 * Actúa como el "Director de Juego" (Game Master) de la aplicación.
 */
public class EventGenerator {
    
    private static final Random random = new Random();
    
    // Almacena temporalmente el índice del boss generado para que su imagen coincida con su nombre
    private static int currentBossIndex = 0;
    
    // Base de datos de Jefes (Lore + Imágenes vinculadas)
    private static final String[] BOSS_NAMES = {
        "El Dragón de la Procrastinación", 
        "El Caballero de las Sombras (Burnout)", 
        "El Rey Bug", 
        "El Demonio de las 100 Pestañas",
        "La Bestia del Código Espagueti"
    };
    
    private static final String[] BOSS_IMAGES = {
        "dragon_boss.png",
        "shadow_knight.png",
        "bug_king.png",
        "procrastination_demon.png",
        "spaghetti_beast.png"
    };

    /**
     * Decide si se debe lanzar un evento basándose en el tiempo y la probabilidad.
     */
    public static EventType generateContextualEvent(int userId, String currentActivity, long timeSinceLastEvent) {
        // Cooldown de 5 minutos (300 segundos) para no abrumar al usuario
        if (timeSinceLastEvent < 300) return null; 
        
        // 40% de probabilidad de que ocurra un evento si ya pasó el cooldown
        if (random.nextInt(100) > 40) return null; 
        
        return selectEventByActivity(currentActivity);
    }
    
    /**
     * Elige el tipo de evento analizando qué está haciendo el usuario.
     */
    private static EventType selectEventByActivity(String activity) {
        if (activity == null) return getRandomEventType();
        String lower = activity.toLowerCase();
        
        int choice = random.nextInt(100);
        
        // 1. Actividad de Programación
        if (lower.contains("code") || lower.contains("vscode") || lower.contains("ide") || 
            lower.contains("intellij") || lower.contains("eclipse") || lower.contains("studio")) {
            
            if (choice < 40) return EventType.BUG_STORM;         // 40% Invasión de bugs
            else if (choice < 65) return EventType.TYPING_CHALLENGE; // 25% Reto de escritura
            else if (choice < 85) return EventType.STRETCH_ROUTINE;  // 20% Pausa para estirar (espalda cansada)
            else return EventType.BOSS_ENCOUNTER;                    // 15% Jefe
        }
        
        // 2. Actividad de Navegación Web
        if (lower.contains("chrome") || lower.contains("firefox") || lower.contains("edge") || lower.contains("brave")) {
            
            if (choice < 40) return EventType.CLICK_RUSH;        // 40% Clicks rápidos
            else if (choice < 70) return EventType.TRIVIA_QUIZ;      // 30% Trivia mental
            else if (choice < 90) return EventType.BREAK_TIME;       // 20% Descanso visual
            else return EventType.BOSS_ENCOUNTER;                    // 10% Jefe
        }

        // 3. Actividad de Productividad / Ofimática
        if (lower.contains("word") || lower.contains("excel") || lower.contains("powerpoint") || lower.contains("notion")) {
            
            if (choice < 40) return EventType.TYPING_CHALLENGE;  // 40% Escribir
            else if (choice < 70) return EventType.TRIVIA_QUIZ;      // 30% Romper la monotonía
            else if (choice < 90) return EventType.STRETCH_ROUTINE;  // 20% Estirar
            else return EventType.BOSS_ENCOUNTER;                    // 10% Jefe
        }
        
        return getRandomEventType();
    }
    
    private static EventType getRandomEventType() {
        int choice = random.nextInt(100);
        if (choice < 20) return EventType.TYPING_CHALLENGE;
        else if (choice < 40) return EventType.CLICK_RUSH;
        else if (choice < 55) return EventType.BUG_STORM;
        else if (choice < 70) return EventType.TRIVIA_QUIZ;
        else if (choice < 85) return EventType.STRETCH_ROUTINE;
        else if (choice < 95) return EventType.BREAK_TIME;
        else return EventType.BOSS_ENCOUNTER;
    }
    
    /**
     * Genera la meta numérica adaptada para ser un reto divertido.
     */
    public static int generateTarget(EventType eventType) {
        return switch (eventType) {
            case TYPING_CHALLENGE -> 50 + random.nextInt(100);  // 50 a 150 palabras
            case CLICK_RUSH -> 40 + random.nextInt(60);         // 40 a 100 clicks veloces
            case BOSS_ENCOUNTER -> 150 + random.nextInt(150);   // 150 a 300 HP del Jefe
            case BUG_STORM -> 15 + random.nextInt(35);          // 15 a 50 Bugs a aplastar
            case BREAK_TIME -> 30 + (random.nextInt(3) * 15);   // 30, 45 o 60 segundos
            case STRETCH_ROUTINE -> 45 + random.nextInt(45);    // 45 a 90 segundos de estiramiento
            case TRIVIA_QUIZ -> 3 + random.nextInt(3);          // 3 a 5 preguntas correctas
            default -> 100;
        };
    }
    
    /**
     * Devuelve una descripción épica al azar basada en el tipo de evento.
     */
    public static String generateDescription(EventType eventType, int target) {
        String[] options;
        
        switch (eventType) {
            case TYPING_CHALLENGE:
                options = new String[]{
                    "¡Ataque de inspiración! Escribe %d palabras para conjurar el hechizo definitivo.",
                    "Un pergamino antiguo requiere tu firma. Teclea %d palabras rápidamente.",
                    "¡El maná se agota! Canaliza tu energía escribiendo %d palabras."
                };
                break;
            case CLICK_RUSH:
                options = new String[]{
                    "¡Frenesí de combate! Realiza %d clicks antes de que tu escudo colapse.",
                    "¡Emboscada de duendes ladrones! Espántalos con %d clicks rápidos.",
                    "¡Una lluvia de meteoritos! Haz click %d veces para destruirlos en el aire."
                };
                break;
            case BOSS_ENCOUNTER:
                currentBossIndex = random.nextInt(BOSS_NAMES.length);
                return BOSS_NAMES[currentBossIndex] + " bloquea tu camino. ¡Haz " + target + " de daño (clicks) para derrotarlo!";
            case BUG_STORM:
                options = new String[]{
                    "¡Alerta Crítica! La consola arroja errores. Aplasta %d bugs que han invadido tu pantalla.",
                    "¡El nido ha eclosionado! Elimina %d insectos digitales de tu código.",
                    "¡Fuga de memoria! Detén la invasión cazando %d bugs rápidamente."
                };
                break;
            case BREAK_TIME:
                options = new String[]{
                    "Has encontrado un Santuario oculto. Medita lejos de la pantalla por %d segundos para recuperar salud.",
                    "Una fogata cálida te invita a sentarte. Cierra los ojos durante %d segundos.",
                    "Tus ojos reflejan cansancio, héroe. Toma una poción de descanso y relájate %d segundos."
                };
                break;
            case STRETCH_ROUTINE:
                options = new String[]{
                    "Un Monje de Hierro se cruza en tu camino. Exige que realices %d segundos de estiramientos corporales.",
                    "Tu armadura se siente pesada. Levántate y estira tus extremidades por %d segundos.",
                    "¡Maldición de parálisis! Rompe el hechizo apartándote de la silla durante %d segundos."
                };
                break;
            case TRIVIA_QUIZ:
                options = new String[]{
                    "Una Esfinge bloquea la entrada a la mazmorra. Demuestra tu intelecto acumulando %d puntos de sabiduría.",
                    "Un viejo sabio te reta a un duelo mental. Supera %d acertijos para continuar.",
                    "¡Trampa rúnica activada! Desactívala respondiendo %d preguntas correctamente."
                };
                break;
            default:
                return "Una perturbación mágica altera tu entorno. Supera el desafío de " + target + " puntos.";
        }
        
        return String.format(options[random.nextInt(options.length)], target);
    }

    /**
     * Genera la ruta de la imagen asegurándose de que el Boss coincida con el nombre generado.
     */
    public static String generateImagePath(EventType eventType) {
        return switch (eventType) {
            case BOSS_ENCOUNTER -> "/assets/images/bosses/" + BOSS_IMAGES[currentBossIndex];
            case BUG_STORM -> "/assets/images/events/bug_swarm.png";
            case BREAK_TIME -> "/assets/images/events/campfire.png";
            case STRETCH_ROUTINE -> "/assets/images/events/stretch_monk.png";
            case TRIVIA_QUIZ -> "/assets/images/events/sphinx_quiz.png";
            case TYPING_CHALLENGE, CLICK_RUSH -> "/assets/images/events/scroll_magic.png";
            default -> "/assets/images/events/default.png";
        };
    }
}