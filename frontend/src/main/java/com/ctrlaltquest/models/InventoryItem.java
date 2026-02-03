package com.ctrlaltquest.models;

/**
 * Representa un item en el inventario del usuario.
 * Contiene información del item y estado de posesión.
 */
public class InventoryItem {
    private int inventoryId; // ID del registro en user_inventory
    private int userId;
    private Item item;
    private int quantity; // Cantidad que tiene (para boosts, potions, etc.)
    private boolean equipped; // Si está equipado (para cosméticos)
    private long acquiredAt; // Timestamp de cuándo fue adquirido
    
    public InventoryItem(int inventoryId, int userId, Item item, int quantity, boolean equipped, long acquiredAt) {
        this.inventoryId = inventoryId;
        this.userId = userId;
        this.item = item;
        this.quantity = quantity;
        this.equipped = equipped;
        this.acquiredAt = acquiredAt;
    }
    
    public InventoryItem(int userId, Item item, int quantity) {
        this.userId = userId;
        this.item = item;
        this.quantity = quantity;
        this.equipped = false;
    }
    
    // Getters
    public int getInventoryId() { return inventoryId; }
    public int getUserId() { return userId; }
    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public boolean isEquipped() { return equipped; }
    public long getAcquiredAt() { return acquiredAt; }
    
    // Setters
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
    
    /**
     * Comprueba si este item es un boost activable.
     */
    public boolean isBoost() {
        return item != null && item.isBoost();
    }
    
    /**
     * Reduce la cantidad en 1 (para boosts consumibles).
     */
    public void consumeOne() {
        if (quantity > 0) {
            quantity--;
        }
    }
    
    @Override
    public String toString() {
        return "InventoryItem{" +
                "item=" + item.getName() +
                ", quantity=" + quantity +
                ", equipped=" + equipped +
                '}';
    }
}
