package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * DAO para gestionar pagos reales (Stripe, PayPal, etc.).
 * Conecta con: payment_orders, payment_transactions, coin_transactions
 */
public class PaymentDAO {

    /**
     * Crea una orden de pago pendiente.
     * @param userId ID del usuario
     * @param productId ID del producto a comprar
     * @param deviceId ID del dispositivo (opcional)
     * @return UUID de la orden, null si falla
     */
    public static String crearOrdenPago(int userId, int productId, Integer deviceId) {
        System.out.println("💳 PaymentDAO: Creando orden de pago para usuario " + userId);
        
        String sql = "INSERT INTO public.payment_orders (user_id, device_id, product_id, order_uuid, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, gen_random_uuid(), 'created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING order_uuid";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            if (deviceId != null) {
                pstmt.setInt(2, deviceId);
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }
            pstmt.setInt(3, productId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String orderUUID = rs.getString("order_uuid");
                System.out.println("✅ Orden creada: " + orderUUID);
                return orderUUID;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error creando orden: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Registra una transacción de pago.
     * @param orderUUID UUID de la orden
     * @param provider Proveedor (stripe, paypal, etc)
     * @param providerTxId ID de transacción del proveedor
     * @param amountCents Monto en centavos (ahora acepta long)
     * @param currency Moneda (USD, EUR, etc)
     * @param status Estado (success, pending, failed)
     * @param rawPayload Respuesta JSON del proveedor
     * @return true si se registró exitosamente
     */
    public static boolean registrarTransaccion(String orderUUID, String provider, String providerTxId, 
                                              long amountCents, String currency, String status, String rawPayload) {
        System.out.println("📝 PaymentDAO: Registrando transacción de pago...");
        
        String sqlGetOrder = "SELECT id FROM public.payment_orders WHERE order_uuid::text = ?";
        String sqlInsertTx = "INSERT INTO public.payment_transactions (order_id, provider, provider_tx_id, amount_cents, currency, status, raw_payload, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)";
        String sqlUpdateOrder = "UPDATE public.payment_orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_uuid::text = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Obtener ID de la orden
            int orderId = -1;
            try (PreparedStatement pst = conn.prepareStatement(sqlGetOrder)) {
                pst.setString(1, orderUUID);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    orderId = rs.getInt("id");
                }
            }
            
            if (orderId == -1) {
                System.err.println("❌ Orden no encontrada: " + orderUUID);
                conn.rollback();
                return false;
            }
            
            // 2. Registrar transacción
            try (PreparedStatement pst = conn.prepareStatement(sqlInsertTx)) {
                pst.setInt(1, orderId);
                pst.setString(2, provider);
                pst.setString(3, providerTxId);
                pst.setLong(4, amountCents); // ✅ Ahora acepta long
                pst.setString(5, currency);
                pst.setString(6, status);
                pst.setString(7, rawPayload != null ? rawPayload : "{}");
                pst.executeUpdate();
            }
            
            // 3. Actualizar estado de la orden
            try (PreparedStatement pst = conn.prepareStatement(sqlUpdateOrder)) {
                pst.setString(1, status);
                pst.setString(2, orderUUID);
                pst.executeUpdate();
            }
            
            conn.commit();
            System.out.println("✅ Transacción registrada: " + providerTxId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error registrando transacción: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sobrecarga para compatibilidad con int
     */
    public static boolean registrarTransaccion(String orderUUID, String provider, String providerTxId, 
                                              int amountCents, String currency, String status, String rawPayload) {
        return registrarTransaccion(orderUUID, provider, providerTxId, (long) amountCents, currency, status, rawPayload);
    }

    /**
     * Procesa una compra exitosa: otorga coins/items al usuario.
     * @param orderUUID UUID de la orden
     * @return true si se procesó exitosamente
     */
    public static boolean procesarCompraExitosa(String orderUUID) {
        System.out.println("🎉 PaymentDAO: Procesando compra exitosa para orden " + orderUUID);
        
        String sqlGetOrder = "SELECT user_id, product_id FROM public.payment_orders WHERE order_uuid::text = ?";
        String sqlGetProduct = "SELECT coins_amount FROM public.payment_product_rewards WHERE product_id = ? LIMIT 1";
        String sqlAddCoins = "UPDATE public.users SET coins = coins + ? WHERE id = ?";
        String sqlAddToInventory = "INSERT INTO public.user_inventory (user_id, item_id, quantity) " +
                                   "SELECT ?, item_id, quantity FROM public.payment_product_rewards WHERE product_id = ? " +
                                   "ON CONFLICT (user_id, item_id) DO UPDATE SET quantity = user_inventory.quantity + EXCLUDED.quantity";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Obtener datos de la orden
            int userId = -1;
            int productId = -1;
            try (PreparedStatement pst = conn.prepareStatement(sqlGetOrder)) {
                pst.setString(1, orderUUID);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                    productId = rs.getInt("product_id");
                }
            }
            
            if (userId == -1 || productId == -1) {
                System.err.println("❌ Orden o usuario no encontrado");
                conn.rollback();
                return false;
            }
            
            // 2. Obtener coins del producto
            int coinsAmount = 0;
            try (PreparedStatement pst = conn.prepareStatement(sqlGetProduct)) {
                pst.setInt(1, productId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    coinsAmount = rs.getInt("coins_amount");
                }
            }
            
            // 3. Otorgar coins
            if (coinsAmount > 0) {
                try (PreparedStatement pst = conn.prepareStatement(sqlAddCoins)) {
                    pst.setInt(1, coinsAmount);
                    pst.setInt(2, userId);
                    pst.executeUpdate();
                }
                
                // Registrar transacción de coins en coin_transactions
                UserDAO.registrarTransaccionCoins(userId, coinsAmount, "Compra Premium - Orden: " + orderUUID);
            }
            
            // 4. Añadir items al inventario
            try (PreparedStatement pst = conn.prepareStatement(sqlAddToInventory)) {
                pst.setInt(1, userId);
                pst.setInt(2, productId);
                pst.executeUpdate();
            }
            
            conn.commit();
            System.out.println("✅ Compra procesada: Usuario " + userId + " recibió " + coinsAmount + " coins");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error procesando compra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el estado de una orden.
     * @param orderUUID UUID de la orden
     * @return Estado (created, pending, success, failed)
     */
    public static String obtenerEstadoOrden(String orderUUID) {
        String sql = "SELECT status FROM public.payment_orders WHERE order_uuid::text = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, orderUUID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("status");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estado: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Obtiene el ID de orden a partir del UUID
     * @param orderUUID UUID de la orden
     * @return ID de la orden, -1 si no existe
     */
    public static int obtenerOrderId(String orderUUID) {
        String sql = "SELECT id FROM public.payment_orders WHERE order_uuid::text = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, orderUUID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo order ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }

    /**
     * Marca una orden como fallida
     * @param orderUUID UUID de la orden
     * @param reason Razón del fallo
     */
    public static void marcarOrdenFallida(String orderUUID, String reason) {
        String sql = "UPDATE public.payment_orders SET status = 'failed', updated_at = CURRENT_TIMESTAMP WHERE order_uuid::text = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, orderUUID);
            pstmt.executeUpdate();
            
            System.out.println("⚠️ Orden marcada como fallida: " + orderUUID + " - Razón: " + reason);
            
        } catch (SQLException e) {
            System.err.println("❌ Error marcando orden como fallida: " + e.getMessage());
            e.printStackTrace();
        }
    }
}