package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.UserDAO;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Servicio para gestionar boosts de XP temporales.
 * Cuando el usuario consume un boost, gana 2x XP durante el período especificado.
 */
public class BoostService {

    private static final BoostService instance = new BoostService();
    
    // Map de userId -> BoostInfo
    private Map<Integer, BoostInfo> activeBoosts = new HashMap<>();
    
    // Multiplicador de XP cuando hay boost activo
    public static final int XP_MULTIPLIER = 2;
    
    public static BoostService getInstance() {
        return instance;
    }
    
    private BoostService() {}
    
    /**
     * Activa un boost de XP para un usuario.
     * @param userId ID del usuario
     * @param durationSeconds Duración en segundos (ej: 3600 para 1 hora)
     * @param itemName Nombre del item para mostrar en notificaciones
     */
    public void activarBoost(int userId, int durationSeconds, String itemName) {
        System.out.println("⚡ BoostService: Activando boost de " + durationSeconds + "s para usuario " + userId);
        
        // Cancelar boost anterior si existe
        if (activeBoosts.containsKey(userId)) {
            BoostInfo boostAnterior = activeBoosts.get(userId);
            boostAnterior.cancelar();
        }
        
        // Crear nuevo boost
        BoostInfo boost = new BoostInfo(userId, itemName, durationSeconds);
        activeBoosts.put(userId, boost);
        
        System.out.println("✅ Boost activado: 2x XP durante " + (durationSeconds / 60) + " minutos");
    }
    
    /**
     * Comprueba si un usuario tiene boost activo.
     * @param userId ID del usuario
     * @return true si tiene boost activo
     */
    public boolean tieneBoostActivo(int userId) {
        return activeBoosts.containsKey(userId) && !activeBoosts.get(userId).estaExpirado();
    }
    
    /**
     * Obtiene el multiplicador de XP para un usuario.
     * @param userId ID del usuario
     * @return 2 si tiene boost, 1 si no
     */
    public int obtenerMultiplicadorXP(int userId) {
        if (tieneBoostActivo(userId)) {
            return XP_MULTIPLIER;
        }
        return 1;
    }
    
    /**
     * Obtiene información del boost activo.
     * @param userId ID del usuario
     * @return Map con info del boost, o null si no hay
     */
    public Map<String, Object> obtenerInfoBoost(int userId) {
        if (!tieneBoostActivo(userId)) {
            return null;
        }
        
        BoostInfo boost = activeBoosts.get(userId);
        Map<String, Object> info = new HashMap<>();
        info.put("itemName", boost.itemName);
        info.put("tiempoRestante", boost.getTiempoRestante());
        info.put("tiempoRestanteFormato", boost.getTiempoRestanteFormato());
        return info;
    }
    
    /**
     * Clase interna para gestionar información de un boost.
     */
    private class BoostInfo {
        int userId;
        String itemName;
        long tiempoFin;
        Timer timer;
        
        BoostInfo(int userId, String itemName, int durationSeconds) {
            this.userId = userId;
            this.itemName = itemName;
            this.tiempoFin = System.currentTimeMillis() + (durationSeconds * 1000L);
            
            // Programar expiración
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("⏰ Boost expirado para usuario " + userId);
                    activeBoosts.remove(userId);
                }
            }, durationSeconds * 1000L);
        }
        
        boolean estaExpirado() {
            return System.currentTimeMillis() >= tiempoFin;
        }
        
        long getTiempoRestante() {
            long restante = tiempoFin - System.currentTimeMillis();
            return Math.max(0, restante / 1000); // En segundos
        }
        
        String getTiempoRestanteFormato() {
            long segundos = getTiempoRestante();
            if (segundos <= 0) return "Expirado";
            
            long minutos = segundos / 60;
            long secs = segundos % 60;
            
            if (minutos > 0) {
                return minutos + "m " + secs + "s";
            }
            return secs + "s";
        }
        
        void cancelar() {
            if (timer != null) {
                timer.cancel();
            }
        }
    }
}
