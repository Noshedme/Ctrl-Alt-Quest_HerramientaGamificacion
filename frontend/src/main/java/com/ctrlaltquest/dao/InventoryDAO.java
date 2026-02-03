package com.ctrlaltquest.dao;

import com.ctrlaltquest.models.Item;
import com.ctrlaltquest.models.InventoryItem;
import com.ctrlaltquest.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar el inventario del usuario.
 * Maneja agregar items, equipar/desequipar, consumir boosts, etc.
 */
public class InventoryDAO {

    /**
     * Obtiene todo el inventario de un usuario.
     * @param userId ID del usuario
     * @return Lista de items en el inventario
     */
    public static List<InventoryItem> obtenerInventario(int userId) {
        List<InventoryItem> inventario = new ArrayList<>();
        
        String sql = "SELECT ui.id, ui.user_id, ui.item_id, i.name, i.type, i.description, " +
                     "i.rarity, i.price, i.image_path, i.boost_duration_seconds, i.boost_multiplier, " +
                     "ui.quantity, ui.equipped, ui.acquired_at " +
                     "FROM user_inventory ui " +
                     "JOIN items i ON ui.item_id = i.id " +
                     "WHERE ui.user_id = ? AND ui.quantity > 0 " +
                     "ORDER BY ui.acquired_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Item item = new Item(
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("description"),
                    rs.getString("rarity"),
                    rs.getBoolean("equipped"),
                    rs.getInt("price"),
                    rs.getString("image_path"),
                    rs.getObject("boost_duration_seconds") != null ? 
                        rs.getInt("boost_duration_seconds") : null,
                    rs.getObject("boost_multiplier") != null ? 
                        rs.getInt("boost_multiplier") : null
                );
                
                InventoryItem invItem = new InventoryItem(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    item,
                    rs.getInt("quantity"),
                    rs.getBoolean("equipped"),
                    rs.getLong("acquired_at")
                );
                
                inventario.add(invItem);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener inventario: " + e.getMessage());
            e.printStackTrace();
        }
        
        return inventario;
    }
    
    /**
     * Obtiene un item específico del inventario.
     * @param userId ID del usuario
     * @param itemId ID del item
     * @return InventoryItem si existe, null si no
     */
    public static InventoryItem obtenerItemInventario(int userId, int itemId) {
        String sql = "SELECT ui.id, ui.user_id, ui.item_id, i.name, i.type, i.description, " +
                     "i.rarity, i.price, i.image_path, i.boost_duration_seconds, i.boost_multiplier, " +
                     "ui.quantity, ui.equipped, ui.acquired_at " +
                     "FROM user_inventory ui " +
                     "JOIN items i ON ui.item_id = i.id " +
                     "WHERE ui.user_id = ? AND ui.item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Item item = new Item(
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("description"),
                    rs.getString("rarity"),
                    rs.getBoolean("equipped"),
                    rs.getInt("price"),
                    rs.getString("image_path"),
                    rs.getObject("boost_duration_seconds") != null ? 
                        rs.getInt("boost_duration_seconds") : null,
                    rs.getObject("boost_multiplier") != null ? 
                        rs.getInt("boost_multiplier") : null
                );
                
                return new InventoryItem(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    item,
                    rs.getInt("quantity"),
                    rs.getBoolean("equipped"),
                    rs.getLong("acquired_at")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener item del inventario: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Equipa un item (para cosméticos).
     * @param userId ID del usuario
     * @param itemId ID del item
     * @return true si fue exitoso
     */
    public static boolean equiparItem(int userId, int itemId) {
        // Primero, desequipar todos los items del mismo tipo
        String desequiparSQL = "UPDATE user_inventory " +
                              "SET equipped = false " +
                              "WHERE user_id = ? AND item_id IN (" +
                              "  SELECT id FROM items WHERE type = " +
                              "  (SELECT type FROM items WHERE id = ?)" +
                              ")";
        
        // Luego, equipar el nuevo item
        String equiparSQL = "UPDATE user_inventory " +
                           "SET equipped = true " +
                           "WHERE user_id = ? AND item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Desequipar items del mismo tipo
            try (PreparedStatement stmt = conn.prepareStatement(desequiparSQL)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, itemId);
                stmt.executeUpdate();
            }
            
            // Equipar nuevo item
            try (PreparedStatement stmt = conn.prepareStatement(equiparSQL)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, itemId);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error al equipar item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Desequipa un item.
     * @param userId ID del usuario
     * @param itemId ID del item
     * @return true si fue exitoso
     */
    public static boolean desequiparItem(int userId, int itemId) {
        String sql = "UPDATE user_inventory " +
                    "SET equipped = false " +
                    "WHERE user_id = ? AND item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error al desequipar item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reduce la cantidad de un item en 1 (para consumibles/boosts).
     * @param userId ID del usuario
     * @param itemId ID del item
     * @return true si fue exitoso
     */
    public static boolean consumirItem(int userId, int itemId) {
        String sql = "UPDATE user_inventory " +
                    "SET quantity = quantity - 1 " +
                    "WHERE user_id = ? AND item_id = ? AND quantity > 0";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            int updated = stmt.executeUpdate();
            return updated > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al consumir item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Agrega un item al inventario (incrementa cantidad o lo crea si no existe).
     * @param userId ID del usuario
     * @param itemId ID del item
     * @param cantidad Cantidad a agregar
     * @return true si fue exitoso
     */
    public static boolean agregarAlInventario(int userId, int itemId, int cantidad) {
        if (cantidad <= 0) return false;
        
        String sql = "INSERT INTO user_inventory (user_id, item_id, quantity, equipped, acquired_at) " +
                    "VALUES (?, ?, ?, false, ?) " +
                    "ON CONFLICT(user_id, item_id) DO UPDATE " +
                    "SET quantity = user_inventory.quantity + ?, updated_at = ? " +
                    "WHERE user_inventory.user_id = ? AND user_inventory.item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            long ahora = System.currentTimeMillis();
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, cantidad);
            stmt.setLong(4, ahora);
            stmt.setInt(5, cantidad);
            stmt.setLong(6, ahora);
            stmt.setInt(7, userId);
            stmt.setInt(8, itemId);
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error al agregar item al inventario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica si el usuario tiene un item.
     * @param userId ID del usuario
     * @param itemId ID del item
     * @return true si tiene el item
     */
    public static boolean tieneItem(int userId, int itemId) {
        String sql = "SELECT 1 FROM user_inventory " +
                    "WHERE user_id = ? AND item_id = ? AND quantity > 0 LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            return stmt.executeQuery().next();
            
        } catch (SQLException e) {
            System.err.println("Error al verificar item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
