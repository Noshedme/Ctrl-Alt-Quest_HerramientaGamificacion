package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Achievement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AchievementsDAO - Data Access Object para gestionar logros del usuario.
 * Maneja búsqueda, verificación y otorgamiento de logros.
 */
public class AchievementsDAO {

    /**
     * Obtiene todos los logros del usuario.
     * 
     * @param userId ID del usuario
     * @return Lista de logros desbloqueados
     */
    public static List<Achievement> getAchievementsForUser(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        String sql = "SELECT a.id, a.name, a.description, a.xp_reward, a.coin_reward, ua.unlocked_at " +
                     "FROM public.achievements a " +
                     "JOIN public.user_achievements ua ON a.id = ua.achievement_id " +
                     "WHERE ua.user_id = ? " +
                     "ORDER BY ua.unlocked_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getTimestamp("unlocked_at") != null,
                    false,
                    "achievement_" + rs.getInt("id")
                );
                achievements.add(achievement);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando logros: " + e.getMessage());
        }
        
        return achievements;
    }

    /**
     * Obtiene todos los logros disponibles (desbloqueados y bloqueados).
     * 
     * @param userId ID del usuario
     * @return Lista de todos los logros
     */
    public static List<Achievement> getAllAvailableAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        String sql = "SELECT a.id, a.name, a.description, a.xp_reward, a.coin_reward, " +
                     "CASE WHEN ua.id IS NOT NULL THEN TRUE ELSE FALSE END as unlocked, " +
                     "ua.unlocked_at " +
                     "FROM public.achievements a " +
                     "LEFT JOIN public.user_achievements ua ON a.id = ua.achievement_id AND ua.user_id = ? " +
                     "ORDER BY CASE WHEN ua.id IS NOT NULL THEN 0 ELSE 1 END, a.name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("unlocked") == 1,
                    false,
                    "achievement_" + rs.getInt("id")
                );
                achievements.add(achievement);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando logros disponibles: " + e.getMessage());
        }
        
        return achievements;
    }

    /**
     * Verifica si un usuario ya tiene desbloqueado un logro.
     * 
     * @param userId ID del usuario
     * @param achievementId ID del logro
     * @return true si está desbloqueado
     */
    public static boolean hasAchievement(int userId, int achievementId) {
        String sql = "SELECT 1 FROM public.user_achievements " +
                     "WHERE user_id = ? AND achievement_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, achievementId);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando logro: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el progreso hacia un logro específico.
     * Útil para mostrar barras de progreso en la UI.
     * 
     * @param userId ID del usuario
     * @param achievementId ID del logro
     * @return Progreso en porcentaje (0-100)
     */
    public static int getAchievementProgress(int userId, int achievementId) {
        // Esta es una implementación simplificada
        // En producción, cada logro tendría su propia lógica de progreso
        
        if (hasAchievement(userId, achievementId)) {
            return 100; // Ya desbloqueado
        }
        
        // Para logros no desbloqueados, calcular progreso basado en condiciones
        String sql = "SELECT condition FROM public.achievements WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, achievementId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String condition = rs.getString("condition");
                return calculateProgressForCondition(userId, condition, conn);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo progreso de logro: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * Calcula el progreso para una condición de logro.
     * 
     * @param userId ID del usuario
     * @param conditionJson JSON con la condición
     * @param conn Conexión a BD
     * @return Progreso en porcentaje (0-100)
     */
    private static int calculateProgressForCondition(int userId, String conditionJson, Connection conn) {
        try {
            // Ejemplos de condiciones y cómo calcular progreso:
            
            // "missions_completed": Contar misiones completadas
            if (conditionJson.contains("missions_completed")) {
                int required = extractIntFromJson(conditionJson, "count");
                int actual = countCompletedMissions(userId, conn);
                return Math.min(100, (actual * 100) / required);
            }
            
            // "total_xp": Total de XP acumulado
            if (conditionJson.contains("total_xp")) {
                int required = extractIntFromJson(conditionJson, "amount");
                int actual = getTotalXP(userId, conn);
                return Math.min(100, (actual * 100) / required);
            }
            
            // "consecutive_days": Días consecutivos activo
            if (conditionJson.contains("consecutive_days")) {
                int required = extractIntFromJson(conditionJson, "days");
                int actual = getConsecutiveDays(userId, conn);
                return Math.min(100, (actual * 100) / required);
            }
            
            // "level_reached": Alcanzar un nivel
            if (conditionJson.contains("level_reached")) {
                int required = extractIntFromJson(conditionJson, "level");
                int actual = getUserLevel(userId, conn);
                return Math.min(100, (actual * 100) / required);
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Error calculando progreso: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * Extrae un valor entero del JSON.
     */
    private static int extractIntFromJson(String json, String key) {
        try {
            String pattern = "\"" + key + "\": ";
            int startIdx = json.indexOf(pattern);
            if (startIdx == -1) return 0;
            
            startIdx += pattern.length();
            int endIdx = json.indexOf(",", startIdx);
            if (endIdx == -1) endIdx = json.indexOf("}", startIdx);
            
            return Integer.parseInt(json.substring(startIdx, endIdx).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Cuenta misiones completadas por el usuario.
     */
    private static int countCompletedMissions(int userId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM public.missions WHERE user_id = ? AND completed = true";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * Obtiene XP total del usuario.
     */
    private static int getTotalXP(int userId, Connection conn) throws SQLException {
        String sql = "SELECT total_xp FROM public.users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_xp");
            }
        }
        return 0;
    }

    /**
     * Obtiene el nivel actual del usuario.
     */
    private static int getUserLevel(int userId, Connection conn) throws SQLException {
        String sql = "SELECT level FROM public.users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("level");
            }
        }
        return 1;
    }

    /**
     * Calcula días consecutivos con actividad.
     * Simplificado: cuenta sesiones activas en los últimos N días.
     */
    private static int getConsecutiveDays(int userId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT DATE(session_start)) as count " +
                     "FROM public.activity_sessions " +
                     "WHERE user_id = ? AND session_start > CURRENT_TIMESTAMP - INTERVAL '30 days' " +
                     "ORDER BY DATE(session_start) DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * Obtiene el total de logros desbloqueados.
     */
    public static int getTotalUnlockedAchievements(int userId) {
        String sql = "SELECT COUNT(*) as count FROM public.user_achievements WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando logros: " + e.getMessage());
        }
        
        return 0;
    }
}
