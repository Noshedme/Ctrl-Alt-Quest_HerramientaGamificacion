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
     * Obtiene TODOS los logros de la base de datos (Normales y Secretos), 
     * marcando cuáles tiene desbloqueados el usuario y cuáles son ocultos.
     */
    public static List<Achievement> getAllAvailableAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        // LEFT JOIN para traer todos los logros + fecha de desbloqueo si existe.
        // Se añade 'a.is_hidden' para soportar los Easter Eggs.
        String sql = "SELECT a.id, a.name, a.description, a.xp_reward, a.coin_reward, a.is_hidden, " +
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
                // Leemos el flag de la BD (si tu tabla aún no tiene la columna, esto fallará hasta que ejecutes el SQL)
                // Si la columna no existe, por defecto asumimos false (no oculto)
                boolean hidden = false;
                try {
                    hidden = rs.getBoolean("is_hidden"); 
                } catch (SQLException e) {
                    // Si la columna no existe, ignoramos el error y queda hidden = false
                }
                
                // Creamos el objeto Achievement
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    unlocked,
                    hidden, 
                    null    // Icono por defecto (se gestiona en la vista)
                );
                
                achievements.add(achievement);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando logros disponibles: " + e.getMessage());
        }
        
        return achievements;
    }

    /**
     * Desbloquea un logro para el usuario en la base de datos.
     * @param userId ID del usuario
     * @param achievementId ID del logro a desbloquear
     * @return true si se desbloqueó (era nuevo), false si ya lo tenía.
     */
    public static boolean unlockAchievement(int userId, int achievementId) {
        // Usamos ON CONFLICT DO NOTHING para evitar errores si ya lo tiene
        String sql = "INSERT INTO public.user_achievements (user_id, achievement_id, unlocked_at) " +
                     "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (user_id, achievement_id) DO NOTHING";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, achievementId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Retorna true solo si fue una inserción nueva
            
        } catch (SQLException e) {
            System.err.println("❌ Error desbloqueando logro: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la información completa de un logro específico con las recompensas.
     * Útil para mostrar notificaciones cuando se desbloquea.
     */
    public static Achievement getAchievementById(int achievementId) {
        String sql = "SELECT id, name, description, xp_reward, coin_reward, is_hidden " +
                     "FROM public.achievements WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, achievementId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                boolean hidden = false;
                try {
                    hidden = rs.getBoolean("is_hidden");
                } catch (SQLException e) {
                    // Columna no existe aún
                }
                
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    false, // No importa el estado de desbloqueo aquí
                    hidden,
                    null
                );
                
                return achievement;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo logro por ID: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Obtiene todos los logros desbloqueados por un usuario.
     */
    public static List<Achievement> getUserUnlockedAchievements(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        
        String sql = "SELECT a.id, a.name, a.description, a.xp_reward, a.coin_reward, a.is_hidden, ua.unlocked_at " +
                     "FROM public.achievements a " +
                     "INNER JOIN public.user_achievements ua ON a.id = ua.achievement_id " +
                     "WHERE ua.user_id = ? " +
                     "ORDER BY ua.unlocked_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                boolean hidden = false;
                try {
                    hidden = rs.getBoolean("is_hidden");
                } catch (SQLException e) {
                    // Columna no existe
                }
                
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    true, // Todos están desbloqueados
                    hidden,
                    null
                );
                
                achievements.add(achievement);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando logros desbloqueados: " + e.getMessage());
        }
        
        return achievements;
    }

    /**
     * Calcula el progreso numérico (0-100%) de un logro específico basado en el JSON de la BBDD.
     */
    public static int getAchievementProgress(int userId, int achievementId) {
        // Si ya lo tiene, 100%
        if (hasAchievement(userId, achievementId)) return 100;

        String sql = "SELECT condition FROM public.achievements WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, achievementId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String conditionJson = rs.getString("condition");
                return calculateProgressForCondition(userId, conditionJson, conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Verifica si un usuario tiene un logro específico desbloqueado.
     */
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

    /**
     * Interpreta las condiciones JSON de tu base de datos y calcula el progreso actual.
     */
    private static int calculateProgressForCondition(int userId, String conditionJson, Connection conn) {
        if (conditionJson == null || conditionJson.equals("{}")) return 0;

        try {
            // LOGRO: Misiones Completadas ("missions_completed", "count")
            if (conditionJson.contains("missions_completed")) {
                int target = extractIntFromJson(conditionJson, "count");
                int current = countCompletedMissions(userId, conn);
                return calcPct(current, target);
            }
            
            // LOGRO: Nivel Alcanzado ("level_reached", "level")
            if (conditionJson.contains("level_reached")) {
                int target = extractIntFromJson(conditionJson, "level");
                int current = getUserLevel(userId, conn);
                return calcPct(current, target);
            }

            // LOGRO: XP Total ("total_xp", "amount")
            if (conditionJson.contains("total_xp")) {
                int target = extractIntFromJson(conditionJson, "amount");
                int current = getTotalXP(userId, conn);
                return calcPct(current, target);
            }

            // LOGRO: Días Consecutivos ("consecutive_days", "days")
            if (conditionJson.contains("consecutive_days")) {
                int target = extractIntFromJson(conditionJson, "days");
                int current = getHealthStreak(userId, conn);
                return calcPct(current, target);
            }

            // LOGROS SECRETOS (Eventos únicos):
            // Suelen ser binarios (0% o 100%), por lo que si no está en user_achievements (verificado arriba), es 0%.
            
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

    // --- Consultas Auxiliares ---

    private static int countCompletedMissions(int userId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM public.missions WHERE user_id = ? AND completed = true";
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

    // Parser JSON manual robusto (sin librerías externas)
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
                if (Character.isDigit(c)) {
                    valueStart = i;
                    break;
                }
            }
            if (valueStart == -1) return 0;

            int valueEnd = valueStart + 1;
            while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
                valueEnd++;
            }

            String valueStr = json.substring(valueStart, valueEnd);
            return Integer.parseInt(valueStr);
        } catch (Exception e) {
            return 0;
        }
    }
}