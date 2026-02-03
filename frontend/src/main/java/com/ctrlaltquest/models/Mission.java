package com.ctrlaltquest.models;

public class Mission {
    private int id;
    private String title;
    private String description;
    private String type; // Valores: "DIARIA", "SEMANAL", "CLASE"
    private int xpReward;
    private int coinReward;
    private double progress; // Valor entre 0.0 y 1.0
    private boolean completed;

    // Constructor completo
    public Mission(int id, String title, String description, String type, int xpReward, int coinReward, double progress, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.xpReward = xpReward;
        this.coinReward = coinReward;
        this.progress = progress;
        this.completed = completed;
    }

    // --- GETTERS (Necesarios para que los controladores funcionen) ---

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getXpReward() {
        return xpReward;
    }

    public int getCoinReward() {
        return coinReward;
    }

    public double getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    // --- SETTERS (Opcionales por ahora, pero útiles) ---
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}