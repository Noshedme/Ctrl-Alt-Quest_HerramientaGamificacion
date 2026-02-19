package com.ctrlaltquest.models;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Event - Modelo que representa un evento contextual (Reto o Jefe) en la aplicación.
 * Mejorado con estados RPG, rareza y utilidades dinámicas para la UI (JavaFX).
 */
public class Event {
    private int id;
    private int userId;
    private EventType type;
    private String description;
    private String imagePath;          
    
    // --- ESTADOS RPG ---
    private boolean isEpic;            // ¿Es un evento raro con recompensas multiplicadas?
    private boolean completed;         // ¿El evento ya terminó? (Por cualquier motivo)
    private boolean successful;        // ¿Terminó en Victoria? (false = Huida / Derrota / Tiempo agotado)
    
    // --- TIEMPO ---
    private LocalDateTime triggeredAt;
    private LocalDateTime completedAt;
    private int timeLimitSeconds;      // Tiempo máximo para completarlo (Opcional)
    
    // --- PROGRESO ---
    private int progress;              // Porcentaje entero: 0-100%
    private int target;                // Clicks totales, HP del Jefe, Palabras, etc.
    private int current;               // Daño hecho o progreso actual acumulado
    
    // --- BOTÍN ---
    private int xpReward;
    private int coinReward;
    
    public Event(int userId, EventType type, String description, int target, int xpReward, int coinReward, String imagePath) {
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.imagePath = imagePath != null ? imagePath : "/assets/images/events/default.png";
        
        this.triggeredAt = LocalDateTime.now();
        this.completed = false;
        this.successful = false;
        
        // Detectar automáticamente si es épico basándose en la descripción del generador
        this.isEpic = description != null && description.contains("[EVENTO ÉPICO]");
        
        // Asignar el tiempo límite base según el tipo de evento
        this.timeLimitSeconds = type.baseDuration;
        
        this.progress = 0;
        this.target = target;
        this.current = 0;
        
        this.xpReward = xpReward;
        this.coinReward = coinReward;
    }
    
    // ==========================================
    // GETTERS Y SETTERS CLÁSICOS
    // ==========================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    
    public int getCoinReward() { return coinReward; }
    public void setCoinReward(int coinReward) { this.coinReward = coinReward; }

    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    
    public int getCurrent() { return current; }
    public void setCurrent(int current) { this.current = current; }
    
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = Math.min(100, Math.max(0, progress)); }

    // ==========================================
    // ESTADOS LÓGICOS Y RPG
    // ==========================================
    
    public boolean isEpic() { return isEpic; }
    public void setEpic(boolean isEpic) { this.isEpic = isEpic; }

    public boolean isCompleted() { return completed; }
    public boolean isSuccessful() { return successful; }

    /**
     * Marca el evento como finalizado (Ya sea por rendirse, tiempo agotado o victoria)
     */
    public void finishEvent(boolean wasSuccess) {
        this.completed = true;
        this.successful = wasSuccess;
        this.completedAt = LocalDateTime.now();
    }

    // ==========================================
    // MÉTODOS DE UTILIDAD PARA LA UI (JAVAFX)
    // ==========================================

    /**
     * Devuelve el progreso en formato Double (0.0 a 1.0).
     * Ideal para inyectar directamente en: progressBar.setProgress(event.getProgressNormalized());
     */
    public double getProgressNormalized() {
        if (target <= 0) return 0.0;
        return (double) current / target;
    }
    
    /**
     * Devuelve cuánto falta para completar (Ej. "Vida restante del Boss")
     */
    public int getRemainingTarget() {
        return Math.max(0, target - current);
    }
    
    /**
     * Verifica si el tiempo límite del evento ya expiró.
     */
    public boolean isExpired() {
        if (timeLimitSeconds <= 0 || completed) return false;
        long secondsElapsed = Duration.between(triggeredAt, LocalDateTime.now()).getSeconds();
        return secondsElapsed >= timeLimitSeconds;
    }

    // ==========================================
    // MECÁNICAS DE JUEGO
    // ==========================================

    /**
     * Incrementa el progreso genérico de la misión (ej. escribir palabras, recolectar items).
     */
    public void incrementProgress(int amount) {
        if (completed) return;
        
        this.current = Math.min(this.current + amount, this.target);
        this.progress = (int) ((this.current * 100.0) / this.target);
        
        if (this.current >= this.target) {
            finishEvent(true); // Se alcanzó la meta = Victoria
        }
    }
    
    /**
     * Semánticamente idéntico a incrementProgress, pero diseñado para usarse
     * cuando el evento es un BOSS_ENCOUNTER.
     * @param damage Cantidad de HP restada al Jefe.
     */
    public void applyDamage(int damage) {
        incrementProgress(damage);
    }
    
    @Override
    public String toString() {
        String rarity = isEpic ? "⭐[ÉPICO]" : "🔹[NORMAL]";
        String status = completed ? (successful ? "🏆 VICTORIA" : "💀 DERROTA") : "⚔️ EN CURSO";
        return String.format("%s %s - %s | Progreso: %d%% (%d/%d)", 
                             rarity, type.displayName, status, progress, current, target);
    }
}