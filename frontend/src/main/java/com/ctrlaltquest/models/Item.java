package com.ctrlaltquest.models;

/**
 * Modelo para representar items que pueden ser comprados o encontrados.
 * Incluye cosméticos, boosts, y otros items del juego.
 */
public class Item {
    private int id;
    private String name;
    private String type; // "HELMET", "CHEST", "LEGS", "CONSUMABLE", "BOOST_XP"
    private String description;
    private String rarity; // "COMMON", "RARE", "EPIC", "LEGENDARY"
    private boolean equipped;
    private int price; // Precio en coins (0 si solo se puede comprar con dinero real)
    private String imagePath; // Ruta relativa del PNG
    
    // Para boosts de XP
    private Integer boostDurationSeconds; // Duración en segundos si es boost (null si no es boost)
    private Integer boostMultiplier; // Multiplicador de XP (ej: 2 para 2x)

    // Constructor completo
    public Item(int id, String name, String type, String description, String rarity, boolean equipped,
                int price, String imagePath, Integer boostDurationSeconds, Integer boostMultiplier) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.rarity = rarity;
        this.equipped = equipped;
        this.price = price;
        this.imagePath = imagePath;
        this.boostDurationSeconds = boostDurationSeconds;
        this.boostMultiplier = boostMultiplier;
    }

    // Constructor simplificado (compatibilidad hacia atrás)
    public Item(int id, String name, String type, String description, String rarity, boolean equipped) {
        this(id, name, type, description, rarity, equipped, 0, null, null, null);
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getRarity() { return rarity; }
    public boolean isEquipped() { return equipped; }
    public int getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public Integer getBoostDurationSeconds() { return boostDurationSeconds; }
    public Integer getBoostMultiplier() { return boostMultiplier; }
    
    // Setters
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
    public void setPrice(int price) { this.price = price; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    /**
     * Comprueba si este item es un boost de XP.
     */
    public boolean isBoost() {
        return "BOOST_XP".equalsIgnoreCase(type) && boostDurationSeconds != null;
    }
    
    /**
     * Obtiene el texto de rareza con emoji.
     */
    public String getRarityEmoji() {
        return switch (rarity.toUpperCase()) {
            case "LEGENDARY" -> "🟡 " + rarity.toUpperCase();
            case "EPIC" -> "🟣 " + rarity.toUpperCase();
            case "RARE" -> "🔵 " + rarity.toUpperCase();
            case "UNCOMMON" -> "🟢 " + rarity.toUpperCase();
            default -> "⚪ " + rarity.toUpperCase();
        };
    }
    
    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", price=" + price +
                ", rarity='" + rarity + '\'' +
                '}';
    }
}