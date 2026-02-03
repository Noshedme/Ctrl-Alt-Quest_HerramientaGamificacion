package com.ctrlaltquest.models;

public class Achievement {
    private int id;
    private String title;
    private String description;
    private boolean unlocked;
    private boolean hidden; // Para Easter Eggs
    private String iconPath; // Nombre del recurso gráfico

    public Achievement(int id, String title, String description, boolean unlocked, boolean hidden, String iconPath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.unlocked = unlocked;
        this.hidden = hidden;
        this.iconPath = iconPath;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isHidden() { return hidden; }
    public String getIconPath() { return iconPath; }
}