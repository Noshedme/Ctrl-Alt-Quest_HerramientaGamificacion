package com.ctrlaltquest.models;

public class Character {
    private int id;
    private String name;
    private int classId;
    private int userId;
    private int level;
    private int slotIndex;
    
    // Atributos adicionales requeridos por HomeController y la BBDD
    private int currentXp;
    private int coins;
    private int healthStreak;
    
    // NUEVO CAMPO: Skin (Nombre del archivo de imagen base)
    private String skin;

    // Constructor vacío (VITAL para instanciación desde DAOs)
    public Character() {}

    // Constructor completo actualizado
    public Character(int id, String name, int classId, int userId, int level, int currentXp, int coins, int healthStreak, String skin) {
        this.id = id;
        this.name = name;
        this.classId = classId;
        this.userId = userId;
        this.level = level;
        this.currentXp = currentXp;
        this.coins = coins;
        this.healthStreak = healthStreak;
        this.skin = skin;
    }

    // --- GETTERS Y SETTERS ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int slotIndex) { this.slotIndex = slotIndex; }

    public int getCurrentXp() { return currentXp; }
    public void setCurrentXp(int currentXp) { this.currentXp = currentXp; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public int getHealthStreak() { return healthStreak; }
    public void setHealthStreak(int healthStreak) { this.healthStreak = healthStreak; }

    // --- MÉTODOS PARA CORREGIR EL ERROR DE COMPILACIÓN ---
    
    public String getSkin() { 
        return skin; 
    }
    
    public void setSkin(String skin) { 
        this.skin = skin; 
    }
}