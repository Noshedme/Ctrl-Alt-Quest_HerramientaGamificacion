package com.ctrlaltquest.models;

public class Character {
    private int id;
    private String name;
    private int classId;
    private int userId;
    private int level;
    private int slotIndex;

    // Constructor vacío (VITAL para el error de la línea 100)
    public Character() {}

    // Constructor completo por si lo necesitas
    public Character(int id, String name, int classId, int userId, int level) {
        this.id = id;
        this.name = name;
        this.classId = classId;
        this.userId = userId;
        this.level = level;
    }

    // Getters y Setters (VITALES para los errores "cannot find symbol")
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
}