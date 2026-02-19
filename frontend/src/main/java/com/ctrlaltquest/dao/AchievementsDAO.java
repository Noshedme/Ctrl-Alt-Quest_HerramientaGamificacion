package com.ctrlaltquest.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Achievement;

public class AchievementsDAO {

    /**
     * Obtiene TODOS los logros.
     * CORRECCIÓN: Se usa 'a.title' en lugar de 'a.name'.
     */
    public static List<Achievement> getAllAvailableAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        String sql = "SELECT a.id, a.title, a.description, a.xp_reward, a.coin_reward, a.is_hidden, " +
                     "ua.unlocked_at, " +
                     "CASE WHEN ua.id IS NOT NULL THEN 1 ELSE 0 END as is_unlocked " +
                     "FROM public.achievements a " +
                     "LEFT JOIN public.user_achievements ua ON a.id = ua.achievement_id AND ua.user_id = ? " +
                     "ORDER BY is_unlocked DESC, a.id ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                boolean unlocked = rs.getInt("is_unlocked") == 1;
                boolean hidden = rs.getBoolean("is_hidden"); 
                
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("title"), // Antes "name"
                    rs.getString("description"),
                    unlocked,
                    hidden, 
                    null
                );
                
                achievements.add(achievement);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando logros disponibles: " + e.getMessage());
        }
        
        return achievements;
    }

    public static boolean unlockAchievement(int userId, int achievementId) {
        String sql = "INSERT INTO public.user_achievements (user_id, achievement_id, unlocked_at) " +
                     "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (user_id, achievement_id) DO NOTHING";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, achievementId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error desbloqueando logro: " + e.getMessage());
            return false;
        }
    }

    public static Achievement getAchievementById(int achievementId) {
        // CORRECCIÓN: 'title' en lugar de 'name'
        String sql = "SELECT id, title, description, xp_reward, coin_reward, is_hidden " +
                     "FROM public.achievements WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, achievementId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    false,
                    rs.getBoolean("is_hidden"),
                    null
                );
                return achievement;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo logro por ID: " + e.getMessage());
        }
        
        return null;
    }

    public static List<Achievement> getUserUnlockedAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        // CORRECCIÓN: 'a.title' en lugar de 'a.name'
        String sql = "SELECT a.id, a.title, a.description, a.xp_reward, a.coin_reward, a.is_hidden, ua.unlocked_at " +
                     "FROM public.achievements a " +
                     "INNER JOIN public.user_achievements ua ON a.id = ua.achievement_id " +
                     "WHERE ua.user_id = ? " +
                     "ORDER BY ua.unlocked_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    true,
                    rs.getBoolean("is_hidden"),
                    null
                );
                achievements.add(achievement);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando logros desbloqueados: " + e.getMessage());
        }
        
        return achievements;
    }

    public static int getAchievementProgress(int userId, int achievementId) {
        if (hasAchievement(userId, achievementId)) return 100;

        // Intentamos leer la columna 'condition'. Si no existe en la BD (versión vieja), retornamos 0.
        String sql = "SELECT condition FROM public.achievements WHERE id = ?"; // Asegúrate de tener esta columna o quitar este bloque
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, achievementId);
            try {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String conditionJson = rs.getString("condition");
                    return calculateProgressForCondition(userId, conditionJson, conn);
                }
            } catch (SQLException ex) {
                // La columna 'condition' no existe en la BD, ignoramos.
                return 0;
            }
        } catch (SQLException e) {
            // Error de conexión general
            e.printStackTrace();
        }
        return 0;
    }

    private static boolean hasAchievement(int userId, int achievementId) {
        String sql = "SELECT 1 FROM public.user_achievements WHERE user_id = ? AND achievement_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, achievementId);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static int calculateProgressForCondition(int userId, String conditionJson, Connection conn) {
        if (conditionJson == null || conditionJson.equals("{}")) return 0;

        try {
            if (conditionJson.contains("missions_completed")) {
                int target = extractIntFromJson(conditionJson, "count");
                int current = countCompletedMissions(userId, conn);
                return calcPct(current, target);
            }
            // ... otros casos ...
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando progreso JSON: " + e.getMessage());
        }
        return 0;
    }

    private static int calcPct(int current, int target) {
        if (target <= 0) return 0;
        int pct = (current * 100) / target;
        return Math.min(100, pct);
    }

    // --- Consultas Auxiliares CORREGIDAS ---

    private static int countCompletedMissions(int userId, Connection conn) throws SQLException {
        // CORRECCIÓN CRÍTICA: No existe 'completed' en missions. 
        // Se busca en mission_progress donde percentage >= 100.
        String sql = "SELECT COUNT(*) FROM public.mission_progress WHERE user_id = ? AND progress_percentage >= 100";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static int getUserLevel(int userId, Connection conn) throws SQLException {
        String sql = "SELECT MAX(level) FROM public.characters WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private static int getTotalXP(int userId, Connection conn) throws SQLException {
        String sql = "SELECT SUM(current_xp) FROM public.characters WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    private static int getHealthStreak(int userId, Connection conn) throws SQLException {
        String sql = "SELECT MAX(health_streak) FROM public.characters WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static int extractIntFromJson(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return 0;
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return 0;
            int valueStart = -1;
            for (int i = colonIndex + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (Character.isDigit(c)) { valueStart = i; break; }
            }
            if (valueStart == -1) return 0;
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) { valueEnd++; }
            String valueStr = json.substring(valueStart, valueEnd);
            return Integer.parseInt(valueStr);
        } catch (Exception e) { return 0; }
    }
}