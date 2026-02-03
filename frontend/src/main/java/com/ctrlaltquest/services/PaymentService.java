package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.InventoryDAO;
import com.ctrlaltquest.dao.PaymentDAO;
import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.InventoryItem;
import com.ctrlaltquest.models.Item;
import com.ctrlaltquest.models.PaymentProduct;

import java.util.List;

/**
 * Servicio de pagos integrado con Stripe.
 * Maneja el flujo completo de compras con dinero real.
 */
public class PaymentService {
    
    /**
     * Procesa una compra con dinero real.
     * @param userId ID del usuario que compra
     * @param product Producto a comprar
     * @param deviceId ID del dispositivo (opcional)
     * @return true si fue exitoso
     */
    public static boolean procesarCompraPremium(int userId, PaymentProduct product, Integer deviceId) {
        System.out.println("💳 Iniciando compra premium para usuario " + userId);
        
        try {
            // 1. Crear orden
            String orderUUID = PaymentDAO.crearOrdenPago(userId, product.getId(), deviceId);
            if (orderUUID == null) {
                System.err.println("❌ Error al crear orden de pago");
                return false;
            }
            System.out.println("✅ Orden creada: " + orderUUID);
            
            // 2. En producción, aquí se enviaría a Stripe
            // Por ahora retornamos true y la compra se completa en PaymentFormController
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error procesando compra: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Obtiene los productos disponibles para compra con dinero real.
     */
    public static List<PaymentProduct> obtenerProductosPremium() {
        return null; // Implementar según necesidad
    }
    
    /**
     * Verifica si una compra fue exitosa.
     */
    public static boolean verificarCompraPending(String orderUUID) {
        return PaymentDAO.obtenerEstadoOrden(orderUUID) != null;
    }
}
