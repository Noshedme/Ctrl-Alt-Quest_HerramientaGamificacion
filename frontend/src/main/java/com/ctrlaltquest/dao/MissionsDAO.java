package com.ctrlaltquest.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Mission;

public class MissionsDAO {

    // ==========================================
    // SECCIÓN 1: MÉTODOS PARA LA INTERFAZ (UI)
    // ==========================================

    public static List<Mission> getMisionesUsuario(int userId) {
        System.out.println("🔍 [MissionsDAO] Buscando misiones globales para userId=" + userId);
        List<Mission> lista = new ArrayList<>();
        
        // MAGIA SQL: Traemos la definición (Missions) + El progreso personal (Mission_Progress)
        // Usamos LEFT JOIN: "Trae todas las misiones globales, y si este usuario tiene progreso, pégalo. Si no, pon null/0".
        String sql = "SELECT m.id, m.title, m.category, m.difficulty, m.xp_reward, m.coin_reward, " +
                     "m.is_daily, m.is_weekly, " +
                     "COALESCE(mp.progress_percentage, 0) as user_progress " +
                     "FROM public.missions m " +
                     "LEFT JOIN public.mission_progress mp ON m.id = mp.mission_id AND mp.user_id = ? " +
                     "WHERE m.user_id IS NULL " + // Solo misiones globales
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
                
                // El progreso viene de la tabla unida (mission_progress)
                // COALESCE en SQL ya se encargó de que si es null sea 0
                double progressPct = rs.getDouble("user_progress") / 100.0;
                
                // Calculamos 'completed' basado en el progreso, no en la tabla missions
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

    // ==========================================
    // SECCIÓN 2: LÓGICA DEL MOTOR DE JUEGO
    // ==========================================

    /**
     * Actualiza el progreso en la tabla mission_progress y devuelve IDs de misiones completadas.
     */
    public static List<Integer> actualizarProgreso(int userId, String metricKey, int amountToAdd) {
        List<Integer> completedMissions = new ArrayList<>();
        
        // 1. Asegurarnos que existan filas en mission_progress para este usuario
        // (Esto es una salvaguarda por si es un usuario nuevo y no se inicializó)
        inicializarMisionesGlobalesParaUsuario(userId);

        // 2. Buscar en mission_progress las filas activas que coincidan con la métrica
        // NOTA: Para que esto funcione, al inicializar debemos guardar el metric_key correcto
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

    // ==========================================
    // SECCIÓN 3: ACCIONES DE ESTADO
    // ==========================================

    /**
     * Marca la misión como completada al 100% PARA UN USUARIO ESPECÍFICO.
     * Crea la fila en mission_progress si no existe.
     */
    public static void reclamarMision(int userId, int missionId) {
        // Obtenemos info básica para saber la métrica (si no existe)
        Mission m = getMisionById(missionId);
        String metric = (m != null) ? mapearCategoriaAMetrica(m.getDescription()) : "manual_check";
        
        // UPSERT: Insertar como completada O actualizar a completada
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
    
    // Método de compatibilidad (¡EVITAR USAR! Solo para que no rompa código viejo)
    public static void reclamarMision(int missionId) {
        System.err.println("⚠️ ERROR CRÍTICO: Se llamó a reclamarMision sin userId. La acción fallará para misiones globales.");
    }

    // ==========================================
    // SECCIÓN 4: UTILIDADES & INICIALIZACIÓN
    // ==========================================

    public static Mission getMisionById(int id) {
        Mission mision = null;
        // Solo necesitamos los datos estáticos de la definición
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
                
                // Al obtener ID suelto, no sabemos el progreso del usuario, devolvemos 0
                mision = new Mission(id, title, description, type, xp, coin, 0.0, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mision; 
    }

    /**
     * Inicializa las filas en mission_progress para TODAS las misiones globales que el usuario
     * aún no tenga registradas.
     * ES VITAL llamar a esto en el login (HomeController).
     */
    public static void inicializarMisionesGlobalesParaUsuario(int userId) {
        // Insert masivo inteligente:
        // Selecciona todas las misiones globales (user_id NULL)
        // Y las inserta en mission_progress SOLO SI no existen ya para ese usuario
        
        // Nota: Asumimos target_value 3600 (1 hora) por defecto.
        String sql = "INSERT INTO public.mission_progress (user_id, mission_id, metric_key, current_value, target_value, progress_percentage) " +
                     "SELECT ?, m.id, " +
                     "CASE " + // Lógica básica de mapeo de métricas en SQL
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
            pstmt.setInt(2, userId); // Para el NOT EXISTS
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
    
    // Métodos legacy para compatibilidad (puedes dejarlos vacíos o borrarlos si nadie los usa)
    public static void inicializarTodasMisiones(int userId) {
        inicializarMisionesGlobalesParaUsuario(userId);
    }
}