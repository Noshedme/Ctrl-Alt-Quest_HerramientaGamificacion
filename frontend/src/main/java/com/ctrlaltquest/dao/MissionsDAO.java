package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Mission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MissionsDAO {

    // ==========================================
    // SECCIÓN 1: MÉTODOS PARA LA INTERFAZ (UI)
    // ==========================================

    public static List<Mission> getMisionesUsuario(int userId) {
        System.out.println("🔍 DEBUG [MissionsDAO]: getMisionesUsuario(userId=" + userId + ")");
        List<Mission> lista = new ArrayList<>();
        
        String sql = "SELECT id, title, category, difficulty, xp_reward, coin_reward, " +
                     "is_daily, is_weekly, progress, completed " +
                     "FROM public.missions WHERE user_id = ? ORDER BY completed ASC, created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            System.out.println("🔍 DEBUG [MissionsDAO]: Ejecutando SQL con userId=" + userId);
            ResultSet rs = pstmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String category = rs.getString("category");
                String difficulty = rs.getString("difficulty");
                int xp = rs.getInt("xp_reward");
                int coin = rs.getInt("coin_reward");
                boolean isDaily = rs.getBoolean("is_daily");
                boolean isWeekly = rs.getBoolean("is_weekly");
                int progressRaw = rs.getInt("progress");
                boolean completed = rs.getBoolean("completed");

                String type = isDaily ? "DIARIA" : (isWeekly ? "SEMANAL" : "CLASE");
                String description = "Objetivo de categoría " + category + " [" + difficulty + "].";
                double progressPct = Math.min(progressRaw / 100.0, 1.0);

                System.out.println("🔍 DEBUG [MissionsDAO]: Misión #" + (++count) + ": " + title + " (tipo=" + type + ", isDaily=" + isDaily + ", isWeekly=" + isWeekly + ")");

                lista.add(new Mission(id, title, description, type, xp, coin, progressPct, completed));
            }
            System.out.println("🔍 DEBUG [MissionsDAO]: Total misiones encontradas: " + count);
        } catch (SQLException e) {
            System.err.println("❌ Error cargando misiones: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // ==========================================
    // SECCIÓN 2: LÓGICA DEL MOTOR DE JUEGO
    // ==========================================

    /**
     * Actualiza el progreso en la tabla mission_progress y devuelve IDs de misiones completadas.
     */
    public static List<Integer> actualizarProgreso(int userId, String metricKey, int amountToAdd) {
        List<Integer> completedMissions = new ArrayList<>();
        
        // 1. Buscar misiones activas que usen esta métrica
        String sqlSelect = "SELECT mp.id, mp.mission_id, mp.current_value, mp.target_value " +
                           "FROM public.mission_progress mp " +
                           "JOIN public.missions m ON mp.mission_id = m.id " +
                           "WHERE mp.user_id = ? AND mp.metric_key = ? AND m.completed = false";

        String sqlUpdateProgress = "UPDATE public.mission_progress SET current_value = ? WHERE id = ?";
        String sqlUpdateMissionPct = "UPDATE public.missions SET progress = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pst = conn.prepareStatement(sqlSelect);
            pst.setInt(1, userId);
            pst.setString(2, metricKey);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int progressId = rs.getInt("id");
                int missionId = rs.getInt("mission_id");
                long current = rs.getLong("current_value");
                long target = rs.getLong("target_value");

                long newValue = current + amountToAdd;
                if (newValue > target) newValue = target;

                // A. Actualizar valor numérico absoluto
                try (PreparedStatement pstUpd = conn.prepareStatement(sqlUpdateProgress)) {
                    pstUpd.setLong(1, newValue);
                    pstUpd.setInt(2, progressId);
                    pstUpd.executeUpdate();
                }

                // B. Actualizar porcentaje visual (0-100) en tabla missions
                int percent = (int) ((double) newValue / target * 100);
                try (PreparedStatement pstUpdM = conn.prepareStatement(sqlUpdateMissionPct)) {
                    pstUpdM.setInt(1, percent);
                    pstUpdM.setInt(2, missionId);
                    pstUpdM.executeUpdate();
                }

                if (newValue >= target) {
                    completedMissions.add(missionId);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando progreso: " + e.getMessage());
            e.printStackTrace();
        }
        return completedMissions;
    }

    // ==========================================
    // SECCIÓN 3: ACCIONES DE ESTADO
    // ==========================================

    /**
     * Marca una misión como completada al 100% en la base de datos.
     * Útil para admin, debug, o para asegurar consistencia al reclamar.
     */
    public static void completarMision(int missionId) {
        String sql = "UPDATE public.missions SET completed = true, completed_at = CURRENT_TIMESTAMP, progress = 100 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, missionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error completando misión: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Acción del usuario al reclamar recompensa.
     * Reutiliza completarMision para asegurar que el progreso visual también sea 100%.
     */
    public static void reclamarMision(int missionId) {
        completarMision(missionId); 
    }

    // ==========================================
    // SECCIÓN 4: UTILIDADES
    // ==========================================

    /**
     * Recupera una misión individual por ID.
     * Necesario para que el GameService calcule XP/Monedas antes de reclamar.
     */
    public static Mission getMisionById(int id) {
        Mission mision = null;
        String sql = "SELECT id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly, progress, completed FROM public.missions WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String title = rs.getString("title");
                String category = rs.getString("category");
                String difficulty = rs.getString("difficulty");
                int xp = rs.getInt("xp_reward");
                int coin = rs.getInt("coin_reward");
                boolean isDaily = rs.getBoolean("is_daily");
                boolean isWeekly = rs.getBoolean("is_weekly");
                int progressRaw = rs.getInt("progress");
                boolean completed = rs.getBoolean("completed");

                String type = isDaily ? "DIARIA" : (isWeekly ? "SEMANAL" : "CLASE");
                String description = "Objetivo: " + category;
                double progressPct = Math.min(progressRaw / 100.0, 1.0);
                
                mision = new Mission(id, title, description, type, xp, coin, progressPct, completed);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error buscando misión por ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return mision; 
    }

    // ==========================================
    // SECCIÓN 5: INICIALIZACIÓN DE PROGRESO
    // ==========================================

    /**
     * Inicializa las filas de seguimiento en mission_progress.
     * Se debe llamar cuando se carga una misión por primera vez.
     * Crea una fila por cada métrica relevante para la misión.
     * 
     * @param userId ID del usuario
     * @param missionId ID de la misión
     * @param category Categoría de la misión (para determinar métrica)
     */
    public static void inicializarMisionProgress(int userId, int missionId, String category) {
        // Determinar qué métrica(s) usa esta misión basándose en su categoría
        String metricKey = mapearCategoriaAMetrica(category);
        
        // Obtener el target value (1 hora = 3600 segundos por defecto)
        long targetValue = 3600;
        
        // ✅ CORREGIDO: ON CONFLICT usa la constraint UNIQUE correcta
        String sqlInsert = "INSERT INTO public.mission_progress " +
                           "(user_id, mission_id, metric_key, current_value, target_value, progress_percentage) " +
                           "VALUES (?, ?, ?, 0, ?, 0.00) " +
                           "ON CONFLICT (user_id, mission_id, metric_key) DO UPDATE SET " +
                           "current_value = EXCLUDED.current_value, " +
                           "progress_percentage = 0.00";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, missionId);
            pstmt.setString(3, metricKey);
            pstmt.setLong(4, targetValue);
            pstmt.executeUpdate();
            
            System.out.println("✅ mission_progress inicializado: usuario=" + userId + 
                              ", misión=" + missionId + ", métrica=" + metricKey);
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error inicializando mission_progress: " + e.getMessage());
            e.printStackTrace();
            // No es crítico, continuar
        }
    }

    /**
     * Mapea la categoría de misión a una clave de métrica.
     */
    private static String mapearCategoriaAMetrica(String category) {
        if (category == null) return "app_usage_generic";
        
        String lower = category.toLowerCase();
        if (lower.contains("program") || lower.contains("code") || lower.contains("cód")) {
            return "time_coding";
        } else if (lower.contains("product") || lower.contains("produc")) {
            return "time_productivity";
        } else if (lower.contains("brows") || lower.contains("naveg")) {
            return "time_browsing";
        }
        return "app_usage_generic";
    }

    /**
     * Inicializa mission_progress para todas las misiones activas de un usuario.
     * Llamar cuando se hace login o inicia una nueva sesión de juego.
     * 
     * @param userId ID del usuario
     */
    public static void inicializarTodasMisiones(int userId) {
        String sql = "SELECT id, category FROM public.missions WHERE user_id = ? AND completed = false";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                int missionId = rs.getInt("id");
                String category = rs.getString("category");
                inicializarMisionProgress(userId, missionId, category);
                count++;
            }
            
            System.out.println("✅ Se inicializaron " + count + " misiones para usuario " + userId);
            
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando misiones: " + e.getMessage());
            e.printStackTrace();
        }
    }
}