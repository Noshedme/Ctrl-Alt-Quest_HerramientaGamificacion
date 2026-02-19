package com.ctrlaltquest.dao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Mission;

public class MissionsDAO {

    public static List<Mission> getMisionesUsuario(int userId) {
        System.out.println("🔍 [MissionsDAO] Buscando misiones globales para userId=" + userId);
        List<Mission> lista = new ArrayList<>();
        
        String sql = "SELECT m.id, m.title, m.category, m.difficulty, m.xp_reward, m.coin_reward, " +
                     "m.is_daily, m.is_weekly, " +
                     "COALESCE(mp.progress_percentage, 0) as user_progress " +
                     "FROM public.missions m " +
                     "LEFT JOIN public.mission_progress mp ON m.id = mp.mission_id AND mp.user_id = ? " +
                     "WHERE m.user_id IS NULL " + 
                     "ORDER BY user_progress ASC, m.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
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
                
                double progressPct = rs.getDouble("user_progress") / 100.0;
                boolean completed = (progressPct >= 1.0);

                String type = isDaily ? "DIARIA" : (isWeekly ? "SEMANAL" : "CLASE");
                String description = (category != null ? category : "General") + " [" + (difficulty != null ? difficulty : "Normal") + "]";

                lista.add(new Mission(id, title, description, type, xp, coin, progressPct, completed));
                count++;
            }
            System.out.println("✅ [MissionsDAO] Total misiones cargadas: " + count);
            
        } catch (SQLException e) {
            System.err.println("❌ [MissionsDAO] Error SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public static List<Integer> actualizarProgreso(int userId, String metricKey, int amountToAdd) {
        List<Integer> completedMissions = new ArrayList<>();
        
        inicializarMisionesGlobalesParaUsuario(userId);

        String sqlSelect = "SELECT id, mission_id, current_value, target_value FROM public.mission_progress " +
                           "WHERE user_id = ? AND metric_key = ? AND progress_percentage < 100";

        String sqlUpdate = "UPDATE public.mission_progress SET current_value = ?, progress_percentage = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pst = conn.prepareStatement(sqlSelect);
            pst.setInt(1, userId);
            pst.setString(2, metricKey);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int rowId = rs.getInt("id");
                int missionId = rs.getInt("mission_id");
                long current = rs.getLong("current_value");
                long target = rs.getLong("target_value");

                long newValue = current + amountToAdd;
                if (newValue > target) newValue = target;

                double newPct = (target > 0) ? ((double) newValue / target * 100.0) : 0;
                if (newPct > 100) newPct = 100;

                try (PreparedStatement pstUpd = conn.prepareStatement(sqlUpdate)) {
                    pstUpd.setLong(1, newValue);
                    pstUpd.setDouble(2, newPct);
                    pstUpd.setInt(3, rowId);
                    pstUpd.executeUpdate();
                }

                if (newPct >= 100) {
                    completedMissions.add(missionId);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando progreso: " + e.getMessage());
            e.printStackTrace();
        }
        return completedMissions;
    }

    public static void reclamarMision(int userId, int missionId) {
        Mission m = getMisionById(missionId);
        String metric = (m != null) ? mapearCategoriaAMetrica(m.getDescription()) : "manual_check";
        
        String sql = "INSERT INTO public.mission_progress (user_id, mission_id, metric_key, current_value, target_value, progress_percentage) " +
                     "VALUES (?, ?, ?, 1, 1, 100) " +
                     "ON CONFLICT (user_id, mission_id, metric_key) " +
                     "DO UPDATE SET progress_percentage = 100, current_value = mission_progress.target_value";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, missionId);
            pstmt.setString(3, metric);
            pstmt.executeUpdate();
            
            System.out.println("✅ Misión " + missionId + " reclamada para usuario " + userId);
            
        } catch (SQLException e) {
            System.err.println("❌ Error reclamando misión: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void reclamarMision(int missionId) {
        System.err.println("⚠️ ERROR CRÍTICO: Se llamó a reclamarMision sin userId.");
    }

    public static boolean exportMissionHistoryToCSV(int userId, File file) {
        // Usa fecha actual porque 'updated_at' no existe en tu esquema actual
        String sql = "SELECT m.title, m.difficulty, m.xp_reward, m.coin_reward " +
                     "FROM public.missions m " +
                     "INNER JOIN public.mission_progress mp ON m.id = mp.mission_id " +
                     "WHERE mp.user_id = ? AND mp.progress_percentage >= 100 " +
                     "ORDER BY m.id DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            writer.write("Mision,Dificultad,XP Ganada,Oro Ganado,Fecha Reporte\n");
            
            int count = 0;
            String fechaHoy = LocalDate.now().toString();

            while (rs.next()) {
                String linea = String.format("%s,%s,%d,%d,%s\n", 
                    rs.getString("title").replace(",", " "), 
                    rs.getString("difficulty") != null ? rs.getString("difficulty") : "Normal",
                    rs.getInt("xp_reward"),
                    rs.getInt("coin_reward"),
                    fechaHoy 
                );
                writer.write(linea);
                count++;
            }
            
            System.out.println("✅ Exportadas " + count + " misiones completadas al archivo CSV");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error exportando CSV: " + e.getMessage());
            return false;
        }
    }

    public static Mission getMisionById(int id) {
        Mission mision = null;
        String sql = "SELECT id, title, category, difficulty, xp_reward, coin_reward, is_daily, is_weekly FROM public.missions WHERE id = ?";
        
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
                
                String type = isDaily ? "DIARIA" : (isWeekly ? "SEMANAL" : "CLASE");
                String description = (category != null) ? category : "General";
                
                mision = new Mission(id, title, description, type, xp, coin, 0.0, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mision; 
    }

    public static void inicializarMisionesGlobalesParaUsuario(int userId) {
        String sql = "INSERT INTO public.mission_progress (user_id, mission_id, metric_key, current_value, target_value, progress_percentage) " +
                     "SELECT ?, m.id, " +
                     "CASE " + 
                     "  WHEN m.category ILIKE '%program%' THEN 'time_coding' " +
                     "  WHEN m.category ILIKE '%product%' THEN 'time_productivity' " +
                     "  ELSE 'app_usage_generic' END, " +
                     "0, 3600, 0 " +
                     "FROM public.missions m " +
                     "WHERE m.user_id IS NULL " +
                     "AND NOT EXISTS (SELECT 1 FROM public.mission_progress mp WHERE mp.mission_id = m.id AND mp.user_id = ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            int insertados = pstmt.executeUpdate();
            
            if (insertados > 0) {
                System.out.println("✅ Inicialización: Se asignaron " + insertados + " misiones globales al usuario " + userId);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando misiones globales: " + e.getMessage());
        }
    }

    private static String mapearCategoriaAMetrica(String category) {
        if (category == null) return "app_usage_generic";
        String lower = category.toLowerCase();
        if (lower.contains("program") || lower.contains("code")) return "time_coding";
        if (lower.contains("product")) return "time_productivity";
        return "app_usage_generic";
    }
    
    public static void inicializarTodasMisiones(int userId) {
        inicializarMisionesGlobalesParaUsuario(userId);
    }
}