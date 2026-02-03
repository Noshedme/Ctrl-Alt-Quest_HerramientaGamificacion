package com.ctrlaltquest.dao;

import com.ctrlaltquest.models.PaymentProduct;
import com.ctrlaltquest.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para gestionar productos de pago con dinero real.
 * Conecta con la tabla payment_products y su contenido.
 */
public class PaymentProductDAO {

    /**
     * Obtiene todos los productos de pago disponibles.
     * @return Lista de productos
     */
    public static List<PaymentProduct> obtenerProductos() {
        List<PaymentProduct> productos = new ArrayList<>();
        
        String sql = "SELECT id, sku, name, description, price_cents, currency, " +
                     "image_path, type, is_active, created_at " +
                     "FROM payment_products " +
                     "WHERE is_active = true " +
                     "ORDER BY price_cents ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PaymentProduct producto = new PaymentProduct(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("price_cents"),
                    rs.getString("currency"),
                    rs.getString("image_path"),
                    rs.getString("type"),
                    obtenerRecompensasProducto(rs.getInt("id"))
                );
                
                productos.add(producto);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener productos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return productos;
    }
    
    /**
     * Obtiene un producto específico por ID.
     * @param productId ID del producto
     * @return PaymentProduct si existe, null si no
     */
    public static PaymentProduct obtenerProducto(int productId) {
        String sql = "SELECT id, sku, name, description, price_cents, currency, " +
                     "image_path, type, is_active, created_at " +
                     "FROM payment_products " +
                     "WHERE id = ? AND is_active = true";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new PaymentProduct(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("price_cents"),
                    rs.getString("currency"),
                    rs.getString("image_path"),
                    rs.getString("type"),
                    obtenerRecompensasProducto(rs.getInt("id"))
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener producto: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtiene las recompensas de un producto (coins, items, etc.).
     * @param productId ID del producto
     * @return Map con las recompensas
     */
    private static Map<String, Object> obtenerRecompensasProducto(int productId) {
        Map<String, Object> recompensas = new HashMap<>();
        
        String sql = "SELECT coins_reward, item_rewards, boost_rewards " +
                     "FROM payment_product_rewards " +
                     "WHERE product_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Integer coins = (Integer) rs.getObject("coins_reward");
                String items = rs.getString("item_rewards");
                String boosts = rs.getString("boost_rewards");
                
                if (coins != null) recompensas.put("coins", coins);
                if (items != null) recompensas.put("items", items); // JSON array
                if (boosts != null) recompensas.put("boosts", boosts); // JSON array
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener recompensas: " + e.getMessage());
            e.printStackTrace();
        }
        
        return recompensas;
    }
    
    /**
     * Obtiene productos por tipo (bundle, coin_pack, item, etc.).
     * @param type Tipo de producto
     * @return Lista de productos de ese tipo
     */
    public static List<PaymentProduct> obtenerProductosPorTipo(String type) {
        List<PaymentProduct> productos = new ArrayList<>();
        
        String sql = "SELECT id, sku, name, description, price_cents, currency, " +
                     "image_path, type, is_active, created_at " +
                     "FROM payment_products " +
                     "WHERE type = ? AND is_active = true " +
                     "ORDER BY price_cents ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, type);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PaymentProduct producto = new PaymentProduct(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("price_cents"),
                    rs.getString("currency"),
                    rs.getString("image_path"),
                    rs.getString("type"),
                    obtenerRecompensasProducto(rs.getInt("id"))
                );
                
                productos.add(producto);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener productos por tipo: " + e.getMessage());
            e.printStackTrace();
        }
        
        return productos;
    }
    
    /**
     * Crea un nuevo producto de pago.
     * @param sku SKU único del producto
     * @param name Nombre del producto
     * @param description Descripción
     * @param priceCents Precio en centavos
     * @param currency Moneda (USD, ARS, etc.)
     * @param imagePath Ruta de imagen
     * @param type Tipo (coin_pack, bundle, item, etc.)
     * @param recompensas Map con recompensas (coins, items, boosts)
     * @return ID del producto creado, -1 si falla
     */
    public static int crearProducto(String sku, String name, String description, 
                                     int priceCents, String currency, String imagePath, 
                                     String type, Map<String, Object> recompensas) {
        String sqlProduct = "INSERT INTO payment_products (sku, name, description, price_cents, " +
                           "currency, image_path, type, is_active, created_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP) " +
                           "RETURNING id";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            int productId = -1;
            
            // Crear producto
            try (PreparedStatement stmt = conn.prepareStatement(sqlProduct)) {
                stmt.setString(1, sku);
                stmt.setString(2, name);
                stmt.setString(3, description);
                stmt.setInt(4, priceCents);
                stmt.setString(5, currency);
                stmt.setString(6, imagePath);
                stmt.setString(7, type);
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    productId = rs.getInt("id");
                }
            }
            
            if (productId > 0 && recompensas != null) {
                // Crear recompensas
                String sqlRecompensas = "INSERT INTO payment_product_rewards (product_id, coins_reward, item_rewards, boost_rewards) " +
                                       "VALUES (?, ?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlRecompensas)) {
                    stmt.setInt(1, productId);
                    stmt.setObject(2, recompensas.get("coins"));
                    stmt.setObject(3, recompensas.get("items"));
                    stmt.setObject(4, recompensas.get("boosts"));
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            return productId;
            
        } catch (SQLException e) {
            System.err.println("Error al crear producto: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Actualiza el estado activo de un producto.
     * @param productId ID del producto
     * @param isActive true para activar, false para desactivar
     * @return true si fue exitoso
     */
    public static boolean actualizarEstado(int productId, boolean isActive) {
        String sql = "UPDATE payment_products SET is_active = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar estado del producto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
