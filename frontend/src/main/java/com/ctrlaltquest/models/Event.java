package com.ctrlaltquest.models;

import java.time.LocalDateTime;

/**
 * Event - Modelo que representa un evento contextual que aparece en la aplicación.
 * Los eventos pueden ser: retos de escritura, tormenta de clicks, encuentros de boss, descansos.
 */
public class Event {
    private int id;
    private int userId;
    private EventType type;
    private String description;
    private LocalDateTime triggeredAt;
    private LocalDateTime completedAt;
    private boolean completed;
    private int progress;              // 0-100%
    private int target;                // Clicks, palabras, segundos, etc.
    private int current;               // Progreso actual
    private int xpReward;
    private int coinReward;
    
    public Event(int userId, EventType type, String description, int target, int xpReward, int coinReward) {
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.triggeredAt = LocalDateTime.now();
        this.completed = false;
        this.progress = 0;
        this.target = target;
        this.current = 0;
        this.xpReward = xpReward;
        this.coinReward = coinReward;
    }
    
    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = Math.min(100, progress); }
    
    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    
    public int getCurrent() { return current; }
    public void setCurrent(int current) { this.current = current; }
    
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    
    public int getCoinReward() { return coinReward; }
    public void setCoinReward(int coinReward) { this.coinReward = coinReward; }
    
    /**
     * Incrementa el progreso actual y calcula el porcentaje.
     * @param amount Cantidad a incrementar
     */
    public void incrementProgress(int amount) {
        this.current = Math.min(this.current + amount, this.target);
        this.progress = (int) ((this.current * 100.0) / this.target);
        
        if (this.current >= this.target) {
            this.completed = true;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    @Override
    public String toString() {
        return type.displayName + " [" + progress + "%]";
    }
}
