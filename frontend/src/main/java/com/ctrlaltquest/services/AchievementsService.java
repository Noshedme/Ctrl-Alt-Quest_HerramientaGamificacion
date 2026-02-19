package com.ctrlaltquest.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.db.DatabaseConnection;

/**
 * AchievementsService - Gestión de logros/achievements desbloqueables
 * 
 * CARACTERÍSTICAS:
 * ✅ Verifica logros basados en condiciones (nivel, XP, misiones, etc)
 * ✅ Desbloquea logros y otorga recompensas
 * ✅ Integrado con XPSyncService para otorgar XP
 * ✅ Guarda progreso en BD
 */
public class AchievementsService {
    
    private static AchievementsService instance;
    
    private AchievementsService() {}
    
    public static synchronized AchievementsService getInstance() {
        if (instance == null) {
            instance = new AchievementsService();
        }
        return instance;
    }
    
    /**
     * Verifica si se deben desbloquear logros basados en condiciones del usuario
     */
    public void checkAchievementConditions(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Obtener todos los logros disponibles
            String sqlAchievements = "SELECT id, name, description, condition, xp_reward, coin_reward " +
                                    "FROM public.achievements WHERE id NOT IN " +
                                    "(SELECT achievement_id FROM public.user_achievements WHERE user_id = ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlAchievements)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    int achievementId = rs.getInt("id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    String condition = rs.getString("condition");
                    int xpReward = rs.getInt("xp_reward");
                    int coinReward = rs.getInt("coin_reward");
                    
                    // Verificar si se cumple la condición
                    if (checkCondition(userId, condition)) {
                        unlockAchievement(userId, achievementId, name, xpReward, coinReward);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error verificando logros: " + e.getMessage());
        }
    }
    
    /**
     * Desbloquea un logro para un usuario
     */
    private void unlockAchievement(int userId, int achievementId, String name, 
                                  int xpReward, int coinReward) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Insertar en user_achievements
            String sqlInsert = "INSERT INTO public.user_achievements (user_id, achievement_id, unlocked_at) " +
                             "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                             "ON CONFLICT (user_id, achievement_id) DO NOTHING";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, achievementId);
                
                int inserted = pstmt.executeUpdate();
                if (inserted > 0) {
                    System.out.println("🏆 ¡LOGRO DESBLOQUEADO! " + name + " (+" + xpReward + " XP)");
                    
                    // Otorgar recompensas
                    XPSyncService.getInstance().awardXPForAchievement(userId, achievementId, xpReward);
                    if (coinReward > 0) {
                        awardCoinsForAchievement(userId, achievementId, coinReward);
                    }
                }
            }
            
            conn.commit();
            
        } catch (SQLException e) {
            System.err.println("⚠️ Error desbloqueando logro: " + e.getMessage());
        }
    }
    
    /**
     * Verifica una condición de logro
     * Formato de condición: JSON con reglas
     * Ej: {"type": "level", "value": 10} = Alcanzar nivel 10
     * Ej: {"type": "total_xp", "value": 50000} = Ganar 50000 XP total
     */
    private boolean checkCondition(int userId, String condition) {
        try {
            // Obtener datos del usuario
            XPSyncService.UserXPData data = XPSyncService.getInstance().getUserXPData(userId);
            if (data == null) return false;
            
            // Parsear condición simple
            if (condition.contains("\"type\":\"level\"")) {
                // Extraer valor
                int requiredLevel = Integer.parseInt(condition.replaceAll("[^0-9]", ""));
                return data.level >= requiredLevel;
            } else if (condition.contains("\"type\":\"total_xp\"")) {
                int requiredXP = Integer.parseInt(condition.replaceAll("[^0-9]", ""));
                return data.totalXP >= requiredXP;
            } else if (condition.contains("\"type\":\"missions_completed\"")) {
                // Contar misiones completadas
                int required = Integer.parseInt(condition.replaceAll("[^0-9]", ""));
                int completed = countCompletedMissions(userId);
                return completed >= required;
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error verificando condición de logro: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Cuenta misiones completadas por el usuario
     */
    private int countCompletedMissions(int userId) {
        String sql = "SELECT COUNT(*) as count FROM public.missions WHERE user_id = ? AND completed = true";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error contando misiones: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Otorga monedas por logro
     */
    private void awardCoinsForAchievement(int userId, int achievementId, int coins) {
        String sql = "INSERT INTO public.coin_transactions (user_id, amount, reason, ref_type, ref_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP); " +
                     "UPDATE public.users SET coins = coins + ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, coins);
            pstmt.setString(3, "Achievement Reward");
            pstmt.setString(4, "achievement");
            pstmt.setInt(5, achievementId);
            pstmt.setInt(6, coins);
            pstmt.setInt(7, userId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("⚠️ Error otorgando monedas: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene todos los logros desbloqueados por un usuario
     */
    public List<Achievement> getUnlockedAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        String sql = "SELECT a.id, a.name, a.description, ua.unlocked_at " +
                    "FROM public.achievements a " +
                    "INNER JOIN public.user_achievements ua ON a.id = ua.achievement_id " +
                    "WHERE ua.user_id = ? " +
                    "ORDER BY ua.unlocked_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                achievements.add(new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getTimestamp("unlocked_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo logros: " + e.getMessage());
        }
        
        return achievements;
    }
    
    /**
     * Modelo simple de Achievement
     */
    public static class Achievement {
        public int id;
        public String name;
        public String description;
        public java.sql.Timestamp unlockedAt;
        
        public Achievement(int id, String name, String description, java.sql.Timestamp unlockedAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.unlockedAt = unlockedAt;
        }
    }
}
