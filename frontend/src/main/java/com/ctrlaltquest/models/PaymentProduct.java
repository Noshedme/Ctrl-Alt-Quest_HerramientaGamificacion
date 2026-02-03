package com.ctrlaltquest.models;

import java.util.Map;

/**
 * Modelo para representar un producto de pago con dinero real.
 */
public class PaymentProduct {
    private int id;
    private String sku; // Identificador único del producto
    private String name;
    private String description;
    private int priceCents; // Precio en centavos (ej: 999 = $9.99 USD)
    private String currency; // USD, ARS, BRL, etc.
    private String imagePath;
    private String type; // "coin_pack", "bundle", "battle_pass", etc.
    private Map<String, Object> recompensas; // {"coins": 1000, "items": [...], "boosts": [...]}
    
    public PaymentProduct(int id, String sku, String name, String description,
                         int priceCents, String currency, String imagePath,
                         String type, Map<String, Object> recompensas) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.currency = currency;
        this.imagePath = imagePath;
        this.type = type;
        this.recompensas = recompensas;
    }
    
    // Getters
    public int getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPriceCents() { return priceCents; }
    public double getPrice() { return priceCents / 100.0; }
    public String getCurrency() { return currency; }
    public String getImagePath() { return imagePath; }
    public String getType() { return type; }
    public Map<String, Object> getRecompensas() { return recompensas; }
    
    /**
     * Obtiene la cantidad de coins que se entregan al comprar este producto.
     */
    public int getCoinsReward() {
        Object coins = recompensas.get("coins");
        return coins instanceof Integer ? (Integer) coins : 0;
    }
    
    /**
     * Obtiene el texto de precio formateado.
     */
    public String getPriceFormatted() {
        return String.format("%.2f %s", getPrice(), currency);
    }
    
    @Override
    public String toString() {
        return "PaymentProduct{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + getPrice() + currency +
                ", type='" + type + '\'' +
                '}';
    }
}
