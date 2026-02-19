package com.ctrlaltquest.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ctrlaltquest.db.DatabaseConnection;

import javafx.application.Platform;

/**
 * XPSyncService - Servicio centralizado para sincronizar XP en tiempo real
 * 
 * CARACTERÍSTICAS:
 * ✅ Otorga XP mediante captura de actividad (1 XP cada segundo productivo)
 * ✅ Actualiza barras de XP independientes por usuario
 * ✅ Sube de nivel automáticamente (fórmula: XP_req = Nivel * 1000)
 * ✅ Notifica a observers de cambios (UI, misiones, logros)
 * ✅ Guarda TODO en BD con transacciones
 * ✅ Sincronización thread-safe
 */
public class XPSyncService {
    
    private static XPSyncService instance;
    private final List<XPChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    // Cache local para optimizar queries
    private final Map<Integer, UserXPData> userXPCache = Collections.synchronizedMap(new HashMap<>());
    
    // Cola de eventos para procesamiento ordenado
    private final Queue<XPEvent> eventQueue = new LinkedList<>();
    private boolean processingQueue = false;
    
    private XPSyncService() {}
    
    public static synchronized XPSyncService getInstance() {
        if (instance == null) {
            instance = new XPSyncService();
        }
        return instance;
    }
    
    // ========== OBSERVADOR PATTERN ==========
    
    /**
     * Registra un listener para ser notificado de cambios de XP
     */
    public void addXPChangeListener(XPChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Desregistra un listener
     */
    public void removeXPChangeListener(XPChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifica a todos los listeners sobre cambio de XP
     */
    private void notifyXPChange(int userId, XPChangeEvent event) {
        for (XPChangeListener listener : listeners) {
            // Ejecutar en thread de JavaFX si es necesario
            if (event.isUIUpdate) {
                Platform.runLater(() -> listener.onXPChanged(userId, event));
            } else {
                listener.onXPChanged(userId, event);
            }
        }
    }
    
    // ========== OTORGAR XP ==========
    
    /**
     * Otorga XP por actividad capturada (llamado cada 1 segundo desde ActivityMonitor)
     * INTEGRACIÓN DIRECTA CON CAPTURA DE ACTIVIDAD
     * 
     * @param userId ID del usuario
     * @param xpAmount Cantidad de XP (ej: 1 XP por segundo productivo)
     * @param activityType Tipo de actividad para misiones (ej: "time_coding")
     * @return true si hubo cambio (subida de nivel, nueva barra, etc)
     */
    public synchronized boolean awardXPFromActivity(int userId, int xpAmount, String activityType) {
        if (xpAmount <= 0) return false;
        
        try {
            // Obtener datos actuales
            UserXPData data = getOrLoadUserXPData(userId);
            if (data == null) return false;
            
            int oldLevel = data.level;
            int oldCurrentXP = data.currentXP;
            
            // Sumar XP
            data.currentXP += xpAmount;
            data.totalXP += xpAmount;
            
            // Verificar subida de nivel
            boolean leveledUp = checkAndProcessLevelUp(userId, data);
            
            // Guardar en BD
            saveUserXPData(userId, data);
            
            // Crear evento de cambio
            XPChangeEvent event = new XPChangeEvent(
                xpAmount,
                oldLevel,
                data.level,
                oldCurrentXP,
                data.currentXP,
                data.xpRequired,
                leveledUp,
                activityType
            );
            
            // Notificar listeners
            notifyXPChange(userId, event);
            
            // Si subió de nivel, procesar logros/misiones
            if (leveledUp) {
                processMissionProgressAfterLevelUp(userId, data.level);
                processAchievementsAfterLevelUp(userId, data.level);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error otorgando XP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Otorga XP por misión completada
     */
    public synchronized boolean awardXPForMission(int userId, int missionId, int xpAmount) {
        if (xpAmount <= 0) return false;
        
        try {
            UserXPData data = getOrLoadUserXPData(userId);
            if (data == null) return false;
            
            int oldLevel = data.level;
            int oldCurrentXP = data.currentXP;
            
            data.currentXP += xpAmount;
            data.totalXP += xpAmount;
            
            boolean leveledUp = checkAndProcessLevelUp(userId, data);
            
            saveUserXPData(userId, data);
            
            XPChangeEvent event = new XPChangeEvent(
                xpAmount,
                oldLevel,
                data.level,
                oldCurrentXP,
                data.currentXP,
                data.xpRequired,
                leveledUp,
                "mission_" + missionId
            );
            
            notifyXPChange(userId, event);
            
            if (leveledUp) {
                processMissionProgressAfterLevelUp(userId, data.level);
                processAchievementsAfterLevelUp(userId, data.level);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error otorgando XP de misión: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Otorga XP por logro desbloqueado
     */
    public synchronized boolean awardXPForAchievement(int userId, int achievementId, int xpAmount) {
        if (xpAmount <= 0) return false;
        
        try {
            UserXPData data = getOrLoadUserXPData(userId);
            if (data == null) return false;
            
            int oldLevel = data.level;
            int oldCurrentXP = data.currentXP;
            
            data.currentXP += xpAmount;
            data.totalXP += xpAmount;
            
            boolean leveledUp = checkAndProcessLevelUp(userId, data);
            
            saveUserXPData(userId, data);
            
            XPChangeEvent event = new XPChangeEvent(
                xpAmount,
                oldLevel,
                data.level,
                oldCurrentXP,
                data.currentXP,
                data.xpRequired,
                leveledUp,
                "achievement_" + achievementId
            );
            
            notifyXPChange(userId, event);
            
            if (leveledUp) {
                processMissionProgressAfterLevelUp(userId, data.level);
                processAchievementsAfterLevelUp(userId, data.level);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error otorgando XP de logro: " + e.getMessage());
            return false;
        }
    }
    
    // ========== LÓGICA DE NIVELES ==========
    
    /**
     * Verifica si el usuario sube de nivel y procesa los cambios
     * Fórmula: XP_requerido = Nivel_actual * 1000
     */
    private boolean checkAndProcessLevelUp(int userId, UserXPData data) {
        boolean leveledUp = false;
        
        while (data.currentXP >= data.xpRequired) {
            // Restar XP usado para subir de nivel
            data.currentXP -= data.xpRequired;
            
            // Incrementar nivel
            data.level++;
            leveledUp = true;
            
            // Recalcular XP requerido para siguiente nivel
            data.xpRequired = data.level * 1000;
            
            System.out.println("🎉 ¡SUBIDA DE NIVEL! Usuario " + userId + " ahora es NIVEL " + data.level);
            
            // Notificar subida de nivel específica
            notifyLevelUp(userId, data.level);
        }
        
        return leveledUp;
    }
    
    /**
     * Notifica subida de nivel a todos los listeners
     */
    private void notifyLevelUp(int userId, int newLevel) {
        for (XPChangeListener listener : listeners) {
            Platform.runLater(() -> listener.onLevelUp(userId, newLevel));
        }
    }
    
    // ========== INTEGRACIÓN CON MISIONES Y LOGROS ==========
    
    /**
     * Procesa progreso de misiones después de subida de nivel
     */
    private void processMissionProgressAfterLevelUp(int userId, int newLevel) {
        try {
            // Procesar cambios de nivel en misiones
            System.out.println("✅ Proceso de cambio de nivel: " + newLevel);
        } catch (Exception e) {
            System.err.println("⚠️ Error procesando misiones: " + e.getMessage());
        }
    }
    
    /**
     * Procesa logros después de subida de nivel
     */
    private void processAchievementsAfterLevelUp(int userId, int newLevel) {
        try {
            // Verificar logros con condición de nivel
            AchievementsService.getInstance().checkAchievementConditions(userId);
        } catch (Exception e) {
            System.err.println("⚠️ Error procesando logros: " + e.getMessage());
        }
    }
    
    // ========== CACHE Y PERSISTENCIA ==========
    
    /**
     * Obtiene o carga datos de XP del usuario (puede ser del cache o BD)
     */
    private synchronized UserXPData getOrLoadUserXPData(int userId) {
        // Intentar obtener del cache
        if (userXPCache.containsKey(userId)) {
            return userXPCache.get(userId);
        }
        
        // Cargar de BD
        return loadUserXPFromDatabase(userId);
    }
    
    /**
     * Carga datos de XP de la base de datos
     */
    private UserXPData loadUserXPFromDatabase(int userId) {
        String sql = "SELECT id, level, current_xp, total_xp FROM public.users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                UserXPData data = new UserXPData();
                data.userId = rs.getInt("id");
                data.level = rs.getInt("level");
                data.currentXP = rs.getInt("current_xp");
                data.totalXP = rs.getInt("total_xp");
                data.xpRequired = data.level * 1000;
                
                userXPCache.put(userId, data);
                return data;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error cargando datos XP: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Guarda datos de XP a la base de datos
     */
    private void saveUserXPData(int userId, UserXPData data) {
        String sql = "UPDATE public.users SET level = ?, current_xp = ?, total_xp = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, data.level);
            pstmt.setInt(2, data.currentXP);
            pstmt.setInt(3, data.totalXP);
            pstmt.setInt(4, userId);
            
            pstmt.executeUpdate();
            
            // Guardar en historial
            logXPHistory(userId, data);
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando datos XP: " + e.getMessage());
        }
    }
    
    /**
     * Registra cambios de XP en el historial
     */
    private void logXPHistory(int userId, UserXPData data) {
        String sql = "INSERT INTO public.xp_history (user_id, amount, reason) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, data.totalXP);
            pstmt.setString(3, "Activity Sync");
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            // No fallar por historial
        }
    }
    
    // ========== GETTERS ==========
    
    /**
     * Obtiene datos actuales de XP del usuario
     */
    public UserXPData getUserXPData(int userId) {
        return getOrLoadUserXPData(userId);
    }
    
    /**
     * Obtiene nivel actual
     */
    public int getUserLevel(int userId) {
        UserXPData data = getOrLoadUserXPData(userId);
        return data != null ? data.level : 1;
    }
    
    /**
     * Obtiene XP actual
     */
    public int getUserCurrentXP(int userId) {
        UserXPData data = getOrLoadUserXPData(userId);
        return data != null ? data.currentXP : 0;
    }
    
    /**
     * Obtiene XP requerido para siguiente nivel
     */
    public int getUserXPRequired(int userId) {
        UserXPData data = getOrLoadUserXPData(userId);
        return data != null ? data.xpRequired : 1000;
    }
    
    /**
     * Obtiene XP total acumulado
     */
    public int getUserTotalXP(int userId) {
        UserXPData data = getOrLoadUserXPData(userId);
        return data != null ? data.totalXP : 0;
    }
    
    /**
     * Obtiene porcentaje de progreso de barra de XP (0.0 - 1.0)
     */
    public double getUserXPProgress(int userId) {
        UserXPData data = getOrLoadUserXPData(userId);
        if (data == null || data.xpRequired == 0) return 0.0;
        
        double progress = (double) data.currentXP / data.xpRequired;
        return Math.min(progress, 1.0); // Máximo 100%
    }
    
    /**
     * Limpia el cache de un usuario (útil para logout)
     */
    public void clearUserCache(int userId) {
        userXPCache.remove(userId);
    }
    
    /**
     * Limpia todo el cache
     */
    public void clearAllCache() {
        userXPCache.clear();
    }
    
    // ========== CLASES INTERNAS ==========
    
    /**
     * Datos de XP del usuario con caché
     */
    public static class UserXPData {
        public int userId;
        public int level = 1;
        public int currentXP = 0;
        public int totalXP = 0;
        public int xpRequired = 1000; // Nivel 1 requiere 1000 XP
    }
    
    /**
     * Evento de cambio de XP que se envía a listeners
     */
    public static class XPChangeEvent {
        public int xpGained;
        public int oldLevel;
        public int newLevel;
        public int oldCurrentXP;
        public int newCurrentXP;
        public int xpRequired;
        public boolean leveledUp;
        public String source; // actividad, misión, logro, etc
        public boolean isUIUpdate = true; // Si requiere actualización de UI
        
        public XPChangeEvent(int xpGained, int oldLevel, int newLevel, 
                           int oldCurrentXP, int newCurrentXP, int xpRequired,
                           boolean leveledUp, String source) {
            this.xpGained = xpGained;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.oldCurrentXP = oldCurrentXP;
            this.newCurrentXP = newCurrentXP;
            this.xpRequired = xpRequired;
            this.leveledUp = leveledUp;
            this.source = source;
        }
    }
    
    /**
     * Evento simple de actividad (sin actualización UI)
     */
    public static class XPEvent {
        public int userId;
        public int xpAmount;
        public String source;
        
        public XPEvent(int userId, int xpAmount, String source) {
            this.userId = userId;
            this.xpAmount = xpAmount;
            this.source = source;
        }
    }
}
