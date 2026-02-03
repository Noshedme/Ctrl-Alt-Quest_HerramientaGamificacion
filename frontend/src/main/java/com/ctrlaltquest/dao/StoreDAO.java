package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.StoreOffer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar ofertas de la tienda y compras con dinero del juego (coins).
 * Conecta con: store_offers, coin_transactions, user_inventory
 */
public class StoreDAO {

    /**
     * Obtiene todas las ofertas de la tienda categorizadas.
     * @return Lista de StoreOffer
     */
    public static List<StoreOffer> obtenerOfertas() {
        System.out.println("🛍️ StoreDAO: Cargando ofertas de BD...");
        List<StoreOffer> ofertas = new ArrayList<>();
        
        String sql = "SELECT id, offer_name, description, coin_price, is_featured, offer_type, metadata " +
                     "FROM public.store_offers WHERE is_active = true ORDER BY is_featured DESC, id ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("offer_name");
                String description = rs.getString("description");
                int price = rs.getInt("coin_price");
                boolean isFeatured = rs.getBoolean("is_featured");
                String offerType = rs.getString("offer_type");
                String metadata = rs.getString("metadata");
                
                // isPremium = true si el precio es 0 coins (significa que es dinero real)
                boolean isPremium = price == 0;
                
                StoreOffer offer = new StoreOffer(id, title, description, price, isPremium, "🎁");
                ofertas.add(offer);
                
                System.out.println("✅ Oferta cargada: " + title + " - " + price + " coins");
            }
            
            System.out.println("✅ Total ofertas: " + ofertas.size());
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando ofertas: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ofertas;
    }

    /**
     * Compra un item con dinero del juego (coins).
     * @param userId ID del usuario
     * @param offerId ID de la oferta
     * @return true si la compra fue exitosa
     */
    public static boolean comprarConCoins(int userId, int offerId) {
        System.out.println("💰 StoreDAO: Comprando oferta " + offerId + " para usuario " + userId);
        
        String sqlGetOffer = "SELECT id, offer_name, coin_price FROM public.store_offers WHERE id = ?";
        String sqlGetCoins = "SELECT coins FROM public.users WHERE id = ?";
        String sqlUpdateCoins = "UPDATE public.users SET coins = coins - ? WHERE id = ?";
        String sqlLogTransaction = "INSERT INTO public.coin_transactions (user_id, amount, reason, ref_type, ref_id) " +
                                   "VALUES (?, ?, 'Compra en Tienda', 'STORE_OFFER', ?)";
        String sqlAddToInventory = "INSERT INTO public.user_inventory (user_id, item_id, quantity) " +
                                   "SELECT ?, item_id, quantity FROM public.store_offer_items WHERE offer_id = ? " +
                                   "ON CONFLICT (user_id, item_id) DO UPDATE SET quantity = quantity + EXCLUDED.quantity";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Obtener precio de la oferta
            int priceCoins = 0;
            String offerName = "";
            try (PreparedStatement pst = conn.prepareStatement(sqlGetOffer)) {
                pst.setInt(1, offerId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    priceCoins = rs.getInt("coin_price");
                    offerName = rs.getString("offer_name");
                }
            }
            
            if (priceCoins <= 0) {
                System.err.println("❌ Oferta inválida o es premium");
                return false;
            }
            
            // 2. Verificar que usuario tiene suficientes coins
            int userCoins = 0;
            try (PreparedStatement pst = conn.prepareStatement(sqlGetCoins)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    userCoins = rs.getInt("coins");
                }
            }
            
            if (userCoins < priceCoins) {
                System.err.println("❌ Insufficient coins. User has " + userCoins + ", needs " + priceCoins);
                conn.rollback();
                return false;
            }
            
            // 3. Descontar coins
            try (PreparedStatement pst = conn.prepareStatement(sqlUpdateCoins)) {
                pst.setInt(1, priceCoins);
                pst.setInt(2, userId);
                pst.executeUpdate();
            }
            
            // 4. Registrar transacción
            try (PreparedStatement pst = conn.prepareStatement(sqlLogTransaction)) {
                pst.setInt(1, userId);
                pst.setInt(2, -priceCoins);
                pst.setInt(3, offerId);
                pst.executeUpdate();
            }
            
            // 5. Añadir items al inventario
            try (PreparedStatement pst = conn.prepareStatement(sqlAddToInventory)) {
                pst.setInt(1, userId);
                pst.setInt(2, offerId);
                pst.executeUpdate();
            }
            
            conn.commit();
            System.out.println("✅ Compra exitosa: " + offerName + " por " + priceCoins + " coins");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error en compra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el inventario del usuario.
     * @param userId ID del usuario
     * @return Lista de items en inventario
     */
    public static List<java.util.Map<String, Object>> obtenerInventario(int userId) {
        System.out.println("📦 StoreDAO: Obteniendo inventario para usuario " + userId);
        List<java.util.Map<String, Object>> items = new ArrayList<>();
        
        String sql = "SELECT ui.item_id, i.name, i.type, ui.quantity, ui.equipped " +
                     "FROM public.user_inventory ui " +
                     "JOIN public.items i ON ui.item_id = i.id " +
                     "WHERE ui.user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", rs.getInt("item_id"));
                item.put("name", rs.getString("name"));
                item.put("type", rs.getString("type"));
                item.put("quantity", rs.getInt("quantity"));
                item.put("equipped", rs.getBoolean("equipped"));
                items.add(item);
            }
            
            System.out.println("✅ Inventario cargado: " + items.size() + " tipos de items");
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando inventario: " + e.getMessage());
        }
        
        return items;
    }

    /**
     * Verifica si un item es un boost activable.
     * @param itemId ID del item
     * @return true si es un boost
     */
    public static boolean esBoost(int itemId) {
        String sql = "SELECT type FROM public.items WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String type = rs.getString("type");
                return type != null && (type.contains("boost") || type.contains("potion"));
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando tipo de item: " + e.getMessage());
        }
        
        return false;
    }
}
