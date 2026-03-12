package com.ctrlaltquest.services;

import com.ctrlaltquest.models.EventType;
import java.util.Random;

/**
 * EventGenerator — "Game Master" que decide qué evento lanzar y genera su contenido.
 *
 * Responsabilidades:
 *  - Decidir el tipo de evento según la actividad actual del usuario
 *  - Generar la meta (target), descripción épica e imagen correspondiente
 *  - Sin estado mutable global excepto currentBossIndex (sincronizado)
 */
public class EventGenerator {

    private static final Random random = new Random();

    // Índice del boss actualmente seleccionado (se usa para que imagen y nombre coincidan)
    private static volatile int currentBossIndex = 0;

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

    // ════════════════════════════════════════════════════════════════════════
    // DECISIÓN DE EVENTO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Decide si debe generarse un evento y de qué tipo.
     *
     * @param userId          ID del usuario (reservado para lógica futura personalizada)
     * @param currentActivity Título de la ventana activa del usuario
     * @param timeSinceLastEvent Segundos desde el último evento (999 = forzar desde scheduler)
     * @return EventType a lanzar, o null si no corresponde generar nada ahora
     */
    public static EventType generateContextualEvent(int userId, String currentActivity,
                                                    long timeSinceLastEvent) {
        // Cooldown de 5 minutos si se llama manualmente; el scheduler ya controla el timing
        if (timeSinceLastEvent < 300 && timeSinceLastEvent != 999) return null;

        // 60% de probabilidad (el scheduler solo llama cada 3 min, así que podemos ser más permisivos)
        if (random.nextInt(100) > 60) return null;

        return selectEventByActivity(currentActivity);
    }

    /**
     * Versión forzada para el scheduler: siempre devuelve un tipo (nunca null).
     */
    public static EventType generateForcedEvent(String currentActivity) {
        EventType type = selectEventByActivity(currentActivity);
        return type != null ? type : getRandomEventType();
    }

    private static EventType selectEventByActivity(String activity) {
        if (activity == null) return getRandomEventType();
        String lower = activity.toLowerCase();
        int choice = random.nextInt(100);

        // Programación
        if (lower.contains("code") || lower.contains("vscode") || lower.contains("intellij") ||
            lower.contains("eclipse") || lower.contains("studio") || lower.contains("ide")) {
            if (choice < 35) return EventType.BUG_STORM;
            if (choice < 58) return EventType.TYPING_CHALLENGE;
            if (choice < 78) return EventType.STRETCH_ROUTINE;
            if (choice < 90) return EventType.TRIVIA_QUIZ;
            return EventType.BOSS_ENCOUNTER;
        }

        // Navegación web
        if (lower.contains("chrome") || lower.contains("firefox") ||
            lower.contains("edge") || lower.contains("brave")) {
            if (choice < 35) return EventType.CLICK_RUSH;
            if (choice < 65) return EventType.TRIVIA_QUIZ;
            if (choice < 85) return EventType.BREAK_TIME;
            return EventType.BOSS_ENCOUNTER;
        }

        // Ofimática
        if (lower.contains("word") || lower.contains("excel") ||
            lower.contains("powerpoint") || lower.contains("notion")) {
            if (choice < 40) return EventType.TYPING_CHALLENGE;
            if (choice < 68) return EventType.TRIVIA_QUIZ;
            if (choice < 88) return EventType.STRETCH_ROUTINE;
            return EventType.BOSS_ENCOUNTER;
        }

        return getRandomEventType();
    }

    private static EventType getRandomEventType() {
        return switch (random.nextInt(7)) {
            case 0 -> EventType.TYPING_CHALLENGE;
            case 1 -> EventType.CLICK_RUSH;
            case 2 -> EventType.BUG_STORM;
            case 3 -> EventType.TRIVIA_QUIZ;
            case 4 -> EventType.STRETCH_ROUTINE;
            case 5 -> EventType.BREAK_TIME;
            default -> EventType.BOSS_ENCOUNTER;
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERADORES DE CONTENIDO
    // ════════════════════════════════════════════════════════════════════════

    public static int generateTarget(EventType type) {
        return switch (type) {
            case TYPING_CHALLENGE -> 3 + random.nextInt(5);    // 3-7 palabras
            case CLICK_RUSH       -> 40 + random.nextInt(60);  // 40-100 clicks
            case BOSS_ENCOUNTER   -> 80 + random.nextInt(120); // 80-200 HP (reducido para que sea jugable)
            case BUG_STORM        -> 15 + random.nextInt(25);  // 15-40 bugs
            case BREAK_TIME       -> 30 + (random.nextInt(3) * 15); // 30, 45 o 60 s
            case STRETCH_ROUTINE  -> 30 + random.nextInt(30);  // 30-60 s
            case TRIVIA_QUIZ      -> 3 + random.nextInt(3);    // 3-5 preguntas
        };
    }

    public static String generateDescription(EventType type, int target) {
        String[] options;
        switch (type) {
            case TYPING_CHALLENGE -> options = new String[]{
                "¡Ataque de inspiración! Escribe %d palabras para conjurar el hechizo.",
                "Un pergamino antiguo requiere tu firma. Teclea %d palabras rápidamente.",
                "¡El maná se agota! Canaliza tu energía escribiendo %d palabras."
            };
            case CLICK_RUSH -> options = new String[]{
                "¡Frenesí de combate! Realiza %d clicks antes de que tu escudo colapse.",
                "¡Emboscada de duendes! Espántalos con %d clicks rápidos.",
                "¡Lluvia de meteoritos! Haz click %d veces para destruirlos."
            };
            case BOSS_ENCOUNTER -> {
                currentBossIndex = random.nextInt(BOSS_NAMES.length);
                return BOSS_NAMES[currentBossIndex] + " bloquea tu camino. " +
                       "¡Haz " + target + " de daño para derrotarlo!";
            }
            case BUG_STORM -> options = new String[]{
                "¡Alerta Crítica! La consola arroja errores. Aplasta %d bugs.",
                "¡El nido ha eclosionado! Elimina %d insectos digitales.",
                "¡Fuga de memoria! Detén la invasión cazando %d bugs."
            };
            case BREAK_TIME -> options = new String[]{
                "Has encontrado un Santuario. Medita lejos de la pantalla por %d segundos.",
                "Una fogata te invita a descansar. Cierra los ojos %d segundos.",
                "Tus ojos piden un descanso. Relájate %d segundos."
            };
            case STRETCH_ROUTINE -> options = new String[]{
                "Un Monje de Hierro exige %d segundos de estiramientos.",
                "Tu armadura pesa. Levántate y estira tus extremidades por %d segundos.",
                "¡Maldición de parálisis! Rompe el hechizo alejándote %d segundos."
            };
            case TRIVIA_QUIZ -> options = new String[]{
                "Una Esfinge bloquea el paso. Responde %d preguntas para continuar.",
                "Un sabio te reta a un duelo mental. Supera %d acertijos.",
                "¡Trampa rúnica! Desactívala respondiendo %d preguntas correctamente."
            };
            default -> { return "Supera el desafío de " + target + " puntos."; }
        }
        return String.format(options[random.nextInt(options.length)], target);
    }

    public static String generateImagePath(EventType type) {
        return switch (type) {
            case BOSS_ENCOUNTER   -> "/assets/images/bosses/" + BOSS_IMAGES[currentBossIndex];
            case BUG_STORM        -> "/assets/images/events/bug_swarm.png";
            case BREAK_TIME       -> "/assets/images/events/campfire.png";
            case STRETCH_ROUTINE  -> "/assets/images/events/stretch_monk.png";
            case TRIVIA_QUIZ      -> "/assets/images/events/sphinx_quiz.png";
            default               -> "/assets/images/events/scroll_magic.png";
        };
    }
}