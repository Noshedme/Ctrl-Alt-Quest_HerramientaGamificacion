package com.ctrlaltquest.services;

import com.ctrlaltquest.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MissionProgressService - Servicio que actualiza misiones automáticamente basado en actividad.
 * Se integra con el monitoreo de actividad para progresar misiones en tiempo real.
 */
public class MissionProgressService {
    
    private static MissionProgressService instance;
    private final RewardsService rewardsService = RewardsService.getInstance();
    private final EventsService eventsService = EventsService.getInstance();
    
    private MissionProgressService() {}
    
    public static synchronized MissionProgressService getInstance() {
        if (instance == null) {
            instance = new MissionProgressService();
        }
        return instance;
    }

    /**
     * 🎯 MÉTODO PÚBLICO - Captura actividad MANUAL cuando el usuario presiona el botón en la UI.
     * Este método es llamado desde el botón "Capturar Actividad" en la sección de Actividad.
     * 
     * @param userId ID del usuario
     * @param appName Nombre de la aplicación (ej: "VSCode", "Chrome")
     * @param isProductive ¿Es actividad productiva?
     */
    public void captureActivityManual(int userId, String appName, boolean isProductive) {
        System.out.println("🎯 [MissionProgressService] Captura MANUAL de actividad");
        System.out.println("   └─ Usuario: " + userId);
        System.out.println("   └─ App: " + appName);
        System.out.println("   └─ Productiva: " + isProductive);
        
        processActivityEvent(userId, appName, isProductive);
    }

    /**
     * Procesa un evento de actividad y actualiza misiones relacionadas.
     * Se llama cada 1 segundo desde ActivityMonitorService.
     * 
     * @param userId ID del usuario
     * @param appName Nombre de la aplicación activa (ej: "VSCode", "Chrome")
     * @param isProductive ¿Es actividad productiva?
     */
    public void processActivityEvent(int userId, String appName, boolean isProductive) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // 1. Determinar la categoría de la app
            String category = categorizeApp(appName, isProductive);
            
            // 2. Actualizar misiones de tiempo (segundos en aplicación)
            updateTimeBasedMissions(userId, category, conn);
            
            // 3. Actualizar misiones de contadores (apps usadas, etc.)
            updateCounterBasedMissions(userId, appName, category, conn);
            
            // 4. Otorgar XP por actividad productiva
            if (isProductive) {
                rewardsService.awardXPForActivity(userId, true);
            }
            
            // 5. Verificar y completar logros
            rewardsService.checkAndAwardAchievements(userId);
            
            // 6. ✨ NUEVO: Verificar y generar eventos contextuales
            eventsService.checkAndGenerateEvent(userId, category);
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error procesando evento de actividad: " + e.getMessage());
        }
    }

    /**
     * Categoriza la aplicación según el nombre.
     * 
     * @param appName Nombre de la aplicación
     * @param isProductive ¿Es productiva?
     * @return Categoría (ej: "CODING", "BROWSING", "PRODUCTIVITY")
     */
    private String categorizeApp(String appName, boolean isProductive) {
        if (appName == null) return "UNKNOWN";
        
        String lower = appName.toLowerCase();
        
        if (lower.contains("vscode") || lower.contains("visual studio") || 
            lower.contains("intellij") || lower.contains("eclipse") || 
            lower.contains("netbeans") || lower.contains("sublime")) {
            return "CODING";
        }
        
        if (lower.contains("chrome") || lower.contains("firefox") || 
            lower.contains("edge") || lower.contains("safari")) {
            return "BROWSING";
        }
        
        if (lower.contains("word") || lower.contains("excel") || 
            lower.contains("powerpoint") || lower.contains("docs") ||
            lower.contains("sheets")) {
            return "OFFICE";
        }
        
        if (lower.contains("slack") || lower.contains("teams") || 
            lower.contains("discord") || lower.contains("telegram")) {
            return "COMMUNICATION";
        }
        
        return isProductive ? "PRODUCTIVITY" : "ENTERTAINMENT";
    }

    /**
     * Actualiza misiones basadas en tiempo de actividad.
     * Incrementa 1 segundo por cada tick de actividad.
     * 
     * @param userId ID del usuario
     * @param category Categoría de la aplicación
     * @param conn Conexión a BD
     */
    private void updateTimeBasedMissions(int userId, String category, Connection conn) throws SQLException {
        // Buscar misiones de tiempo para esta categoría
        String sql = "SELECT mp.id, mp.mission_id, mp.current_value, mp.target_value, mp.progress_percentage " +
                     "FROM public.mission_progress mp " +
                     "JOIN public.missions m ON mp.mission_id = m.id " +
                     "WHERE mp.user_id = ? " +
                     "  AND m.completed = false " +
                     "  AND mp.metric_key LIKE ? " +
                     "  AND mp.current_value < mp.target_value";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, "time_" + category.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int progressId = rs.getInt("id");
                int missionId = rs.getInt("mission_id");
                long currentValue = rs.getLong("current_value");
                long targetValue = rs.getLong("target_value");
                double currentProgress = rs.getDouble("progress_percentage");
                
                // Incrementar 1 segundo
                long newValue = Math.min(currentValue + 1, targetValue);
                double newProgress = (double) newValue / targetValue * 100;
                
                // Actualizar mission_progress
                String updateSql = "UPDATE public.mission_progress " +
                                  "SET current_value = ?, progress_percentage = ? " +
                                  "WHERE id = ?";
                
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, newValue);
                    updateStmt.setDouble(2, newProgress);
                    updateStmt.setInt(3, progressId);
                    updateStmt.executeUpdate();
                }
                
                // Si se completó la misión
                if (newValue >= targetValue && currentValue < targetValue) {
                    completeMission(userId, missionId, conn);
                }
            }
        }
    }

    /**
     * Actualiza misiones basadas en contadores (aplicaciones únicas usadas, etc.).
     * 
     * @param userId ID del usuario
     * @param appName Nombre de la aplicación
     * @param category Categoría de la aplicación
     * @param conn Conexión a BD
     */
    private void updateCounterBasedMissions(int userId, String appName, String category, Connection conn) throws SQLException {
        // Ejemplos: "apps_used", "categories_used", "productive_apps_used"
        
        // 1. Contar apps únicas usadas en esta sesión
        String countAppsSql = "SELECT COUNT(DISTINCT app_id) as count " +
                             "FROM public.app_usage_logs aul " +
                             "JOIN public.activity_sessions a ON aul.session_id = a.id " +
                             "WHERE a.user_id = ? AND DATE(aul.start_time) = CURRENT_DATE";
        
        try (PreparedStatement pstmt = conn.prepareStatement(countAppsSql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int uniqueApps = rs.getInt("count");
                updateMissionCounter(userId, "apps_used", uniqueApps, conn);
            }
        }
        
        // 2. Contar categorías únicas usadas
        String countCatSql = "SELECT COUNT(DISTINCT LOWER(a.category)) as count " +
                            "FROM public.app_usage_logs aul " +
                            "JOIN public.apps a ON aul.app_id = a.id " +
                            "JOIN public.activity_sessions act ON aul.session_id = act.id " +
                            "WHERE act.user_id = ? AND DATE(aul.start_time) = CURRENT_DATE";
        
        try (PreparedStatement pstmt = conn.prepareStatement(countCatSql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int uniqueCategories = rs.getInt("count");
                updateMissionCounter(userId, "categories_used", uniqueCategories, conn);
            }
        }
    }

    /**
     * Actualiza el contador de una misión.
     * 
     * @param userId ID del usuario
     * @param metricKey Clave de la métrica
     * @param value Nuevo valor del contador
     * @param conn Conexión a BD
     */
    private void updateMissionCounter(int userId, String metricKey, int value, Connection conn) throws SQLException {
        String sql = "SELECT mp.id, mp.mission_id, mp.target_value, mp.progress_percentage " +
                     "FROM public.mission_progress mp " +
                     "JOIN public.missions m ON mp.mission_id = m.id " +
                     "WHERE mp.user_id = ? " +
                     "  AND m.completed = false " +
                     "  AND mp.metric_key = ? " +
                     "  AND mp.current_value < ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, metricKey);
            pstmt.setInt(3, value);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int progressId = rs.getInt("id");
                int missionId = rs.getInt("mission_id");
                long targetValue = rs.getLong("target_value");
                
                double newProgress = (double) value / targetValue * 100;
                
                String updateSql = "UPDATE public.mission_progress " +
                                  "SET current_value = ?, progress_percentage = ? " +
                                  "WHERE id = ?";
                
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, value);
                    updateStmt.setDouble(2, newProgress);
                    updateStmt.setInt(3, progressId);
                    updateStmt.executeUpdate();
                }
                
                // Si se completó
                if (value >= targetValue) {
                    completeMission(userId, missionId, conn);
                }
            }
        }
    }

    /**
     * Completa una misión: actualiza BD, otorga recompensas y verifica logros.
     * 
     * @param userId ID del usuario
     * @param missionId ID de la misión
     * @param conn Conexión a BD
     */
    private void completeMission(int userId, int missionId, Connection conn) throws SQLException {
        // 1. Marcar misión como completada
        String completeSql = "UPDATE public.missions " +
                            "SET completed = true, completed_at = CURRENT_TIMESTAMP, progress = 100 " +
                            "WHERE id = ? AND user_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(completeSql)) {
            pstmt.setInt(1, missionId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
        
        // 2. Obtener recompensas de la misión
        String rewardSql = "SELECT xp_reward, coin_reward, title FROM public.missions WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(rewardSql)) {
            pstmt.setInt(1, missionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int xpReward = rs.getInt("xp_reward");
                int coinReward = rs.getInt("coin_reward");
                String title = rs.getString("title");
                
                // 3. Otorgar recompensas
                if (xpReward > 0) {
                    String xpSql = "UPDATE public.users SET current_xp = current_xp + ?, total_xp = total_xp + ? WHERE id = ?";
                    try (PreparedStatement xpStmt = conn.prepareStatement(xpSql)) {
                        xpStmt.setInt(1, xpReward);
                        xpStmt.setInt(2, xpReward);
                        xpStmt.setInt(3, userId);
                        xpStmt.executeUpdate();
                    }
                }
                
                if (coinReward > 0) {
                    rewardsService.awardCoinsForMission(userId, missionId, coinReward);
                }
                
                System.out.println("✅ [MissionProgressService] Misión Completada: " + title + 
                                 " | +XP: " + xpReward + " | +Monedas: " + coinReward);
            }
        }
        
        // 4. Verificar logros (ej: "Completar 5 misiones")
        rewardsService.checkAndAwardAchievements(userId);
    }
}
